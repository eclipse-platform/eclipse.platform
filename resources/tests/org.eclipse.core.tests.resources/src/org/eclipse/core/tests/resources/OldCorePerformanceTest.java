/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import junit.framework.TestResult;
import org.eclipse.core.tests.harness.LoggingPerformanceTestResult;
import org.eclipse.core.tests.harness.PerformanceTestResult;

/**
 * This class used to be the common superclass for all performance tests for
 * the Core project.  It provides extensions to the JUnit infrastructure
 * to incorporate timers into test cases. This class has been superceded by
 * a newer eclipse-wide performance test suite.
 *
 * There are two distinct sets of timing mechanisms available, depending
 * on the type of operation being testing.  For long running operations
 * with noticeable delays, use startTimer() and stopTimer().  This is
 * a more heavy-weight mechanism that allows logging, multiple
 * timers, and overlapping timers.
 *
 * For benchmarking operations that should be instantaneous, use the
 * startBench(), stopBench() methods.  Here is an example:
 *
 *	<pre><code>
 * 	String benchName = "myOperation";
 * 	int repeat = 500;
 *	startBench();
 *	for (int i = 0; i &lt; repeat; i++) {
 *		//run the operation to be tested here
 *	}
 *	stopBench(benchName, repeat);
 *	</code></pre>
 * Note that only one operation can be tested at a time with this mechanism.
 *
 * If an instance of this class is run using a LoggingPerformanceTestResult,
 * an HTML log file will be maintained of all timing and garbage collecting,
 * in addition to any messages added using the log() method.  In the absence
 * of a logging test result, all log events are written to the standard output.
 */
public abstract class OldCorePerformanceTest extends ResourceTest {
	protected long benchStart;
	protected LoggingPerformanceTestResult logger = null;
	protected PerformanceTestResult result = null;

	public OldCorePerformanceTest() {
		super();
	}

	public OldCorePerformanceTest(String name) {
		super(name);
	}

	protected PerformanceTestResult defaultTest() {
		return new PerformanceTestResult();
	}

	/**
	 * Logs or writes string to console.
	 */
	public void perfLog(String s) {
		if (logger != null) {
			logger.log(s);
		} else {
			System.out.println(s);
		}
	}

	/**
	 * A convenience method to run this test, collecting the results with a
	 * default PerformanceTestResult object.
	 *
	 * @see PerformanceTestResult
	 */
	@Override
	public TestResult run() {
		PerformanceTestResult test = defaultTest();
		run(test);
		return test;
	}

	/**
	 * Runs the test case and collects the results in a PerformanceTestResult.
	 * This is the template method that defines the control flow
	 * for running a test case.
	 */
	public void run(PerformanceTestResult test) {
		result = test;
		if (test instanceof LoggingPerformanceTestResult) {
			logger = (LoggingPerformanceTestResult) test;
		}
		super.run(test);
	}

	protected void startBench() {
		for (int i = 0; i < 20; ++i) {
			System.gc();
		}
		benchStart = System.currentTimeMillis();
	}

	/**
	 * Tell the result to start a timer with the given name.
	 * If no timer exists with that name, result creates a new timer
	 * and starts it running.
	 */
	protected void startTimer(String timerName) {
		result.startTimer(timerName);
	}

	protected void stopBench(String benchName, int numOperations) {
		long duration = System.currentTimeMillis() - benchStart;
		double perOp = (double) duration / (double) numOperations;
		String opString;
		if (perOp > 100.0) {
			opString = "(" + perOp + "ms per operation)"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			//Note us == microseconds
			opString = "(" + (perOp * 1000.0) + "us per operation)"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		System.out.println(benchName + " took " + duration + "ms " + opString); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tell the result to stop the timer with the given name.
	 */
	protected void stopTimer(String timerName) {
		result.stopTimer(timerName);
	}
}
