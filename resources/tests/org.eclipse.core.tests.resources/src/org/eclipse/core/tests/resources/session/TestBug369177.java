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

import java.net.URI;
import java.net.URISyntaxException;
import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Test for bug 369177
 */
public class TestBug369177 extends WorkspaceSessionTest {
	public void test01_prepareWorkspace() throws CoreException, URISyntaxException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		ensureExistsInWorkspace(project, true);

		IFile link = project.getFile("link_to_file");
		link.createLink(new URI("bug369177:/dummy_path.txt"), IResource.ALLOW_MISSING_LOCAL, getMonitor());

		workspace.save(true, getMonitor());
	}

	public void test02_startWorkspace() {
		// nothing to do, if we get this far without an error then we have already passed the test
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug369177.class);
	}
}
