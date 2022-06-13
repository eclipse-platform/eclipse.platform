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
 *     Daniel Kruegler <daniel.kruegler@gmail.com> - Bug 493697
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

/**
 * Tests for the context injection functionality using 2 contexts
 */
public class InjectStaticContextTest {
	static class TestClass {
		public IEclipseContext injectedContext;
		public String aString;
		public String bString;
		public String cString;

		public String aConstructorString;
		public String bConstructorString;

		public int postConstructCalled = 0;
		public int preDestroyCalled = 0;

		@Inject
		public void contextSet(@Optional IEclipseContext context) {
			injectedContext = context;
		}

		@Inject
		public void setA(@Optional @Named("a") String aString) {
			this.aString = aString;
		}

		@Inject
		public void setB(@Named("b") String bString) {
			this.bString = bString;
		}

		@Inject
		public void setC(@Named("c") String cString) {
			this.cString = cString;
		}

		@Inject
		public void InjectedMethod(@Named("aConstructor") String aString, @Named("bConstructor") String bString) {
			aConstructorString = aString;
			bConstructorString = bString;
		}

		@PostConstruct
		public void init() {
			postConstructCalled++;
		}

		@PreDestroy
		public void dispose() {
			preDestroyCalled++;
		}
	}

	static class TestInvokeClass {
		public String aString;
		public String bString;

		public IEclipseContext context;

		@Execute
		public String testMethod(@Named("a") String aString, @Named("b") String bString, IEclipseContext context) {
			this.aString = aString;
			this.bString = bString;
			this.context = context;
			return aString + bString;
		}
	}

	@Test
	public void testStaticMake() {
		IEclipseContext trackedContext = EclipseContextFactory.create();
		trackedContext.set("a", "abc");
		trackedContext.set("aConstructor", "abcConstructor");
		trackedContext.set("b", "bbc");

		IEclipseContext staticContext = EclipseContextFactory.create();
		staticContext.set("b", "123"); // local values override
		staticContext.set("bConstructor", "123Constructor");
		staticContext.set("c", "xyz");

		TestClass testObject = ContextInjectionFactory.make(TestClass.class, trackedContext, staticContext);

		assertEquals(trackedContext, testObject.injectedContext);
		assertEquals("abcConstructor", testObject.aConstructorString);
		assertEquals("123Constructor", testObject.bConstructorString);
		assertEquals("abc", testObject.aString);
		assertEquals("123", testObject.bString);
		assertEquals("xyz", testObject.cString);
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(0, testObject.preDestroyCalled);

		// modify local context -> should have no effect
		staticContext.set("b", "_123_");
		staticContext.set("bConstructor", "_123Constructor_");
		staticContext.set("c", "_xyz_");

		assertEquals("abcConstructor", testObject.aConstructorString);
		assertEquals("123Constructor", testObject.bConstructorString);
		assertEquals("abc", testObject.aString);
		assertEquals("123", testObject.bString);
		assertEquals("xyz", testObject.cString);
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(0, testObject.preDestroyCalled);

		// dispose local context -> should have no effect
		staticContext.dispose();

		assertEquals("abcConstructor", testObject.aConstructorString);
		assertEquals("123Constructor", testObject.bConstructorString);
		assertEquals("abc", testObject.aString);
		assertEquals("123", testObject.bString);
		assertEquals("xyz", testObject.cString);
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(0, testObject.preDestroyCalled);

		// modify parent context -> should propagate
		trackedContext.set("a", "_abc_");
		trackedContext.set("b", "_bbc_");

		assertEquals("_abc_", testObject.aString);
		assertEquals("123", testObject.bString);

		// uninject from the parent context
		ContextInjectionFactory.uninject(testObject, trackedContext);

		assertNull(testObject.injectedContext);
		assertNull(testObject.aString);

		assertEquals(1, testObject.postConstructCalled);
		assertEquals(1, testObject.preDestroyCalled);

		// further changes should have no effect
		trackedContext.set("a", "+abc+");
		assertNull(testObject.aString);

		trackedContext.dispose();
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(1, testObject.preDestroyCalled);
	}

	@Test
	public void testStaticInject() {
		IEclipseContext trackedContext = EclipseContextFactory.create();
		trackedContext.set("a", "abc");
		trackedContext.set("aConstructor", "abcConstructor");
		trackedContext.set("b", "bbc");

		IEclipseContext staticContext = EclipseContextFactory.create();
		staticContext.set("b", "123"); // local values override
		staticContext.set("bConstructor", "123Constructor");
		staticContext.set("c", "xyz");

		TestClass testObject = new TestClass();

		assertNull(testObject.injectedContext);
		assertNull(testObject.aConstructorString);
		assertNull(testObject.bConstructorString);
		assertNull(testObject.aString);
		assertNull(testObject.bString);
		assertNull(testObject.cString);
		assertEquals(0, testObject.postConstructCalled);
		assertEquals(0, testObject.preDestroyCalled);

		ContextInjectionFactory.inject(testObject, trackedContext, staticContext);

		assertEquals(trackedContext, testObject.injectedContext);
		assertEquals("abcConstructor", testObject.aConstructorString);
		assertEquals("123Constructor", testObject.bConstructorString);
		assertEquals("abc", testObject.aString);
		assertEquals("123", testObject.bString);
		assertEquals("xyz", testObject.cString);
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(0, testObject.preDestroyCalled);

		// modify local context -> should have no effect
		staticContext.set("b", "_123_");
		staticContext.set("bConstructor", "_123Constructor_");
		staticContext.set("c", "_xyz_");

		assertEquals("abcConstructor", testObject.aConstructorString);
		assertEquals("123Constructor", testObject.bConstructorString);
		assertEquals("abc", testObject.aString);
		assertEquals("123", testObject.bString);
		assertEquals("xyz", testObject.cString);
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(0, testObject.preDestroyCalled);

		// dispose local context -> should have no effect
		staticContext.dispose();

		assertEquals("abcConstructor", testObject.aConstructorString);
		assertEquals("123Constructor", testObject.bConstructorString);
		assertEquals("abc", testObject.aString);
		assertEquals("123", testObject.bString);
		assertEquals("xyz", testObject.cString);
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(0, testObject.preDestroyCalled);

		// modify parent context -> should propagate
		trackedContext.set("a", "_abc_");
		trackedContext.set("b", "_bbc_");

		assertEquals("_abc_", testObject.aString);
		assertEquals("123", testObject.bString);

		// uninject from the parent context
		ContextInjectionFactory.uninject(testObject, trackedContext);

		assertNull(testObject.injectedContext);
		assertNull(testObject.aString);

		assertEquals(1, testObject.postConstructCalled);
		assertEquals(1, testObject.preDestroyCalled);

		// further changes should have no effect
		trackedContext.set("a", "+abc+");
		assertNull(testObject.aString);

		trackedContext.dispose();
		assertEquals(1, testObject.postConstructCalled);
		assertEquals(1, testObject.preDestroyCalled);
	}

	@Test
	public void testStaticInvoke() {
		IEclipseContext trackedContext = EclipseContextFactory.create("main");
		trackedContext.set("a", "abc");

		IEclipseContext staticContext = EclipseContextFactory.create("static");
		staticContext.set("b", "123");

		TestInvokeClass testObject = new TestInvokeClass();
		assertNull(testObject.aString);
		assertNull(testObject.bString);

		Object result = ContextInjectionFactory.invoke(testObject, Execute.class, trackedContext, staticContext, null);

		assertEquals("abc123", result);

		assertEquals("abc", testObject.aString);
		assertEquals("123", testObject.bString);

		assertEquals(trackedContext, testObject.context);
	}
}
