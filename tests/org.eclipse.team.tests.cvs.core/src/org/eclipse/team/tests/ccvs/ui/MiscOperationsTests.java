/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.operations.DisconnectOperation;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;

/**
 * Miscellaneous operation tests
 */
public class MiscOperationsTests extends CVSOperationTest {

	public MiscOperationsTests() {
		super();
	}

	public MiscOperationsTests(String name) {
		super(name);
	}

	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(MiscOperationsTests.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new MiscOperationsTests(testName));
		}
	}
	
	public void testDisconnect() throws TeamException, CoreException {
		IProject project = createProject(new String[] { "file1.txt", "folder1/", "folder1/a.txt", "folder1/b.txt"});
		
		// First, disconnect but leave the CVS folders
		run(new DisconnectOperation(null, new IProject[] {project }, false));
		assertNull(RepositoryProvider.getProvider(project));
		assertTrue(project.getFolder("CVS").exists());
		
		// Next, disconnect and purge the CVS folders
		RepositoryProvider.map(project, CVSProviderPlugin.getTypeId());
		run(new DisconnectOperation(null, new IProject[] {project }, true));
		assertNull(RepositoryProvider.getProvider(project));
		assertTrue(project.getFolder("folder1").exists());
		assertTrue(project.getFile("file1.txt").exists());
		assertTrue(!project.getFolder("CVS").exists());
	}
}
