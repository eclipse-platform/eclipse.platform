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
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Stores all the relationships between locks (rules are also considered locks),
 * and the threads that own them. All the relationships are stored in a 2D integer array.
 * The rows in the array are threads, while the columns are locks.
 * Two corresponding arrayLists store the actual threads and locks.
 * The index of a thread in the first arrayList is the index of the row in the graph.
 * The index of a lock in the second arrayList is the index of the column in the graph.
 * An entry greater than 0 in the graph is the number of times a thread in the entry's row
 * acquired the lock in the entry's column.
 * An entry of -1 means that the thread is waiting to acquire the lock.
 * An entry of 0 means that the thread and the lock have no relationship.
 *
 * The difference between rules and locks is that locks can be suspended, while
 * rules are implicit locks and as such cannot be suspended.
 * To resolve deadlock, the graph will first try to find a thread that only owns
 * locks. Failing that, it will find a thread in the deadlock that owns at least
 * one lock and suspend it.
 *
 * Deadlock can only occur among locks, or among locks in combination with rules.
 * Deadlock among rules only is impossible. Therefore, in any deadlock one can always
 * find a thread that owns at least one lock that can be suspended.
 *
 * The implementation of the graph assumes that a thread can only own 1 rule at
 * any one time. It can acquire that rule several times, but a thread cannot
 * acquire 2 non-conflicting rules at the same time.
 *
 * The implementation of the graph will sometimes also find and resolve bogus deadlocks.
 * 		graph:				assuming this rule hierarchy:
 * 		   R2 R3 L1						R1
 * 		J1  1  0  0					   /  \
 * 		J2  0  1 -1					  R2  R3
 * 		J3 -1  0  1
 *
 * If in the above situation job4 decides to acquire rule1, then the graph will transform
 * to the following:
 * 		   R2 R3 R1 L1
 * 		J1  1  0  1  0
 * 		J2  1  1  1 -1
 * 		J3 -1  0  0  1
 * 		J4  0  0 -1  0
 *
 * and the graph will assume that job2 and job3 are deadlocked and suspend lock1 of job3.
 * The reason the deadlock is bogus is that the deadlock is unlikely to actually happen (the threads
 * are currently not deadlocked, but might deadlock later on when it is too late to detect it)
 * Therefore, in order to make sure that no deadlock is possible,
 * the deadlock will still be resolved at this point.
 */
public class DeadlockDetector {
	private static int NO_STATE = 0;
	//state variables in the graph
	private static int WAITING_FOR_LOCK = -1;
	//empty matrix
	private static final int[][] EMPTY_MATRIX = new int[0][0];
	//matrix of relationships between threads and locks
	private int[][] graph = EMPTY_MATRIX;
	//index is column in adjacency matrix for the lock
	private final ArrayList<ISchedulingRule> locks = new ArrayList<>();
	//index is row in adjacency matrix for the thread
	private final ArrayList<Thread> lockThreads = new ArrayList<>();
	//whether the graph needs to be resized
	private boolean resize = false;
	private static volatile boolean noDeadlockReport;

	/**
	 * Recursively check if any of the threads that prevent the current thread from running
	 * are actually deadlocked with the current thread.
	 * Add the threads that form deadlock to the deadlockedThreads list.
	 */
	private boolean addCycleThreads(ArrayList<Thread> deadlockedThreads, Thread next) {
		//get the thread that block the given thread from running
		Thread[] blocking = blockingThreads(next);
		//if the thread is not blocked by other threads, then it is not part of a deadlock
		if (blocking.length == 0)
			return false;
		boolean inCycle = false;
		for (Thread element : blocking) {
			//if we have already visited the given thread, then we found a cycle
			if (deadlockedThreads.contains(element)) {
				inCycle = true;
			} else {
				//otherwise, add the thread to our list and recurse deeper
				deadlockedThreads.add(element);
				//if the thread is not part of a cycle, remove it from the list
				if (addCycleThreads(deadlockedThreads, element))
					inCycle = true;
				else
					deadlockedThreads.remove(element);
			}
		}
		return inCycle;
	}

