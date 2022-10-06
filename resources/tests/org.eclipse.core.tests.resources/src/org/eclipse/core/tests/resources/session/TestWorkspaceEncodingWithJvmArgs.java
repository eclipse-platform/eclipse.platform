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

import junit.framework.Test;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.Setup;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests that encoding is set according to jvm arguments
 */
public class TestWorkspaceEncodingWithJvmArgs extends WorkspaceSessionTest {

	private static final String CHARSET = "UTF-16";

	public static Test suite() {
		WorkspaceSessionTestSuite suite = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS,
				TestWorkspaceEncodingWithJvmArgs.class);
		try {

			// add pluginCustomization argument
			Setup setup = suite.getSetup();
			setup.setSystemProperty("file.encoding", CHARSET);
		} catch (SetupException e) {
			// ignore, the test will fail for us
		}
		return suite;
	}

	public TestWorkspaceEncodingWithJvmArgs() {
		super();
	}

	public void testExpectedEncoding() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// Should be system default
		assertEquals(CHARSET, ResourcesPlugin.getEncoding());
		assertEquals(CHARSET, workspace.getRoot().getDefaultCharset(true));

		// and also defined in workspace
		String charset = workspace.getRoot().getDefaultCharset(false);
		assertEquals(CHARSET, charset);
	}
}
