/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

public class PR_1GHOM0N_Test extends ResourceTest {

	/*
	 * Ensure that we get ADDED and OPEN in the delta when we create and open
	 * a project in a workspace runnable.
	 */
	public void test_1GEAB3C() throws CoreException {
		// setup the project
		final IProject project = getWorkspace().getRoot().getProject("MyProject");
		IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
		ICommand command = description.newCommand();
		command.setBuilderName(SimpleBuilder.BUILDER_ID);
		description.setBuildSpec(new ICommand[] { command });
		project.create(description, createTestMonitor());
		project.open(createTestMonitor());

		// try and reproduce the error (there are problems when calling an incremental
		// build from within an operation...it leaves the tree immutable)
		IWorkspaceRunnable body = monitor -> {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
			IFile file = project.getFile("test.txt");
			file.create(getRandomContents(), true, createTestMonitor());
		};
		getWorkspace().run(body, createTestMonitor());
	}
}
