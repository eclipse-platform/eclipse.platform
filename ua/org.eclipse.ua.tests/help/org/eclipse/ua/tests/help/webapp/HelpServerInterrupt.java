/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
import java.net.URLConnection;

import org.eclipse.help.internal.server.WebappManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to see if the help server is interruptable
 */

public class HelpServerInterrupt {

	private static boolean enableTimeout = true;
	private int iterations;
	private int sleepTime = 10;
	private static class ServerStarter extends Thread {

		private Exception exception = null;

		@Override
		public synchronized void run() {
			try {
				WebappManager.start("help");
			} catch (Exception e) {
				exception = e;
			}
		}

		public Exception getException() {
			return exception;
		}
	}

	@Test
	public void testServerWithoutInterrupt() throws Exception {
		WebappManager.stop("help");
		startServerWithoutInterrupt();
		checkServer();
		WebappManager.stop("help");
	}

	@Test
	public void testServerWithInterrupt() throws Exception {
		WebappManager.stop("help");
		startServerWithInterrupt();
		checkServer();
		WebappManager.stop("help");
	}

	private void startServerWithoutInterrupt() throws Exception {
		ServerStarter starter = new ServerStarter();
		starter.start();
		// Now wait for the thread to complete
		iterations = 0;
		do {
			iterations++;
			if (enableTimeout && sleepTime * iterations > 10000) {
				fail("Test did not complete within 10 seconds");
			}
			Thread.sleep(sleepTime);
		} while (starter.isAlive());
		Exception exception = starter.getException();
		if (exception != null) {
			throw exception;
		}
	}

	private void startServerWithInterrupt() throws Exception {
		ServerStarter starter = new ServerStarter();
		starter.start();
		// Now wait for the thread to complete
		iterations = 0;
		do {
			iterations++;
			if (enableTimeout && sleepTime * iterations > 10000) {
				fail("Test did not complete within 10 seconds");
			}
			starter.interrupt();
			Thread.sleep(sleepTime);
		} while (starter.isAlive());
		Exception exception = starter.getException();
		if (exception != null) {
			throw exception;
		}
	}

	private void checkServer() throws Exception {
		long start = System.currentTimeMillis();
		try {
			int port = WebappManager.getPort();
			URL url = new URL("http", "localhost", port, "/help/index.jsp");
			URLConnection connection = url.openConnection();
			setTimeout(connection, 5000);
			try (InputStream input = connection.getInputStream()) {
				int firstbyte = input.read();
				Assert.assertTrue(firstbyte > 0);
			}
		} catch (Exception e) {
			long elapsed = System.currentTimeMillis() - start;
			System.out.println("Fail, milliseconds = " + elapsed);
			throw e;
		}
	}

	private static void setTimeout(URLConnection conn, int milliseconds) {
		conn.setConnectTimeout(milliseconds);
		conn.setReadTimeout(milliseconds);
	}

}
