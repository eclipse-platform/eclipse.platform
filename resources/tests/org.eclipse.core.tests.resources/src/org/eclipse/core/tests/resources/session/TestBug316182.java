/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Test for bug 316182
 */
public class TestBug316182 extends WorkspaceSessionTest {
	public static Exception CAUGHT_EXCEPTION = null;

	public void test01_prepareWorkspace() throws CoreException {
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, true);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project_TestBug316182");
		ensureExistsInWorkspace(project, true);
		workspace.save(true, getMonitor());
		// reset last caught exception
		CAUGHT_EXCEPTION = null;
	}

	public void test02_startWorkspace() {
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		if (CAUGHT_EXCEPTION != null) {
			fail("Test failed", CAUGHT_EXCEPTION);
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug316182.class);
	}
}
