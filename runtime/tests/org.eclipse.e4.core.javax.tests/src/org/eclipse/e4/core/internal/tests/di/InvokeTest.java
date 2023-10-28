/*******************************************************************************
 * Copyright (c) 2010, 2015  IBM Corporation and others.
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
package org.eclipse.e4.core.internal.tests.di;

import static org.junit.Assert.assertEquals;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.di.annotations.Execute;
import org.junit.Test;

public class InvokeTest {

	/**
	 * Superclass
	 */
	static class TestSuperclass {
		public int saveCount = 0;
		@Execute
		void something() {
			saveCount++;
		}
	}

	/**
	 * Subclass
	 */
	static class TestSubclass extends TestSuperclass {
	}

	/**
	 * Checks that superclass methods are called
	 */
	@Test
	public void testSuperclassMethods() {
		TestSubclass editor = new TestSubclass();
		ContextInjectionFactory.invoke(editor, Execute.class, EclipseContextFactory
				.create());
		assertEquals(1, editor.saveCount);
	}
}
