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
 *     IBM - Initial API and implementation
 *     James Blackburn (Broadcom Corp.) - Bug 311863 Ordered Lock lost after interrupt
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.ArrayDeque;
import java.util.Queue;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A lock used to control write access to an exclusive resource.
 *
 * The lock avoids circular waiting deadlocks by detecting the deadlocks
 * and resolving them through the suspension of all locks owned by one
 * of the threads involved in the deadlock. This makes it impossible for n such
 * locks to deadlock while waiting for each other.  The down side is that this means
 * that during an interval when a process owns a lock, it can be forced
 * to give the lock up and wait until all locks it requires become
 * available.  This removes the feature of exclusive access to the
 * resource in contention for the duration between acquire() and
 * release() calls.
 *
 * The lock implementation prevents starvation by granting the
 * lock in the same order in which acquire() requests arrive. In
 * this scheme, starvation is only possible if a thread retains
 * a lock indefinitely.
 */
public class OrderedLock implements ILock, ISchedulingRule {

	private static final boolean DEBUG = false;
	/**
	 * Locks are sequentially ordered for debugging purposes.
	 */
	private static int nextLockNumber = 0;
	/**
	 * The thread of the operation that currently owns the lock.
	 */
	private volatile Thread currentOperationThread;
	/**
	 * Records the number of successive acquires in the same
	 * thread. The lock is released only when the depth
	 * reaches zero.
	 */
	private int depth;
	/**
	 * The manager that implements the deadlock detection and resolution protocol.
	 */
	private final LockManager manager;
	private final int number;

	/**
	 * Queue of semaphores for threads currently waiting
	 * on the lock. This queue is not thread-safe, so access
	 * to this queue must be synchronized on the lock instance.
	 */
	private final Queue<Semaphore> operations = new ArrayDeque<>();

	/**
	 * Creates a new workspace lock.
	 */
	OrderedLock(LockManager manager) {
		this.manager = manager;
		this.number = nextLockNumber++;
	}

