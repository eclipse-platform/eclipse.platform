/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Christoph Läubrich - Issue #80 - CharsetManager access the ResourcesPlugin.getWorkspace before init
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Batches the activity of a job as a single operation, without obtaining the workspace
 * lock.
 */
public abstract class InternalWorkspaceJob extends Job {
	private Workspace workspace;

	public InternalWorkspaceJob(String name, Workspace workspace) {
		super(name);
		this.workspace = workspace;
	}

	@Override
	public final IStatus run(IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		try {
			int depth = -1;
			final WorkManager workManager = workspace.getWorkManager();
			try {
				workspace.prepareOperation(null, monitor);
				workspace.beginOperation(true);
				depth = workManager.beginUnprotected();
				return runInWorkspace(monitor);
			} catch (OperationCanceledException e) {
				workManager.operationCanceled();
				return Status.CANCEL_STATUS;
			} finally {
				if (depth >= 0)
					workManager.endUnprotected(depth);
				workspace.endOperation(null, false);
			}
		} catch (CoreException e) {
			return e.getStatus();
		}
	}

	protected abstract IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException;
}
