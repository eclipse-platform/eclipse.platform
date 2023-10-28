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
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.junit.Test;

/**
 * Tests for the basic context injection functionality
 */
public class ContextInjectionTest {


	/**
	 * Test trivial method injection and finalize method with context as an argument
	 */
	@Test
	public void testContextSetOneArg() {
		class TestData {
		}
		class Injected {
			int contextSetCalled = 0;
			int setMethodCalled = 0;

			public TestData value;

			@Inject
			public void contextSet(IEclipseContext context) {
				contextSetCalled++;
			}

			@Inject
			public void InjectedMethod(TestData arg) {
				setMethodCalled++;
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set(TestData.class.getName(), methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertEquals(1, object.setMethodCalled);
		assertEquals(1, object.contextSetCalled);

		TestData methodValue2 = new TestData();
		context.set(TestData.class.getName(), methodValue2);
		assertEquals(2, object.setMethodCalled);
		assertEquals(methodValue2, object.value);
		assertEquals(1, object.contextSetCalled);
	}

	/**
	 * Test filnalize method - no args
	 */
	@Test
	public void testContextSetZeroArgs() {
		class TestData {
		}
		class Injected {
			int contextSetCalled = 0;
			int setMethodCalled = 0;

			public TestData value;

			@Inject
			public void contextSet() {
				contextSetCalled++;
			}

			@Inject
			public void InjectedMethod(TestData arg) {
				setMethodCalled++;
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set(TestData.class.getName(), methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertEquals(1, object.setMethodCalled);
		assertEquals(1, object.contextSetCalled);

		TestData methodValue2 = new TestData();
		context.set(TestData.class.getName(), methodValue2);
		assertEquals(2, object.setMethodCalled);
		assertEquals(methodValue2, object.value);
		assertEquals(1, object.contextSetCalled);
	}

	/**
	 * Tests basic context injection
	 */
	@Test
	public synchronized void testInjection() {
		Integer testInt = Integer.valueOf(123);
		String testString = "abc";
		Double testDouble = Double.valueOf(1.23);
		Float testFloat = Float.valueOf(12.3f);
		Character testChar = Character.valueOf('v');

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class, testInt);
		context.set(String.class, testString);
		context.set(Double.class, testDouble);
		context.set(Float.class, testFloat);
		context.set(Character.class, testChar);

		ObjectBasic userObject = new ObjectBasic();
		ContextInjectionFactory.inject(userObject, context);

		// check field injection
		assertEquals(testString, userObject.injectedString);
		assertEquals(testInt, userObject.getInt());
		// assertEquals(context, userObject.context);

		// check method injection
		assertEquals(1, userObject.setMethodCalled);
		assertEquals(1, userObject.setMethodCalled2);
		assertEquals(testDouble, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertEquals(testChar, userObject.c);

		// check post processing
		assertTrue(userObject.finalized);
	}

	/**
	 * Tests injection of objects from parent context
	 */
	@Test
	public synchronized void testInjectionFromParent() {
		Integer testInt = Integer.valueOf(123);
		String testString = "abc";
		Double testDouble = Double.valueOf(1.23);
		Float testFloat = Float.valueOf(12.3f);
		Character testChar = Character.valueOf('v');

		// create parent context
		IEclipseContext parentContext = EclipseContextFactory.create();
		parentContext.set(Integer.class.getName(), testInt);
		parentContext.set(String.class.getName(), testString);

		// create child context
		IEclipseContext context = parentContext.createChild();
		context.set(Double.class.getName(), testDouble);
		context.set(Float.class.getName(), testFloat);
		context.set(Character.class.getName(), testChar);

		ObjectBasic userObject = new ObjectBasic();
		ContextInjectionFactory.inject(userObject, context);

		// check field injection
		assertEquals(testString, userObject.injectedString);
		assertEquals(testInt, userObject.getInt());
		// assertEquals(context, userObject.context);

		// check method injection
		assertEquals(1, userObject.setMethodCalled);
		assertEquals(1, userObject.setMethodCalled2);
		assertEquals(testDouble, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertEquals(testChar, userObject.c);

		// check post processing
		assertTrue(userObject.finalized);
	}

	/**
	 * Tests injection into classes with inheritance
	 */
	@Test
	public synchronized void testInjectionAndInheritance() {
		Integer testInt = Integer.valueOf(123);
		String testString = "abc";
		Float testFloat = Float.valueOf(12.3f);

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class.getName(), testInt);
		context.set(String.class.getName(), testString);
		context.set(Float.class.getName(), testFloat);

		ObjectSubClass userObject = new ObjectSubClass();
		ContextInjectionFactory.inject(userObject, context);

		// check inherited portion
		assertEquals(testString, userObject.getString());
		// assertEquals(context, userObject.getContext());
		assertEquals(testString, userObject.getStringViaMethod());
		assertEquals(1, userObject.setStringCalled);

		// check declared portion
		assertEquals(testInt, userObject.getInteger());
		assertEquals(testFloat, userObject.getObjectViaMethod());
		assertEquals(1, userObject.setObjectCalled);

		// make sure overridden injected method was called only once
		assertEquals(1, userObject.setOverriddenCalled);

		// check post processing
		assertEquals(1, userObject.getFinalizedCalled());
	}

	static public class BaseOverrideTest {
		public String selectionString;
		public String inputString;
		public boolean finishCalled = false;

		@Inject
		public void setSelection(String selectionString) {
			this.selectionString = selectionString;
		}

		@Inject
		public void setInput(String inputString) {
			this.inputString = inputString;
		}

		@PostConstruct
		public void finish() {
			finishCalled = true;
		}
	}

	static public class OverrideTest extends BaseOverrideTest {
		public Integer selectionNum;
		public String inputStringSubclass;
		public Double inputDouble;
		public Boolean arg;
		public boolean finishOverrideCalled = false;

		@Inject
		public void setSelection(Integer selectionNum) {
			this.selectionNum = selectionNum;
		}

		@Inject
		public void setInput(String inputString, Double inputDouble) {
			this.inputStringSubclass = inputString;
			this.inputDouble = inputDouble;

		}

		@PostConstruct
		public void finish(Boolean arg) {
			finishOverrideCalled = true;
			this.arg = arg;
		}
	}

	/**
	 * Tests injection of similar, but not overridden methods
	 */
	@Test
	public synchronized void testInjectionCloseOverride() {
		Integer testInt = Integer.valueOf(123);
		String testString = "abc";
		Double testDouble = Double.valueOf(12.3);
		Boolean testBoolean = Boolean.TRUE;

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class, testInt);
		context.set(String.class, testString);
		context.set(Double.class, testDouble);
		context.set(Boolean.class, testBoolean);

		OverrideTest userObject = new OverrideTest();
		ContextInjectionFactory.inject(userObject, context);

		// check inherited portion
		assertEquals(testString, userObject.selectionString);
		assertEquals(testString, userObject.inputString);
		assertTrue(userObject.finishCalled);

		// check similar methods portion
		assertEquals(testInt, userObject.selectionNum);
		assertEquals(testString, userObject.inputStringSubclass);
		assertEquals(testDouble, userObject.inputDouble);
		assertTrue(userObject.finishOverrideCalled);
	}

	@Test
	public void testBug374421() {
		try {
			IEclipseContext context = EclipseContextFactory.create();
			context.runAndTrack(new RunAndTrack() {
				@Override
				public boolean changed(IEclipseContext context) {
					IEclipseContext staticContext = EclipseContextFactory.create();
					ContextInjectionFactory.make(Object.class, context, staticContext);
					return true;
				}
			});
		} catch (StackOverflowError e) {
			fail("See bug 374421 for details.");
		}
	}

}
