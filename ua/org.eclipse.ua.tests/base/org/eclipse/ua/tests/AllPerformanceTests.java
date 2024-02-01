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
import org.eclipse.ua.tests.intro.AllIntroPerformanceTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/*
 * Tests all user assistance performance (automated).
 */
@Suite
@SelectClasses({ //
		AllCheatSheetPerformanceTests.class, //
		AllHelpPerformanceTests.class, //
		AllIntroPerformanceTests.class, //
})
public class AllPerformanceTests {
}
