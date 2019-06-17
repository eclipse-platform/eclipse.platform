/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.team.internal.ccvs.ui.model.AllRootsElement;
import org.eclipse.team.internal.ccvs.ui.model.BranchCategory;
import org.eclipse.team.internal.ccvs.ui.model.CVSTagElement;
import org.eclipse.team.internal.ccvs.ui.model.RemoteContentProvider;
import org.eclipse.team.internal.ccvs.ui.model.RemoteModule;
import org.eclipse.team.internal.ccvs.ui.model.VersionCategory;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class RepositoriesViewTests extends EclipseTest {

	public RepositoriesViewTests(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// clear repository root cache
		RepositoryRoot repositoryRoot = getRepositoryRoot();
		String remotePaths[] = repositoryRoot.getKnownRemotePaths();
		for (String remotePath : remotePaths) {
			repositoryRoot.removeTags(remotePath, repositoryRoot.getAllKnownTags(remotePath));
		}
	}

	private RepositoryRoot getRepositoryRoot() {
		RemoteContentProvider rcp = new RemoteContentProvider();
		AllRootsElement are = new AllRootsElement();
		Object[] repositoryRoots = rcp.getElements(are);
		for (Object root : repositoryRoots) {
			RepositoryRoot repositoryRoot = (RepositoryRoot) root;
			if (getRepository().equals(repositoryRoot.getRoot())) {
				return repositoryRoot;
			}
		}
		fail(getRepository() + " not found");
		return null;
	}

	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(RepositoriesViewTests.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new RepositoriesViewTests(testName));
		}
	}

	public void testBranchSubmoduleChildren() throws TeamException,
			CoreException {

		String time = Long.toString(System.currentTimeMillis());
		String moduleName = "TestBranchSubmoduleChildrenTestModule" + time;
		String branchName = "TestBranchSubmoduleChildrenBranch" + time;
		String versionName = "Root_" + branchName;

		// create project
		IProject project = getUniqueTestProject("TestBranchSubmoduleChildrenProject");
		// share project under module
		shareProject(getRepository(), project,
				moduleName + "/" + project.getName(), DEFAULT_MONITOR);
		assertValidCheckout(project);

		// add some files
		addResources(project, new String[] { "file1.txt" }, true);

		// make branch
		CVSTag version = new CVSTag(versionName, CVSTag.VERSION);
		CVSTag branch = new CVSTag(branchName, CVSTag.BRANCH);

		makeBranch(new IResource[] { project }, version, branch, true);

		// check if module is the only branch child
		RemoteContentProvider rcp = new RemoteContentProvider();
		Object[] categories = rcp.getChildren(getRepositoryRoot());
		assertEquals(4, categories.length);
		assertTrue(categories[1] instanceof BranchCategory);
		Object[] branches = rcp.getChildren(categories[1]);
		assertEquals(1, branches.length);
		assertEquals(branchName, ((CVSTagElement) (branches[0])).getTag()
				.getName());
		Object[] modules = rcp.getChildren(branches[0]);
		assertEquals(1, modules.length);
		assertEquals(moduleName, ((RemoteResource) modules[0]).getName());

		// check if after refresh module is still the only branch child
		branches = rcp.getChildren(categories[1]);
		assertEquals(1, branches.length);
		assertEquals(branchName, ((CVSTagElement) (branches[0])).getTag()
				.getName());
		modules = rcp.getChildren(branches[0]);
		assertEquals(1, modules.length);
		assertEquals(moduleName, ((RemoteResource) modules[0]).getName());
	}

	public void testTagSubmoduleChildren() throws TeamException, CoreException {

		String time = Long.toString(System.currentTimeMillis());
		String moduleName = "TestTagSubmoduleChildrenTestModule" + time;
		String versionName = "TestTagSubmoduleChildrenBranch" + time;

		// create project
		IProject project = getUniqueTestProject("TestTagSubmoduleChildrenProject");
		// share project under module
		shareProject(getRepository(), project,
				moduleName + "/" + project.getName(), DEFAULT_MONITOR);
		assertValidCheckout(project);

		// make some changes
		addResources(project, new String[] { "file1.txt" }, true);

		// tag project
		CVSTag tag = new CVSTag(versionName, CVSTag.VERSION);

		tagProject(project, tag, true);


		RemoteContentProvider rcp = new RemoteContentProvider();
		Object[] categories = rcp.getChildren(getRepositoryRoot());
		assertEquals(4, categories.length);

		// check if version exists for module
		assertTrue(categories[2] instanceof VersionCategory);
		Object[] modules = rcp.getChildren(categories[2]);
		for (Object module : modules) {
			if (module instanceof RemoteModule && ((RemoteModule) (module)).getCVSResource().getName().equals(moduleName)) {
				Object[] folders = rcp.getChildren(module);
				assertEquals(1, folders.length);
				assertEquals(versionName, ((RemoteFolder) folders[0]).getTag()
						.getName());
				return;
			}
		}
		fail(moduleName + " not found");
	}

	public void testTagsOnDifferentLevels() throws CoreException {
		String time = Long.toString(System.currentTimeMillis());
		String firstModule = "Module_1" + time;
		String secondModule = "Module_2" + time;
		String secondModulePath = firstModule + "/" + secondModule;
		// Create repository data
		// Module_1/Project_1
		IProject project1 = getUniqueTestProject("Project_1");
		shareProject(getRepository(), project1,
				firstModule + "/" + project1.getName(), DEFAULT_MONITOR);
		// Module_1/Module_2/Project_2
		IProject project2 = getUniqueTestProject("Project_2");
		shareProject(getRepository(), project2, secondModulePath + "/"
				+ project2.getName(), DEFAULT_MONITOR);
		// Module_1/Module_2/Project_3
		IProject project3 = getUniqueTestProject("Project_3");
		shareProject(getRepository(), project3, secondModulePath + "/"
				+ project3.getName(), DEFAULT_MONITOR);
		// Module_1/Project_4
		IProject project4 = getUniqueTestProject("Project_4");
		shareProject(getRepository(), project4,
				firstModule + "/" + project4.getName(), DEFAULT_MONITOR);

		// Create branches
		String branch1 = "Branch_1" + time;
		String version1 = "Root_" + branch1;
		String branch2 = "Branch_2" + time;
		String version2 = "Root_" + branch2;

		// Tag projects:
		// Module_1/Project_1 -> [Branch_1][Branch_2]
		// Module_1/Module_2/Project_2 -> [Branch_1][Branch_2]
		// Module_1/Module_2/Project_3 -> [Branch_2]
		// Module_1/Project_4 -> [Branch_4]
		makeBranch(new IResource[] { project1, project2 }, new CVSTag(version1,
				CVSTag.VERSION), new CVSTag(branch1, CVSTag.BRANCH), true);
		makeBranch(new IResource[] { project1, project2, project2, project4 },
				new CVSTag(version2, CVSTag.VERSION), new CVSTag(branch2,
						CVSTag.BRANCH), true);

		// verify if tree structure is built from cache
		RemoteContentProvider rcp = new RemoteContentProvider();
		Object[] categories = rcp.getChildren(getRepositoryRoot());
		assertEquals(4, categories.length);
		assertTrue(categories[1] instanceof BranchCategory);
		Object[] branches = rcp.getChildren(categories[1]);
		assertEquals(2, branches.length); // should be [Branch_1] and [Branch_2]
		CVSTagElement branch1Element;
		CVSTagElement branch2Element;
		if (((CVSTagElement) branches[0]).getTag().getName().equals(branch1)) {
			branch1Element = (CVSTagElement) branches[0];
			branch2Element = (CVSTagElement) branches[1];
		} else {
			branch1Element = (CVSTagElement) branches[1];
			branch2Element = (CVSTagElement) branches[0];
		}
		Object[] modules = rcp.getChildren(branch1Element);
		assertEquals(1, modules.length); // should be [Branch_1]/Module_1
		assertEquals(firstModule, ((RemoteResource) modules[0]).getName());
		modules = rcp.getChildren(modules[0]);
		// should contain:
		// [Branch_1]/Module_1/Project_1
		// [Branch_1]/Module_1/Module_2
		assertEquals(2, modules.length);
		for (Object module : modules) {
			if (((RemoteResource) (module)).getName().equals(project1.getName())) {
				// Project_1 should have contents retrieved from CVS
				assertTrue(rcp.hasChildren(module));
			} else if (((RemoteResource) (module)).getName().equals(secondModule)) {
				// should be only [Branch_1]/Module_1/Module_2/Project_2.
				// [Branch_1]/Module_1/Module_2/Project_3 should NOT be on the
				// list, it is not branched with Branch_1
				Object[] module2Children = rcp.getChildren(module);
				assertEquals(1, module2Children.length);
				assertEquals(project2.getName(),
						((RemoteResource) module2Children[0]).getName());
			}
		}
		modules = rcp.getChildren(branch2Element);
		assertEquals(1, modules.length); // should be [Branch_2]/Module_1
		assertEquals(firstModule, ((RemoteResource) modules[0]).getName());
		// should contain:
		// [Branch_2]/Module_1/Project_1
		// [Branch_2]/Module_1/Module_2
		// [Branch_2]/Module_1/Project_4
		modules = rcp.getChildren(modules[0]);
		assertEquals(3, modules.length);
	}
}
