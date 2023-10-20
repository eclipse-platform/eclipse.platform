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
import org.eclipse.ua.tests.help.performance.LowIterationHelpServerTest;
import org.eclipse.ua.tests.help.performance.TocAssemblePerformanceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/*
 * Tests help performance (automated).
 */
@Suite
@SelectClasses({ TocAssemblePerformanceTest.class,
	IndexAssemblePerformanceTest.class,
	LowIterationHelpServerTest.class,
	BuildHtmlSearchIndex.class,
	HelpServerTest.class,
	// OpenHelpTest.class // Disabled due to inability to get reliable results. Browser/SWT changes in timing of listener events no longer consistent in 3.3.
})
public class AllHelpPerformanceTests {

}
