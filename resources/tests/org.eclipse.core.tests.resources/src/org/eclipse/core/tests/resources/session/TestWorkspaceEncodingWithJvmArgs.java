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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that encoding is set according to jvm arguments
 */
public class TestWorkspaceEncodingWithJvmArgs {

	private static final String CHARSET = "ASCII";

	@RegisterExtension
	SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@BeforeEach
	@ExecuteInHost
	public void setUpSession() {
		sessionTestExtension.setSystemProperty("file.encoding", CHARSET);
	}

	@Test
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
