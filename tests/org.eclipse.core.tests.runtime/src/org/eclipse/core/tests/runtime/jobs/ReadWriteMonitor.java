/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime.jobs;

import java.util.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.ILock;

/**
 * Monitor ensuring no more than one writer working concurrently.
 * Multiple readers are allowed to perform simultaneously.
 */
public class ReadWriteMonitor {
	private final Set<Thread> blockedThreads = Collections.synchronizedSet(new HashSet<>());
	private ILock lock;
	/**
	 * &lt;0 : writing (cannot go beyond -1, i.e one concurrent writer)<br>
	 * =0 : idle<br>
	 * &gt;0 : reading (number of concurrent readers)
	 */
	private int status = 0;

	public ReadWriteMonitor(ILock lock) {
		this.lock = lock;
	}

	/**
	 * Concurrent reading is allowed
	 * Blocking only when already writing.
	 */
	public void enterRead() {
		if (!incrementRead()) {
			blockedThreads.add(Thread.currentThread());
			//wait until this thread or another reader acquires the lock
			while (!incrementRead()) {
				try {
					lock.acquire(Long.MAX_VALUE);
					setStatus(1);
					break;
				} catch (InterruptedException e) {
				}
			}
			blockedThreads.remove(Thread.currentThread());
		}
		//interrupt other threads so all readers can proceed
		interruptBlockedThreads();
	}

	/**
	 * Only one writer at a time is allowed to perform
	 * Blocking only when already writing or reading.
	 */
	public void enterWrite() {
		blockedThreads.add(Thread.currentThread());
		while (true) {
			try {
				lock.acquire(Long.MAX_VALUE);
				setStatus(-1);
				break;
			} catch (InterruptedException e) {
			}
		}
		blockedThreads.remove(Thread.currentThread());
	}

	/**
	 * Synchronized to ensure field value is reconciled.
	 */
	private synchronized void setStatus(int s) {
		this.status = s;
	}

	/**
	 * Only notify waiting writer(s) if last reader
	 */
	public synchronized void exitRead() {
		Assert.isTrue(status > 0, "exitRead without enterRead");
		if (--status == 0) {
			lock.release();
			interruptBlockedThreads();
		}
	}

	/**
	 * When writing is over, all readers and possible
	 * writers are granted permission to restart concurrently
	 */
	public synchronized void exitWrite() {
		Assert.isTrue(status == -1, "exitWrite without enterWrite");
		status = 0;
		lock.release();
		interruptBlockedThreads();
	}

	/**
	 * Atomic exitWrite/enterRead: Allows to keep monitor in between
	 * exit write and next enter read.
	 * When writing is over, all readers are granted permissing to restart
	 * concurrently.
	 * This is the same as:
	 * <pre>
	 * synchronized(monitor) {
	 *   monitor.exitWrite();
	 *   monitor.enterRead();
	 * }
	 * </pre>
	 */
	public synchronized void exitWriteEnterRead() {
		//don't release the lock, just fix the counter to indicate a single reader
		status = 1;
		interruptBlockedThreads();
	}

	/**
	 * Increment the reader count if it is safe to do so.
	 */
	private synchronized boolean incrementRead() {
		if (status <= 0)
			return false;
		status++;
		return true;
	}

	/**
	 * Wake up all waiting threads so they can compete for the counter again.
	 */
	private void interruptBlockedThreads() {
		for (Thread thread : blockedThreads)
			thread.interrupt();
	}
}
