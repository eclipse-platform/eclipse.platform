/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - Bug 311863 Ordered Lock lost after interrupt
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.Objects;

public class Semaphore {
	protected long notifications;
	protected Runnable runnable;
	private static final int NANOS_IN_MS = 1_000_000;

	public Semaphore(Runnable runnable) {
		this.runnable = runnable;
		notifications = 0;
	}

	/**
	 * Attempts to acquire this semaphore.  Returns true if it was successfully acquired,
	 * and false otherwise.
	 */
	public synchronized boolean acquire(long delay) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		long start = System.nanoTime();
		long timeLeft = delay;
		while (true) {
			if (notifications > 0) {
				notifications--;
				return true;
			}
			if (timeLeft <= 0)
				return false;
			wait(timeLeft);
			timeLeft = ((start - System.nanoTime()) / NANOS_IN_MS) + delay;
		}
	}

	/**
	 * Attempt to acquire the semaphore without waiting.
	 * Returns true if successfully acquired, false otherwise.
	 */
	public synchronized boolean attempt() {
		if (notifications > 0) {
			notifications--;
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Semaphore)) {
			return false;
		}
		return (runnable == ((Semaphore) obj).runnable);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(runnable);
	}

	public synchronized void release() {
		notifications++;
		notifyAll();
	}

	// for debug only
	@Override
	public String toString() {
		return "Semaphore(" + runnable + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
