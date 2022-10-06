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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.internal.resources.TestingSupport;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.SessionTestSuite;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Test for bug 294854
 */
public class TestBug294854 extends WorkspaceSessionTest {
	private static final String PROJECT_OLD_NAME = "project_old_name";
	private static final String PROJECT_NEW_NAME = "project_new_name";

	public static Test suite() {
		TestSuite suite = new TestSuite(TestBug294854.class.getName());
		//		suite.addTest(new TestBug294854("testRenameUsingResourcePath_01"));

		SessionTestSuite scenario1 = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, "renameUsingProjectDescription");
		scenario1.addCrashTest(new TestBug294854("testRenameUsingProjectDescription_01"));
		scenario1.addTest(new TestBug294854("testRenameUsingProjectDescription_02"));

		SessionTestSuite scenario2 = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, "renameUsingResourcePath");
		scenario2.addCrashTest(new TestBug294854("testRenameUsingResourcePath_01"));
		scenario2.addTest(new TestBug294854("testRenameUsingResourcePath_02"));

		SessionTestSuite scenario3 = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, "delete");
		scenario3.addCrashTest(new TestBug294854("testDelete_01"));
		scenario3.addTest(new TestBug294854("testDelete_02"));

		suite.addTest(scenario1);
		suite.addTest(scenario2);
		suite.addTest(scenario3);

		return suite;
	}

	public TestBug294854(String name) {
		super(name);
	}

	private IProject createProject() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_OLD_NAME);
		ensureExistsInWorkspace(project, true);
		assertTrue("1.0", project.exists());

		// make sure we do not have .snap file
		TestingSupport.waitForSnapshot();
		workspace.save(true, getMonitor());

		return project;
	}

	private boolean checkProjectExists(String name) {
		IProject project = getWorkspace().getRoot().getProject(name);
		return project.exists();
	}

	public void testRenameUsingProjectDescription_01() throws CoreException, InterruptedException {
		IProject project = createProject();

		// move project using IProjectDescription
		IProjectDescription description = project.getDescription();
		description.setName(PROJECT_NEW_NAME);
		project.move(description, true, getMonitor());

		// give a moment for the snapshot to run
		Thread.sleep(1000);

		// simulate process kill
		System.exit(1);
	}

	public void testRenameUsingProjectDescription_02() {
		assertFalse("1.0", checkProjectExists(PROJECT_OLD_NAME));
		assertTrue("2.0", checkProjectExists(PROJECT_NEW_NAME));
	}

	public void testRenameUsingResourcePath_01() throws CoreException, InterruptedException {
		IProject project = createProject();

		// move project using IPath
		project.move(project.getFullPath().removeLastSegments(1).append(PROJECT_NEW_NAME), true, getMonitor());

		// give a moment for the snapshot to run
		Thread.sleep(1000);

		// simulate process kill
		System.exit(1);
	}

	public void testRenameUsingResourcePath_02() {
		assertFalse("1.0", checkProjectExists(PROJECT_OLD_NAME));
		assertTrue("2.0", checkProjectExists(PROJECT_NEW_NAME));
	}

	public void testDelete_01() throws CoreException {
		IProject project = createProject();

		// delete project
		project.delete(true, getMonitor());

		// simulate process kill
		System.exit(1);
	}

	public void testDelete_02() {
		assertFalse("1.0", checkProjectExists(PROJECT_OLD_NAME));
	}
}
