/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.SessionTestSuite;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

public class Test1GALH44 extends WorkspaceSessionTest {

	public Test1GALH44(String name) {
		super(name);
	}

	/**
	 * Prepares the environment.  Create some resources and save the workspace.
	 */
	public void test1() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IProjectDescription description = getWorkspace().newProjectDescription("MyProject");
		ICommand command = description.newCommand();
		command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
		description.setBuildSpec(new ICommand[] {command});
		project.create(getMonitor());
		project.open(getMonitor());
		project.setDescription(description, getMonitor());

		IFile file = project.getFile("foo.txt");
		file.create(getRandomContents(), true, getMonitor());

		getWorkspace().save(true, getMonitor());
	}

	/**
	 * Step 2, edit a file then immediately crash.
	 */
	public void test2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("foo.txt");
		file.setContents(getRandomContents(), true, true, getMonitor());
		// crash
		System.exit(-1);
	}

	/**
	 * Now immediately try to save after recovering from crash.
	 */
	public void test3() throws CoreException {
		getWorkspace().save(true, getMonitor());
	}

	public static Test suite() {
		SessionTestSuite suite = new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, Test1GALH44.class.getName());
		suite.addTest(new Test1GALH44("test1"));
		suite.addCrashTest(new Test1GALH44("test2"));
		suite.addTest(new Test1GALH44("test3"));
		return suite;
	}
}
