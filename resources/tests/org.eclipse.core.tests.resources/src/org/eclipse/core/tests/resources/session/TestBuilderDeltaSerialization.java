/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;

import java.io.ByteArrayInputStream;
import java.util.Map;
import junit.framework.Test;
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
import org.eclipse.core.tests.internal.builders.SortBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests that builder deltas are correctly serialized.
 */
public class TestBuilderDeltaSerialization extends WorkspaceSerializationTest {
	//various resource handles
	private IProject project1, project2;
	private IFolder sorted1, sorted2, unsorted1, unsorted2;
	private IFile unsortedFile1, unsortedFile2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IWorkspaceRoot root = getWorkspace().getRoot();
		project1 = root.getProject("Project1");
		unsorted1 = project1.getFolder(SortBuilder.UNSORTED_FOLDER);
		sorted1 = project1.getFolder(SortBuilder.SORTED_FOLDER);
		unsortedFile1 = unsorted1.getFile("File1");

		project2 = root.getProject("Project2");
		unsorted2 = project2.getFolder(SortBuilder.UNSORTED_FOLDER);
		sorted2 = project2.getFolder(SortBuilder.SORTED_FOLDER);
		unsortedFile2 = unsorted2.getFile("File2");
	}

	/**
	 * Create projects, setup a builder, and do an initial build.
	 */
	public void test1() throws CoreException {
		IResource[] resources = {project1, project2, unsorted1, unsorted2, sorted1, sorted2, unsortedFile1, unsortedFile2};
		createInWorkspace(resources);

		// give unsorted files some initial content
		unsortedFile1.setContents(new ByteArrayInputStream(new byte[] { 1, 4, 3 }), true, true, null);
		unsortedFile2.setContents(new ByteArrayInputStream(new byte[] { 1, 4, 3 }), true, true, null);

		// set build order
		IWorkspaceDescription workspaceDescription = workspace.getDescription();
		workspaceDescription.setBuildOrder(new String[] { project1.getName(), project2.getName() });
		workspace.setDescription(workspaceDescription);
		setAutoBuilding(false);

		// configure builder for project1
		IProjectDescription description = project1.getDescription();
		ICommand command = description.newCommand();
		Map<String, String> args = command.getArguments();
		args.put(TestBuilder.BUILD_ID, "Project1Build1");
		args.put(TestBuilder.INTERESTING_PROJECT, project2.getName());
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		command.setArguments(args);
		description.setBuildSpec(new ICommand[] { command });
		project1.setDescription(description, createTestMonitor());

		// configure builder for project2
		description = project1.getDescription();
		command = description.newCommand();
		args = command.getArguments();
		args.put(TestBuilder.BUILD_ID, "Project2Build1");
		args.put(TestBuilder.INTERESTING_PROJECT, project1.getName());
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		command.setArguments(args);
		description.setBuildSpec(new ICommand[] { command });
		project2.setDescription(description, createTestMonitor());

		// initial build -- created sortedFile1 and sortedFile2
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());

		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Do another build immediately after restart.  Builder1 should be invoked because it cares
	 * about changes made by Builder2 during the last build phase.
	 */
	public void test2() throws CoreException {
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		//Only builder1 should have been built
		SortBuilder[] builders = SortBuilder.allInstances();
		assertEquals("1.0", 2, builders.length);
		assertTrue("1.1", builders[0].wasBuilt());
		assertTrue("1.2", builders[0].wasIncrementalBuild());
		assertTrue("1.3", !builders[1].wasBuilt());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBuilderDeltaSerialization.class);
	}

}
