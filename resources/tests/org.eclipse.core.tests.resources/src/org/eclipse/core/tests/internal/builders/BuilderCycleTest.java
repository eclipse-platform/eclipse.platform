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

import org.eclipse.core.resources.*;
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

	public void testIsBeforeThisProject() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		IProject before1 = root.getProject("Before1");
		IProject before2 = root.getProject("Before2");
		IProject after1 = root.getProject("After1");
		IProject after2 = root.getProject("After2");
		ensureExistsInWorkspace(new IResource[] {project, before1, before2, after1, after2}, true);

		try {
			setBuildOrder(before1, before2, project, after1, after2);
			setAutoBuilding(false);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		try {
			IProjectDescription description = project.getDescription();
			ICommand command1 = createCommand(description, CycleBuilder.BUILDER_NAME, "Build0");
			description.setBuildSpec(new ICommand[] {command1});
			project.setDescription(description, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		CycleBuilder builder = CycleBuilder.getInstance();
		builder.resetBuildCount();
		builder.setBeforeProjects(new IProject[] {before1, before2});
		builder.setAfterProjects(new IProject[] {after1, after2});

		try {
			//create a file to ensure incremental build is called
			project.getFile("Foo.txt").create(getRandomContents(), IResource.NONE, getMonitor());
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			builder.resetBuildCount();
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
	}

	public void skipTestNeedRebuild() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		IFolder unsorted = project.getFolder(SortBuilder.DEFAULT_UNSORTED_FOLDER);
		IFile unsortedFile = unsorted.getFile("File.txt");
		ensureExistsInWorkspace(project, true);
		ensureExistsInWorkspace(unsorted, true);
		ensureExistsInWorkspace(unsortedFile, true);

		//setup so that the sortbuilder and cycle builder are both touching files in the project
		try {
			setAutoBuilding(true);
			IProjectDescription description = project.getDescription();
			ICommand command1 = createCommand(description, CycleBuilder.BUILDER_NAME, "Build0");
			ICommand command2 = createCommand(description, SortBuilder.BUILDER_NAME, "Build1");
			description.setBuildSpec(new ICommand[] {command1, command2});
			project.setDescription(description, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		CycleBuilder builder = CycleBuilder.getInstance();
		builder.resetBuildCount();
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		//don't request rebuilds and ensure we're only called once
		builder.setRebuildsToRequest(0);
		builder.resetBuildCount();
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
		assertEquals("4.1", 1, builder.getBuildCount());

		//force an incremental build
		IFile file = project.getFile("foo.txt");
		builder.resetBuildCount();
		try {
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("4.2", e);
		}
		assertEquals("4.3", 1, builder.getBuildCount());

		//request 1 rebuild and ensure we're called twice
		builder.setRebuildsToRequest(1);
		builder.resetBuildCount();
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}
		assertEquals("5.1", 2, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		try {
			file.setContents(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("5.2", e);
		}
		assertEquals("5.3", 2, builder.getBuildCount());

		//request 5 rebuilds and ensure we're called six times
		builder.setRebuildsToRequest(5);
		builder.resetBuildCount();
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("6.0", e);
		}
		assertEquals("6.1", 6, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		try {
			file.setContents(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("6.2", e);
		}
		assertEquals("6.3", 6, builder.getBuildCount());

		//request many rebuilds and ensure we're called according to the build policy
		int maxBuilds = getWorkspace().getDescription().getMaxBuildIterations();
		builder.setRebuildsToRequest(maxBuilds * 2);
		builder.resetBuildCount();
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}
		assertEquals("7.1", maxBuilds, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		try {
			file.setContents(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("7.2", e);
		}
		assertEquals("7.3", maxBuilds, builder.getBuildCount());

		//change the rebuild policy and ensure we're called the correct number of times
		maxBuilds = 7;
		try {
			IWorkspaceDescription desc = getWorkspace().getDescription();
			desc.setMaxBuildIterations(maxBuilds);
			getWorkspace().setDescription(desc);
		} catch (CoreException e) {
			fail("8.0", e);
		}
		builder.setRebuildsToRequest(maxBuilds * 2);
		builder.resetBuildCount();
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("8.1", e);
		}
		assertEquals("8.2", maxBuilds, builder.getBuildCount());

		//force an incremental build
		builder.resetBuildCount();
		try {
			file.setContents(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("8.3", e);
		}
		assertEquals("8.4", maxBuilds, builder.getBuildCount());

	}
}
