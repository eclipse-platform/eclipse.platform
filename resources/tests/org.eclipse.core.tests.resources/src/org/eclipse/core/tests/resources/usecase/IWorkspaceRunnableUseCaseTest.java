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
package org.eclipse.core.tests.resources.usecase;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.resources.ResourceTest;

public class IWorkspaceRunnableUseCaseTest extends ResourceTest {

	protected IWorkspaceRunnable createRunnable(final IProject project, final IWorkspaceRunnable nestedOperation, final boolean triggerBuild, final Exception exceptionToThrow) {
		return monitor -> {
			if (exceptionToThrow != null) {
				if (exceptionToThrow instanceof CoreException ce) {
					throw ce;
				}
				if (exceptionToThrow instanceof RuntimeException re) {
					throw re;
				}
				throw new IllegalArgumentException(exceptionToThrow);
			}
			if (triggerBuild) {
				project.touch(createTestMonitor());
			}
			if (nestedOperation != null) {
				getWorkspace().run(nestedOperation, createTestMonitor());
			}
		};
	}

	public void testNestedOperationsAndBuilds() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		setAutoBuilding(true);
		IProjectDescription prjDescription = getWorkspace().newProjectDescription("MyProject");
		ICommand command = prjDescription.newCommand();
		command.setBuilderName(SignaledBuilder.BUILDER_ID);
		prjDescription.setBuildSpec(new ICommand[] { command });
		project.create(prjDescription, createTestMonitor());
		project.open(createTestMonitor());
		waitForBuild();
		SignaledBuilder builder = SignaledBuilder.getInstance(project);

		{
			/* should trigger a build */
			IWorkspaceRunnable op1 = createRunnable(project, null, true, null);
			IWorkspaceRunnable op2 = createRunnable(project, op1, false, null);
			IWorkspaceRunnable op3 = createRunnable(project, op2, false, null);
			builder.reset();
			getWorkspace().run(op3, createTestMonitor());
			waitForBuild();
			assertTrue("1.1", builder.wasExecuted());
		}

		{
		/* should not trigger a build */
			IWorkspaceRunnable op1 = createRunnable(project, null, true, new OperationCanceledException());
			IWorkspaceRunnable op2 = createRunnable(project, op1, true, null);
			IWorkspaceRunnable op3 = createRunnable(project, op2, true, null);
			builder.reset();
			assertThrows(OperationCanceledException.class, () -> getWorkspace().run(op3, createTestMonitor()));
			// waitForBuild(); // TODO: The test is invalid since it fails if this line is
			// uncommented.
			assertTrue("2.2", !builder.wasExecuted());
		}

		{
			/* should not trigger a build */
			IWorkspaceRunnable op1 = createRunnable(project, null, true, new CoreException(Status.CANCEL_STATUS));
			IWorkspaceRunnable op2 = createRunnable(project, op1, true, null);
			IWorkspaceRunnable op3 = createRunnable(project, op2, true, null);
			builder.reset();
			CoreException exception = assertThrows(CoreException.class, () -> getWorkspace().run(op3, createTestMonitor()));
			assertEquals(Status.CANCEL_STATUS, exception.getStatus());
			// waitForBuild(); // TODO: The test is invalid since it fails if this line is
			// uncommented.
			assertTrue("3.1", !builder.wasExecuted());
		}

		{
			/* should not trigger a build */
			IWorkspaceRunnable op1 = createRunnable(project, null, false, null);
			IWorkspaceRunnable op2 = createRunnable(project, op1, false, null);
			IWorkspaceRunnable op3 = createRunnable(project, op2, false, null);
			builder.reset();
			getWorkspace().run(op3, createTestMonitor());
			// waitForBuild(); // TODO: The test is invalid since it fails if this line is
			// uncommented.
			assertTrue("4.1", !builder.wasExecuted());
		}
	}
}
