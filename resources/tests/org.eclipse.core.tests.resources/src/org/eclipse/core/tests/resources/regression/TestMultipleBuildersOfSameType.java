/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;

import java.io.ByteArrayInputStream;
import java.util.Map;
import junit.framework.Test;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.SortBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests that multiple builders of the same type can be installed on a single
 * project, and that their deltas are correctly serialized and associated with
 * the correct builder in the next session.
 */
public class TestMultipleBuildersOfSameType extends WorkspaceSessionTest {
	//various resource handles
	private IProject project1;
	private IFolder sorted1, unsorted1;
	private IFile unsortedFile1;

	@Override
	protected void setUp() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		project1 = root.getProject("Project1");
		unsorted1 = project1.getFolder(SortBuilder.UNSORTED_FOLDER);
		sorted1 = project1.getFolder(SortBuilder.SORTED_FOLDER);
		unsortedFile1 = unsorted1.getFile("File1");
	}

	/**
	 * Create projects, setup a builder, and do an initial build.
	 */
	public void test1() throws CoreException {
		IResource[] resources = {project1, unsorted1, sorted1, unsortedFile1};
		ensureExistsInWorkspace(resources, true);

		// give unsorted files some initial content
		unsortedFile1.setContents(new ByteArrayInputStream(new byte[] { 1, 4, 3 }), true, true, null);

		setAutoBuilding(false);

		// configure builder for project1
		IProjectDescription description = project1.getDescription();
		description.setBuildSpec(new ICommand[] { createCommand(description, "Project1Build1"),
				createCommand(description, "Project1Build2") });
		project1.setDescription(description, getMonitor());

		// initial build -- created sortedFile1
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());

		getWorkspace().save(true, getMonitor());
	}

	protected ICommand createCommand(IProjectDescription description, String builderId) {
		ICommand command = description.newCommand();
		Map<String, String> args = command.getArguments();
		args.put(TestBuilder.BUILD_ID, builderId);
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		command.setArguments(args);
		return command;
	}

	/**
	 * Do another build immediately after restart.  Builder1 should be invoked because it cares
	 * about changes made by Builder2 during the last build phase.
	 */
	public void test2() throws CoreException {
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		//Only builder1 should have been built
		SortBuilder[] builders = SortBuilder.allInstances();
		assertEquals("1.0", 2, builders.length);
		assertTrue("1.1", builders[0].wasBuilt());
		assertTrue("1.2", builders[0].wasIncrementalBuild());
		assertTrue("1.3", !builders[1].wasBuilt());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestMultipleBuildersOfSameType.class);
	}

}
