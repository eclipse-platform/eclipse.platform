/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests;

import org.eclipse.ua.tests.cheatsheet.AllCheatSheetPerformanceTests;
import org.eclipse.ua.tests.help.AllHelpPerformanceTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/*
 * Tests all user assistance performance (automated).
 */
@RunWith(Suite.class)
@SuiteClasses({ AllCheatSheetPerformanceTests.class, AllHelpPerformanceTests.class })
public class AllPerformanceTests {

	/*
	 * Disabled due to inability to backport test to 3.2. Internal test hooks
	 * were added in 3.2.2 code base but do not exist in 3.2 so the test will
	 * not be accurate.
	 */
	// addTest(AllIntroPerformanceTests.suite());
}
