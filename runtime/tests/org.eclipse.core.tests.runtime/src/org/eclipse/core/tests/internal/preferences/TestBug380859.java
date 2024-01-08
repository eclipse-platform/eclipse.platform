/*******************************************************************************
 *  Copyright (c) 2013, 2015 Google Inc and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Thirumala Reddy Mutchukota, Google Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.preferences;

import static org.eclipse.core.tests.runtime.RuntimeTestsPlugin.PI_RUNTIME_TESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for bug 380859.
 */
public class TestBug380859 {
	private static final String CUSTOMIZATION_FILE_NAME = "plugin_customization_380859.ini";
	private static final String NOT_FOUND = "not_found";

	@TempDir
	static Path tempDirectory;

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RUNTIME_TESTS).create();

	@BeforeAll
	public static void createCustomizationFile() throws IOException {
		Path customizationFilePath = tempDirectory.resolve(CUSTOMIZATION_FILE_NAME);
		try (BufferedWriter writer = Files.newBufferedWriter(customizationFilePath)) {
			writer.write(PI_RUNTIME_TESTS + "/a=v1\n");
			writer.write(PI_RUNTIME_TESTS + "//b=v2\n");
			writer.write(PI_RUNTIME_TESTS + "///c=v3\n");
			writer.write(PI_RUNTIME_TESTS + "////d=v4\n");
			writer.write(PI_RUNTIME_TESTS + "/a/b=v5\n");
			writer.write(PI_RUNTIME_TESTS + "/c//d=v6\n");
			writer.write(PI_RUNTIME_TESTS + "//e//f=v7\n");
			writer.write(PI_RUNTIME_TESTS + "/a/b/c=v8\n");
			writer.write(PI_RUNTIME_TESTS + "/a/b//c/d=v9\n");
			writer.write(PI_RUNTIME_TESTS + "/a/b//c//d=v10\n");
		}
		sessionTestExtension.setEclipseArgument("pluginCustomization", customizationFilePath.toString());
	}

	@Test
	public void testBug() {
		IPreferencesService preferenceService = Platform.getPreferencesService();
		IScopeContext[] defaultScope = { DefaultScope.INSTANCE };

		assertEquals("v1", preferenceService.getString(PI_RUNTIME_TESTS, "a", NOT_FOUND, defaultScope));
		assertEquals("v1", preferenceService.getString(PI_RUNTIME_TESTS, "/a", NOT_FOUND, defaultScope));
		assertEquals("v1", preferenceService.getString(PI_RUNTIME_TESTS, "//a", NOT_FOUND, defaultScope));

		assertEquals("v2", preferenceService.getString(PI_RUNTIME_TESTS, "b", NOT_FOUND, defaultScope));
		assertEquals("v2", preferenceService.getString(PI_RUNTIME_TESTS, "/b", NOT_FOUND, defaultScope));
		assertEquals("v2", preferenceService.getString(PI_RUNTIME_TESTS, "//b", NOT_FOUND, defaultScope));

		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "c", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "/c", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "//c", NOT_FOUND, defaultScope));
		assertEquals("v3", preferenceService.getString(PI_RUNTIME_TESTS, "///c", NOT_FOUND, defaultScope));

		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "/d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "//d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "///d", NOT_FOUND, defaultScope));
		assertEquals("v4", preferenceService.getString(PI_RUNTIME_TESTS, "////d", NOT_FOUND, defaultScope));

		assertEquals("v5", preferenceService.getString(PI_RUNTIME_TESTS, "a/b", NOT_FOUND, defaultScope));
		assertEquals("v5", preferenceService.getString(PI_RUNTIME_TESTS, "/a/b", NOT_FOUND, defaultScope));
		assertEquals("v5", preferenceService.getString(PI_RUNTIME_TESTS, "a//b", NOT_FOUND, defaultScope));
		assertEquals("v5", preferenceService.getString(PI_RUNTIME_TESTS, "/a//b", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "//a//b", NOT_FOUND, defaultScope));

		assertEquals("v6", preferenceService.getString(PI_RUNTIME_TESTS, "c/d", NOT_FOUND, defaultScope));
		assertEquals("v6", preferenceService.getString(PI_RUNTIME_TESTS, "/c/d", NOT_FOUND, defaultScope));
		assertEquals("v6", preferenceService.getString(PI_RUNTIME_TESTS, "c//d", NOT_FOUND, defaultScope));
		assertEquals("v6", preferenceService.getString(PI_RUNTIME_TESTS, "/c//d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "//c//d", NOT_FOUND, defaultScope));

		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "e/f", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "/e/f", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "e//f", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(PI_RUNTIME_TESTS, "/e//f", NOT_FOUND, defaultScope));
		assertEquals("v7", preferenceService.getString(PI_RUNTIME_TESTS, "//e//f", NOT_FOUND, defaultScope));

		assertEquals("v8", preferenceService.getString(PI_RUNTIME_TESTS, "a/b/c", NOT_FOUND, defaultScope));
		assertEquals("v8", preferenceService.getString(PI_RUNTIME_TESTS, "/a/b/c", NOT_FOUND, defaultScope));
		assertEquals("v8", preferenceService.getString(PI_RUNTIME_TESTS, "a/b//c", NOT_FOUND, defaultScope));
		assertEquals("v8", preferenceService.getString(PI_RUNTIME_TESTS, "/a/b//c", NOT_FOUND, defaultScope));

		assertEquals("v9", preferenceService.getString(PI_RUNTIME_TESTS, "a/b//c/d", NOT_FOUND, defaultScope));
		assertEquals("v9", preferenceService.getString(PI_RUNTIME_TESTS, "/a/b//c/d", NOT_FOUND, defaultScope));

		assertEquals("v10", preferenceService.getString(PI_RUNTIME_TESTS, "a/b//c//d", NOT_FOUND, defaultScope));
		assertEquals("v10", preferenceService.getString(PI_RUNTIME_TESTS, "/a/b//c//d", NOT_FOUND, defaultScope));
	}
}
