/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.usecase;

import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;

/**
 * A class to help testing use cases that access the workspace concurrently but in
 * a deterministic way.
 * Operations can be defined with some sync points. It means that the operation will
 * run to that point and wait for some other thread signal (call #proceed) before continuing.
 *
 * Some of the constructs can lead to deadlocks if not used correctly.
 *
 * One tip to avoid deadlocks is aways honour the sync points defined by the operation.
 * For example, exceptions should be logged using #logException instead of thrown.
 *
 * Although operations of this type can run using IWorkspace.run(), it was designed
 * to run in a separate thread.
 * Example: new Thread(concurrentOperation).start();
 */
public abstract class ConcurrentOperation implements Runnable, IWorkspaceRunnable {
	/** workspace */
	protected IWorkspace workspace;

	/** synchronization flags */
	protected boolean go;
	protected boolean isWaiting;
	protected int hasStarted;

	/** log any exception we get */
	protected MultiStatus status;

	/** locks */
	protected Object startedLock = new Object();

	/** constants */
	protected static final int STARTED_NONE = 0;
	protected static final int STARTED_YES = 1;
	protected static final int STARTED_NO = 2;

	public ConcurrentOperation(IWorkspace workspace) {
		this.workspace = workspace;
		reset();
	}

	/**
	 * This method should verify all pre-requisites necessaries in order
	 * to the operation run properly.
	 */
	abstract protected void assertRequisites() throws Exception;

	public IStatus getStatus() {
		return status;
	}

	/**
	 * Returns only when we get out of the STARTED_NONE state.
	 */
	public boolean hasStarted() {

		synchronized (startedLock) {
			while (hasStarted == STARTED_NONE) {
				try {
					startedLock.wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		return hasStarted == STARTED_YES;
	}

	protected boolean isReadyToStart() {
		boolean ok = true;
		try {
			assertRequisites();
		} catch (Exception e) {
			logException(e);
			ok = false;
		}
		return ok;
	}

	protected void logException(Exception e) {
		if (e instanceof CoreException) {
			status.add(((CoreException) e).getStatus());
		} else {
			status.add(new ResourceStatus(0, null, null, e));
		}
	}

	/**
	 * @see #waitNotification
	 */
	public synchronized void proceed() {
		go = true;
		notify();
	}

	public void reset() {
		go = false;
		isWaiting = false;
		hasStarted = STARTED_NONE;
		status = new MultiStatus("a plugin", IStatus.INFO, "", null);
	}

	/**
	 * Only returns from this method if the operation is in
	 * a sync point.
	 *
	 * This method can cause deadlock.
	 */
	public synchronized void returnWhenInSyncPoint() {
		while (!isWaiting && hasStarted()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	@Override
	public void run() {

		if (isReadyToStart()) {
			setHasStarted(true);
			try {
				workspace.run(this, null);
			} catch (Exception e) {
				logException(e);
			}
		} else {
			setHasStarted(false);
		}

	}

	protected void setHasStarted(boolean value) {

		synchronized (startedLock) {
			hasStarted = value ? STARTED_YES : STARTED_NO;
			startedLock.notify();
		}
	}

	/**
	 * Waits until #proceed is called.
	 *
	 * This method can cause deadlock.
	 */
	protected synchronized void syncPoint() {
		/* we set "go" to false because we want to continue only when proceed is called */
		go = false;
		isWaiting = true;
		notify(); // notify we are in a sync point
		while (!go) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		isWaiting = false;
	}
}