	/**
	 * Get the thread(s) that own the lock this thread is waiting for.
	 */
	private Thread[] blockingThreads(Thread current) {
		//find the lock this thread is waiting for
		ISchedulingRule lock = (ISchedulingRule) getWaitingLock(current);
		return getThreadsOwningLock(lock);
	}

	/**
	 * Check that the addition of a waiting thread did not produce deadlock.
	 * If deadlock is detected return true, else return false.
	 */
	private boolean checkWaitCycles(int[] waitingThreads, int lockIndex) {
		/**
		 * find the lock that this thread is waiting for
		 * recursively check if this is a cycle (i.e. a thread waiting on itself)
		 */
		for (int i = 0; i < graph.length; i++) {
			if (graph[i][lockIndex] > NO_STATE) {
				if (waitingThreads[i] > NO_STATE) {
					return true;
				}
				//keep track that we already visited this thread
				waitingThreads[i]++;
				for (int j = 0; j < graph[i].length; j++) {
					if (graph[i][j] == WAITING_FOR_LOCK) {
						if (checkWaitCycles(waitingThreads, j))
							return true;
					}
				}
				//this thread is not involved in a cycle yet, so remove the visited flag
				waitingThreads[i]--;
			}
		}
		return false;
	}

	/**
	 * Returns true IFF the matrix contains a row for the given thread.
	 * (meaning the given thread either owns locks or is waiting for locks)
	 */
	boolean contains(Thread t) {
		return lockThreads.contains(t);
	}

	/**
	 * A new rule was just added to the graph.
	 * Find a rule it conflicts with and update the new rule with the number of times
	 * it was acquired implicitly when threads acquired conflicting rule.
	 */
	private void fillPresentEntries(ISchedulingRule newLock, int lockIndex) {
		//fill in the entries for the new rule from rules it conflicts with
		for (int j = 0; j < locks.size(); j++) {
			if ((j != lockIndex) && (newLock.isConflicting(locks.get(j)))) {
				for (int[] g : graph) {
					if ((g[j] > NO_STATE) && (g[lockIndex] == NO_STATE)) {
						g[lockIndex] = g[j];
					}
				}
			}
		}
		//now back fill the entries for rules the current rule conflicts with
		for (int j = 0; j < locks.size(); j++) {
			if ((j != lockIndex) && (newLock.isConflicting(locks.get(j)))) {
				for (int[] g : graph) {
					if ((g[lockIndex] > NO_STATE) && (g[j] == NO_STATE)) {
						g[j] = g[lockIndex];
					}
				}
			}
		}
	}

	/**
	 * Returns all the locks owned by the given thread
	 */
	private Object[] getOwnedLocks(Thread current) {
		ArrayList<ISchedulingRule> ownedLocks = new ArrayList<>(1);
		int index = indexOf(current, false);

		for (int j = 0; j < graph[index].length; j++) {
			if (graph[index][j] > NO_STATE)
				ownedLocks.add(locks.get(j));
		}
		if (ownedLocks.isEmpty())
			Assert.isLegal(false, "A thread with no locks is part of a deadlock."); //$NON-NLS-1$
		return ownedLocks.toArray();
	}

	/**
	 * Returns an array of threads that form the deadlock (usually 2).
	 */
	private Thread[] getThreadsInDeadlock(Thread cause) {
		ArrayList<Thread> deadlockedThreads = new ArrayList<>(2);
		/**
		 * if the thread that caused deadlock doesn't own any locks, then it is not part
		 * of the deadlock (it just caused it because of a rule it tried to acquire)
		 */
		if (ownsLocks(cause))
			deadlockedThreads.add(cause);
		addCycleThreads(deadlockedThreads, cause);
		return deadlockedThreads.toArray(new Thread[deadlockedThreads.size()]);
	}

