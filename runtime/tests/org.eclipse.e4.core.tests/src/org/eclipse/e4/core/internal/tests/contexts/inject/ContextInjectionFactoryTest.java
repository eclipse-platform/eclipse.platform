/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.junit.Before;
import org.junit.Test;

public class ContextInjectionFactoryTest {

	static class TestObject {

		private int executed = 0;

		private int executedWithParams = 0;

		@Execute
		public void execute() {
			executed++;
		}

		@CanExecute
		public void executeWithParams(String string) {
			executedWithParams++;
		}

		public int getExecuted() {
			return executed;
		}

		public int getExecutedWithParams() {
			return executedWithParams;
		}

	}

	static class TestConstructorObjectBasic {
		public boolean defaultConstructorCalled = false;

		public TestConstructorObjectBasic() {
			defaultConstructorCalled = true;
		}
	}

	private TestObject testObject;
	private IEclipseContext context;

	@Before
	public void setUp() throws Exception {
		testObject = new TestObject();
		context = EclipseContextFactory.create();
	}

	@Test
	public void testInvoke() throws Exception {
		ContextInjectionFactory.invoke(testObject, Execute.class, context, null);

		assertEquals(1, testObject.getExecuted());
		assertEquals(0, testObject.getExecutedWithParams());
	}

	@Test
	public void testInvokeWithParameters() throws Exception {
		context.set(String.class.getName(), "");

		ContextInjectionFactory.invoke(testObject, CanExecute.class, context, null);

		assertEquals(0, testObject.getExecuted());
		assertEquals(1, testObject.getExecutedWithParams());
	}

	/**
	 * If no other constructors are available, the default constructor should be used
	 */
	@Test
	public void testConstructorInjectionBasic() {
		IEclipseContext context = EclipseContextFactory.create();
		// add an extra argument for the inner class constructors
		context.set(ContextInjectionFactoryTest.class.getName(), this);

		Object basicResult = ContextInjectionFactory
				.make(TestConstructorObjectBasic.class, context);
		assertNotNull(basicResult);
		assertTrue(basicResult instanceof TestConstructorObjectBasic);
		assertTrue(((TestConstructorObjectBasic) basicResult).defaultConstructorCalled);
	}
}
