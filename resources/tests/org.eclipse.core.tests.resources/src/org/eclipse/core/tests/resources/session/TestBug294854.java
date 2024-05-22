/*******************************************************************************
 *  Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.session;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.internal.resources.TestingSupport;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.session.CustomSessionWorkspace;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionShouldError;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for bug 294854
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBug294854 {
	private static final String PROJECT_OLD_NAME = "project_old_name";
	private static final String PROJECT_NEW_NAME = "project_new_name";

	private static final String RESET_WORKSPACE_BEFORE_TAG = "RESET_WORKSPACE";

	private static CustomSessionWorkspace sessionWorkspace = SessionTestExtension.createCustomWorkspace();

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(sessionWorkspace).create();

	private static IProject createProject() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_OLD_NAME);
		createInWorkspace(project);
		assertTrue(project.exists());

		// make sure we do not have .snap file
		TestingSupport.waitForSnapshot();
		workspace.save(true, createTestMonitor());

		return project;
	}

	private static boolean checkProjectExists(String name) {
		IProject project = getWorkspace().getRoot().getProject(name);
		return project.exists();
	}

	private static boolean checkProjectIsOpen(String name) {
		IProject project = getWorkspace().getRoot().getProject(name);
		return project.isOpen();
	}

	@BeforeEach
	@ExecuteInHost
	public void resetWorkspace(TestInfo testInfo) throws IOException {
		if (testInfo.getTags().contains(RESET_WORKSPACE_BEFORE_TAG)) {
			Path newWorkspace = Files.createTempDirectory(null);
			newWorkspace.toFile().deleteOnExit();
			sessionWorkspace.setWorkspaceDirectory(newWorkspace);
		}
	}

	@Test
	@SessionShouldError
	@Order(1)
	public void testRenameUsingProjectDescription_01() throws CoreException, InterruptedException {
		IProject project = createProject();

		// move project using IProjectDescription
		IProjectDescription description = project.getDescription();
		description.setName(PROJECT_NEW_NAME);
		project.move(description, true, createTestMonitor());

		// wait for the snapshot job to run
		TestingSupport.waitForSnapshot();

		// simulate process kill
		System.exit(1);
	}

	@Test
	@Order(2)
	public void testRenameUsingProjectDescription_02() {
		assertFalse(checkProjectExists(PROJECT_OLD_NAME));
		assertTrue(checkProjectExists(PROJECT_NEW_NAME));
	}

	@Test
	@Tag(RESET_WORKSPACE_BEFORE_TAG)
	@SessionShouldError
	@Order(11)
	public void testRenameUsingResourcePath_01() throws CoreException, InterruptedException {
		IProject project = createProject();
		IPath newLocation = project.getFullPath().removeLastSegments(1).append(PROJECT_NEW_NAME);
		// move project using IPath
		project.move(newLocation, true, createTestMonitor());
		// wait for the snapshot job to run
		TestingSupport.waitForSnapshot();

		// simulate process kill
		System.exit(1);
	}

	@Test
	@Order(12)
	public void testRenameUsingResourcePath_02() {
		assertFalse(checkProjectExists(PROJECT_OLD_NAME));
		assertTrue(checkProjectExists(PROJECT_NEW_NAME));
	}

	@Test
	@Tag(RESET_WORKSPACE_BEFORE_TAG)
	@SessionShouldError
	@Order(21)
	public void testDelete_01() throws CoreException {
		IProject project = createProject();

		// delete project
		project.delete(true, createTestMonitor());

		// wait for the snapshot job to run
		TestingSupport.waitForSnapshot();

		// simulate process kill
		System.exit(1);
	}

	@Test
	@Order(22)
	public void testDelete_02() {
		assertFalse(checkProjectExists(PROJECT_OLD_NAME));
	}

	@Test
	@Tag(RESET_WORKSPACE_BEFORE_TAG)
	@SessionShouldError
	@Order(31)
	public void testDeleteWithoutWaitingForSnapshot_01() throws CoreException {
		IProject project = createProject();

		// simulate process kill after deleting project but before persisting the state
		// in the snapshot job
		IResourceChangeListener selfDeregisteringExistingChangeListener = new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				getWorkspace().removeResourceChangeListener(this);
				System.exit(1);
			}
		};
		getWorkspace().addResourceChangeListener(selfDeregisteringExistingChangeListener,
				IResourceChangeEvent.POST_CHANGE);

		// delete project
		project.delete(true, createTestMonitor());
	}

	@Test
	@Order(32)
	public void testDeleteWithoutWaitingForSnapshot_02() {
		assertTrue(checkProjectExists(PROJECT_OLD_NAME));
		assertFalse(checkProjectIsOpen(PROJECT_OLD_NAME));
	}

}
