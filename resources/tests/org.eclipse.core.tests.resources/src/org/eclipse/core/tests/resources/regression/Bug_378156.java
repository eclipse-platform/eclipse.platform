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

import java.util.concurrent.Semaphore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.usecase.SignaledBuilder;

/**
 * Tests a timing problem where a canceled waiting thread could cause a change
 * in another thread to skip building.
 */
public class Bug_378156 extends ResourceTest {

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
			jobFile.setContents(getRandomContents(), IResource.NONE, null);
			//wait for signal
			try {
				jobFlag.acquire();
			} catch (InterruptedException e) {
				fail("0.99", e);
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

	public void testBugTwoThreads() throws Exception {
		//setup
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project1 = root.getProject("Bug_378156");
		final IFile file = project1.getFile("content.txt");
		ensureExistsInWorkspace(project1, true);
		//add a builder that can tell us if it was called
		IProjectDescription desc = project1.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SignaledBuilder.BUILDER_ID);
		desc.setBuildSpec(new ICommand[] {command});
		project1.setDescription(desc, getMonitor());
		ensureExistsInWorkspace(file, getRandomContents());
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
		assertTrue("1.0", builder.wasExecuted());
	}

	public void testBugOneThread() throws Exception {
		//setup
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project1 = root.getProject("Bug_378156");
		final IFile file = project1.getFile("content.txt");
		ensureExistsInWorkspace(project1, true);
		//add a builder that can tell us if it was called
		IProjectDescription desc = project1.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SignaledBuilder.BUILDER_ID);
		desc.setBuildSpec(new ICommand[] {command});
		project1.setDescription(desc, getMonitor());
		ensureExistsInWorkspace(file, getRandomContents());
		waitForBuild();

		//initialize the builder
		SignaledBuilder builder = SignaledBuilder.getInstance(project1);
		builder.reset();

		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			//modify the file so autobuild is needed
			file.setContents(getRandomContents(), IResource.NONE, null);
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
		assertTrue("1.0", builder.wasExecuted());
	}

}
