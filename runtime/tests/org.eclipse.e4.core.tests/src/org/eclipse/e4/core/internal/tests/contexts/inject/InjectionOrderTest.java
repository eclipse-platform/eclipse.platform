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

import static org.junit.Assert.assertTrue;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;

public class InjectionOrderTest {

	public static class InjectTargetMethod {

		boolean nonNull = false;

		Object o;

		@Inject
		void set(@Named("inject") Object o) {
			this.o = o;
		}

		@PreDestroy
		void pd() {
			// methods should always be uninjected after @PD is called
			nonNull = o != null;
		}
	}

	public static class InjectTargetField {

		boolean nonNull = false;

		@Inject @Named("inject")
		Object o;

		@PreDestroy
		void pd() {
			// fields should always be uninjected after @PD is called
			nonNull = o != null;
		}
	}

	/**
	 * Tests to ensure that the injection/uninjection order of fields is correct.
	 * <p>
	 * See bug 304859.
	 * </p>
	 */
	@Test
	public void testDisposeMethod() throws Exception {
		// create a context
		IEclipseContext appContext = EclipseContextFactory.create();
		// set a value
		appContext.set("inject", "a");

		// instantiate the object
		InjectTargetMethod injectTargetMethod = ContextInjectionFactory.make(InjectTargetMethod.class, appContext);
		// change the requested value so another injection occurs
		appContext.set("inject", "b");

		// now we dispose the context
		appContext.dispose();

		// check that the second 'set' invocation did not alter the order of notifications
		assertTrue("@PreDestroy was incorrectly called after the method was uninjected", injectTargetMethod.nonNull);
	}

	/**
	 * Tests to ensure that the injection/uninjection order of methods is correct.
	 * <p>
	 * See bug 304859.
	 * </p>
	 */
	@Test
	public void testDisposeField() throws Exception {
		// create a context
		IEclipseContext appContext = EclipseContextFactory.create();
		// set a value
		appContext.set("inject", "a");

		// instantiate the object
		InjectTargetField injectTargetField = ContextInjectionFactory.make(InjectTargetField.class, appContext);
		// change the requested value so another injection occurs
		appContext.set("inject", "b");

		// now we dispose the context
		appContext.dispose();

		assertTrue("@PreDestroy was incorrectly called after the field was uninjected", injectTargetField.nonNull);
	}
}
