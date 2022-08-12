/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.SubMonitor;
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
	private final List<Throwable> errors = new ArrayList<>();
	private WorkProgressMonitor currentMonitor;
	private IRunnableWithProgress currentTask;
	private final String taskName;

	/**
	 * Progress monitor that supports local cancellation of a task.
	 */
	private static class WorkProgressMonitor extends ProgressMonitorWrapper {
		private boolean localCancel;

		protected WorkProgressMonitor(IProgressMonitor monitor) {
			super(monitor);
		}

		public void cancelTask() {
			localCancel = true;
		}

		@Override
		public boolean isCanceled() {
			return localCancel || super.isCanceled();
		}
	}

	public Worker(String taskName) {
		this.taskName = taskName;
	}

	@Override
	public void run(IProgressMonitor monitor) {
		errors.clear();
		SubMonitor progress = SubMonitor.convert(monitor, getTaskName(), 100);
		try {
			isWorking = true;
			while (!work.isEmpty()) {
				try {
					performNextTask(progress);
					progress.checkCanceled();
				} catch (OperationCanceledException | InterruptedException e) {
					// Only cancel all the work if the outer monitor is canceled.
					progress.checkCanceled();
				} catch (InvocationTargetException e) {
					handleError(e.getTargetException());
				}
				progress.setWorkRemaining(100);
			}
			progress.done();
		} catch (OperationCanceledException e) {
			// The user chose to cancel
			work.clear();
		} finally {
			isWorking = false;
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
		return errors.toArray(new Throwable[errors.size()]);
	}

	protected String getTaskName() {
		return taskName;
	}

	private void performNextTask(SubMonitor pm) throws InvocationTargetException, InterruptedException {
		synchronized (this) {
			if (work.isEmpty())
				return;
			currentTask = work.remove();
			currentMonitor= subMonitorFor(pm, 10);
		}
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