	/**
	 * Returns the thread(s) that own the given lock.
	 */
	private Thread[] getThreadsOwningLock(ISchedulingRule rule) {
		if (rule == null)
			return new Thread[0];
		int lockIndex = indexOf(rule, false);
		ArrayList<Thread> blocking = new ArrayList<>(1);
		for (int i = 0; i < graph.length; i++) {
			if (graph[i][lockIndex] > NO_STATE)
				blocking.add(lockThreads.get(i));
		}
		if ((blocking.isEmpty()) && (JobManager.DEBUG_LOCKS))
			System.out.println("Lock " + rule + " is involved in deadlock but is not owned by any thread."); //$NON-NLS-1$ //$NON-NLS-2$
		if ((blocking.size() > 1) && (rule instanceof ILock) && (JobManager.DEBUG_LOCKS))
			System.out.println("Lock " + rule + " is owned by more than 1 thread, but it is not a rule."); //$NON-NLS-1$ //$NON-NLS-2$
		return blocking.toArray(new Thread[blocking.size()]);
	}

	/**
	 * Returns the lock the given thread is waiting for.
	 */
	private Object getWaitingLock(Thread current) {
		int index = indexOf(current, false);
		//find the lock that this thread is waiting for
		for (int j = 0; j < graph[index].length; j++) {
			if (graph[index][j] == WAITING_FOR_LOCK)
				return locks.get(j);
		}
		//it can happen that a thread is not waiting for any lock (it is not really part of the deadlock)
		return null;
	}

	/**
	 * Returns the index of the given lock in the lock array. If the lock is
	 * not present in the array, it is added to the end.
	 */
	private int indexOf(ISchedulingRule lock, boolean add) {
		int index = locks.indexOf(lock);
		if ((index < 0) && add) {
			locks.add(lock);
			resize = true;
			index = locks.size() - 1;
		}
		return index;
	}

	/**
	 * Returns the index of the given thread in the thread array. If the thread
	 * is not present in the array, it is added to the end.
	 */
	private int indexOf(Thread owner, boolean add) {
		int index = lockThreads.indexOf(owner);
		if ((index < 0) && add) {
			lockThreads.add(owner);
			resize = true;
			index = lockThreads.size() - 1;
		}
		return index;
	}

	/**
	 * Returns true IFF the adjacency matrix is empty.
	 */
	boolean isEmpty() {
		return (locks.isEmpty()) && (lockThreads.isEmpty()) && (graph.length == 0);
	}

	/**
	 * The given lock was acquired by the given thread.
	 */
	void lockAcquired(Thread owner, ISchedulingRule lock) {
		int lockIndex = indexOf(lock, true);
		int threadIndex = indexOf(owner, true);
		if (resize)
			resizeGraph();
		if (graph[threadIndex][lockIndex] == WAITING_FOR_LOCK)
			graph[threadIndex][lockIndex] = NO_STATE;
		/**
		 * acquire all locks that conflict with the given lock
		 * or conflict with a lock the given lock will acquire implicitly
		 * (locks are acquired implicitly when a conflicting lock is acquired)
		 */
		ArrayList<ISchedulingRule> conflicting = new ArrayList<>(1);
		//only need two passes through all the locks to pick up all conflicting rules
		int NUM_PASSES = 2;
		conflicting.add(lock);
		graph[threadIndex][lockIndex]++;
		for (int i = 0; i < NUM_PASSES; i++) {
			for (int k = 0; k < conflicting.size(); k++) {
				ISchedulingRule current = conflicting.get(k);
				for (int j = 0; j < locks.size(); j++) {
					ISchedulingRule possible = locks.get(j);
					if (current.isConflicting(possible) && !conflicting.contains(possible)) {
						conflicting.add(possible);
						graph[threadIndex][j]++;
					}
				}
			}
		}
	}

