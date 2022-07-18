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
import static org.junit.Assert.assertThrows;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;
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

	private static String readContents(String tag, URL url) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			return reader.lines().collect(Collectors.joining());
		}
	}

	public static Test suite() {
		ConfigurationSessionTestSuite suite = new ConfigurationSessionTestSuite(PI_RUNTIME_TESTS, PlatformURLSessionTest.class);
		suite.setReadOnly(true);
		suite.setCascaded(true);
		suite.addMinimalBundleSet();
		suite.addThisBundle();
		return suite;
	}

	public PlatformURLSessionTest(String name) {
		super(name);
	}

	private void createData(String tag) throws IOException {
		// create some data for this and following test cases
		URL childConfigURL = Platform.getConfigurationLocation().getURL();
		// tests run with file based configuration
		assertEquals(tag + ".1", "file", childConfigURL.getProtocol());
		File childConfigPrivateDir = new File(childConfigURL.getPath(), PI_RUNTIME_TESTS);
		createFileInFileSystem(new File(childConfigPrivateDir, FILE_CHILD_ONLY), getContents(DATA_CHILD));
		createFileInFileSystem(new File(childConfigPrivateDir, FILE_BOTH_PARENT_AND_CHILD), getContents(DATA_CHILD));

		Location parent = Platform.getConfigurationLocation().getParentLocation();
		// tests run with cascaded configuration
		assertNotNull(tag + ".3", parent);
		URL parentConfigURL = parent.getURL();
		// tests run with file based configuration
		assertEquals(tag + ".4", "file", parentConfigURL.getProtocol());
		File parentConfigPrivateDir = new File(parentConfigURL.getPath(), PI_RUNTIME_TESTS);
		createFileInFileSystem(new File(parentConfigPrivateDir, FILE_PARENT_ONLY), getContents(DATA_PARENT));
		createFileInFileSystem(new File(parentConfigPrivateDir, FILE_ANOTHER_PARENT_ONLY), getContents(DATA_PARENT));
		createFileInFileSystem(new File(parentConfigPrivateDir, FILE_BOTH_PARENT_AND_CHILD), getContents(DATA_PARENT));
	}

	/**
	 * Creates test data in both child and parent configurations.
	 */
	public void test0FirstSession() throws IOException {
		createData("1");
		// try to modify a file in the parent configuration area - should fail
		URL configURL = new URL(CONFIG_URL + FILE_ANOTHER_PARENT_ONLY);
		URLConnection connection = configURL.openConnection();
		connection.setDoOutput(true);
		assertThrows(IOException.class, () -> {
			try (var o = connection.getOutputStream()) {
			}
		});
	}

	public void test1OutputOnReadOnly() throws IOException {
		// try to modify a file in the configuration area - should fail
		URL configURL = new URL(CONFIG_URL + FILE_CHILD_ONLY);
		URLConnection connection = configURL.openConnection();
		connection.setDoOutput(true);
		assertThrows(IOException.class, () -> {
			try (var o = connection.getOutputStream()) {
			}
		});
	}

	public void test2Resolution() throws IOException {
		URL parent = new URL(CONFIG_URL + FILE_PARENT_ONLY);
		URL child = new URL(CONFIG_URL + FILE_CHILD_ONLY);
		URL both = new URL(CONFIG_URL + FILE_BOTH_PARENT_AND_CHILD);
		URL none = new URL(CONFIG_URL + "none.txt");

		assertEquals("1.0", DATA_PARENT, readContents("1.1", parent));
		assertEquals("2.0", DATA_CHILD, readContents("2.1", child));
		assertEquals("3.0", DATA_CHILD, readContents("3.1", both));
		URL resolvedURL = FileLocator.resolve(none);
		assertNotEquals("4.1", none, resolvedURL);
		assertTrue("4.2",
				resolvedURL.toExternalForm().startsWith(Platform.getConfigurationLocation().getURL().toExternalForm()));
	}

}
