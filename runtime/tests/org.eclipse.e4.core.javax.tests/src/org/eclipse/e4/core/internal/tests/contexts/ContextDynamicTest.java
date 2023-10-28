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
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.internal.tests.contexts.inject.ObjectBasic;
import org.junit.Test;

/**
 * Tests for the basic context functionality
 */
public class ContextDynamicTest {


	@Test
	public void testReplaceFunctionWithStaticValue() {
		IEclipseContext parent = EclipseContextFactory.create();
		IEclipseContext context = parent.createChild();
		assertNull(context.getLocal("bar"));
		context.set("bar", "baz1");
		context.set("bar", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return "baz1";
			}
		});
		parent.set("bar", "baz2");
		assertEquals("baz1", context.get("bar"));
		context.set("bar", "baz3");
		assertEquals("baz3", context.get("bar"));
	}

	/**
	 * Tests objects being added and removed from the context
	 */
	@Test
	public synchronized void testAddRemove() {
		Integer testInt = Integer.valueOf(123);
		String testString = "abc";
		Double testDouble = Double.valueOf(1.23);
		Float testFloat = Float.valueOf(12.3f);
		Character testChar = Character.valueOf('v');

		// create original context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class.getName(), testInt);
		context.set(String.class.getName(), testString);
		context.set(Double.class.getName(), testDouble);
		context.set(Float.class.getName(), testFloat);
		context.set(Character.class.getName(), testChar);

		ObjectBasic userObject = new ObjectBasic();
		ContextInjectionFactory.inject(userObject, context);

		// check basic injection
		assertEquals(testString, userObject.injectedString);
		assertEquals(testInt, userObject.getInt());
		assertEquals(context, userObject.context);
		assertEquals(1, userObject.setMethodCalled);
		assertEquals(1, userObject.setMethodCalled2);
		assertEquals(testDouble, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertEquals(testChar, userObject.c);

		// change value
		Double testDouble2 = Double.valueOf(3.45);
		Integer testInt2 = Integer.valueOf(123);
		context.set(Double.class.getName(), testDouble2);
		context.set(Integer.class.getName(), testInt2);

		// and check
		assertEquals(testString, userObject.injectedString);
		assertEquals(testInt2, userObject.getInt());
		assertEquals(context, userObject.context);
		assertEquals(2, userObject.setMethodCalled);
		assertEquals(1, userObject.setMethodCalled2);
		assertEquals(testDouble2, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertEquals(testChar, userObject.c);

		// remove element
		context.remove(String.class.getName());
		context.remove(Character.class.getName());

		// and check
		assertNull(userObject.injectedString);
		assertEquals(testInt2, userObject.getInt());
		assertEquals(context, userObject.context);
		assertEquals(2, userObject.setMethodCalled);
		assertEquals(2, userObject.setMethodCalled2);
		assertEquals(testDouble2, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertNull(userObject.c);
	}

	/**
	 * Tests objects being added and removed from the context
	 */
	@Test
	public synchronized void testParentAddRemove() {
		Integer testInt = Integer.valueOf(123);
		String testString = "abc";
		Double testDouble = Double.valueOf(1.23);
		Float testFloat = Float.valueOf(12.3f);
		Character testChar = Character.valueOf('v');

		// create original context
		IEclipseContext parentContext = EclipseContextFactory.create();
		parentContext.set(Integer.class.getName(), testInt);
		parentContext.set(String.class.getName(), testString);
		parentContext.set(Double.class.getName(), testDouble);
		parentContext.set(Float.class.getName(), testFloat);
		parentContext.set(Character.class.getName(), testChar);
		IEclipseContext context = parentContext.createChild();

		ObjectBasic userObject = new ObjectBasic();
		ContextInjectionFactory.inject(userObject, context);

		// check basic injection
		assertEquals(testString, userObject.injectedString);
		assertEquals(testInt, userObject.getInt());
		assertEquals(context, userObject.context);
		assertEquals(1, userObject.setMethodCalled);
		assertEquals(1, userObject.setMethodCalled2);
		assertEquals(testDouble, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertEquals(testChar, userObject.c);

		// change value
		Double testDouble2 = Double.valueOf(3.45);
		Integer testInt2 = Integer.valueOf(123);
		context.set(Double.class.getName(), testDouble2);
		context.set(Integer.class.getName(), testInt2);

		// and check
		assertEquals(testString, userObject.injectedString);
		assertEquals(testInt2, userObject.getInt());
		assertEquals(context, userObject.context);
		assertEquals(2, userObject.setMethodCalled);
		assertEquals(1, userObject.setMethodCalled2);
		assertEquals(testDouble2, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertEquals(testChar, userObject.c);

		// remove element
		parentContext.remove(String.class.getName());
		parentContext.remove(Character.class.getName());

		// and check
		assertNull(userObject.injectedString);
		assertEquals(testInt2, userObject.getInt());
		assertEquals(context, userObject.context);
		assertEquals(2, userObject.setMethodCalled);
		assertEquals(2, userObject.setMethodCalled2);
		assertEquals(testDouble2, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertNull(userObject.c);
	}

}
