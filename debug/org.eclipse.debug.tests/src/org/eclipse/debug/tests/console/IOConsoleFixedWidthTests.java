/*******************************************************************************
 * Copyright (c) 2019 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import org.eclipse.debug.tests.TestUtil;

/**
 * Same as {@link IOConsoleTests} but with fixed width console enabled.
 */
public class IOConsoleFixedWidthTests extends IOConsoleTests {

	@Override
	protected IOConsoleTestUtil getTestUtil(String title) {
		final IOConsoleTestUtil c = super.getTestUtil(title);
		// Varying the width may reveal new bugs. There is no width value
		// which is invalid. (but remember most test output is quite short)
		// And try the most beautiful width of 1 aka the vertical console.
		c.getConsole().setConsoleWidth(3);
		c.setIgnoreFixedConsole(true);
		// console width is applied asynchronous
		TestUtil.waitForJobs(name.getMethodName(), 50, 1000);
		return c;
	}

	// the actual tests are inherited from IOConsoleTests
}
