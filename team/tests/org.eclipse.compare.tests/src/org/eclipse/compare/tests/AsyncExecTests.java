/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.internal.WorkQueue;
import org.eclipse.compare.internal.Worker;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.junit.Test;

public class AsyncExecTests {

	@Test
	public void testQueueAdd() {
		WorkQueue q = new WorkQueue();
		assertTrue(q.isEmpty());
		IRunnableWithProgress r = monitor -> {
			// Nothing to do for now
		};
		IRunnableWithProgress r2 = monitor -> {
			// Nothing to do for now
		};
		// Ensure that adding an element adds it
		q.add(r);
		assertEquals(1, q.size());
		assertTrue(q.contains(r));
		assertEquals(r, q.remove());
		assertTrue(q.isEmpty());
		// Ensure that adding an element again replaces it
		q.add(r);
		q.add(r);
		assertEquals(1, q.size());
		assertTrue(q.contains(r));
		// Ensure remove order matches add order
		q.add(r2);
		assertEquals(2, q.size());
		assertTrue(q.contains(r));
		assertTrue(q.contains(r2));
		assertEquals(r, q.remove());
		assertEquals(1, q.size());
		assertEquals(r2, q.remove());
		assertTrue(q.isEmpty());
		// Ensure remove order adjusted when same element added
		q.add(r);
		q.add(r2);
		q.add(r);
		assertEquals(2, q.size());
		assertTrue(q.contains(r));
		assertTrue(q.contains(r2));
		assertEquals(r2, q.remove());
		assertEquals(1, q.size());
		assertEquals(r, q.remove());
		assertTrue(q.isEmpty());
	}

	@Test
	public void testWorker() {
		final Worker w = new Worker("");
		final List<IRunnableWithProgress> worked = new ArrayList<>();
		IRunnableWithProgress r = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				assertTrue(w.isWorking());
				assertTrue(w.hasWork());
				worked.add(this);
			}
		};
		IRunnableWithProgress r2 = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				assertTrue(w.isWorking());
				assertTrue(w.hasWork());
				worked.add(this);
			}
		};
		// Test one task
		w.add(r);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		assertTrue(worked.isEmpty());
		w.run(new NullProgressMonitor());
		assertFalse(w.hasWork());
		assertFalse(w.isWorking());
		assertEquals(1, worked.size());
		assertEquals(r, worked.get(0));
		// Test two tasks
		worked.clear();
		w.add(r);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		w.add(r2);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		w.run(new NullProgressMonitor());
		assertFalse(w.hasWork());
		assertFalse(w.isWorking());
		assertEquals(2, worked.size());
		assertEquals(r, worked.get(0));
		assertEquals(r2, worked.get(1));
		// Test re-add order
		worked.clear();
		w.add(r);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		w.add(r2);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		w.add(r);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		w.run(new NullProgressMonitor());
		assertFalse(w.hasWork());
		assertFalse(w.isWorking());
		assertEquals(2, worked.size());
		assertEquals(r, worked.get(1));
		assertEquals(r2, worked.get(0));
	}

	@Test
	public void testCancelOnRequeue() {
		final Worker w = new Worker("");
		final List<IRunnableWithProgress> worked = new ArrayList<>();
		IRunnableWithProgress r = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				if (worked.isEmpty()) {
					worked.add(this);
					w.add(this);
					assertTrue(monitor.isCanceled());
					throw new OperationCanceledException();
				}
				assertTrue(w.isWorking());
				assertTrue(w.hasWork());
				worked.add(this);
			}
		};
		IRunnableWithProgress r2 = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				assertTrue(w.isWorking());
				assertTrue(w.hasWork());
				worked.add(this);
			}
		};
		worked.clear();
		w.add(r);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		w.add(r2);
		assertTrue(w.hasWork());
		assertFalse(w.isWorking());
		w.run(new NullProgressMonitor());
		assertFalse(w.hasWork());
		assertFalse(w.isWorking());
		assertEquals(3, worked.size());
		assertEquals(r, worked.get(0));
		assertEquals(r2, worked.get(1));
		assertEquals(r, worked.get(2));
	}
}
