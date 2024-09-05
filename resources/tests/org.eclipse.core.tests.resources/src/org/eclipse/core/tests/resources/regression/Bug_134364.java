/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.core.tests.resources.regression;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.internal.builders.SortBuilder;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests an error that occurs when a thread tries to read the workspace
 * during snapshot.  Since snapshot collapses unused trees, it is a destructive
 * operation on the tree's parent chain.  Any reader traversing the tree parent
 * chain during that destructive operation risks encountering the tree in a
 * malformed state.  The fix was to synchronize the routine that collapses
 * unused trees in ElementTree.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_134364 {

	/**
	 * Creates a project with a builder attached
	 */
	private IProject createOtherProject() throws CoreException {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject other = workspace.getRoot().getProject("Other");
		IProjectDescription desc = workspace.newProjectDescription(other.getName());
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] {command});
		other.create(desc, null);
		other.open(null);
		return other;
	}

	@Test
	public void test1() throws Exception {
		final IProject other = createOtherProject();
		final boolean[] done = new boolean[] {false};
		final RuntimeException[] failure = new RuntimeException[1];
		//create a job that continually tries to read the workspace tree
		new Job("Reader-134364") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				try {
					while (!done[0]) {
						root.getProjects();
						try {
							other.members();
						} catch (CoreException e) {
							//ignore
						}
					}
					return Status.OK_STATUS;
				} catch (RuntimeException e) {
					failure[0] = e;
					throw e;
				}
			}
		}.schedule();
		//create a job that continually creates projects, thus causing snapshots to occur
		Job writer = new Job("Writer-134364") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("TestBug134364");
				for (int i = 0; i < 100; i++) {
					if (failure[0] != null) {
						System.out.println("Failure: " + i);
						break;
					}
					try {
						//create a few extra tree layers
						project.create(null);
						project.open(null);
						project.touch(null);
						project.delete(IResource.NONE, null);
					} catch (CoreException e) {
						//just bail out
						e.printStackTrace();
						return Status.OK_STATUS;
					}
				}
				return Status.OK_STATUS;
			}
		};
		writer.schedule();
		writer.join();
		done[0] = true;
		if (failure[0] != null) {
			throw new AssertionError("1.0", failure[0]);
		}
	}

}
