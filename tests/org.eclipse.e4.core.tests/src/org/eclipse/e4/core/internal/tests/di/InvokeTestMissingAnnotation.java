/*******************************************************************************
 * Copyright (c) 2014, 2016 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/
package org.eclipse.e4.core.internal.tests.di;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.junit.Test;

/**
 * Tests that that no method is called, it the @Execute annotation is not
 * present and that an exception is thrown from the DI framework
 */
public class InvokeTestMissingAnnotation {

	/**
	 * Class to invoke for the test
	 */
	static class TestSuperclass {
		public int saveCount = 0;

		// @Execute annotation missing on purpose
		void execute() {
			saveCount++;
		}
	}

	/**
	 * Checks that no methods is called and that an execution is thrown
	 */
	@Test(expected = InjectionException.class)
	public void testCallMethodsWithMissingAnnotation() {
		TestSuperclass editor = new TestSuperclass();
		ContextInjectionFactory.invoke(editor, Execute.class, EclipseContextFactory.create());
	}

	/**
	 * Checks that no methods is called and that no execution is thrown if a
	 * default is provide
	 */
	@Test
	public void testCallMethodsWithMissingAnnotationNoExecution() {
		TestSuperclass editor = new TestSuperclass();
		ContextInjectionFactory.invoke(editor, Execute.class,
				EclipseContextFactory.create(), this);

	}

}