	@Override
	public void acquire() {
		//spin until the lock is successfully acquired
		//NOTE: spinning here allows the UI thread to service pending syncExecs
		//if the UI thread is waiting to acquire a lock.
		boolean interrupted = false;
		while (true) {
			try {
				if (acquire(Long.MAX_VALUE))
					break;
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		//preserve thread interrupt state
		if (interrupted)
			Thread.currentThread().interrupt();
	}

	@Override
	public boolean acquire(long delay) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();

		if (delay <= 0)
			return attempt();
		Semaphore semaphore = createSemaphore();
		if (semaphore == null)
			return true;
		if (DEBUG)
			System.out.println("[" + Thread.currentThread() + "] Operation waiting to be executed... " + this); //$NON-NLS-1$ //$NON-NLS-2$
		boolean success = doAcquire(semaphore, delay);
		manager.resumeSuspendedLocks(Thread.currentThread());
		if (DEBUG)
			System.out.println("[" + Thread.currentThread() + //$NON-NLS-1$
					(success ? "] Operation started... " : "] Operation timed out... ") + this); //$NON-NLS-1$ //$NON-NLS-2$ //}
		if (!success && Thread.interrupted())
			throw new InterruptedException();
		return success;
	}

	/**
	 * Attempts to acquire the lock.  Returns false if the lock is not available and
	 * true if the lock has been successfully acquired.
	 */
	private synchronized boolean attempt() {
		//return true if we already own the lock
		//also, if nobody is waiting, grant the lock immediately
		if ((currentOperationThread == Thread.currentThread()) || (currentOperationThread == null && operations.isEmpty())) {
			depth++;
			setCurrentOperationThread(Thread.currentThread());
			return true;
		}
		return false;
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		return rule == this;
	}

	/**
	 * Returns null if acquired and a Semaphore object otherwise. If a
	 * waiting semaphore already exists for this thread, it will be returned,
	 * otherwise a new semaphore will be created, enqueued, and returned.
	 */
	private synchronized Semaphore createSemaphore() {
		return attempt() ? null : enqueue(new Semaphore(Thread.currentThread()));
	}

	/**
	 * Attempts to acquire this lock.  Callers will block until this lock comes available to
	 * them, or until the specified delay has elapsed.
	 */
	private boolean doAcquire(Semaphore semaphore, long delay) {
		boolean success = false;
		//notify hook to service pending syncExecs before falling asleep
		if (manager.aboutToWait(this.currentOperationThread)) {
			//hook granted immediate access
			//remove semaphore for the lock request from the queue
			//do not log in graph because this thread did not really get the lock
			removeFromQueue(semaphore);
			depth++;
			manager.addLockThread(currentOperationThread, this);
			return true;
		}
		//Make sure the semaphore is in the queue before we start waiting
		//It might have been removed from the queue while servicing syncExecs
		//This is will return our existing semaphore if it is still in the queue
		semaphore = createSemaphore();
		if (semaphore == null)
			return true;
		final Thread currentThread = Thread.currentThread();
		manager.addLockWaitThread(currentThread, this);
		try {
			success = semaphore.acquire(delay);
		} catch (InterruptedException e) {
			if (DEBUG)
				System.out.println("[" + currentThread + "] Operation interrupted while waiting... :-|"); //$NON-NLS-1$ //$NON-NLS-2$
			//remember the interrupt to throw it later
			currentThread.interrupt();
		}
		return updateOperationQueue(semaphore, success);
	}

	/**
	 * Releases this lock from the thread that used to own it.
	 * Grants this lock to the next thread in the queue.
	 */
	private synchronized void doRelease() {
		//notify hook
		manager.aboutToRelease();
		depth = 0;
		Semaphore next = operations.peek();
		setCurrentOperationThread(null);
		if (next != null)
			next.release();
	}

	/**
	 * If there is another semaphore with the same runnable in the
	 * queue, the other is returned and the new one is not added.
	 */
	private synchronized Semaphore enqueue(Semaphore newSemaphore) {
		Semaphore semaphore = operations.stream().filter(s -> s.equals(newSemaphore)).findAny().orElse(null);
		if (semaphore == null) {
			operations.offer(newSemaphore);
			return newSemaphore;
		}
		return semaphore;
	}

	/**
	 * Suspend this lock by granting the lock to the next lock in the queue.
	 * Return the depth of the suspended lock.
	 */
	protected int forceRelease() {
		int oldDepth = depth;
		doRelease();
		return oldDepth;
	}

	@Override
	public int getDepth() {
		return depth;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return rule == this;
	}

	@Override
	public void release() {
		if (depth == 0)
			return;
		//only release the lock when the depth reaches zero
		Assert.isTrue(depth >= 0, "Lock released too many times"); //$NON-NLS-1$
		if (--depth == 0)
			doRelease();
		else
			manager.removeLockThread(currentOperationThread, this);
	}

	/**
	 * Removes a semaphore from the queue of waiting operations.
	 *
	 * @param semaphore The semaphore to remove
	 */
	private synchronized void removeFromQueue(Semaphore semaphore) {
		operations.remove(semaphore);
	}

	/**
	 * If newThread is null, release this lock from its previous owner.
	 * If newThread is not null, grant this lock to newThread.
	 */
	private void setCurrentOperationThread(Thread newThread) {
		if ((currentOperationThread != null) && (newThread == null))
			manager.removeLockThread(currentOperationThread, this);
		this.currentOperationThread = newThread;
		if (currentOperationThread != null)
			manager.addLockThread(currentOperationThread, this);
	}

	/**
	 * Forces the lock to be at the given depth.
	 * Used when re-acquiring a suspended lock.
	 */
	protected void setDepth(int newDepth) {
		for (int i = depth; i < newDepth; i++) {
			manager.addLockThread(currentOperationThread, this);
		}
		this.depth = newDepth;
	}

	/**
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		return "OrderedLock (" + number + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * This lock has just been granted to a new thread (the thread waited for it).
	 * Remove the request from the queue and update both the graph and the lock.
	 */
	private synchronized void updateCurrentOperation() {
		operations.poll();
		setCurrentOperationThread(Thread.currentThread());
	}

	/**
	 * We have finished waiting on the given semaphore. Update the operation queue according
	 * to whether we succeeded in obtaining the lock.
	 *
	 * @param semaphore The semaphore that we waited on
	 * @param acquired <code>true</code> if we successfully acquired the semaphore, and <code>false</code> otherwise
	 * @return whether the lock was successfully obtained
	 */
	private synchronized boolean updateOperationQueue(Semaphore semaphore, boolean acquired) {
		// Bug 311863 - Semaphore may have been released concurrently, so check again before discarding it
		if (!acquired)
			acquired = semaphore.attempt();
		if (acquired) {
			depth++;
			updateCurrentOperation();
		} else {
			removeFromQueue(semaphore);
			manager.removeLockWaitThread(Thread.currentThread(), this);
		}
		return acquired;
	}

}
