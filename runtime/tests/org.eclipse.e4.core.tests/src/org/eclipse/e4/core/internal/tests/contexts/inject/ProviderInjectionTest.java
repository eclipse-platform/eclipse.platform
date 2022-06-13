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

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectorFactory;
import org.eclipse.e4.core.di.annotations.Execute;
import org.junit.Test;


/**
 * Testing provider interface
 */
public class ProviderInjectionTest {

	static public class TestData {

		public String data;

		@Inject
		public TestData(String tmp) {
			data = tmp;
		}
	}

	static public class TestInvokeClass {
		public Provider<TestData> provider;
		public TestInvokeClass() {
			// placeholder
		}
		@Execute
		public int execute(Provider<TestData> arg) {
			provider = arg;
			return 1;
		}
	}

	static public class TestConstructorClass {
		public Provider<TestData> provider;

		@Inject
		public TestConstructorClass(Provider<TestData> provider) {
			this.provider = provider;
		}
	}

	@Test
	public synchronized void testInvokeWithProvider() {

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(String.class.getName(), "abc");
		IInjector injector = InjectorFactory.getDefault();
		injector.addBinding(TestData.class);

		TestInvokeClass userObject = new TestInvokeClass();
		assertEquals(1, ContextInjectionFactory.invoke(userObject, Execute.class, context, null));

		assertNotNull(userObject.provider.get());
		assertEquals("abc", userObject.provider.get().data);
	}

	@Test
	public synchronized void testConstructorWithProvider() {
		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(String.class.getName(), "abc");
		IInjector injector = InjectorFactory.getDefault();
		injector.addBinding(TestData.class);

		TestConstructorClass userObject = ContextInjectionFactory.make(TestConstructorClass.class, context);

		assertNotNull(userObject);
		assertNotNull(userObject.provider);
		assertNotNull(userObject.provider.get());
		assertEquals("abc", userObject.provider.get().data);
	}

}
