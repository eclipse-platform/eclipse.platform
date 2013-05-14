/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * A worker performs a set of tasks in order and accumulates any errors
 * that may have occurred. If the same task is queued multiple times,
 * the last occurrence will be run. If a task is queued while it is
 * running, the running task will be canceled and the task added
 * to the end of the work queue.
 */
public class Worker implements IRunnableWithProgress {

	private final WorkQueue work = new WorkQueue();
	private boolean isWorking;
	private final List errors = new ArrayList();
	private WorkProgressMonitor currentMonitor;
	private IRunnableWithProgress currentTask;
	private final String taskName;
	
	/**
	 * Progress monitor that supports local cancelation of a task.
	 */
	private static class WorkProgressMonitor extends ProgressMonitorWrapper {
		private boolean localCancel;
		protected WorkProgressMonitor(IProgressMonitor monitor) {
			super(monitor);
		}
		public void cancelTask() {
			localCancel = true;
		}
		public boolean isCanceled() {
			return localCancel || super.isCanceled();
		}
	}
	
	public Worker(String taskName) {
		this.taskName = taskName;
	}
	
	public void run(IProgressMonitor monitor) {
		errors.clear();
		SubMonitor pm = SubMonitor.convert(monitor, getTaskName(), 100);
		try {
			isWorking = true;
			while (!work.isEmpty()) {
				try {
					performNextTask(pm);
					checkCancelled(pm);
				} catch (OperationCanceledException e) {
					// Only cancel all the work if the outer monitor is canceled
					checkCancelled(pm);
				} catch (InterruptedException e) {
					// Only cancel all the work if the outer monitor is canceled
					checkCancelled(pm);
				} catch (InvocationTargetException e) {
					handleError(e.getTargetException());
				}
				pm.setWorkRemaining(100);
			}
		} catch (OperationCanceledException e) {
			// The user chose to cancel
			work.clear();
		} finally {
			isWorking = false;
			if (monitor!= null)
				monitor.done();
			currentMonitor = null;
			currentTask = null;
		}
	}

	private WorkProgressMonitor subMonitorFor(SubMonitor pm, int ticks) {
		return new WorkProgressMonitor(pm.newChild(ticks));
	}

	private void handleError(Throwable targetException) {
		errors.add(targetException);
	}
	
	public Throwable[] getErrors() {
		return (Throwable[]) errors.toArray(new Throwable[errors.size()]);
	}

	private void checkCancelled(SubMonitor pm) {
		if (pm.isCanceled())
			throw new OperationCanceledException();
	}

	protected String getTaskName() {
		return taskName;
	}

	private void performNextTask(SubMonitor pm) throws InvocationTargetException, InterruptedException {
		synchronized (this) {
			if (work.isEmpty())
				return;
			currentTask = work.remove();
		}
		currentMonitor = subMonitorFor(pm, 10);
		currentTask.run(currentMonitor);
	}

	public synchronized void add(IRunnableWithProgress r) {
		if (currentTask != null && currentTask.equals(r)) {
			currentMonitor.cancelTask();
		}
		work.add(r);
	}

	public boolean isWorking() {
		return isWorking;
	}

	public boolean hasWork() {
		return isWorking() || !work.isEmpty();
	}

}
