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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.junit.Test;

public class InjectionOrderTest {

	public interface ITestObject {

	}

	static public class InjectUnsatisfied {

		@Inject
		ITestObject object;

		static public int count = 0;

		@PostConstruct
		public void postConstruct() {
			count++;
			object.toString();
		}

		@PreDestroy
		public void preDestroy() {
			count++;
			object.toString();
		}
	}

	/**
	 * Make sure special methods are not getting called in case injection failed
	 */
	@Test
	public void testSpecialMethodOnFailure() {
		IEclipseContext appContext = EclipseContextFactory.create();
		boolean exception = false;
		try {
			ContextInjectionFactory.make(InjectUnsatisfied.class, appContext);
		} catch (InjectionException e) {
			exception = true;
		}
		assertTrue(exception);
		appContext.dispose();
		assertEquals(0, InjectUnsatisfied.count);
	}
}
