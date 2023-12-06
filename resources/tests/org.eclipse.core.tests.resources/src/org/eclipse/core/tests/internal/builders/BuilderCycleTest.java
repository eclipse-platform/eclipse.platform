/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

/**
 * Tests platform support for build cycles.  Namely, the ability of builders to
 * request that a rebuild occur automatically if it modifies projects that came
 * before it in the build order.
 */
public class BuilderCycleTest extends AbstractBuilderTest {
	public BuilderCycleTest(String name) {
		super(name);
	}

	public void testIsBeforeThisProject() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		IProject before1 = root.getProject("Before1");
		IProject before2 = root.getProject("Before2");
		IProject after1 = root.getProject("After1");
		IProject after2 = root.getProject("After2");
		createInWorkspace(new IResource[] {project, before1, before2, after1, after2});

		setBuildOrder(before1, before2, project, after1, after2);
		setAutoBuilding(false);

		IProjectDescription description = project.getDescription();
		ICommand command1 = createCommand(description, CycleBuilder.BUILDER_NAME, "Build0");
		description.setBuildSpec(new ICommand[] { command1 });
		project.setDescription(description, IResource.NONE, createTestMonitor());
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		CycleBuilder builder = CycleBuilder.getInstance();
		builder.resetBuildCount();
		builder.setBeforeProjects(new IProject[] {before1, before2});
		builder.setAfterProjects(new IProject[] {after1, after2});

		// create a file to ensure incremental build is called
		project.getFile("Foo.txt").create(getRandomContents(), IResource.NONE, createTestMonitor());
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		builder.resetBuildCount();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
	}

	public void skipTestNeedRebuild() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		IFolder unsorted = project.getFolder(SortBuilder.DEFAULT_UNSORTED_FOLDER);
		IFile unsortedFile = unsorted.getFile("File.txt");
		createInWorkspace(project);
		createInWorkspace(unsorted);
		createInWorkspace(unsortedFile);

		//setup so that the sortbuilder and cycle builder are both touching files in the project
		setAutoBuilding(true);
		IProjectDescription description = project.getDescription();
		ICommand command1 = createCommand(description, CycleBuilder.BUILDER_NAME, "Build0");
		ICommand command2 = createCommand(description, SortBuilder.BUILDER_NAME, "Build1");
		description.setBuildSpec(new ICommand[] { command1, command2 });
		project.setDescription(description, IResource.NONE, createTestMonitor());

		CycleBuilder builder = CycleBuilder.getInstance();
		builder.resetBuildCount();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		//don't request rebuilds and ensure we're only called once
		builder.setRebuildsToRequest(0);
		builder.resetBuildCount();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, builder.getBuildCount());

		//force an incremental build
		IFile file = project.getFile("foo.txt");
		builder.resetBuildCount();
		file.create(getRandomContents(), IResource.NONE, createTestMonitor());
		assertEquals(1, builder.getBuildCount());

		//request 1 rebuild and ensure we're called twice
		builder.setRebuildsToRequest(1);
		builder.resetBuildCount();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		file.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
		assertEquals(2, builder.getBuildCount());

		//request 5 rebuilds and ensure we're called six times
		builder.setRebuildsToRequest(5);
		builder.resetBuildCount();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(6, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		file.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
		assertEquals(6, builder.getBuildCount());

		//request many rebuilds and ensure we're called according to the build policy
		int maxBuilds = getWorkspace().getDescription().getMaxBuildIterations();
		builder.setRebuildsToRequest(maxBuilds * 2);
		builder.resetBuildCount();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(maxBuilds, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		file.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
		assertEquals(maxBuilds, builder.getBuildCount());

		//change the rebuild policy and ensure we're called the correct number of times
		maxBuilds = 7;
		IWorkspaceDescription desc = getWorkspace().getDescription();
		desc.setMaxBuildIterations(maxBuilds);
		getWorkspace().setDescription(desc);
		builder.setRebuildsToRequest(maxBuilds * 2);
		builder.resetBuildCount();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(maxBuilds, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		file.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
		assertEquals(maxBuilds, builder.getBuildCount());

	}
}
