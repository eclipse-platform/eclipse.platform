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

import static org.eclipse.core.tests.runtime.RuntimeTestsPlugin.PI_RUNTIME_TESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Test for bug 388004.
 */
public class TestBug388004 {
	private static final String CUSTOMIZATION_FILE_NAME = "plugin_customization.ini";
	private static final String NODE = "dummy_node";
	private static final String KEY = "key";
	private static final String VALUE = "value";

	@TempDir
	static Path tempDirectory;

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RUNTIME_TESTS).create();

	@BeforeAll
	public static void createCustomizationFile() throws IOException {
		Path customizationFilePath = tempDirectory.resolve(CUSTOMIZATION_FILE_NAME);
		try (BufferedWriter writer = Files.newBufferedWriter(customizationFilePath)) {
			writer.write("org.eclipse.core.tests.runtime/dummy_node/key=value");
		}
		sessionTestExtension.setEclipseArgument("pluginCustomization", customizationFilePath.toString());
	}

	@Test
	public void testBug() throws BackingStoreException {
		Preferences node = Platform.getPreferencesService().getRootNode().node(DefaultScope.SCOPE);

		// test relative path of ancestor
		assertTrue(node.nodeExists(PI_RUNTIME_TESTS), "This node exists in pluginCustomization file.");
		// test absolute path of ancestor
		assertTrue(node.nodeExists("/default/" + PI_RUNTIME_TESTS), "This node exists in pluginCustomization file.");

		// test relative path
		assertTrue(node.nodeExists(PI_RUNTIME_TESTS + "/" + NODE), "This node exists in pluginCustomization file.");
		// test absolute path
		assertTrue(node.nodeExists("/default/" + PI_RUNTIME_TESTS + "/" + NODE),
				"This node exists in pluginCustomization file.");

		// test relative path of non-existing node
		assertFalse(node.nodeExists(PI_RUNTIME_TESTS + "/" + NODE + "/" + KEY),
				"This node does not exist in pluginCustomization file.");
		// test absolute path of non-existing node
		assertFalse(node.nodeExists("/default/" + PI_RUNTIME_TESTS + "/" + NODE + "/" + KEY),
				"This node does not exist in pluginCustomization file.");

		node = node.node(PI_RUNTIME_TESTS + "/" + NODE);
		String value = node.get(KEY, null);
		assertEquals(VALUE, value);
	}
}
