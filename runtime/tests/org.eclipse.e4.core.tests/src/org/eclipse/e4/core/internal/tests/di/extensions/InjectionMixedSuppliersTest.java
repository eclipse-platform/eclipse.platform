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
package org.eclipse.e4.core.internal.tests.di.extensions;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class InjectionMixedSuppliersTest {

	static class InjectTarget {
		public String pref;
		public String other;

		@Inject
		public void setPrefs(@Named("testMixed") String otherString, @Preference("injectedPrefs") String string) {
			pref = string;
			other = otherString;
		}
	}

	@Test
	public void testPreferencesQualifier() throws BackingStoreException, InvocationTargetException, InstantiationException {
		IEclipseContext context = EclipseContextFactory.create();
		setPreference("injectedPrefs", "abc");
		context.set("testMixed", "other");
		InjectTarget target = ContextInjectionFactory.make(InjectTarget.class, context);

		// test
		assertEquals("abc", target.pref);
		assertEquals("other", target.other);

		// change
		setPreference("injectedPrefs", "xyz");
		context.set("testMixed", "bingo");

		// re-test
		assertEquals("xyz", target.pref);
		assertEquals("bingo", target.other);
	}

	private void setPreference(String key, String value) throws BackingStoreException {
		String nodePath = CoreTestsActivator.getDefault().getBundleContext().getBundle().getSymbolicName();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(nodePath);
		node.put(key, value);
		node.flush();
	}

}
