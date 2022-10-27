/*******************************************************************************
 *  Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * A linked list based priority queue.
 */
public final class JobQueue implements Iterable<InternalJob> {
	/**
	 * The dummy entry sits between the head and the tail of the queue.
	 * dummy.previous() is the head, and dummy.next() is the tail.
	 */
	protected final InternalJob dummy;

	/**
	 * If true, conflicting jobs will be allowed to overtake others in the
	 * queue that have lower priority. If false, higher priority jumps can only
	 * move up the queue by overtaking jobs that they don't conflict with.
	 */
	private final boolean allowConflictOvertaking;

	private final boolean allowPriorityOvertaking;

	/**
	 * Create a new job queue.
	 */
	public JobQueue(boolean allowConflictOvertaking) {
		this(allowConflictOvertaking, true);
	}

	/**
	 * Create a new job queue.
	 */
	public JobQueue(boolean allowConflictOvertaking, boolean allowPriorityOvertaking) {
		this.allowPriorityOvertaking = allowPriorityOvertaking;
		//compareTo on dummy is never called
		dummy = new InternalJob("Queue-Head") {//$NON-NLS-1$
			@Override
			public IStatus run(IProgressMonitor m) {
				return Status.OK_STATUS;
			}
		};
		dummy.setNext(dummy);
		dummy.setPrevious(dummy);
		this.allowConflictOvertaking = allowConflictOvertaking;
	}

	/**
	 * remove all elements
	 */
	public void clear() {
		dummy.setNext(dummy);
		dummy.setPrevious(dummy);
	}

	/**
	 * Return and remove the element with highest priority, or null if empty.
	 */
	public InternalJob dequeue() {
		InternalJob toRemove = dummy.previous();
		if (toRemove == dummy)
			return null;
		return toRemove.remove();
	}

	/**
	 * Adds an item to the queue
	 */
	public void enqueue(InternalJob newEntry) {
		//assert new entry is does not already belong to some other data structure
		Assert.isTrue(newEntry.next() == null);
		Assert.isTrue(newEntry.previous() == null);
		InternalJob tail = dummy.next();
		//overtake lower priority jobs. Only overtake conflicting jobs if allowed to
		while (canOvertake(newEntry, tail))
			tail = tail.next();
		//new entry is smaller than tail
		final InternalJob tailPrevious = tail.previous();
		newEntry.setNext(tail);
		newEntry.setPrevious(tailPrevious);
		tailPrevious.setNext(newEntry);
		tail.setPrevious(newEntry);
	}

	/**
	 * Returns whether the new entry to overtake the existing queue entry.
	 * @param newEntry The entry to be added to the queue
	 * @param queueEntry The existing queue entry
	 */
	private boolean canOvertake(InternalJob newEntry, InternalJob queueEntry) {
		//can never go past the end of the queue
		if (queueEntry == dummy)
			return false;
		//if the new entry was already in the wait queue, ensure it is re-inserted in correct position (bug 211799)
		if (newEntry.getWaitQueueStamp() > 0 && newEntry.getWaitQueueStamp() < queueEntry.getWaitQueueStamp())
			return true;
		//if the new entry has lower priority, there is no need to overtake the existing entry
		if (allowPriorityOvertaking && queueEntry.compareTo(newEntry) >= 0)
			return false;
		//the new entry has higher priority, but only overtake the existing entry if the queue allows it
		return allowConflictOvertaking || !newEntry.isConflicting(queueEntry);
	}

	/**
	 * Removes the given element from the queue.
	 */
	public void remove(InternalJob toRemove) {
		toRemove.remove();
		//previous of toRemove might now bubble up
	}

	/**
	 * The given object has changed priority. Reshuffle the heap until it is
	 * valid.
	 */
	public void resort(InternalJob entry) {
		remove(entry);
		enqueue(entry);
	}

	/**
	 * Returns true if the queue is empty, and false otherwise.
	 */
	public boolean isEmpty() {
		return dummy.next() == dummy;
	}

	/**
	 * Return greatest element without removing it, or null if empty
	 */
	public InternalJob peek() {
		return dummy.previous() == dummy ? null : dummy.previous();
	}

	@Override
	public Iterator<InternalJob> iterator() {
		return new Iterator<>() {
			InternalJob pointer = dummy;

			@Override
			public boolean hasNext() {
				if (pointer.previous() == dummy)
					pointer = null;
				else
					pointer = pointer.previous();
				return pointer != null;
			}

			@Override
			public InternalJob next() {
				return pointer;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	/** for debugging only **/
	@Override
	public String toString() {
		List<InternalJob> all = new ArrayList<>();
		iterator().forEachRemaining(all::add);
		return all.toString();
	}
}
