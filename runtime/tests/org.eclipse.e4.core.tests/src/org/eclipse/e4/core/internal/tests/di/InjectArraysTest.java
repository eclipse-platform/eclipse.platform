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
package org.eclipse.e4.core.internal.tests.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Checks injection of arrays
 */
public class InjectArraysTest {

	static class TestClass {
		@Inject @Named("test_array_String")
		public String[] stringArray;

		@Inject
		public Integer[] integerArray;


		public int[] intArray;
		public char[] charAray;

		@Inject
		public void set(@Named("test_array_int") int[] intArray, @Named("test_array_char") char[] charAray) {
			this.intArray = intArray;
			this.charAray = charAray;
		}
	}

	@Test
	public void testArrayInjection() {
		String[] arrayString = new String[] { "abc", "xyz", "ttt" };
		Integer[] arrayInteger = new Integer[] { 5, 6, 7 };
		int[] arrayInt = new int[] { 1, 2, 3 };
		char[] arrayChar = new char[] { 'a', 'b', 'c' };

		IEclipseContext context = EclipseContextFactory.create();
		context.set("test_array_String", arrayString);
		context.set(Integer[].class, arrayInteger);
		context.set("test_array_int", arrayInt);
		context.set("test_array_char", arrayChar);

		TestClass testClass = ContextInjectionFactory.make(TestClass.class, context);
		checkArraysEqual(arrayString, testClass.stringArray);
		checkArraysEqual(arrayInteger, testClass.integerArray);
		checkArraysEqual(arrayInt, testClass.intArray);
		checkArraysEqual(arrayChar, testClass.charAray);
	}

	private void checkArraysEqual(Object array1, Object array2) {
		assertNotNull(array2);
		assertEquals(array1, array2);
	}
}
