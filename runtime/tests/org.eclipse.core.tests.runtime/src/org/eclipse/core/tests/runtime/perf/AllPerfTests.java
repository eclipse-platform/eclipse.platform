/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime.perf;

import junit.framework.*;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.eclipse.core.tests.session.*;
import org.eclipse.core.tests.session.SetupManager.SetupException;

public class AllPerfTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllPerfTests.class.getName());

		// make sure that the first run of the startup test is not recorded - it is heavily
		// influenced by the presence and validity of the cached information
		try {
			PerformanceSessionTestSuite firstRun = new PerformanceSessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, 1, StartupTest.class);
			Setup setup = firstRun.getSetup();
			setup.setSystemProperty("eclipseTest.ReportResults", "false");
			suite.addTest(firstRun);
		} catch (SetupException e) {
			fail("Unable to create warm up test");
		}

		// For this test to take advantage of the new runtime processing, we set "-eclipse.activateRuntimePlugins=false"
		try {
			PerformanceSessionTestSuite headlessSuite = new PerformanceSessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, 5, StartupTest.class);
			Setup headlessSetup = headlessSuite.getSetup();
			headlessSetup.setSystemProperty("eclipse.activateRuntimePlugins", "false");
			suite.addTest(headlessSuite);
		} catch (SetupException e) {
			fail("Unable to setup headless startup performance test");
		}

		suite.addTest(new UIPerformanceSessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, 5, UIStartupTest.class));
		suite.addTestSuite(BenchPath.class);
		suite.addTest(ContentTypePerformanceTest.suite());
		suite.addTestSuite(PreferencePerformanceTest.class);
		return suite;
	}
}
