/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Mücke - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.junit.Before;
import org.junit.Test;

public class DependenciesLeakTest {

	final static String LEGACY_H_ID = "legacy::handler::"; //$NON-NLS-1$

	static class HandlerSelectionFunction extends ContextFunction {
		private String commandId;
		public HandlerSelectionFunction(String commandId) {
			this.commandId = commandId;
		}
		@Override
		public Object compute(IEclipseContext context, String contextKey) {
			return context.get(LEGACY_H_ID + commandId);
		}
		@Override
		public String toString() {
			return "HandlerSelectionFunction [commandId=" + commandId + "]";
		}
	}

	private IEclipseContext windowContext;
	private IEclipseContext perspectiveContext;
	private IEclipseContext partContext;

	@Before
	public void setUp() throws Exception {
		windowContext = EclipseContextFactory.create("Window");
		perspectiveContext = windowContext.createChild("Perspective");
		partContext = perspectiveContext.createChild("Part");
	}

	@Test
	public void testBug() {
		// register a handler
		Object handler = "<foo.bar.handler>";
		windowContext.set("legacy::handler::foo.bar", handler); // fake activate legacy handler
		windowContext.set("foo.bar", new HandlerSelectionFunction("foo.bar"));

		// there may be no listeners initially
		assertNoListeners(windowContext);
		assertNoListeners(perspectiveContext);
		assertNoListeners(partContext);

		// cause a ValueComputation to be created
		Object object = partContext.get("foo.bar");
		assertEquals(object, handler);

		// now invalidate the name; this should notify the part context
		windowContext.set("foo.bar", null);
		//windowContext.remove("foo.bar");

		// all ValueComputation listeners must have been removed
		assertNoListeners(windowContext);
		assertNoListeners(perspectiveContext);
		assertNoListeners(partContext);
	}

	@Test
	public void testInvalidateDirectly() {
		windowContext.set("x", 42);
		windowContext.set("y", 11);
		windowContext.set("some.handler", new AddContextFunction());
		assertNoListeners(windowContext);
		assertNoListeners(perspectiveContext);
		assertNoListeners(partContext);

		Object object = partContext.get("some.handler");
		assertEquals(object, 53);

		windowContext.set("some.handler", null); // invalidate
		assertNoListeners(windowContext);
		assertNoListeners(perspectiveContext);
		assertNoListeners(partContext);
	}

	private void assertNoListeners(IEclipseContext context) {
		EclipseContext c = (EclipseContext) context;
		try {
			assertTrue(c.getListeners().isEmpty());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
