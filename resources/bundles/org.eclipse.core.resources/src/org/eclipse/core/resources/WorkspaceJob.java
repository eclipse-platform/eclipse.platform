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
package org.eclipse.core.resources;

import org.eclipse.core.internal.resources.InternalWorkspaceJob;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A job that makes an atomic modification to the workspace.  Clients must
 * implement the abstract method <code>runInWorkspace</code> instead
 * of the usual <code>Job.run</code> method.
 * <p>
 * After running a method that modifies resources in the workspace,
 * registered listeners receive after-the-fact notification of
 * what just transpired, in the form of a resource change event.
 * This method allows clients to call a number of
 * methods that modify resources and only have resource
 * change event notifications reported at the end of the entire
 * batch. This mechanism is used to avoid unnecessary builds
 * and notifications.
 * </p>
 * <p>
 * Platform may decide to perform notifications during the operation.
 * The reason for this is that it is possible for multiple threads
 * to be modifying the workspace concurrently. When one thread finishes modifying
 * the workspace, a notification is required to prevent responsiveness problems,
 * even if the other operation has not yet completed.
 * </p>
 * <p>
 * A WorkspaceJob is the asynchronous equivalent of ICoreRunnable
 * </p>
 * <p>
 * Note that the workspace is not locked against other threads during the execution
 * of a workspace job. Other threads can be modifying the workspace concurrently
 * with a workspace job.  To obtain exclusive access to a portion of the workspace,
 * set the scheduling rule on the job to be a resource scheduling rule.  The
 * interface {@link IResourceRuleFactory} is used to create a  scheduling rule
 * for a particular workspace modification operation.
 * </p>
 * @see ICoreRunnable
 * @see org.eclipse.core.resources.IResourceRuleFactory
 * @see IWorkspace#run(ICoreRunnable, ISchedulingRule, int, IProgressMonitor)
 * @since 3.0
 */
public abstract class WorkspaceJob extends InternalWorkspaceJob {
	/**
	 * Creates a new workspace job with the specified name. The job name is
	 * a human-readable value that is displayed to users. The name does not
	 * need to be unique, but it must not be <code>null</code>.
	 *
	 * @param name the name of the job
	 */
	public WorkspaceJob(String name) {
		super(name, (Workspace) ResourcesPlugin.getWorkspace());
	}

	/**
	 * Runs the operation, reporting progress to and accepting
	 * cancellation requests from the given progress monitor.
	 * <p>
	 * Implementors of this method should check the progress monitor
	 * for cancellation when it is safe and appropriate to do so.  The cancellation
	 * request should be propagated to the caller by throwing
	 * <code>OperationCanceledException</code>.
	 * </p>
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *     reporting and cancellation are not desired
	 * @return the result of running the operation
	 * @exception CoreException if this operation fails.
	 */
	@Override
	public abstract IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException;
}
