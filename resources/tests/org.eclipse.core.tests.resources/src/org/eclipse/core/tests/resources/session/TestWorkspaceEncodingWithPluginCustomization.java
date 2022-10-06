/*******************************************************************************
 * Copyright (c) 2022 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import java.io.*;
import junit.framework.Test;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.Setup;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests that encoding is set according to plugin customization
 */
public class TestWorkspaceEncodingWithPluginCustomization extends WorkspaceSessionTest {

	private static final String CHARSET = "UTF-16";
	private static final String FILE_NAME = FileSystemHelper.getTempDir().append("plugin_customization_encoding.ini").toOSString();

	public static Test suite() {
		WorkspaceSessionTestSuite suite = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS,
				TestWorkspaceEncodingWithPluginCustomization.class);
		try {
			// create plugin_customization.ini file
			File file = new File(FILE_NAME);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				writer.write("org.eclipse.core.resources/encoding=" + CHARSET);
			}

			// add pluginCustomization argument
			Setup setup = suite.getSetup();
			setup.setEclipseArgument("pluginCustomization", file.toString());
		} catch (IOException | SetupException e) {
			// ignore, the test will fail for us
		}
		return suite;
	}

	public TestWorkspaceEncodingWithPluginCustomization() {
		super();
	}

	public void testExpectedEncoding() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// Should be system default
		assertEquals(CHARSET, ResourcesPlugin.getEncoding());
		assertEquals(CHARSET, workspace.getRoot().getDefaultCharset(true));

		// and defined in workspace because it is the custom product preference
		String charset = workspace.getRoot().getDefaultCharset(false);
		assertEquals(CHARSET, charset);
	}
}
