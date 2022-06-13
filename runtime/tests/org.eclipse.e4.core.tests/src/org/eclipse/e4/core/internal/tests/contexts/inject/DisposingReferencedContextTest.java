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
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.junit.Test;

public class DisposingReferencedContextTest {

	static class MandatoryTarget {
		@Inject @Named("object")
		Object object;

		@Inject
		void setActiveContext(@Named(EclipseContext.ACTIVE_CHILD) IEclipseContext partContext) {
			if (partContext != null) {
				partContext.get("someVar");
			}
		}
	}

	static class OptionalTarget {
		@Inject @Named("object")
		Object object;

		@Inject
		void setActiveContext(@Optional @Named(EclipseContext.ACTIVE_CHILD) IEclipseContext partContext) {
			if (partContext != null) {
				partContext.get("someVar");
			}
		}
	}

	@Test
	public void testContextDisposeCausesCompleteUninjection_Mandatory_True() {
		testContextDisposeCausesCompleteUninjection_Mandatory(true);
	}

	@Test
	public void testContextDisposeCausesCompleteUninjection_Mandatory_False() {
		testContextDisposeCausesCompleteUninjection_Mandatory(false);
	}

	@Test
	public void testContextDisposeCausesCompleteUninjection_Optional_True() {
		testContextDisposeCausesCompleteUninjection_Optional(true);
	}

	@Test
	public void testContextDisposeCausesCompleteUninjection_Optional_False() {
		testContextDisposeCausesCompleteUninjection_Optional(false);
	}

	private void testContextDisposeCausesCompleteUninjection_Mandatory(boolean disposeFirst) {
		IEclipseContext windowContext = EclipseContextFactory.create("windowContext");
		IEclipseContext partContext = windowContext.createChild("partContext");

		partContext.activate();

		Object o = new Object();
		windowContext.set("object", o);

		MandatoryTarget target = new MandatoryTarget();
		ContextInjectionFactory.inject(target, windowContext);
		assertEquals("The object should have been injected", o, target.object);

		partContext.dispose();

		assertEquals("The object should not have been uninjected", o, target.object);
	}

	private void testContextDisposeCausesCompleteUninjection_Optional(boolean disposeFirst) {
		IEclipseContext windowContext = EclipseContextFactory.create();
		IEclipseContext partContext = windowContext.createChild();

		partContext.activate();

		Object o = new Object();
		windowContext.set("object", o);

		OptionalTarget target = new OptionalTarget();
		ContextInjectionFactory.inject(target, windowContext);
		assertEquals("The object should have been injected", o, target.object);

		partContext.dispose();

		assertEquals("The object should not have been uninjected", o, target.object);
	}
}
