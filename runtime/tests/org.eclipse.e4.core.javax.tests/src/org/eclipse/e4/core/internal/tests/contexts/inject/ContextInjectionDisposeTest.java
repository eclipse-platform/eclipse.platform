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

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.junit.Test;

/**
 * Tests for injection handling of context dispose, and handling disposal of
 * injected objects.
 */
public class ContextInjectionDisposeTest {

	@Test
	public void testContextDisposedNoArg() {
		class Injected {

			boolean disposeInvoked = false;

			@SuppressWarnings("unused")
			@Inject
			private String Field;

			@PreDestroy
			public void dispose() {
				disposeInvoked = true;
			}
		}

		IEclipseContext context = EclipseContextFactory.create();
		context.set(String.class, "hello");
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		context.dispose();
		assertTrue(object.disposeInvoked);
	}

	@Test
	public void testDisposeContext() {
		class Injected {
			boolean disposeInvoked = false;

			@Inject
			Object Field;
			String methodValue;

			@PreDestroy
			public void dispose() {
				disposeInvoked = true;
			}

			@Inject
			public void InjectedMethod(String arg) {
				methodValue = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		Object fieldValue = new Object();
		Object methodValue = "abc";
		context.set(Object.class, fieldValue);
		context.set(String.class.getName(), methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);

		assertEquals(fieldValue, object.Field);
		assertEquals(methodValue, object.methodValue);

		// disposing context calls @PreDestory, but does not clear injected
		// values
		context.dispose();
		assertNotNull(object.Field);
		assertNotNull(object.methodValue);
		assertTrue(object.disposeInvoked);
	}

	@Test
	public void testReleaseObject() {
		class Injected {
			boolean disposeInvoked = false;

			@Inject
			Integer Field;
			Object methodValue;

			@PreDestroy
			public void dispose() {
				disposeInvoked = true;
			}

			@Inject
			public void InjectedMethod(@Optional String arg) {
				methodValue = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		Integer fieldValue = Integer.valueOf(123);
		String methodValue = "abc";
		context.set(Integer.class, fieldValue);
		context.set(String.class, methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);

		assertEquals(fieldValue, object.Field);
		assertEquals(methodValue, object.methodValue);

		// releasing should have the same effect on the single object as
		// disposing the context does.
		ContextInjectionFactory.uninject(object, context);

		assertEquals(fieldValue, object.Field);
		assertNull(object.methodValue);
		assertTrue(object.disposeInvoked);
	}

}
