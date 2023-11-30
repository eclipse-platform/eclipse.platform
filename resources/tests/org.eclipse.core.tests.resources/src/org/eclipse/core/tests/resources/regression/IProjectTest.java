/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.internal.builders.AbstractBuilderTest;
import org.eclipse.core.tests.resources.usecase.SignaledBuilder;

public class IProjectTest extends AbstractBuilderTest {
	public IProjectTest(String name) {
		super(name);
	}

	public void test_1G0XIMA() throws CoreException {
		/* common objects */
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		/* define a visitor that renames all resources it visits */
		IResourceVisitor renameVisitor = resource -> {
			if (!resource.exists()) {
				return false;
			}
			IPath path = resource.getFullPath();
			path = path.removeLastSegments(1);
			long timestamp = System.currentTimeMillis();
			path = path.append(resource.getName() + " renamed at " + timestamp);
			resource.move(path, false, null);
			return true;
		};
		/* test */
		project.accept(renameVisitor);
		// cleanup
		project.delete(true, createTestMonitor());
	}

	public void test_1G5I6PV() throws CoreException {
		/* common objects */
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		/* test */
		project.setLocal(true, IResource.DEPTH_ZERO, createTestMonitor());

		// cleanup
		project.delete(true, createTestMonitor());
	}

	/**
	 * 1GC2FKV: ITPCORE:BuildManager triggers incremental build when doing full builds
	 */
	public void testAutoBuild_1GC2FKV() throws CoreException {
		// set auto build OFF
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setAutoBuilding(false);
		getWorkspace().setDescription(description);

		// create project with a builder
		IProject projectONE = getWorkspace().getRoot().getProject("Project_ONE");
		IProjectDescription prjDescription = getWorkspace().newProjectDescription("ProjectONE");
		ICommand command = prjDescription.newCommand();
		command.setBuilderName(SignaledBuilder.BUILDER_ID);
		prjDescription.setBuildSpec(new ICommand[] { command });
		projectONE.create(prjDescription, createTestMonitor());
		projectONE.open(createTestMonitor());

		// create project with a builder
		IProject projectTWO = getWorkspace().getRoot().getProject("Project_TWO");
		prjDescription = getWorkspace().newProjectDescription("Project_TWO");
		command = prjDescription.newCommand();
		command.setBuilderName(SignaledBuilder.BUILDER_ID);
		prjDescription.setBuildSpec(new ICommand[] { command });
		projectTWO.create(prjDescription, createTestMonitor());
		projectTWO.open(createTestMonitor());

		// set auto build ON
		description = getWorkspace().getDescription();
		description.setAutoBuilding(true);
		getWorkspace().setDescription(description);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		waitForBuild();

		SignaledBuilder projectONEbuilder = SignaledBuilder.getInstance(projectONE);
		SignaledBuilder projectTWObuilder = SignaledBuilder.getInstance(projectTWO);
		projectONEbuilder.reset();
		projectTWObuilder.reset();
		projectONE.build(IncrementalProjectBuilder.FULL_BUILD, null);
		waitForBuild();
		assertTrue("1.1", projectONEbuilder.wasExecuted());
		assertTrue("1.2", !projectTWObuilder.wasExecuted());

		projectONEbuilder.reset();
		projectTWObuilder.reset();
		projectTWO.build(IncrementalProjectBuilder.FULL_BUILD, SignaledBuilder.BUILDER_ID, null, null);
		waitForBuild();
		assertTrue("2.1", !projectONEbuilder.wasExecuted());
		assertTrue("2.2", projectTWObuilder.wasExecuted());

		projectONEbuilder.reset();
		projectTWObuilder.reset();
		projectTWO.touch(null);
		waitForBuild();
		//project one won't be executed because project didn't change.
		assertTrue("3.1", !projectONEbuilder.wasExecuted());
		assertTrue("3.2", projectTWObuilder.wasExecuted());
	}

	/*
	 * Create a project with resources already existing on disk and ensure
	 * that the resources are automatically discovered and brought into the workspace.
	 */
	public void testBug78711() throws CoreException {
		String name = getUniqueString();
		IProject project = getWorkspace().getRoot().getProject(name);
		IFolder folder = project.getFolder("folder");
		IFile file1 = project.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");

		IPath location = Platform.getLocation().append(project.getFullPath());
		location.toFile().mkdirs();

		// create in file-system
		location.append(folder.getName()).toFile().mkdirs();
		createFileInFileSystem(location.append(folder.getName()).append(file2.getName()));
		createFileInFileSystem(location.append(file1.getName()));

		// create
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// verify discovery
		assertTrue("2.0", project.isAccessible());
		assertTrue("2.1", folder.exists());
		assertTrue("2.2", file1.exists());
		assertTrue("2.3", file2.exists());
	}

	/**
	 * 1GDW1RX: ITPCORE:ALL - IResource.delete() without force not working correctly
	 */
	public void testDelete_1GDW1RX() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		String[] paths = new String[] {"/1/", "/1/1", "/1/2", "/1/3", "/2/", "/2/1"};
		IResource[] resources = buildResources(project, paths);
		ensureExistsInWorkspace(resources, true);

		IFolder folder = project.getFolder("folder");
		ensureExistsInFileSystem(folder);

		IFile file = folder.getFile("MyFile");
		ensureExistsInFileSystem(file);

		assertThrows(CoreException.class, () -> project.delete(false, createTestMonitor()));

		// clean up
		project.delete(true, true, createTestMonitor());
	}

	public void testRefreshDotProject() throws CoreException {
		String name = getUniqueString();
		IProject project = getWorkspace().getRoot().getProject(name);
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		project.create(null);
		project.open(null);
		touchInFilesystem(dotProject);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IProjectDescription description = project.getDescription();
		description.setComment("Changed description");
		project.setDescription(description, IResource.NONE, null);
	}
}
