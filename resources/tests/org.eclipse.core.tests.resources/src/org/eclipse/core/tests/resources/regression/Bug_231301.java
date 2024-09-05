/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.util.function.Predicate;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class Bug_231301 {

	@Test
	public void testBug() throws CoreException, InterruptedException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project1 = workspace.getRoot().getProject("Project1");
		project1.create(null);
		project1.open(null);

		final IProject project2 = workspace.getRoot().getProject("Project2");
		project2.create(null);
		project2.open(null);

		final WorkspaceJob job = new WorkspaceJob("job") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				project2.close(null);
				return Status.OK_STATUS;
			}
		};

		// the listener will schedule another project close
		IResourceChangeListener projectClosingChangeListener = event -> {
			if (event.getResource() == project1) {
				// because notification is run in a protected block,
				// this job will start after the notification
				job.schedule();
				//give the job a chance to start before continuing
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					//ignore
				}
			}
		};

		try {
			workspace.addResourceChangeListener(projectClosingChangeListener, IResourceChangeEvent.PRE_CLOSE);
			// close project
			project1.close(createTestMonitor());
			job.join();
		} finally {
			workspace.removeResourceChangeListener(projectClosingChangeListener);
		}

		assertThat(job.getResult().isOK()).withFailMessage(job.getResult().toString()).isTrue();
		assertThat(project1).matches(Predicate.not(IProject::isOpen), "is not open");
		assertThat(project2).matches(Predicate.not(IProject::isOpen), "is not open");
	}

}
