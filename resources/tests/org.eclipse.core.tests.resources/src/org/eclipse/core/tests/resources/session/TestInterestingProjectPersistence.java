/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * When a builder is run, it reports what projects it is interested in obtaining
 * deltas for the next time it is run.  This class tests that this list of "interesting"
 * projects is correctly saved between sessions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestInterestingProjectPersistence {

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	//various resource handles
	private IProject project1;
	private IProject project2;
	private IProject project3;
	private IProject project4;
	private IFile file1;
	private IFile file2;
	private IFile file3;
	private IFile file4;

	@BeforeEach
	public void setUp() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		project1 = root.getProject("Project1");
		project2 = root.getProject("Project2");
		project3 = root.getProject("Project3");
		project4 = root.getProject("Project4");
		file1 = project1.getFile("File1");
		file2 = project2.getFile("File2");
		file3 = project3.getFile("File3");
		file4 = project4.getFile("File4");
	}

	/**
	 * Create projects, setup a builder, and do an initial build.
	 */
	@Test
	@Order(1)
	public void test1() throws CoreException {
		IResource[] resources = {project1, project2, project3, project4, file1, file2, file3, file4};
		createInWorkspace(resources);
		setAutoBuilding(false);

		// create a project and configure builder
		updateProjectDescription(project1).addingCommand(DeltaVerifierBuilder.BUILDER_NAME)
				.withTestBuilderId("Project1Build1").apply();

		// initial build requesting no projects
		project1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Check that "no interesting builders" case suceeded, then ask for more projects.
	 */
	@Test
	@Order(2)
	public void test2() throws CoreException {
		DeltaVerifierBuilder builder = DeltaVerifierBuilder.getInstance();
		builder.checkDeltas(new IProject[] {project1, project2, project3, project4});
		builder.requestDeltas(new IProject[] {project1, project2, project4});
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		ArrayList<IProject> received = builder.getReceivedDeltas();

		// should have received only a delta for project1
		assertThat(received).hasSize(1);
		assertTrue(received.contains(project1));

		// should be no empty deltas
		ArrayList<IProject> empty = builder.getEmptyDeltas();
		assertThat(empty).isEmpty();

		// save
		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Check that interesting projects (1, 2, and 4) were stored and retrieved
	 */
	@Test
	@Order(3)
	public void test3() throws CoreException {
		DeltaVerifierBuilder builder = DeltaVerifierBuilder.getInstance();
		builder.checkDeltas(new IProject[] {project1, project2, project3, project4});
		// dirty projects 1, 2, 3
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file2.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file3.setContents(createRandomContentsStream(), true, true, createTestMonitor());

		// build
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		ArrayList<IProject> received = builder.getReceivedDeltas();

		// should have received deltas for 1, 2, and 4
		assertEquals(3, received.size());
		assertTrue(received.contains(project1));
		assertTrue(received.contains(project2));
		assertFalse(received.contains(project3));
		assertTrue(received.contains(project4));

		// delta for project4 should be empty
		ArrayList<IProject> empty = builder.getEmptyDeltas();
		assertEquals(1, empty.size());
		assertTrue(empty.contains(project4));

		// save
		getWorkspace().save(true, createTestMonitor());
	}

}
