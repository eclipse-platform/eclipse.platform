/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.*;

/**
 * Distributes progress information from this monitor to multiple monitors.
 */
public class ProgressDistributor implements IProgressMonitor {
	private int totalWork = -1;
	private double worked = 0;
	private boolean done = false;
	String taskName;
	String subTaskName;
	/**
	 * Map work indexed by montitor
	 */
	private Collection<IProgressMonitor> monitors = new ArrayList<>();

	@Override
	public synchronized void beginTask(String name, int totalWork) {
		this.totalWork = totalWork;
		this.worked = 0;
		this.done = false;
		for (IProgressMonitor m : monitors) {
			m.beginTask(name, totalWork);
		}
	}

	@Override
	public synchronized void done() {
		done = true;
		for (IProgressMonitor m : monitors) {
			m.done();
		}
	}

	@Override
	public void internalWorked(double work) {
		worked += work;
		for (IProgressMonitor m : monitors) {
			m.internalWorked(work);
		}
	}

	@Override
	public synchronized boolean isCanceled() {
		for (IProgressMonitor m : monitors) {
			if (m.isCanceled()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setCanceled(boolean value) {
	}

	@Override
	public synchronized void setTaskName(String name) {
		taskName = name;
		for (IProgressMonitor m : monitors) {
			m.setTaskName(name);
		}
	}

	@Override
	public synchronized void subTask(String name) {
		subTaskName = name;
		for (IProgressMonitor m : monitors) {
			m.subTask(name);
		}
	}

	@Override
	public synchronized void worked(int work) {
		internalWorked(work);
	}

	public synchronized void addMonitor(IProgressMonitor m) {
		if (totalWork > -1)
			m.beginTask(taskName, totalWork);
		if (subTaskName != null)
			m.subTask(subTaskName);
		if (worked > 0)
			m.internalWorked(worked);
		if (done)
			m.done();
		monitors.add(m);
	}
	public synchronized void removeMonitor(IProgressMonitor m) {
		monitors.remove(m);
	}
	public synchronized void operationCanceled() {
		totalWork = -1;
		worked = 0;
		done = false;
	}
}
