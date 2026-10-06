/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.connector.process.internal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.cdt.utils.spawner.Spawner;

/**
 * Destroys terminal processes without blocking the caller, which is often the
 * UI thread.
 */
public final class ProcessDestroyer {

	private static final Set<Thread> PENDING = ConcurrentHashMap.newKeySet();

	private ProcessDestroyer() {
	}

	/**
	 * Destroys the given process, in the background if destroying it may block.
	 */
	public static void destroy(Process process) {
		if (!(process instanceof Spawner)) {
			process.destroy();
			return;
		}
		// Spawner.destroy() waits up to a second for a shell that ignores SIGTERM
		Thread thread = new Thread(() -> {
			try {
				process.destroy();
			} finally {
				PENDING.remove(Thread.currentThread());
			}
		}, "Terminal Process Destroy Thread"); //$NON-NLS-1$
		thread.setDaemon(true);
		PENDING.add(thread);
		thread.start();
	}

	/**
	 * Waits until all pending destroys have finished or the timeout has elapsed,
	 * so the JVM does not exit before the processes are killed.
	 */
	static void awaitPending(long timeout, TimeUnit unit) {
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		try {
			for (Thread thread : PENDING) {
				long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
				if (remainingMillis <= 0) {
					return;
				}
				thread.join(remainingMillis);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
