/*******************************************************************************
 *  Copyright (c) 2012, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;

import java.util.concurrent.Semaphore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.resources.usecase.SignaledBuilder;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests a timing problem where a canceled waiting thread could cause a change
 * in another thread to skip building.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_378156 {

	class ModifyFileJob extends WorkspaceJob {
		private boolean cancel;
		private final IFile jobFile;
		private final Semaphore jobFlag;

		/**
		 * Modifies a file and then waits for a signal before returning.
		 */
		public ModifyFileJob(IFile file, Semaphore semaphore) {
			super("Modifying " + file);
			this.jobFlag = semaphore;
			jobFile = file;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			if (cancel) {
				throw new OperationCanceledException();
			}
			jobFile.setContents(createRandomContentsStream(), IResource.NONE, null);
			//wait for signal
			try {
				jobFlag.acquire();
			} catch (InterruptedException e) {
				throw new CoreException(
						new Status(IStatus.ERROR, PI_RESOURCES_TESTS, "Failed to acquire job flag log", e));
			}
			return Status.OK_STATUS;
		}

		/**
		 * Tells this job to cancel itself while waiting
		 */
		public void setCancel() {
			this.cancel = true;
		}
	}

	@Test
	public void testBugTwoThreads() throws Exception {
		//setup
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project1 = root.getProject("Bug_378156");
		final IFile file = project1.getFile("content.txt");
		createInWorkspace(project1);
		//add a builder that can tell us if it was called
		updateProjectDescription(project1).addingCommand(SignaledBuilder.BUILDER_ID).apply();
		createInWorkspace(file, createRandomString());
		//build may not be triggered immediately
		Thread.sleep(2000);
		waitForBuild();

		//initialize the builder
		SignaledBuilder builder = SignaledBuilder.getInstance(project1);
		builder.reset();

		//create a job that will modify the file and then wait for a signal
		final Semaphore semaphore = new Semaphore(0);
		ModifyFileJob runningJob = new ModifyFileJob(file, semaphore);
		runningJob.setRule(file);
		runningJob.schedule();

		//create another copy of the job and immediately cancel it before it gets the lock
		ModifyFileJob waitingJob = new ModifyFileJob(file, semaphore);
		waitingJob.setCancel();
		waitingJob.schedule();
		waitingJob.join();

		//now let the first job finish
		semaphore.release();
		runningJob.join();
		waitForBuild();

		//the builder should have run if the bug is fixed
		assertThat(builder).matches(SignaledBuilder::wasExecuted, "was executed");
	}

	@Test
	public void testBugOneThread() throws Exception {
		//setup
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project1 = root.getProject("Bug_378156");
		final IFile file = project1.getFile("content.txt");
		createInWorkspace(project1);
		//add a builder that can tell us if it was called
		updateProjectDescription(project1).addingCommand(SignaledBuilder.BUILDER_ID).apply();
		createInWorkspace(file, createRandomString());
		waitForBuild();

		//initialize the builder
		SignaledBuilder builder = SignaledBuilder.getInstance(project1);
		builder.reset();

		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			//modify the file so autobuild is needed
			file.setContents(createRandomContentsStream(), IResource.NONE, null);
			//create a nested operation that immediately cancels
			try {
				getWorkspace().run((IWorkspaceRunnable) monitor1 -> {
					throw new OperationCanceledException();
				}, null);
			} catch (OperationCanceledException e) {
				//don't let this propagate - we changed our mind about canceling
			}
		}, null);
		waitForBuild();
		//the builder should have run if the bug is fixed
		assertThat(builder).matches(SignaledBuilder::wasExecuted, "was executed");
	}

}
