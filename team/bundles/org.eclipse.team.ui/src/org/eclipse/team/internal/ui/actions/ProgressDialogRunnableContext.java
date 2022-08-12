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
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * This CVS runnable context blocks the UI and can therfore have a shell assigned to
 * it (since the shell won't be closed by the user before the runnable completes.
 */
public class ProgressDialogRunnableContext implements ITeamRunnableContext {

	private IRunnableContext runnableContext;
	private ISchedulingRule schedulingRule;
	private boolean postponeBuild;

	public ProgressDialogRunnableContext() {
	}

	/**
	 * Set whether the auto-build will be postponed while this
	 * context is executing a runnable.
	 * @param postponeBuild whether to postpone the auto-build.
	 */
	public void setPostponeBuild(boolean postponeBuild) {
		this.postponeBuild = postponeBuild;
	}

	/**
	 * Set the scheduling rule that will be obtained before the context
	 * executes a runnable or <code>null</code> if no scheduling rule is to be onbtained.
	 * @param schedulingRule The schedulingRule to be obtained or <code>null</code>.
	 */
	public void setSchedulingRule(ISchedulingRule schedulingRule) {
		this.schedulingRule = schedulingRule;
	}

	/**
	 * Set the runnable context that is used to execute the runnable. By default,
	 * the workbench's progress service is used by clients can provide their own.
	 * @param runnableContext the runnable contentx used to execute runnables.
	 */
	public void setRunnableContext(IRunnableContext runnableContext) {
		this.runnableContext = runnableContext;
	}

	@Override
	public void run(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		getRunnableContext().run(true /* fork */, true /* cancelable */, wrapRunnable(runnable));
	}

	private IRunnableContext getRunnableContext() {
		if (runnableContext == null) {
			return (fork, cancelable, runnable) -> {
				IProgressService manager = PlatformUI.getWorkbench().getProgressService();
				manager.busyCursorWhile(runnable);
			};
		}
		return runnableContext;
	}

	/*
	 * Return an IRunnableWithProgress that sets the task name for the progress monitor
	 * and runs in a workspace modify operation if requested.
	 */
	private IRunnableWithProgress wrapRunnable(final IRunnableWithProgress runnable) {
		return monitor -> {
			try {
				if (schedulingRule == null && !postponeBuild) {
					runnable.run(monitor);
				} else {
					final Exception[] exception = new Exception[] { null };
					ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) pm -> {
						try {
							runnable.run(pm);
						} catch (InvocationTargetException e1) {
							exception[0] = e1;
						} catch (InterruptedException e2) {
							exception[0] = e2;
						}
					}, schedulingRule, 0 /* allow updates */, monitor);
					if (exception[0] != null) {
						if (exception[0] instanceof InvocationTargetException) {
							throw (InvocationTargetException)exception[0];
						} else if (exception[0] instanceof InterruptedException) {
							throw (InterruptedException)exception[0];
						}
					}
				}
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
	}

}
