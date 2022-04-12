/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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

import java.util.Random;
import org.eclipse.core.runtime.jobs.ILock;

public class RandomTestRunnable extends Thread {
	private ILock[] locks;
	private Random random = new Random();
	private boolean alive;
	private boolean needRandomization;
	volatile int runs;

	/**
	 * This runnable will randomly acquire the given locks for
	 * random periods of time, in the given order, or in random order (if specified)
	 */
	public RandomTestRunnable(ILock[] locks, String name, boolean addRandomness) {
		super(name);
		this.locks = new ILock[locks.length];
		System.arraycopy(locks, 0, this.locks, 0, locks.length);
		this.alive = true;
		this.needRandomization = addRandomness;
	}

	public void kill() {
		alive = false;
	}

	@Override
	public void run() {
		while (alive) {
			if (needRandomization) {
				for (int i = 0; i < locks.length; i++) {
					int nextFlip = random.nextInt(locks.length);
					ILock temp = locks[i];
					locks[i] = locks[nextFlip];
					locks[nextFlip] = temp;
				}
			}
			for (ILock lock : locks) {
				lock.acquire();
				// wait a random time to get random order from different threads.
				// XXX a pure waste of time. Would be better to have a fixed sequence of
				// acquire() calls across all threads
				try {
					int r = random.nextInt(3);
					if (r > 0) {
						Thread.sleep(r);
					}
				} catch (InterruptedException e1) {
				}
			}
			//release all locks
			for (int i = locks.length; --i >= 0;) {
				locks[i].release();
			}
			runs++;
		}
	}

}
