/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.core.tests.internal.runtime;

import static org.junit.Assert.assertNotEquals;

import java.io.*;
import java.net.*;
import junit.framework.Test;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.runtime.RuntimeTest;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.service.datalocation.Location;

public class PlatformURLSessionTest extends RuntimeTest {

	private static final String CONFIG_URL = "platform:/config/" + PI_RUNTIME_TESTS + "/";
	private static final String DATA_CHILD = "child";
	private static final String DATA_PARENT = "parent";
	private static final String FILE_ANOTHER_PARENT_ONLY = "parent2.txt";
	private static final String FILE_BOTH_PARENT_AND_CHILD = "both.txt";
	private static final String FILE_CHILD_ONLY = "child.txt";
	private static final String FILE_PARENT_ONLY = "parent.txt";

	public static void assertEquals(String tag, URL expected, URL actual, boolean external) {
		if (external) {
			assertEquals(tag, expected, actual);
			return;
		}
		assertEquals(tag + " different protocol", expected.getProtocol(), actual.getProtocol());
		assertEquals(tag + " different host", expected.getHost(), actual.getHost());
		assertEquals(tag + " different path", expected.getPath(), actual.getPath());
		assertEquals(tag + " different port", expected.getPort(), actual.getPort());
	}

	private static String readContents(String tag, URL url) {
		URLConnection connection = null;
		try {
			connection = url.openConnection();
		} catch (IOException e) {
			fail(tag + ".1", e);
		}
		InputStream input = null;
		try {
			input = connection.getInputStream();
		} catch (IOException e) {
			fail(tag + ".2", e);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		StringBuilder result = new StringBuilder();
		try {
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
			return result.toString();
		} catch (IOException e) {
			fail(tag + ".99", e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// not interested
			}
		}
		// never happens
		return null;
	}

	public static Test suite() {
		ConfigurationSessionTestSuite suite = new ConfigurationSessionTestSuite(PI_RUNTIME_TESTS, PlatformURLSessionTest.class);
		suite.setReadOnly(true);
		suite.setCascaded(true);
		String[] ids = ConfigurationSessionTestSuite.MINIMAL_BUNDLE_SET;
		for (String id : ids) {
			suite.addBundle(id);
		}
		suite.addBundle(PI_RUNTIME_TESTS);
		return suite;
	}

	public PlatformURLSessionTest(String name) {
		super(name);
	}

	private void createData(String tag) {
		// create some data for this and following test cases
		URL childConfigURL = Platform.getConfigurationLocation().getURL();
		//tests run with file based configuration
		assertEquals(tag + ".1", "file", childConfigURL.getProtocol());
		File childConfigPrivateDir = new File(childConfigURL.getPath(), PI_RUNTIME_TESTS);
		try {
			createFileInFileSystem(new File(childConfigPrivateDir, FILE_CHILD_ONLY), getContents(DATA_CHILD));
			createFileInFileSystem(new File(childConfigPrivateDir, FILE_BOTH_PARENT_AND_CHILD), getContents(DATA_CHILD));
		} catch (IOException e) {
			fail(tag + ".2", e);
		}

		Location parent = Platform.getConfigurationLocation().getParentLocation();
		//tests run with cascaded configuration
		assertNotNull(tag + ".3", parent);
		URL parentConfigURL = parent.getURL();
		//tests run with file based configuration
		assertEquals(tag + ".4", "file", parentConfigURL.getProtocol());
		File parentConfigPrivateDir = new File(parentConfigURL.getPath(), PI_RUNTIME_TESTS);
		try {
			createFileInFileSystem(new File(parentConfigPrivateDir, FILE_PARENT_ONLY), getContents(DATA_PARENT));
			createFileInFileSystem(new File(parentConfigPrivateDir, FILE_ANOTHER_PARENT_ONLY), getContents(DATA_PARENT));
			createFileInFileSystem(new File(parentConfigPrivateDir, FILE_BOTH_PARENT_AND_CHILD), getContents(DATA_PARENT));
		} catch (IOException e) {
			fail(tag + ".5", e);
		}
	}

	/**
	 * Creates test data in both child and parent configurations.
	 */
	public void test0FirstSession() {
		createData("1");

		// try to modify a file in the parent configuration area  - should fail
		URL configURL = null;
		try {
			configURL = new URL(CONFIG_URL + FILE_ANOTHER_PARENT_ONLY);
		} catch (MalformedURLException e) {
			fail("2.0", e);
		}
		URLConnection connection = null;
		try {
			connection = configURL.openConnection();
		} catch (IOException e) {
			fail("3.0", e);
		}
		connection.setDoOutput(true);
		OutputStream output = null;
		try {
			output = connection.getOutputStream();
			fail("4.0 - should have failed");
		} catch (IOException e) {
			// that is expected - parent configuration area is read-only
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// not interested
				}
			}
		}
	}

	public void test1OutputOnReadOnly() {
		// try to modify a file in the configuration area  - should fail
		URL configURL = null;
		try {
			configURL = new URL(CONFIG_URL + FILE_CHILD_ONLY);
		} catch (MalformedURLException e) {
			fail("1.0", e);
		}
		URLConnection connection = null;
		try {
			connection = configURL.openConnection();
		} catch (IOException e) {
			fail("2.0", e);
		}
		connection.setDoOutput(true);
		OutputStream output = null;
		try {
			output = connection.getOutputStream();
			fail("3.0 - should have failed");
		} catch (IOException e) {
			// that is expected - configuration area is read-only
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// not interested
				}
			}
		}
	}

	public void test2Resolution() {
		URL parent = null;
		URL child = null;
		URL both = null;
		URL none = null;
		try {
			parent = new URL(CONFIG_URL + FILE_PARENT_ONLY);
			child = new URL(CONFIG_URL + FILE_CHILD_ONLY);
			both = new URL(CONFIG_URL + FILE_BOTH_PARENT_AND_CHILD);
			none = new URL(CONFIG_URL + "none.txt");
		} catch (MalformedURLException e) {
			fail("0.1", e);
		}
		assertEquals("1.0", DATA_PARENT, readContents("1.1", parent));
		assertEquals("2.0", DATA_CHILD, readContents("2.1", child));
		assertEquals("3.0", DATA_CHILD, readContents("3.1", both));
		URL resolvedURL = null;
		try {
			resolvedURL = FileLocator.resolve(none);
		} catch (IOException e) {
			fail("4.0", e);
		}
		assertNotEquals("4.1", none, resolvedURL);
		assertTrue("4.2", resolvedURL.toExternalForm().startsWith(Platform.getConfigurationLocation().getURL().toExternalForm()));
	}

}
