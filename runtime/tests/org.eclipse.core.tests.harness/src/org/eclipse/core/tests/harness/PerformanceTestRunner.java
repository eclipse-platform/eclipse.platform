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
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.harness;

import static org.junit.Assert.fail;

import junit.framework.TestCase;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

/**
 * Helper class for executing a performance test. Takes care of starting, stopping,
 * and committing performance timers.
 */
public abstract class PerformanceTestRunner {

	private String regressionReason;
	private String fingerprintName;

	public void setRegressionReason(String comment) {
		this.regressionReason = comment;
	}

	/**
	 * Implemented by subclasses to perform the work to be measured.
	 */
	protected abstract void test() throws Exception;

	/**
	 * Executes the performance test the given number of times. Use the outer time
	 * to execute the test several times in order to obtain a normalized average. Use
	 * the inner loop for very fast tests that would otherwise be difficult to measure
	 * due to Java's poor timer granularity.  The inner loop is not needed for long
	 * tests that typically take more than a second to execute.
	 *
	 * @param testCase The test that is running (used to obtain an appropriate meter)
	 * @param outer The number of repetitions of the test.
	 * @param inner The number of repetitions within the performance timer.
	 */
	public final void run(TestCase testCase, int outer, int inner) {
		run(testCase, null, outer, inner);
	}

	/**
	 * Executes the performance test the given number of times. Use the outer time
	 * to execute the test several times in order to obtain a normalized average. Use
	 * the inner loop for very fast tests that would otherwise be difficult to measure
	 * due to Java's poor timer granularity.  The inner loop is not needed for long
	 * tests that typically take more than a second to execute.
	 *
	 * @param testCase The test that is running (used to obtain an appropriate meter)
	 * @param localName the short name used to tag the local test
	 * @param outer The number of repetitions of the test.
	 * @param inner The number of repetitions within the performance timer.
	 */
	public final void run(TestCase testCase, String localName, int outer, int inner) {
		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(perf.getDefaultScenarioId(testCase));
		try {
			runTest(meter, localName, outer, inner);
		} catch (Exception e) {
			fail("Failed performance test with exception:" + e);
		}
	}

	/**
	 * Executes the performance test the given number of times. Use the outer time
	 * to execute the test several times in order to obtain a normalized average.
	 * Use the inner loop for very fast tests that would otherwise be difficult to
	 * measure due to Java's poor timer granularity. The inner loop is not needed
	 * for long tests that typically take more than a second to execute.
	 *
	 * @param testClass      The test class that is currently executed (used to
	 *                       obtain an appropriate meter)
	 * @param testMethodName The test method name (or some other identifier) that is
	 *                       currently executed (used to obtain an appropriate
	 *                       meter)
	 * @param outer          The number of repetitions of the test.
	 * @param inner          The number of repetitions within the performance timer.
	 */
	public final void run(Class<?> testClass, String testMethodName, int outer, int inner) throws Exception {
		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(perf.getDefaultScenarioId(testClass, testMethodName));
		runTest(meter, null, outer, inner);
	}

	private void runTest(PerformanceMeter meter, String localName, int outer, int inner) throws Exception {
		Performance perf = Performance.getDefault();
		if (regressionReason != null) {
			perf.setComment(meter, Performance.EXPLAINS_DEGRADATION_COMMENT, regressionReason);
		}
		try {
			for (int i = 0; i < outer; i++) {
				setUp();
				meter.start();
				for (int j = 0; j < inner; j++) {
					test();
				}
				meter.stop();
				tearDown();
			}
			if (localName != null) {
				perf.tagAsSummary(meter, localName, Dimension.ELAPSED_PROCESS);
			}
			if (fingerprintName != null) {
				perf.tagAsSummary(meter, fingerprintName, Dimension.ELAPSED_PROCESS);
			}
			meter.commit();
			perf.assertPerformance(meter);
		} finally {
			meter.dispose();
		}
	}

	protected void setUp() throws Exception {
		// subclasses to override
	}

	protected void tearDown() throws Exception {
		// subclasses to override
	}

	/**
	 * Sets the finger print name. Setting this value will make the test part
	 * of the component finger print results.  A value of null indicates that the
	 * test is not a finger print test.
	 */
	public void setFingerprintName(String fingerprintName) {
		this.fingerprintName = fingerprintName;
	}
}
