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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.server.WebappManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test to see if the help server binds to host 127.0.0.1 in Workbench mode
 */

public class HelpServerBinding {

	private int previousMode;
	// Tests to access the server using it's IP need to be disabled
	// for testing in the build because a firewall can block this access
	// To enable these tests for local testing set testUsingIP to true.
	private final boolean testUsingIP = false;

	@Before
	public void setUp() throws Exception {
		previousMode = BaseHelpSystem.getMode();
	}

	@After
	public void tearDown() throws Exception {
		BaseHelpSystem.setMode(previousMode);
	}

	private String getHostIP() throws UnknownHostException {
		InetAddress host = InetAddress.getLocalHost();
		byte[] ipAddr = host.getAddress();
		String result = "" + ipAddr[0];
		for (int i = 1; i < ipAddr.length; i++) {
			result += '.';
			result += ipAddr[i];
		}
		return result;
	}

	@Test
	public void testInfocenterBinding() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		WebappManager.stop("help");
		WebappManager.start("help");
		assertTrue(canAccessServer("127.0.0.1"));
		if (testUsingIP) {
			assertTrue(canAccessServer(getHostIP()));
		}
	}

	@Test
	public void testWorkbenchBinding() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_WORKBENCH);
		WebappManager.stop("help");
		WebappManager.start("help");
		assertTrue(canAccessServer("127.0.0.1"));
		if (testUsingIP) {
			assertFalse(canAccessServer(getHostIP()));
		}
	}

	@Test
	public void testStandaloneBinding() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_STANDALONE);
		WebappManager.stop("help");
		WebappManager.start("help");
		assertTrue(canAccessServer("127.0.0.1"));
		if (testUsingIP) {
			assertTrue(canAccessServer(getHostIP()));
		}
	}

	private boolean canAccessServer(String host) throws Exception {
		try {
			int port = WebappManager.getPort();
			URL url = new URL("http", host, port, "/help/index.jsp");
			URLConnection connection = url.openConnection();
			setTimeout(connection, 5000);
			try (InputStream input = connection.getInputStream()) {
				int firstbyte = input.read();
				return firstbyte > 0;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private static void setTimeout(URLConnection conn, int milliseconds) {
		conn.setConnectTimeout(milliseconds);
		conn.setReadTimeout(milliseconds);
	}

}
