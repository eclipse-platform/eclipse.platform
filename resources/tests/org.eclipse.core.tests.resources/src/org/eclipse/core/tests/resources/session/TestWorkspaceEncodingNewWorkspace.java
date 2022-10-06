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
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests that encoding is set to UTF-8 in an empty workspace and only if no
 * preference set already
 */
public class TestWorkspaceEncodingNewWorkspace extends WorkspaceSessionTest {

	private static final String CHARSET = "UTF-16";

	public static Test suite() {
		WorkspaceSessionTestSuite suite = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS,
				TestWorkspaceEncodingNewWorkspace.class);
		// no special setup
		return suite;
	}

	public TestWorkspaceEncodingNewWorkspace() {
		super();
	}

	public void testExpectedEncoding1() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String charset = workspace.getRoot().getDefaultCharset(false);
		// Should be default
		assertEquals("UTF-8", charset);
		// Set something else
		workspace.getRoot().setDefaultCharset(CHARSET, getMonitor());
		workspace.save(true, getMonitor());
	}

	public void testExpectedEncoding2() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String charset = workspace.getRoot().getDefaultCharset(false);
		// Shouldn't be changed anymore
		assertEquals(CHARSET, charset);
	}
}
