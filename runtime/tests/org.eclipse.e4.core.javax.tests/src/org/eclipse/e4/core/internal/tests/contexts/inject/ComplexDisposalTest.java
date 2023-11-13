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

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;

/**
 * See bug 296337: duplicate disposal of an object
 */
public class ComplexDisposalTest {

	public static class MyTest {
		private int count = 0;

		@Inject
		@Named("aString")
		String string;

		public int getCount() {
			return count;
		}

		@PreDestroy
		void preDestroy() {
			count++;
		}
	}

	public static class TestFunction extends ContextFunction {
		@Override
		public Object compute(IEclipseContext context, String contextKey) {
			return ContextInjectionFactory.make(MyTest.class, context);
		}
	}

	@Test
	public void testU() {
		IEclipseContext parentContext = EclipseContextFactory.create();
		parentContext.set("aString", "");
		parentContext.set(MyTest.class.getName(), new TestFunction());
		IEclipseContext context = parentContext.createChild();

		MyTest test = context.get(MyTest.class);

		assertEquals(0, test.getCount());
		context.dispose();
		assertEquals("Context disposed, @PreDestroy should've been called", 1, test.getCount());
		parentContext.dispose();
		assertEquals("Parent context disposed, @PreDestroy should not have been called again", 1, test.getCount());
	}

	@Test
	public void testV() {
		IEclipseContext parentContext = EclipseContextFactory.create();
		parentContext.set("aString", "");
		IEclipseContext context = parentContext.createChild();

		MyTest test = ContextInjectionFactory.make(MyTest.class, context);

		assertEquals(0, test.getCount());
		context.dispose();
		assertEquals("Context disposed, @PreDestroy should've been called", 1, test.getCount());
		parentContext.dispose();
		assertEquals("Parent context disposed, @PreDestroy should not have been called again", 1, test.getCount());
	}

	@Test
	public void testW() {
		IEclipseContext parentContext = EclipseContextFactory.create();
		parentContext.set("aString", "");
		IEclipseContext context = parentContext.createChild();

		MyTest test = new MyTest();
		ContextInjectionFactory.inject(test, context);

		assertEquals(0, test.getCount());
		context.dispose();
		assertEquals("Context disposed, @PreDestroy should've been called", 1, test.getCount());
		parentContext.dispose();
		assertEquals("Parent context disposed, @PreDestroy should not have been called again", 1, test.getCount());
	}
}
