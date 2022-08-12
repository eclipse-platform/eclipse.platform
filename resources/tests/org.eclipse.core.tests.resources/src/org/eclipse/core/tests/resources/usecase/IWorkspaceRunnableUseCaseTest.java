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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.ResourceTest;

public class IWorkspaceRunnableUseCaseTest extends ResourceTest {

	protected IWorkspaceRunnable createRunnable(final IProject project, final IWorkspaceRunnable nestedOperation, final boolean triggerBuild, final Exception exceptionToThrow) {
		return monitor -> {
			if (exceptionToThrow != null) {
				if (exceptionToThrow instanceof CoreException) {
					throw (CoreException) exceptionToThrow;
				}
				if (exceptionToThrow instanceof RuntimeException) {
					throw (RuntimeException) exceptionToThrow;
				}
				throw new IllegalArgumentException(exceptionToThrow);
			}
			if (triggerBuild) {
				project.touch(getMonitor());
			}
			if (nestedOperation != null) {
				getWorkspace().run(nestedOperation, getMonitor());
			}
		};
	}

	public void testNestedOperationsAndBuilds() {
		IWorkspaceDescription original = getWorkspace().getDescription();
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		try {
			IWorkspaceDescription description = getWorkspace().getDescription();
			description.setAutoBuilding(true);
			getWorkspace().setDescription(description);
			IProjectDescription prjDescription = getWorkspace().newProjectDescription("MyProject");
			ICommand command = prjDescription.newCommand();
			command.setBuilderName(SignaledBuilder.BUILDER_ID);
			prjDescription.setBuildSpec(new ICommand[] {command});
			project.create(prjDescription, getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}
		waitForBuild();
		SignaledBuilder builder = SignaledBuilder.getInstance(project);

		/* should trigger a build */
		IWorkspaceRunnable op1 = createRunnable(project, null, true, null);
		IWorkspaceRunnable op2 = createRunnable(project, op1, false, null);
		IWorkspaceRunnable op3 = createRunnable(project, op2, false, null);
		builder.reset();
		try {
			getWorkspace().run(op3, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		waitForBuild();
		assertTrue("1.1", builder.wasExecuted());

		/* should not trigger a build */
		op1 = createRunnable(project, null, true, new OperationCanceledException());
		op2 = createRunnable(project, op1, true, null);
		op3 = createRunnable(project, op2, true, null);
		builder.reset();
		try {
			getWorkspace().run(op3, getMonitor());
			fail("2.0");
		} catch (CoreException e) {
			fail("2.1", e);
		} catch (OperationCanceledException e) {
			// expected
		}
		//waitForBuild();  // TODO: The test is invalid since it fails if this line is uncommented.
		assertTrue("2.2", !builder.wasExecuted());

		/* should not trigger a build */
		op1 = createRunnable(project, null, true, new CoreException(Status.CANCEL_STATUS));
		op2 = createRunnable(project, op1, true, null);
		op3 = createRunnable(project, op2, true, null);
		builder.reset();
		try {
			getWorkspace().run(op3, getMonitor());
			fail("3.0");
		} catch (CoreException e) {
			assertEquals(Status.CANCEL_STATUS, e.getStatus());
		}
		//waitForBuild();  // TODO: The test is invalid since it fails if this line is uncommented.
		assertTrue("3.1", !builder.wasExecuted());

		/* should not trigger a build */
		op1 = createRunnable(project, null, false, null);
		op2 = createRunnable(project, op1, false, null);
		op3 = createRunnable(project, op2, false, null);
		builder.reset();
		try {
			getWorkspace().run(op3, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
		//waitForBuild();  // TODO: The test is invalid since it fails if this line is uncommented.
		assertTrue("4.1", !builder.wasExecuted());

		/* remove trash */
		try {
			project.delete(true, getMonitor());
			getWorkspace().setDescription(original);
		} catch (CoreException e) {
			fail("20.0", e);
		}
	}
}