	/**
	 * The given lock was released by the given thread. Update the graph.
	 */
	void lockReleased(Thread owner, ISchedulingRule lock) {
		int lockIndex = indexOf(lock, false);
		int threadIndex = indexOf(owner, false);
		//make sure the lock and thread exist in the graph
		if (threadIndex < 0) {
			if (JobManager.DEBUG_LOCKS)
				System.out.println("[lockReleased] Lock " + lock + " was already released by thread " + owner.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (lockIndex < 0) {
			if (JobManager.DEBUG_LOCKS)
				System.out.println("[lockReleased] Thread " + owner.getName() + " already released lock " + lock); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		//if this lock was suspended, set it to NO_STATE
		if ((lock instanceof ILock) && (graph[threadIndex][lockIndex] == WAITING_FOR_LOCK)) {
			graph[threadIndex][lockIndex] = NO_STATE;
			return;
		}
		//release all locks that conflict with the given lock
		//or release all rules that are owned by the given thread, if we are releasing a rule
		for (int j = 0; j < graph[threadIndex].length; j++) {
			if ((lock.isConflicting(locks.get(j))) || (!(lock instanceof ILock) && !(locks.get(j) instanceof ILock) && (graph[threadIndex][j] > NO_STATE))) {
				if (graph[threadIndex][j] == NO_STATE) {
					if (JobManager.DEBUG_LOCKS)
						System.out.println("[lockReleased] More releases than acquires for thread " + owner.getName() + " and lock " + lock); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					graph[threadIndex][j]--;
				}
			}
		}
		//if this thread just released the given lock, try to simplify the graph
		if (graph[threadIndex][lockIndex] == NO_STATE)
			reduceGraph(threadIndex, lock);
	}

	/**
	 * The given scheduling rule is no longer used because the job that invoked it is done.
	 * Release this rule regardless of how many times it was acquired.
	 */
	void lockReleasedCompletely(Thread owner, ISchedulingRule rule) {
		int ruleIndex = indexOf(rule, false);
		int threadIndex = indexOf(owner, false);
		//need to make sure that the given thread and rule were not already removed from the graph
		if (threadIndex < 0) {
			if (JobManager.DEBUG_LOCKS)
				System.out.println("[lockReleasedCompletely] Lock " + rule + " was already released by thread " + owner.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (ruleIndex < 0) {
			if (JobManager.DEBUG_LOCKS)
				System.out.println("[lockReleasedCompletely] Thread " + owner.getName() + " already released lock " + rule); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		/**
		 * set all rules that are owned by the given thread to NO_STATE
		 * (not just rules that conflict with the rule we are releasing)
		 * if we are releasing a lock, then only update the one entry for the lock
		 */
		for (int j = 0; j < graph[threadIndex].length; j++) {
			if (!(locks.get(j) instanceof ILock) && (graph[threadIndex][j] > NO_STATE))
				graph[threadIndex][j] = NO_STATE;
		}
		reduceGraph(threadIndex, rule);
	}

	/**
	 * The given thread could not get the given lock and is waiting for it.
	 * Update the graph.
	 */
	Deadlock lockWaitStart(Thread client, ISchedulingRule lock) {
		setToWait(client, lock, false);
		int lockIndex = indexOf(lock, false);
		int[] temp = new int[lockThreads.size()];
		//check if the addition of the waiting thread caused deadlock
		if (!checkWaitCycles(temp, lockIndex))
			return null;
		//there is a deadlock in the graph
		Thread[] threads = getThreadsInDeadlock(client);
		//find a thread whose locks can be suspended to resolve the deadlock
		Thread candidate = resolutionCandidate(threads);
		ISchedulingRule[] locksToSuspend = realLocksForThread(candidate);
		Deadlock deadlock = new Deadlock(threads, locksToSuspend, candidate);
		reportDeadlock(deadlock);
		if (JobManager.DEBUG_DEADLOCK)
			throw new IllegalStateException("Deadlock detected. Caused by thread " + client.getName() + '.'); //$NON-NLS-1$
		// Update the graph to indicate that the locks will now be suspended.
		// To indicate that the lock will be suspended, we set the thread to wait for the lock.
		// When the lock is forced to be released, the entry will be cleared.
		for (ISchedulingRule element : locksToSuspend)
			setToWait(deadlock.getCandidate(), element, true);
		return deadlock;
	}

	/**
	 * The given thread has stopped waiting for the given lock.
	 * Update the graph.
	 * If the lock has already been granted, then it isn't removed.
	 */
	void lockWaitStop(Thread owner, ISchedulingRule lock) {
		int lockIndex = indexOf(lock, false);
		int threadIndex = indexOf(owner, false);
		//make sure the thread and lock exist in the graph
		if (threadIndex < 0) {
			if (JobManager.DEBUG_LOCKS)
				System.out.println("Thread " + owner.getName() + " was already removed."); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (lockIndex < 0) {
			if (JobManager.DEBUG_LOCKS)
				System.out.println("Lock " + lock + " was already removed."); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (graph[threadIndex][lockIndex] != WAITING_FOR_LOCK) {
			// Lock has already been granted, nothing to do...
			if (JobManager.DEBUG_LOCKS)
				System.out.println("Lock " + lock + " already granted to depth: " + graph[threadIndex][lockIndex]); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		graph[threadIndex][lockIndex] = NO_STATE;
		reduceGraph(threadIndex, lock);
	}

	/**
	 * Returns true IFF the given thread owns a single lock
	 */
	private boolean ownsLocks(Thread cause) {
		int threadIndex = indexOf(cause, false);
		for (int j = 0; j < graph[threadIndex].length; j++) {
			if (graph[threadIndex][j] > NO_STATE)
				return true;
		}
		return false;
	}

	/**
	 * Returns true IFF the given thread owns a single real lock.
	 * A real lock is a lock that can be suspended.
	 */
	private boolean ownsRealLocks(Thread owner) {
		int threadIndex = indexOf(owner, false);
		for (int j = 0; j < graph[threadIndex].length; j++) {
			if (graph[threadIndex][j] > NO_STATE) {
				Object lock = locks.get(j);
				if (lock instanceof ILock)
					return true;
			}
		}
		return false;
	}

	/**
	 * Return true IFF this thread owns rule locks (i.e. implicit locks which
	 * cannot be suspended)
	 */
	private boolean ownsRuleLocks(Thread owner) {
		int threadIndex = indexOf(owner, false);
		for (int j = 0; j < graph[threadIndex].length; j++) {
			if (graph[threadIndex][j] > NO_STATE) {
				Object lock = locks.get(j);
				if (!(lock instanceof ILock))
					return true;
			}
		}
		return false;
	}

	/**
	 * Returns an array of real locks that are owned by the given thread.
	 * Real locks are locks that implement the ILock interface and can be suspended.
	 */
	private ISchedulingRule[] realLocksForThread(Thread owner) {
		int threadIndex = indexOf(owner, false);
		ArrayList<ISchedulingRule> ownedLocks = new ArrayList<>(1);
		for (int j = 0; j < graph[threadIndex].length; j++) {
			if ((graph[threadIndex][j] > NO_STATE) && (locks.get(j) instanceof ILock))
				ownedLocks.add(locks.get(j));
		}
		if (ownedLocks.isEmpty())
			Assert.isLegal(false, "A thread with no real locks was chosen to resolve deadlock."); //$NON-NLS-1$
		return ownedLocks.toArray(new ISchedulingRule[ownedLocks.size()]);
	}

	/**
	 * The matrix has been simplified. Check if any unnecessary rows or columns
	 * can be removed.
	 */
	private void reduceGraph(int row, ISchedulingRule lock) {
		int numLocks = locks.size();
		boolean[] emptyColumns = new boolean[numLocks];

		/**
		 * find all columns that could possibly be empty
		 * (consist of locks which conflict with the given lock, or of locks which are rules)
		 */
		for (int j = 0; j < numLocks; j++) {
			if ((lock.isConflicting(locks.get(j))) || !(locks.get(j) instanceof ILock))
				emptyColumns[j] = true;
		}

		boolean rowEmpty = true;
		int numEmpty = 0;
		//check if the given row is empty
		for (int j = 0; j < graph[row].length; j++) {
			if (graph[row][j] != NO_STATE) {
				rowEmpty = false;
				break;
			}
		}
		/**
		 * Check if the possibly empty columns are actually empty.
		 * If a column is actually empty, remove the corresponding lock from the list of locks
		 * Start at the last column so that when locks are removed from the list,
		 * the index of the remaining locks is unchanged. Store the number of empty columns.
		 */
		for (int j = emptyColumns.length - 1; j >= 0; j--) {
			for (int[] element : graph) {
				if (emptyColumns[j] && (element[j] != NO_STATE)) {
					emptyColumns[j] = false;
					break;
				}
			}
			if (emptyColumns[j]) {
				locks.remove(j);
				numEmpty++;
			}
		}
		//if no columns or rows are empty, return right away
		if ((numEmpty == 0) && (!rowEmpty))
			return;

		if (rowEmpty)
			lockThreads.remove(row);

		//new graph (the list of locks and the list of threads are already updated)
		final int numThreads = lockThreads.size();
		numLocks = locks.size();
		//optimize empty graph case
		if (numThreads == 0 && numLocks == 0) {
			graph = EMPTY_MATRIX;
			return;
		}
		int[][] tempGraph = new int[numThreads][numLocks];

		//the number of rows we need to skip to get the correct entry from the old graph
		int numRowsSkipped = 0;
		for (int i = 0; i < graph.length - numRowsSkipped; i++) {
			if ((i == row) && rowEmpty) {
				numRowsSkipped++;
				//check if we need to skip the last row
				if (i >= graph.length - numRowsSkipped)
					break;
			}
			//the number of columns we need to skip to get the correct entry from the old graph
			//needs to be reset for every new row
			int numColsSkipped = 0;
			for (int j = 0; j < graph[i].length - numColsSkipped; j++) {
				while (emptyColumns[j + numColsSkipped]) {
					numColsSkipped++;
					//check if we need to skip the last column
					if (j >= graph[i].length - numColsSkipped)
						break;
				}
				//need to break out of the outer loop
				if (j >= graph[i].length - numColsSkipped)
					break;
				tempGraph[i][j] = graph[i + numRowsSkipped][j + numColsSkipped];
			}
		}
		graph = tempGraph;
		Assert.isTrue(numThreads == graph.length, "Rows and threads don't match."); //$NON-NLS-1$
		Assert.isTrue(numLocks == ((graph.length > 0) ? graph[0].length : 0), "Columns and locks don't match."); //$NON-NLS-1$
	}

	/**
	 * For tests that are supposed to create deadlocks.
	 */
	public static void runSilent(Runnable runnable) {
		DeadlockDetector.noDeadlockReport = true;
		try {
			runnable.run();
		} finally {
			DeadlockDetector.noDeadlockReport = false;
		}
	}

	/**
	 * Adds a 'deadlock detected' message to the log with a stack trace.
	 */
	private void reportDeadlock(Deadlock deadlock) {
		if (noDeadlockReport) {
			return;
		}
		String msg = "Deadlock detected. All locks owned by thread " + deadlock.getCandidate().getName() + " will be suspended."; //$NON-NLS-1$ //$NON-NLS-2$
		MultiStatus main = new MultiStatus(JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, msg, new IllegalStateException());
		Thread[] threads = deadlock.getThreads();
		for (Thread thread : threads) {
			Object[] ownedLocks = getOwnedLocks(thread);
			Object waitLock = getWaitingLock(thread);
			StringBuilder buf = new StringBuilder("Thread "); //$NON-NLS-1$
			buf.append(thread.getName());
			buf.append(" has locks: "); //$NON-NLS-1$
			for (int j = 0; j < ownedLocks.length; j++) {
				buf.append(ownedLocks[j]);
				buf.append((j < ownedLocks.length - 1) ? ", " : " "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			buf.append("and is waiting for lock "); //$NON-NLS-1$
			buf.append(waitLock);
			StackTraceElement[] stackTrace = thread.getStackTrace();
			if (stackTrace.length > 0) {
				buf.append(System.lineSeparator());
				for (StackTraceElement stackTraceElement : stackTrace) {
					buf.append('\t');
					buf.append("at "); //$NON-NLS-1$
					buf.append(stackTraceElement);
					buf.append(System.lineSeparator());
				}
			}
			Status child = new Status(IStatus.ERROR, JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, buf.toString(), null);
			main.add(child);
		}
		RuntimeLog.log(main);
	}

	/**
	 * The number of threads/locks in the graph has changed. Update the
	 * underlying matrix.
	 */
	private void resizeGraph() {
		// a new row and/or a new column was added to the graph.
		// since new rows/columns are always added to the end, just transfer
		// old entries to the new graph, with the same indices.
		final int newRows = lockThreads.size();
		final int newCols = locks.size();
		//optimize 0x0 and 1x1 matrices
		if (newRows == 0 && newCols == 0) {
			graph = EMPTY_MATRIX;
			return;
		}
		int[][] tempGraph = new int[newRows][newCols];
		for (int i = 0; i < graph.length; i++)
			System.arraycopy(graph[i], 0, tempGraph[i], 0, graph[i].length);
		graph = tempGraph;
		resize = false;
	}

	/**
	 * Get the thread whose locks can be suspended. (i.e. all locks it owns are
	 * actual locks and not rules). Return the first thread in the array by default.
	 */
	private Thread resolutionCandidate(Thread[] candidates) {
		//first look for a candidate that has no scheduling rules
		for (Thread candidate : candidates) {
			if (!ownsRuleLocks(candidate)) {
				return candidate;
			}
		}
		//next look for any candidate with a real lock (a lock that can be suspended)
		for (Thread candidate : candidates) {
			if (ownsRealLocks(candidate))
				return candidate;
		}
		//unnecessary, return the first entry in the array by default
		return candidates[0];
	}

	/**
	 * The given thread is waiting for the given lock. Update the graph.
	 */
	private void setToWait(Thread owner, ISchedulingRule lock, boolean suspend) {
		boolean needTransfer = false;
		/**
		 * if we are adding an entry where a thread is waiting on a scheduling rule,
		 * then we need to transfer all positive entries for a conflicting rule to the
		 * newly added rule in order to synchronize the graph.
		 */
		if (!suspend && !(lock instanceof ILock))
			needTransfer = true;
		int lockIndex = indexOf(lock, !suspend);
		int threadIndex = indexOf(owner, !suspend);
		if (resize)
			resizeGraph();

		graph[threadIndex][lockIndex] = WAITING_FOR_LOCK;
		if (needTransfer)
			fillPresentEntries(lock, lockIndex);
	}

	/**
	 * Prints out the current matrix to standard output.
	 * Only used for debugging.
	 */
	public String toDebugString() {
		StringWriter sWriter = new StringWriter();
		PrintWriter out = new PrintWriter(sWriter, true);
		out.println(" :: "); //$NON-NLS-1$
		for (ISchedulingRule lock : locks) {
			out.print(" " + lock + ','); //$NON-NLS-1$
		}
		out.println();
		for (int i = 0; i < graph.length; i++) {
			out.print(" " + lockThreads.get(i).getName() + " : "); //$NON-NLS-1$ //$NON-NLS-2$
			for (int j = 0; j < graph[i].length; j++) {
				out.print(" " + graph[i][j] + ','); //$NON-NLS-1$
			}
			out.println();
		}
		out.println("-------"); //$NON-NLS-1$
		return sWriter.toString();
	}
}
