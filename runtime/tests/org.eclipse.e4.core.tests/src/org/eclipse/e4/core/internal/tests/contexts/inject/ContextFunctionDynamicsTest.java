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
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.junit.Test;

public class ContextFunctionDynamicsTest {

	private static final String SELECTION = "selection"; //$NON-NLS-1$

	static class InjectTarget {
		Object input;

		@Inject
		@Optional
		void setInput(@Named(SELECTION) Object input) {
			this.input = input;
		}
	}

	/**
	 * Changing context function should update injected values
	 */
	@Test
	public void testChangeICF() {
		IEclipseContext context1 = EclipseContextFactory.create("context1");
		IEclipseContext context2 = context1.createChild("context2");

		context1.set(SELECTION, new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return "func1";
			}
		});

		InjectTarget target = new InjectTarget();
		ContextInjectionFactory.inject(target, context2);

		assertEquals("func1", target.input);

		context1.set(SELECTION, new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return "func2";
			}
		});

		assertEquals("func2", target.input);
	}

	/**
	 * Overriding context function with a regular value on a child node
	 */
	@Test
	public void testOverrideICF() {
		IEclipseContext context1 = EclipseContextFactory.create("context1");
		IEclipseContext context2 = context1.createChild("context2");
		IEclipseContext context3 = context2.createChild("context3");

		context1.set(SELECTION, new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return "func1";
			}
		});

		InjectTarget target = new InjectTarget();
		ContextInjectionFactory.inject(target, context3);

		assertEquals("func1", target.input);

		Object o = new Object();
		context2.set(SELECTION, o);

		assertEquals(o, target.input);
	}

	/**
	 * Tests updates in a chain of 4 contexts
	 */
	@Test
	public void testLongChain() {
		IEclipseContext context1 = EclipseContextFactory.create("context1");
		IEclipseContext context2 = context1.createChild("context2");
		IEclipseContext context3 = context2.createChild("context3");
		IEclipseContext context4 = context3.createChild("context4");

		// ICF set on top context
		context1.set(SELECTION, new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return "func1";
			}
		});

		InjectTarget target = new InjectTarget();
		ContextInjectionFactory.inject(target, context4);

		assertEquals("func1", target.input);

		// Override ICF set on the 2nd context
		context2.set(SELECTION, new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return "func2";
			}
		});

		assertEquals("func2", target.input);

		// Override removed
		context2.remove(SELECTION);
		assertEquals("func1", target.input);

		// Override simple value set on 3rd context
		context3.set(SELECTION, "abc");
		assertEquals("abc", target.input);

		// Simple value override removed from 3rd context
		context3.remove(SELECTION);
		assertEquals("func1", target.input);
	}



	@Test
	public void testBug315109() throws Exception {
		IEclipseContext appContext = EclipseContextFactory.create();
		IEclipseContext windowContext = appContext.createChild();
		IEclipseContext partContext = windowContext.createChild();

		partContext.activateBranch();

		appContext.set(SELECTION, new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				IEclipseContext parent = context.getParent();
				while (parent != null) {
					context = parent;
					parent = context.getParent();
				}
				return context.getActiveLeaf().get("out.selection");
			}
		});

		InjectTarget target = new InjectTarget();
		ContextInjectionFactory.inject(target, partContext);

		assertNull("No selection has been set, should be null", target.input); //$NON-NLS-1$

		Object o = new Object();
		windowContext.set(SELECTION, o);

		assertEquals(
				"A selection was set into the window, should have been injected into the part", //$NON-NLS-1$
				o, target.input);
	}
}
