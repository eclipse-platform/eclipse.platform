/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs the sniff tests for the build. All tests listed here should be
 * automated.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ org.eclipse.core.tests.internal.runtime.AllInternalRuntimeTests.class,
		org.eclipse.core.tests.runtime.AllRuntimeTests.class, org.eclipse.core.tests.runtime.jobs.AllJobTests.class,
		org.eclipse.core.tests.internal.preferences.AllPreferenceTests.class })
public class AutomatedRuntimeTests {

}
