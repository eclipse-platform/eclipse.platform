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

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.tests.harness.session.CustomSessionWorkspace;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that explicit workspace encoding not set if there are projects defined
 */
public class TestWorkspaceEncodingExistingWorkspace {

	private CustomSessionWorkspace sessionWorkspace = SessionTestExtension.createCustomWorkspace();

	@RegisterExtension
	SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(sessionWorkspace).create();

	@BeforeEach
	@ExecuteInHost
	public void setUpWorkspace() throws IOException {
		Path projectsTree = sessionWorkspace.getWorkspaceDirectory().resolve(".metadata/.plugins/org.eclipse.core.resources/.projects");
		Files.createDirectories(projectsTree);
	}

	@Test
	public void testExpectedEncoding1() throws Exception {
		String defaultValue = System.getProperty("native.encoding");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		// Should be system default
		assertEquals(Charset.forName(defaultValue), Charset.forName(ResourcesPlugin.getEncoding()));
		assertEquals(Charset.forName(defaultValue), Charset.forName(workspace.getRoot().getDefaultCharset(true)));

		// and not defined in workspace
		String charset = workspace.getRoot().getDefaultCharset(false);
		assertEquals(null, charset);
	}

}
