/*******************************************************************************
 *  Copyright (c) 2012, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.preferences;

import java.io.*;
import junit.framework.Test;
import junit.framework.TestCase;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.eclipse.core.tests.session.*;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Test for bug 388004.
 */
public class TestBug388004 extends TestCase {
	private static final String FILE_NAME = FileSystemHelper.getTempDir().append("plugin_customization.ini").toOSString();
	private static final String NODE = "dummy_node";
	private static final String KEY = "key";
	private static final String VALUE = "value";

	public static Test suite() {
		SessionTestSuite suite = new SessionTestSuite(RuntimeTestsPlugin.PI_RUNTIME_TESTS, TestBug388004.class.getName());
		try {
			// create plugin_customization.ini file
			File file = new File(FILE_NAME);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				writer.write("org.eclipse.core.tests.runtime/dummy_node/key=value");
			}

			// add pluginCustomization argument
			Setup setup = suite.getSetup();
			setup.setEclipseArgument("pluginCustomization", file.toString());
		} catch (IOException | SetupException e) {
			// ignore, the test will fail for us
		}
		suite.addTest(new TestBug388004("testBug"));
		return suite;
	}

	public TestBug388004() {
		super();
	}

	public TestBug388004(String name) {
		super(name);
	}

	@Override
	protected void tearDown() throws Exception {
		new File(FILE_NAME).delete();
	}

	public void testBug() throws BackingStoreException {
		Preferences node = Platform.getPreferencesService().getRootNode().node(DefaultScope.SCOPE);

		// test relative path of ancestor
		if (!node.nodeExists(RuntimeTestsPlugin.PI_RUNTIME_TESTS))
			fail("This node exists in pluginCustomization file.");
		// test absolute path of ancestor
		if (!node.nodeExists("/default/" + RuntimeTestsPlugin.PI_RUNTIME_TESTS))
			fail("This node exists in pluginCustomization file.");

		// test relative path
		if (!node.nodeExists(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/" + NODE))
			fail("This node exists in pluginCustomization file.");
		// test absolute path
		if (!node.nodeExists("/default/" + RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/" + NODE))
			fail("This node exists in pluginCustomization file.");

		// test relative path of non-existing node
		if (node.nodeExists(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/" + NODE + "/" + KEY))
			fail("This node does not exist in pluginCustomization file.");
		// test absolute path of non-existing node
		if (node.nodeExists("/default/" + RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/" + NODE + "/" + KEY))
			fail("This node does not exist in pluginCustomization file.");

		node = node.node(RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/" + NODE);
		String value = node.get(KEY, null);
		assertEquals(VALUE, value);
	}
}
