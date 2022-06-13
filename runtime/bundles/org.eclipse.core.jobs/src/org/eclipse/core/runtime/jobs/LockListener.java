/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.internal.jobs.LockManager;

/**
 * A lock listener is notified whenever a thread is about to wait
 * on a lock, and when a thread is about to release a lock.
 * <p>
 * This class is for internal use by the platform-related plug-ins.
 * Clients outside of the base platform should not reference or subclass this class.
 * </p>
 *
 * @see IJobManager#setLockListener(LockListener)
 * @since 3.0
 */
public class LockListener {
	private final LockManager manager = ((JobManager) Job.getJobManager()).getLockManager();

	/**
	 * Notification that a thread is about to block on an attempt to acquire a lock.
	 * Returns whether the thread should be granted immediate access to the lock.
	 * <p>
	 * This default implementation always returns <code>false</code>.
	 * Subclasses may override.
	 *
	 * @param lockOwner the thread that currently owns the lock this thread is
	 * waiting for, or <code>null</code> if unknown.
	 * @return <code>true</code> if the thread should be granted immediate access,
	 * and <code>false</code> if it should wait for the lock to be available
	 */
	public boolean aboutToWait(Thread lockOwner) {
		return false;
	}

	/**
	 * Notification that a thread is about to release a lock.
	 * <p>
	 * This default implementation does nothing. Subclasses may override.
	 */
	public void aboutToRelease() {
		//do nothing
	}

	/**
	 * Returns if it is safe for the calling thread to block while waiting to obtain
	 * a lock. When blocking in the calling thread is not safe, the caller will ensure
	 * that the thread is kept alive and responsive to cancellation while waiting.
	 *
	 * @return <code>true</code> if this thread can block, and
	 * <code>false</code> otherwise.
	 *
	 * @since org.eclipse.core.jobs 3.5
	 */
	public boolean canBlock() {
		return true;
	}

	/**
	 * Returns whether this thread currently owns any locks
	 * @return <code>true</code> if this thread owns any locks, and
	 * <code>false</code> otherwise.
	 */
	protected final boolean isLockOwnerThread() {
		return manager.isLockOwner();
	}
}
