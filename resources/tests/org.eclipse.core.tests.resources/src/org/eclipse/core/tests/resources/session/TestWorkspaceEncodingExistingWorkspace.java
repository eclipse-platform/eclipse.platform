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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.Test;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests that explicit workspace encoding not set if there are projects defined
 */
public class TestWorkspaceEncodingExistingWorkspace extends WorkspaceSessionTest {

	public static Test suite() {
		WorkspaceSessionTestSuite suite = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS,
				TestWorkspaceEncodingExistingWorkspace.class);
		Path wspRoot = suite.getInstanceLocation().toFile().toPath();
		Path projectsTree = wspRoot.resolve(".metadata/.plugins/org.eclipse.core.resources/.projects");
		try {
			Files.createDirectories(projectsTree);
		} catch (IOException e) {
			fail("Unable to create directories: " + projectsTree, e);
		}
		return suite;
	}

	public TestWorkspaceEncodingExistingWorkspace() {
		super();
	}

	public void testExpectedEncoding1() throws Exception {
		String defaultValue = System.getProperty("file.encoding");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		// Should be system default
		assertEquals(Charset.forName(defaultValue), Charset.forName(ResourcesPlugin.getEncoding()));
		assertEquals(Charset.forName(defaultValue), Charset.forName(workspace.getRoot().getDefaultCharset(true)));

		// and not defined in workspace
		String charset = workspace.getRoot().getDefaultCharset(false);
		assertEquals(null, charset);
	}

}
