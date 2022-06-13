/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.session.samples;

import junit.framework.TestCase;
import junit.framework.TestResult;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.session.SessionTestRunner;
import org.eclipse.core.tests.session.SessionTestSuite;
import org.eclipse.core.tests.session.SetupManager;
import org.eclipse.core.tests.session.TestDescriptor;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

public class MultipleRunsTest extends TestCase {
	public void testMultipleRuns() throws SetupManager.SetupException {
		// the test case to run multiple times
		TestDescriptor test = new TestDescriptor(SampleSessionTest.class.getName(), "testApplicationStartup");
		test.setApplicationId(SessionTestSuite.CORE_TEST_APPLICATION);
		test.setPluginId(CoreTest.PI_HARNESS);
		test.setTestRunner(new SessionTestRunner());
		// setup the command line to be passed to the multiple runs so it has the right system properties
		test.setSetup(SetupManager.getInstance().getDefaultSetup());
		test.getSetup().setSystemProperty("eclipse.perf.dbloc", System.getProperty("eclipse.perf.dbloc"));
		test.getSetup().setSystemProperty("eclipse.perf.config", System.getProperty("eclipse.perf.config"));
		// runs the test case several times - only to collect data, won't do any assertions
		TestResult result = new TestResult();
		for (int i = 0; i < 5; i++) {
			test.run(result);
			if (result.failureCount() > 0) {
				result.failures().nextElement().thrownException().printStackTrace();
				return;
			}
			if (result.errorCount() > 0) {
				result.errors().nextElement().thrownException().printStackTrace();
				return;
			}
		}
		// create a performance meter whose scenario id matches the one used in the test case run
		// our convention: scenario IDs are <test case class name> + '.' + <test case method name>
		PerformanceMeter meter = Performance.getDefault().createPerformanceMeter(test.getTestClass() + '.' + test.getTestMethod());
		// finally do the assertion
		Performance.getDefault().assertPerformanceInRelativeBand(meter, Dimension.ELAPSED_PROCESS, -50, 5);
	}
}
