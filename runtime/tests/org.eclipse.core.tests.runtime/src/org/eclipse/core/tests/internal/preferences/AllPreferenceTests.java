/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.internal.preferences;

import org.eclipse.core.tests.runtime.PreferenceExportTest;
import org.eclipse.core.tests.runtime.PreferenceForwarderTest;
import org.eclipse.core.tests.runtime.PreferencesTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ //
		EclipsePreferencesTest.class, //
		PreferencesServiceTest.class, //
		IScopeContextTest.class, //
		TestBug388004.class, //
		TestBug380859.class, //
		PreferenceExportTest.class, //
		PreferenceForwarderTest.class, //
		PreferencesTest.class, //
})
@SuppressWarnings("deprecation")
public class AllPreferenceTests {
}
