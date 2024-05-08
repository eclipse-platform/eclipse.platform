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

import static org.eclipse.core.tests.runtime.RuntimeTestsPlugin.PI_RUNTIME_TESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.osgi.service.datalocation.Location;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(Lifecycle.PER_CLASS)
public class PlatformURLSessionTest {

	@RegisterExtension
	SessionTestExtension extension = SessionTestExtension.forPlugin(PI_RUNTIME_TESTS)
			.withCustomization(SessionTestExtension.createCustomConfiguration().setCascaded().setReadOnly()).create();

	private static final String CONFIG_URL = "platform:/config/" + PI_RUNTIME_TESTS + "/";
	private static final String DATA_CHILD = "child";
	private static final String DATA_PARENT = "parent";
	private static final String FILE_ANOTHER_PARENT_ONLY = "parent2.txt";
	private static final String FILE_BOTH_PARENT_AND_CHILD = "both.txt";
	private static final String FILE_CHILD_ONLY = "child.txt";
	private static final String FILE_PARENT_ONLY = "parent.txt";

	/**
	 * Creates test data in both child and parent configurations.
	 */
	@Order(0)
	@Test
	public void test0FirstSession() throws IOException {
		createData();
		// try to modify a file in the parent configuration area - should fail
		URL configURL = new URL(CONFIG_URL + FILE_ANOTHER_PARENT_ONLY);
		URLConnection connection = configURL.openConnection();
		connection.setDoOutput(true);
		assertThrows(IOException.class, () -> {
			try (var o = connection.getOutputStream()) {
			}
		});
	}

	private void createData() throws IOException {
		// create some data for this and following test cases
		URL childConfigURL = Platform.getConfigurationLocation().getURL();
		// tests run with file based configuration
		assertEquals("file", childConfigURL.getProtocol());
		File childConfigPrivateDir = new File(childConfigURL.getPath(), PI_RUNTIME_TESTS);
		childConfigPrivateDir.mkdirs();
		createFileWithContents(new File(childConfigPrivateDir, FILE_CHILD_ONLY), DATA_CHILD);
		createFileWithContents(new File(childConfigPrivateDir, FILE_BOTH_PARENT_AND_CHILD), DATA_CHILD);

		Location parent = Platform.getConfigurationLocation().getParentLocation();
		// tests run with cascaded configuration
		assertNotNull(parent);
		URL parentConfigURL = parent.getURL();
		// tests run with file based configuration
		assertEquals("file", parentConfigURL.getProtocol());
		File parentConfigPrivateDir = new File(parentConfigURL.getPath(), PI_RUNTIME_TESTS);
		parentConfigPrivateDir.mkdirs();
		createFileWithContents(new File(parentConfigPrivateDir, FILE_PARENT_ONLY), DATA_PARENT);
		createFileWithContents(new File(parentConfigPrivateDir, FILE_ANOTHER_PARENT_ONLY), DATA_PARENT);
		createFileWithContents(new File(parentConfigPrivateDir, FILE_BOTH_PARENT_AND_CHILD), DATA_PARENT);
	}

	private void createFileWithContents(File file, String contents) throws IOException {
		try (InputStream input = new ByteArrayInputStream(contents.getBytes());
				FileOutputStream output = new FileOutputStream(file)) {
			input.transferTo(output);
		}

	}

	@Order(1)
	@Test
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

	@Order(2)
	@Test
	public void test2Resolution() throws IOException {
		URL parent = new URL(CONFIG_URL + FILE_PARENT_ONLY);
		URL child = new URL(CONFIG_URL + FILE_CHILD_ONLY);
		URL both = new URL(CONFIG_URL + FILE_BOTH_PARENT_AND_CHILD);
		URL none = new URL(CONFIG_URL + "none.txt");

		assertEquals(DATA_PARENT, readContents("1.1", parent));
		assertEquals(DATA_CHILD, readContents("2.1", child));
		assertEquals(DATA_CHILD, readContents("3.1", both));
		URL resolvedURL = FileLocator.resolve(none);
		assertNotEquals(none, resolvedURL);
		assertTrue(
				resolvedURL.toExternalForm().startsWith(Platform.getConfigurationLocation().getURL().toExternalForm()));
	}

	private static String readContents(String tag, URL url) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			return reader.lines().collect(Collectors.joining());
		}
	}

}
