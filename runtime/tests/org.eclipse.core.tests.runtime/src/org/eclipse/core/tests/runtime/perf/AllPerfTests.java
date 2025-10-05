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

import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.eclipse.core.tests.session.PerformanceSessionTestSuite;
import org.eclipse.core.tests.session.Setup;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.eclipse.core.tests.session.UIPerformanceSessionTestSuite;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import java.util.ArrayList;
import java.util.List;

@Suite
@SelectClasses({ //
		BenchPath.class, //
		PreferencePerformanceTest.class, //
})
public class AllPerfTests {

	@TestFactory
	List<DynamicTest> createPerformanceTests() {
		List<DynamicTest> tests = new ArrayList<>();

		// make sure that the first run of the startup test is not recorded - it is heavily
		// influenced by the presence and validity of the cached information
		try {
			PerformanceSessionTestSuite firstRun = new PerformanceSessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, 1, StartupTest.class);
			Setup setup = firstRun.getSetup();
			setup.setSystemProperty("eclipseTest.ReportResults", "false");
			tests.add(DynamicTest.dynamicTest("Warm-up StartupTest", () -> {
				// Execute the warm-up test
				firstRun.run(null);
			}));
		} catch (SetupException e) {
			tests.add(DynamicTest.dynamicTest("Warm-up test setup failed", () -> {
				throw new RuntimeException("Unable to create warm up test", e);
			}));
		}

		// For this test to take advantage of the new runtime processing, we set "-eclipse.activateRuntimePlugins=false"
		try {
			PerformanceSessionTestSuite headlessSuite = new PerformanceSessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, 5, StartupTest.class);
			Setup headlessSetup = headlessSuite.getSetup();
			headlessSetup.setSystemProperty("eclipse.activateRuntimePlugins", "false");
			tests.add(DynamicTest.dynamicTest("Headless StartupTest", () -> {
				// Execute the headless performance test
				headlessSuite.run(null);
			}));
		} catch (SetupException e) {
			tests.add(DynamicTest.dynamicTest("Headless test setup failed", () -> {
				throw new RuntimeException("Unable to setup headless startup performance test", e);
			}));
		}

		// UI startup performance test
		UIPerformanceSessionTestSuite uiSuite = new UIPerformanceSessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, 5, UIStartupTest.class);
		tests.add(DynamicTest.dynamicTest("UI StartupTest", () -> {
			// Execute the UI performance test
			uiSuite.run(null);
		}));

		// Content type performance tests (handled via dynamic test to preserve complex suite behavior)
		tests.add(DynamicTest.dynamicTest("ContentType Performance Tests", () -> {
			ContentTypePerformanceTest.suite().run(null);
		}));

		return tests;
	}
}
