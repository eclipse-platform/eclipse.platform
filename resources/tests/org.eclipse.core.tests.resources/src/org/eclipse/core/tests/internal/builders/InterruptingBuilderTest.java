/*******************************************************************************
 * Copyright (c) 2022 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Tests that the AutoBuild job doesn't raise its interrupt flag when doing
 * workspace modifications.
 */
public class InterruptingBuilderTest extends AbstractBuilderTest {

	private static final String TEST_PROJECT_NAME = "WorkerProject";
	private static final String TEST_FILE_NAME = "test.txt";
	private IProject project;
	private IFile file;

	public InterruptingBuilderTest() {
		super("Interrupting Builder");
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		setAutoBuilding(false);
		waitForBuild();
		project = setUpProject();
		file = setUpFile(project);
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		project.delete(true, new NullProgressMonitor());
		setAutoBuilding(true);
	}

	private IProject setUpProject() throws CoreException {
		IWorkspaceRoot workspaceRoot = getWorkspace().getRoot();
		IProject result = workspaceRoot.getProject(TEST_PROJECT_NAME);
		result.create(new NullProgressMonitor());
		result.open(new NullProgressMonitor());
		return result;
	}

	private IFile setUpFile(IProject project) throws CoreException {
		IFile result = project.getFile(TEST_FILE_NAME);
		result.create(getRandomContents(), true, new NullProgressMonitor());
		return result;
	}

	/**
	 * This test case checks whether the interrupt flag of the auto-builder is set
	 * upon workspace modifications. This interrupt flag is set when another thread
	 * performs such modifications, while the auto-builder is running.
	 *
	 * This interruption then causes the auto-builder to reschedule itself, if the
	 * current job has been canceled by the user. If this thread has been created
	 * from within the auto-builder it is therefore no longer possible to gracefully
	 * cancel the auto-build, given that this interrupt flag is set by its own
	 * thread.
	 *
	 * @throws CoreException
	 *             If one of the initial workspace modifications failed for
	 *             unexpected reasons.
	 */
	public void testBuildProject() throws CoreException {
		addBuilder(project, InterruptingBuilder.BUILDER_NAME);

		try {
			setAutoBuilding(true);

			dirty(file);

			assertTrue("The auto-builder didn't start in time...", InterruptingBuilder.waitForStart());

			assertTrue("The auto-builder didn't finish in time...", InterruptingBuilder.waitForEnd());

			assertFalse("The auto-builder should be idle!", isBuildPending());
		} finally {
			// Otherwise the auto-builder gets stuck in an infinite loop
			setAutoBuilding(false);
		}
	}

	private boolean isBuildPending() {
		return ((Workspace) getWorkspace()).getBuildManager().isAutobuildBuildPending();
	}
}
