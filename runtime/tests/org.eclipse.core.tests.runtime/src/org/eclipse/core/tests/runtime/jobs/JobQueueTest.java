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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.internal.jobs.InternalJob;
import org.eclipse.core.internal.jobs.JobQueue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class JobQueueTest {
	class Entry extends InternalJob {
		Entry(int value) {
			super("Entry");
			setPriority(value);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			return Status.OK_STATUS;
		}
	}

	private JobQueue queue;

	@BeforeEach
	public void setUp() throws Exception {
		this.queue = new JobQueue(false);
	}

	@Test
	public void testEqualValues() {
		//if several equal values are entered, they should come out in FIFO order
		final int NUM_ENTRIES = 10;
		Entry[] entries = new Entry[NUM_ENTRIES];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = new Entry(Job.LONG);
			queue.enqueue(entries[i]);
			assertEquals(entries[0], queue.peek(), i + "");
		}
		for (int i = 0; i < entries.length; i++) {
			assertEquals(entries[i], queue.dequeue(), i + "");
		}
	}

	@Test
	public void testBasic() {
		Entry[] entries = createEntries();
		assertTrue(queue.isEmpty());
		assertNull(queue.dequeue());
		assertNull(queue.peek());
		for (Entry entry : entries) {
			queue.enqueue(entry);
			assertNotNull(queue.peek());
		}
		for (int i = 0; i < entries.length; i++) {
			queue.remove(entries[i]);
			if (i + 1 < entries.length) {
				assertNotNull(queue.peek(), i + "");
			}
		}
		assertTrue(queue.isEmpty());
		assertNull(queue.dequeue());
		assertNull(queue.peek());
		for (Entry entry : entries) {
			queue.enqueue(entry);
		}
		int count = entries.length;
		while (!queue.isEmpty()) {
			InternalJob peek = queue.peek();
			InternalJob removed = queue.dequeue();
			assertEquals(peek, removed, count + "");
			count--;
		}
		assertEquals(0, count);
	}

	private Entry[] createEntries() {
		return new Entry[] {new Entry(Job.INTERACTIVE), new Entry(Job.BUILD), new Entry(Job.INTERACTIVE), new Entry(Job.SHORT), new Entry(Job.DECORATE), new Entry(Job.LONG), new Entry(Job.SHORT), new Entry(Job.BUILD), new Entry(Job.LONG), new Entry(Job.DECORATE),};
	}
}
