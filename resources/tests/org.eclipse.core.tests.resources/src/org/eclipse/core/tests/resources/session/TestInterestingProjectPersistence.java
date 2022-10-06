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

import java.util.ArrayList;
import java.util.Map;
import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * When a builder is run, it reports what projects it is interested in obtaining
 * deltas for the next time it is run.  This class tests that this list of "interesting"
 * projects is correctly saved between sessions.
 */
public class TestInterestingProjectPersistence extends WorkspaceSessionTest {
	//various resource handles
	private IProject project1;
	private IProject project2;
	private IProject project3;
	private IProject project4;
	private IFile file1;
	private IFile file2;
	private IFile file3;
	private IFile file4;

	/**
	 * Sets the workspace autobuilding to the desired value.
	 */
	protected void setAutoBuilding(boolean value) throws CoreException {
		IWorkspace workspace = getWorkspace();
		if (workspace.isAutoBuilding() == value) {
			return;
		}
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(value);
		workspace.setDescription(desc);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
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
	public void test1() {
		IResource[] resources = {project1, project2, project3, project4, file1, file2, file3, file4};
		ensureExistsInWorkspace(resources, true);
		try {
			//turn off autobuild
			IWorkspace workspace = getWorkspace();
			IWorkspaceDescription desc = workspace.getDescription();
			desc.setAutoBuilding(false);
			workspace.setDescription(desc);

			//create a project and configure builder
			IProjectDescription description = project1.getDescription();
			ICommand command = description.newCommand();
			Map<String, String> args = command.getArguments();
			args.put(TestBuilder.BUILD_ID, "Project1Build1");
			command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
			command.setArguments(args);
			description.setBuildSpec(new ICommand[] {command});
			project1.setDescription(description, getMonitor());

			//initial build requesting no projects
			project1.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());

			getWorkspace().save(true, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
	}

	/**
	 * Check that "no interesting builders" case suceeded, then ask for more projects.
	 */
	public void test2() {
		DeltaVerifierBuilder builder = DeltaVerifierBuilder.getInstance();
		builder.checkDeltas(new IProject[] {project1, project2, project3, project4});
		builder.requestDeltas(new IProject[] {project1, project2, project4});
		try {
			file1.setContents(getRandomContents(), true, true, getMonitor());
			project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			ArrayList<IProject> received = builder.getReceivedDeltas();

			//should have received only a delta for project1
			assertEquals("1.0", 1, received.size());
			assertTrue("1.1", received.contains(project1));

			//should be no empty deltas
			ArrayList<IProject> empty = builder.getEmptyDeltas();
			assertEquals("1.2", 0, empty.size());

			//save
			getWorkspace().save(true, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	/**
	 * Check that interesting projects (1, 2, and 4) were stored and retrieved
	 */
	public void test3() {
		DeltaVerifierBuilder builder = DeltaVerifierBuilder.getInstance();
		builder.checkDeltas(new IProject[] {project1, project2, project3, project4});
		try {
			//dirty projects 1, 2, 3
			file1.setContents(getRandomContents(), true, true, getMonitor());
			file2.setContents(getRandomContents(), true, true, getMonitor());
			file3.setContents(getRandomContents(), true, true, getMonitor());

			//build
			project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
			ArrayList<IProject> received = builder.getReceivedDeltas();

			//should have received deltas for 1, 2, and 4
			assertEquals("1.0", 3, received.size());
			assertTrue("1.1", received.contains(project1));
			assertTrue("1.2", received.contains(project2));
			assertTrue("1.3", !received.contains(project3));
			assertTrue("1.4", received.contains(project4));

			//delta for project4 should be empty
			ArrayList<IProject> empty = builder.getEmptyDeltas();
			assertEquals("1.2", 1, empty.size());
			assertTrue("1.3", empty.contains(project4));

			//save
			getWorkspace().save(true, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestInterestingProjectPersistence.class);
	}
}
