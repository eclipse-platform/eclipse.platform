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

import java.io.*;
import junit.framework.Test;
import junit.framework.TestCase;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.eclipse.core.tests.session.*;
import org.eclipse.core.tests.session.SetupManager.SetupException;

/**
 * Test for bug 380859.
 */
public class TestBug380859 extends TestCase {
	private static final String NOT_FOUND = "not_found";
	private static final String FILE_NAME = FileSystemHelper.getTempDir().append("plugin_customization_380859.ini").toOSString();

	public static Test suite() {
		SessionTestSuite suite = new SessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, TestBug380859.class.getName());
		try {
			// create plugin_customization.ini file
			File file = new File(FILE_NAME);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/a=v1\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "//b=v2\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "///c=v3\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "////d=v4\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/a/b=v5\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/c//d=v6\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "//e//f=v7\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/a/b/c=v8\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/a/b//c/d=v9\n");
				writer.write(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/a/b//c//d=v10\n");
			}

			// add pluginCustomization argument
			Setup setup = suite.getSetup();
			setup.setEclipseArgument("pluginCustomization", file.toString());
		} catch (IOException | SetupException e) {
			// ignore, the test will fail for us
		}
		suite.addTest(new TestBug380859("testBug"));
		return suite;
	}

	public TestBug380859() {
		super();
	}

	public TestBug380859(String name) {
		super(name);
	}

	@Override
	protected void tearDown() throws Exception {
		new File(FILE_NAME).delete();
	}

	public void testBug() {
		IPreferencesService preferenceService = Platform.getPreferencesService();
		IScopeContext[] defaultScope = {DefaultScope.INSTANCE};

		assertEquals("v1", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "a", NOT_FOUND, defaultScope));
		assertEquals("v1", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/a", NOT_FOUND, defaultScope));
		assertEquals("v1", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "//a", NOT_FOUND, defaultScope));

		assertEquals("v2", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "b", NOT_FOUND, defaultScope));
		assertEquals("v2", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/b", NOT_FOUND, defaultScope));
		assertEquals("v2", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "//b", NOT_FOUND, defaultScope));

		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "c", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/c", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "//c", NOT_FOUND, defaultScope));
		assertEquals("v3", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "///c", NOT_FOUND, defaultScope));

		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "//d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "///d", NOT_FOUND, defaultScope));
		assertEquals("v4", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "////d", NOT_FOUND, defaultScope));

		assertEquals("v5", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "a/b", NOT_FOUND, defaultScope));
		assertEquals("v5", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/a/b", NOT_FOUND, defaultScope));
		assertEquals("v5", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "a//b", NOT_FOUND, defaultScope));
		assertEquals("v5", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/a//b", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "//a//b", NOT_FOUND, defaultScope));

		assertEquals("v6", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "c/d", NOT_FOUND, defaultScope));
		assertEquals("v6", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/c/d", NOT_FOUND, defaultScope));
		assertEquals("v6", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "c//d", NOT_FOUND, defaultScope));
		assertEquals("v6", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/c//d", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "//c//d", NOT_FOUND, defaultScope));

		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "e/f", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/e/f", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "e//f", NOT_FOUND, defaultScope));
		assertEquals(NOT_FOUND, preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/e//f", NOT_FOUND, defaultScope));
		assertEquals("v7", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "//e//f", NOT_FOUND, defaultScope));

		assertEquals("v8", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "a/b/c", NOT_FOUND, defaultScope));
		assertEquals("v8", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/a/b/c", NOT_FOUND, defaultScope));
		assertEquals("v8", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "a/b//c", NOT_FOUND, defaultScope));
		assertEquals("v8", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/a/b//c", NOT_FOUND, defaultScope));

		assertEquals("v9", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "a/b//c/d", NOT_FOUND, defaultScope));
		assertEquals("v9", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/a/b//c/d", NOT_FOUND, defaultScope));

		assertEquals("v10", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "a/b//c//d", NOT_FOUND, defaultScope));
		assertEquals("v10", preferenceService.getString(RuntimeTestsPlugin.PI_RUNTIME_TESTS, "/a/b//c//d", NOT_FOUND, defaultScope));
	}
}
