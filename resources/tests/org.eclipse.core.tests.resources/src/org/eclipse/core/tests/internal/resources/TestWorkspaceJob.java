/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * A simple workspace job that runs for a defined amount of time.
 */
public class TestWorkspaceJob extends WorkspaceJob {
	private static final int tickLength = 10;
	private long duration;
	private boolean touch = false;

	/**
	 * Creates a workspace job that will run for the specified duration in milliseconds.
	 * @param duration
	 */
	public TestWorkspaceJob(long duration) {
		super("TestWorkspaceJob");
		this.duration = duration;
		//only allow durations that are a multiple of the tick length for simplicity
		if (duration % tickLength > 0) {
			throw new IllegalArgumentException("Use a job duration that it is a multiple of " + tickLength);
		}
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		int ticks = (int) (duration / tickLength);
		monitor.beginTask(getName(), ticks <= 0 ? 1 : ticks);
		if (touch) {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				project.touch(null);
			}
		}
		try {
			for (int i = 0; i < ticks; i++) {
				monitor.subTask("Tick: " + i);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				try {
					Thread.sleep(tickLength);
				} catch (InterruptedException e) {
					//ignore
				}
				monitor.worked(1);
			}
			if (ticks <= 0) {
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Indicates whether this job should touch the projects in the workspace. By
	 * default, this is set to false.
	 */
	public void setTouch(boolean value) {
		this.touch = value;
	}
}
