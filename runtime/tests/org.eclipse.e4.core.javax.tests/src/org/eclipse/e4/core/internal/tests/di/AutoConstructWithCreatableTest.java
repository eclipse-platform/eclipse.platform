/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274, 496305
 ******************************************************************************/
package org.eclipse.e4.core.internal.tests.di;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AutoConstructWithCreatableTest {

	private IEclipseContext context;

	@Creatable
	static class Dependent1 {
		@Inject
		public Dependent1() {
			// placeholder
		}
	}

	static class Dependent2 {
		@Inject
		public Dependent2() {
			// placeholder
		}
	}

	static class Consumer1 {
		@Inject
		public Consumer1(Dependent1 dep) {
			// placeholder
		}
	}

	static class Consumer2 {
		@Inject
		public Consumer2(Dependent2 dep) {
			// placeholder
		}
	}

	@BeforeEach
	public void createContext() {
		context = EclipseContextFactory.create();
	}

	/**
	 * Checks that classes with @Creatable are auto-constructed
	 */
	@Test
	public void testCreatableIsCreated() {
		Consumer1 consumer1 = ContextInjectionFactory.make(Consumer1.class, context);
		assertNotNull(consumer1);

		boolean exception = false;
		try {
			ContextInjectionFactory.make(Consumer2.class, context);
		} catch (InjectionException e) {
			exception = true; // expected
		}
		assertTrue(exception);

		context.set(Dependent2.class, new Dependent2());
		Consumer2 consumer2 = ContextInjectionFactory.make(Consumer2.class, context);
		assertNotNull(consumer2);
	}

	/**
	 * Checks that only classes with @Creatable are auto-constructed
	 */
	@Test
	public void testNonCreatableInstanceAreNotCreated() {
		assertThrows(InjectionException.class, () -> ContextInjectionFactory.make(Consumer2.class, context));
	}

	@Test // ensure "normal" dependency injection
	public void testNonCreatableInstancesAreUsedFromContext() {
		context.set(Dependent2.class, new Dependent2());
		Consumer2 consumer2 = ContextInjectionFactory.make(Consumer2.class, context);
		assertNotNull(consumer2);
	}

}
