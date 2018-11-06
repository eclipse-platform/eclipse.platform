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

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.operations.CVSOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutMultipleProjectsOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutSingleProjectOperation;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;

public class CheckoutOperationTests extends CVSOperationTest { 

	public CheckoutOperationTests() {
	}

	public CheckoutOperationTests(String name) {
		super(name);
	}

	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(CheckoutOperationTests.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new CheckoutOperationTests(testName));
		}
	}
	public void testSimpleCheckout() throws CoreException, TeamException, IOException {
		IProject project = createProject("testSimpleCheckout", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// move the created project so we can do a simple checkout
		project.move(new Path("moved-project"), false /* force */, DEFAULT_MONITOR);
		IProject movedProject = ResourcesPlugin.getWorkspace().getRoot().getProject("moved-project");
		
		// checkout the project to the default location		
		CVSOperation op = new CheckoutMultipleProjectsOperation(
			null /* shell */, 
			new ICVSRemoteFolder[] { (ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(movedProject) },
			null /*target location*/);
		run(op);
		
		assertEquals(project, movedProject);
	}
	
	public void testNonRootCheckout() throws CoreException, TeamException {
		IProject project = createProject("testNonRootCheckout", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// checkout the non-root folder as a project to the default location		
		CVSOperation op = new CheckoutMultipleProjectsOperation(
			null /* shell */, 
			new ICVSRemoteFolder[] { (ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(project.getFolder("folder1")) },
			null /*target location*/);
		run(op);
		
		IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject("folder1");
		assertTrue(newProject.exists());
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(newProject);
		FolderSyncInfo projectInfo = cvsFolder.getFolderSyncInfo();
		assertTrue(projectInfo != null);
		ICVSFolder cvsFolder2 = CVSWorkspaceRoot.getCVSFolderFor(project.getFolder("folder1"));
		FolderSyncInfo folderInfo = cvsFolder2.getFolderSyncInfo();
		assertTrue(folderInfo != null);
		assertTrue(projectInfo.equals(folderInfo));
	}
	
	public void testMulitpleCheckout() throws CoreException, TeamException {
		IProject project1 = createProject("testNonRootCheckout1", new String[] { "file.txt", "folder1/", "folder1/a.txt" });
		IProject project2 = createProject("testNonRootCheckout2", new String[] { "file2.txt", "folder2/", "folder2/b.txt" });

		// move the created project so we can do a simple checkout
		project1.move(new Path("moved-project1"), false /* force */, DEFAULT_MONITOR);
		IProject movedProject1 = ResourcesPlugin.getWorkspace().getRoot().getProject("moved-project1");
		project2.move(new Path("moved-project2"), false /* force */, DEFAULT_MONITOR);
		IProject movedProject2 = ResourcesPlugin.getWorkspace().getRoot().getProject("moved-project2");


		// checkout the project to the default location		
		CVSOperation op = new CheckoutMultipleProjectsOperation(
			null /* shell */, 
			new ICVSRemoteFolder[] { 
				(ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(movedProject1),
				(ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(movedProject2)
			},
			null /*target location*/);
		run(op);
	}
	
	public void testCheckoutAs() throws TeamException, CoreException, IOException {
		IProject project = createProject("testCheckoutAs", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		IProject copy = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName() + "-copy");
		
		// checkout the project to the default location		
		CVSOperation op = new CheckoutSingleProjectOperation(
			null /* shell */, 
			(ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(project),
			copy,
			null /*target location*/,
			false);
		run(op);
		
		assertEquals(project, copy);
	}

}
