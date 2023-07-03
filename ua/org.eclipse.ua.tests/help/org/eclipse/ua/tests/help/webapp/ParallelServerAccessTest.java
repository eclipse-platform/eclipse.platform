/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.ua.tests.help.util.LoadServletUtil;
import org.junit.After;
import org.junit.Test;

/**
 * Test the performance of the help server without launching the Help UI
 * Note, this test has been contributed to Equinox, see Bug 362511
 * It is no longer run as part of the UA test suite
 */

public class ParallelServerAccessTest {

	@After
	public void tearDown() throws Exception {
		LoadServletUtil.stopServer();
	}

	@Test
	public void testServletReadInParallel() throws Exception {
		LoadServletUtil.startServer();
		int iterations = 1;  // Change this to increase the length of the test
		for (int i=0; i < iterations; ++i) {
			accessInParallel(10);
		}
	}

	private void accessInParallel(int numberOfThreads) throws Exception {
		ReadThread[] readers = new ReadThread[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++) {
			readers[i] = new ReadThread();
		}
		for (int i = 0; i < numberOfThreads; i++) {
			readers[i].start();
		}
		// Now wait for the threads to complete
		boolean complete = false;
		int iterations = 0;
		do {
			complete = true;
			iterations++;
			if (iterations > 1000) {
				fail("Test did not complete within 100 seconds");
			}
			for (int i = 0; i < numberOfThreads && complete; i++) {
				if (readers[i].isAlive()) {
					complete = false;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						fail("Interrupted Exception");
					}
				}
			}
		} while (!complete);
		for (int i = 0; i < numberOfThreads; i++) {
			if (readers[i].exception != null) {
				throw readers[i].exception;
			}
		}
	}

	private class ReadThread extends Thread {

		public Exception exception;

		@Override
		public void run() {
			for (int j = 0; j <= 100; j++) {
				try {
					readLoadServlet(200);
				} catch (Exception e) {
					this.exception = e;
					e.printStackTrace();
				}
			}
		}
	}

	private static class UnexpectedValueException extends Exception {
		private static final long serialVersionUID = 1L;
		private long expected;
		private long actual;

		UnexpectedValueException(long expected, long actual) {
			this.expected = expected;
			this.actual = actual;
		}

		@Override
		public String getMessage() {
			return "Expected: " + expected +" Actual: " + actual;
		}
	}

	long readOperations = 0;

	public void readLoadServlet(int paragraphs) throws Exception {
		int port = WebappManager.getPort();
		// Use a unique parameter to defeat caching
		long uniqueId = getReadOperations();
		URL url = new URL("http", "localhost", port,
				"/help/loadtest?value=" + uniqueId + "&repeat=" + paragraphs);
		long value = 0;
		try (InputStream input = url.openStream()) {
			int nextChar;
			// The loadtest servlet returns the uniqueParam in an opening
			// comment such as <!--1234-->
			// Read this to verify that we are not getting a cached page
			boolean inFirstComment = true;
			do {
				nextChar = input.read();
				if (inFirstComment) {
					if (nextChar == '>') {
						inFirstComment = false;
					} else if (Character.isDigit((char) nextChar)) {
						value = value * 10 + (nextChar - '0');
					}
				}
			} while (nextChar != '$');
		}
		if (uniqueId != value) {
			throw new UnexpectedValueException(uniqueId, value);
		}
	}

	private synchronized long getReadOperations() {
		return ++readOperations;
	}

}
