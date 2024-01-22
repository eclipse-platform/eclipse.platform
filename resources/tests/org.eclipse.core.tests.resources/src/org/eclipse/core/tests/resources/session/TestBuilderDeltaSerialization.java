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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setBuildOrder;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;

import java.io.ByteArrayInputStream;
import junit.framework.Test;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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

		setBuildOrder(project1, project2);
		setAutoBuilding(false);

		// configure builder for project1
		updateProjectDescription(project1).addingCommand(SortBuilder.BUILDER_NAME).withTestBuilderId("Project1Build1")
				.withAdditionalBuildArgument(TestBuilder.INTERESTING_PROJECT, project2.getName()).apply();

		// configure builder for project2
		updateProjectDescription(project2).addingCommand(SortBuilder.BUILDER_NAME).withTestBuilderId("Project2Build1")
				.withAdditionalBuildArgument(TestBuilder.INTERESTING_PROJECT, project1.getName()).apply();

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
		assertThat(builders).hasSize(2).satisfiesExactly(first -> {
			assertThat(first.wasBuilt()).isTrue();
			assertThat(first.wasIncrementalBuild()).isTrue();
		}, second -> assertThat(second.wasAutoBuild()).isFalse());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBuilderDeltaSerialization.class);
	}

}
