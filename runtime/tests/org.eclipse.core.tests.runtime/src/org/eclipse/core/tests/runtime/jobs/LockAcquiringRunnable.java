/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.jobs.ILock;

public class LockAcquiringRunnable implements Runnable {
	static class RandomOrder {
		private final Queue<LockAcquiringRunnable> randomRunnables = new ConcurrentLinkedQueue<>();
		private final int workerCount;
		private final AtomicInteger waiting = new AtomicInteger();
		private final AtomicInteger busy = new AtomicInteger();

		RandomOrder(ArrayList<LockAcquiringRunnable> allRunnables, int maxRounds) {
			workerCount = allRunnables.size();
			// create a random order in which the threads should do their operations:
			List<LockAcquiringRunnable> randomOrder = new ArrayList<>();
			for (LockAcquiringRunnable l : allRunnables) {
				for (int i = 1; i < maxRounds * (1 + l.locks.length); i++) {
					randomOrder.add(l);
				}
			}
			Collections.shuffle(randomOrder);
			this.randomRunnables.addAll(randomOrder);
		}

		/**
		 * wait "random time" till thread is the next in the random order
		 **/
		public boolean randomWait(LockAcquiringRunnable me) {
			int waitingCount = waiting.incrementAndGet();
			try {
				while (randomRunnables.peek() != me) {
					if (randomRunnables.isEmpty()) {
						return false; // no more work
					}
					if (waitingCount >= workerCount - busy.get()) {
						// the head thread is not progressing (waiting in
						// acquire), so stop waiting
						LockAcquiringRunnable head = randomRunnables.poll();
						if (head != null) {
							randomRunnables.add(head);
							return true;
						}
					}
					Thread.yield();
				}
				// fast path - do not wait any fixed time
				randomRunnables.remove();
				return true;
			} finally {
				waiting.decrementAndGet();
			}
		}

		public void waitForEnd() {
			while (!randomRunnables.isEmpty()) {
				Thread.yield();
			}
		}

		public void busy(Runnable runner) {
			busy.incrementAndGet();
			try {
				runner.run();
			} finally {
				busy.decrementAndGet();
			}
		}
	}

	private final ILock[] locks;
	private volatile RandomOrder rnd;

	/**
	 * This runnable will randomly acquire the given lock for random periods of
	 * time, in the given order
	 */
	public LockAcquiringRunnable(ILock[] locks) {
		this.locks = locks;
	}

	@Override
	public void run() {
		while (rnd.randomWait(this)) {
			for (ILock lock : locks) {
				rnd.busy(lock::acquire);
				rnd.randomWait(this);
			}
			// release all locks
			for (int i = locks.length; --i >= 0;) {
				locks[i].release();
			}
		}
	}

	public void setRandomOrder(RandomOrder rnd) {
		this.rnd = rnd;
	}
}
