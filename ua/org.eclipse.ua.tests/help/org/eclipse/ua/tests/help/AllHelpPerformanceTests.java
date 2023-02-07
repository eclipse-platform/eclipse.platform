/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.ua.tests.help;

import org.eclipse.ua.tests.help.performance.BuildHtmlSearchIndex;
import org.eclipse.ua.tests.help.performance.HelpServerTest;
import org.eclipse.ua.tests.help.performance.IndexAssemblePerformanceTest;
import org.eclipse.ua.tests.help.performance.TocAssemblePerformanceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/*
 * Tests help performance (automated).
 */
@RunWith(Suite.class)
@SuiteClasses({ TocAssemblePerformanceTest.class, IndexAssemblePerformanceTest.class, BuildHtmlSearchIndex.class,
		HelpServerTest.class })
public class AllHelpPerformanceTests {

	/*
	 * Disabled due to inability to get reliable results. Browser/SWT changes in
	 * timing of listener events no longer consistent in 3.3.
	 */

	// addTest(OpenHelpTest.suite());
}
