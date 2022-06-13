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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.junit.Test;

import junit.framework.AssertionFailedError;
/**
 * Tests for the basic context injection functionality
 */
public class AnnotationsInjectionTest {


	@Test
	public void testContextSetOneArg() {
		class TestData {
			// empty
		}
		class Injected {
			int contextSetCalled = 0;
			int setMethodCalled = 0;

			public TestData value;

			@Inject
			public void settings(IEclipseContext context) {
				contextSetCalled++;
			}

			@Inject
			public void injectedMethod(@Named("testing123") TestData arg) {
				setMethodCalled++;
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set("testing123", methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertEquals(1, object.setMethodCalled);
		assertEquals(1, object.contextSetCalled);

		TestData methodValue2 = new TestData();
		context.set("testing123", methodValue2);
		assertEquals(2, object.setMethodCalled);
		assertEquals(methodValue2, object.value);
		assertEquals(1, object.contextSetCalled);
	}

	@Test
	public void testPostConstruct() {
		class TestData {
			// empty
		}
		class Injected {
			int postConstructCalled = 0;
			int setMethodCalled = 0;
			public TestData value;

			@PostConstruct
			public void init() {
				postConstructCalled++;
			}

			@Inject
			public void setData(TestData arg) {
				setMethodCalled++;
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set(TestData.class, methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertEquals(1, object.setMethodCalled);
		assertEquals(1, object.postConstructCalled);

		TestData methodValue2 = new TestData();
		context.set(TestData.class, methodValue2);
		assertEquals(2, object.setMethodCalled);
		assertEquals(1, object.postConstructCalled);
		assertEquals(methodValue2, object.value);
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
		assertEquals(context, userObject.context);

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
	 * Tests that fields are injected before methods.
	 */
	@Test
	public void testFieldMethodOrder() {
		final AssertionFailedError[] error = new AssertionFailedError[1];
		class TestData {
			// empty
		}
		class Injected {
			@Inject @Named("valueField")
			Object injectedField;
			Object methodValue;

			@Inject
			public void injectedMethod(@Optional @Named("valueMethod") Object arg) {
				try {
					assertTrue(injectedField != null);
				} catch (AssertionFailedError e) {
					error[0] = e;
				}
				methodValue = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData fieldValue = new TestData();
		TestData methodValue = new TestData();
		context.set("valueField", fieldValue);
		context.set("valueMethod", methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		if (error[0] != null) {
			throw error[0];
		}
		assertEquals(fieldValue, object.injectedField);
		assertEquals(methodValue, object.methodValue);

		// removing method value, the field should still have value
		context.remove("valueMethod");
		if (error[0] != null) {
			throw error[0];
		}
		assertEquals(fieldValue, object.injectedField);
		assertNull(object.methodValue);

		context.dispose();
		if (error[0] != null) {
			throw error[0];
		}
	}

	@Test
	public void testOptionalInjection() {
		Integer testInt = Integer.valueOf(123);
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class, testInt);

		OptionalAnnotations userObject = new OptionalAnnotations();
		ContextInjectionFactory.inject(userObject, context);

		assertEquals(0, userObject.methodOptionalCalled);
		assertEquals(1, userObject.methodRequiredCalled);
		assertEquals(testInt, userObject.i);
		assertNull(userObject.s);
		assertNull(userObject.d);
		assertNull(userObject.f);

		// add optional services
		String testString = "abc";
		Double testDouble = Double.valueOf(1.23);
		Float testFloat = Float.valueOf(12.3f);
		context.set(String.class, testString);
		context.set(Double.class, testDouble);
		context.set(Float.class, testFloat);

		assertEquals(1, userObject.methodOptionalCalled);
		assertEquals(2, userObject.methodRequiredCalled);
		assertEquals(testInt, userObject.i);
		assertEquals(testString, userObject.s);
		assertEquals(testDouble, userObject.d);
		assertEquals(testFloat, userObject.f);
	}

	@Test
	public void testOptionalInvoke() {

		class TestObject {
			public int called = 0;

			@Execute
			public String something(@Optional String param) {
				called++;
				return param;
			}
		}

		IEclipseContext context = EclipseContextFactory.create();
		Object notAnObject = new Object();
		TestObject testObject = new TestObject();
		context.set(String.class.getName(), testObject);

		Object result = ContextInjectionFactory.invoke(testObject, Execute.class, context, notAnObject);
		assertNull(result);
		assertEquals(1, testObject.called);

		String string = "sample";
		context.set(String.class, string);
		result = ContextInjectionFactory.invoke(testObject, Execute.class, context, notAnObject);
		assertEquals(string, result);
		assertEquals(2, testObject.called);
	}

	/**
	 * Tests that a class with multiple inherited post-construct / pre-destroy methods.
	 */
	@Test
	public void testInheritedSpecialMethods() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class, Integer.valueOf(123));
		context.set(String.class, "abc");
		context.set(Float.class, Float.valueOf(12.3f));

		ObjectSubClass userObject = new ObjectSubClass();
		ContextInjectionFactory.inject(userObject, context);
		assertEquals(1, userObject.superPostConstructCount);
		assertEquals(1, userObject.subPostConstructCount);
		assertEquals(0, userObject.superPreDestroyCount);
		assertEquals(0, userObject.subPreDestroyCount);
		assertEquals(0, userObject.overriddenPreDestroyCount);

		context.set(Float.class, Float.valueOf(45.6f));
		assertEquals(1, userObject.superPostConstructCount);
		assertEquals(1, userObject.subPostConstructCount);
		assertEquals(0, userObject.superPreDestroyCount);
		assertEquals(0, userObject.subPreDestroyCount);
		assertEquals(0, userObject.overriddenPreDestroyCount);

		context.dispose();
		assertEquals(1, userObject.superPreDestroyCount);
		assertEquals(1, userObject.subPreDestroyCount);
		assertEquals(1, userObject.overriddenPreDestroyCount);
	}

	@Test
	public void testInvoke() {
		class TestData {
			public String value;

			public TestData(String tmp) {
				value = tmp;
			}
		}
		class Injected {
			public String myString;

			public Injected() {
				// placeholder
			}

			@Execute
			public String something(@Named("testing123") TestData data) {
				myString = data.value;
				return "true";
			}
		}
		IEclipseContext context = EclipseContextFactory.create();

		TestData methodValue = new TestData("abc");
		context.set("testing123", methodValue);
		Injected object = new Injected();
		assertNull(object.myString);

		assertEquals("true", ContextInjectionFactory.invoke(object, Execute.class, context, null));
		assertEquals("abc", object.myString);
	}

	@Test
	public void testPreDestroy() {
		class TestData {
			// empty
		}
		class Injected {
			int preDestoryCalled = 0;
			public TestData value;

			@Inject
			public TestData directFieldInjection;

			@PreDestroy
			public void aboutToClose() {
				preDestoryCalled++;
				assertNotNull(value);
				assertNotNull(directFieldInjection);
			}

			@Inject
			public void setData(TestData arg) {
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set(TestData.class, methodValue);

		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertNotNull(object.value);
		assertNotNull(object.directFieldInjection);

		context.dispose();

		assertEquals(1, object.preDestoryCalled);
		assertNotNull(object.value);
		assertNotNull(object.directFieldInjection);
	}
}
