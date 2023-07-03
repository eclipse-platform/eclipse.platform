/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.util;

import org.junit.Assert;


public class ParallelTestSupport {

	public interface ITestCase {
		/**
		 * Runs a test case one time
		 * @return null if the test passes, otherwise a string representing
		 * the reason for the failure
		 * @throws Exception
		 */
		public String runTest() throws Exception;
	}

	public static void testSingleCase(ITestCase testCase, int repetitions) {
		String result = null;
		for (int i = 0; i < repetitions && result == null; i++) {
			try {
				result = testCase.runTest();
			} catch (Exception e) {
				result = e.getMessage();
			}
			if (result != null) {
				Assert.fail(result);
			}
		}
	}

	public static void testInParallel(ITestCase[] testCases, int repetitions) {
		int numberOfThreads = testCases.length;
		TestThread[] testThreads = new TestThread[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++) {
			testThreads[i] = new TestThread(i, testCases[i], repetitions);
			testThreads[i].start();
		}
		// Now wait for the threads to complete
		boolean complete = false;
		do {
			complete = true;
			for (int i = 0; i < numberOfThreads && complete; i++) {
				if (testThreads[i].isAlive()) {
					complete = false;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Assert.fail("Interrupted Exception");
					}
				}
			}
		} while (!complete);
		for (int i = 0; i < numberOfThreads; i++) {
			if (testThreads[i].failureReason != null) {
				Assert.fail(testThreads[i].failureReason);
			}
		}
	}

	private static class TestThread extends Thread {

		private int index;
		private ITestCase testCase;
		private int repetitions;

		public TestThread(int index, ITestCase testCase, int repetitions) {
			this.index = index;
			this.testCase = testCase;
			this.repetitions = repetitions;
		}

		public String failureReason = null;

		@Override
		public void run() {
			for (int j = 0; j <= repetitions; j++) {
				try {
					String unexpected = testCase.runTest();
					if (unexpected != null) {
						failureReason = "Thread " + index + ", iteration " + j + ": " + unexpected;
						return;
					}
				} catch (Exception e) {
					failureReason = e.getMessage();
					return;
				}
			}
		}
	}

}
