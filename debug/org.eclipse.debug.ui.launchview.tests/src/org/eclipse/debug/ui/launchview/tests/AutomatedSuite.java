/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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
 *     Freescale Semiconductor - Bug 293618, Breakpoints view sorts up to first colon only
 *     Anton Kosyakov (Itemis AG) - Bug 438621 - [step filtering] Provide an extension point to enhance methods step filtering.
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.tests;

import org.eclipse.debug.ui.launchview.tests.launchview.LaunchViewSmokeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests for integration and nightly builds.
 *
 * @since 1.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		// Launch Configuration View
		LaunchViewSmokeTest.class,
})
public class AutomatedSuite {
}
