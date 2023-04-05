/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.function.Function;
import org.eclipse.core.runtime.jobs.LockListener;
import org.junit.Assert;

/**
 * A lock listener used for testing that ensures wait/release calls are correctly paired.
 */
public class TestLockListener extends LockListener {
	private boolean hasBeenWaiting;
	private boolean waiting;
	private final Runnable executeWhenAboutToWait;

	public TestLockListener() {
		this(Function::identity);
	}

	/**
	 * Instantiates a lock listener that runs the given runnable when
	 * {@link #aboutToWait} is called on this listener.
	 */
	public TestLockListener(Runnable executeWhenAboutToWait) {
		this.executeWhenAboutToWait = executeWhenAboutToWait;
	}

	@Override
	public synchronized void aboutToRelease() {
		waiting = false;
	}

	@Override
	public synchronized boolean aboutToWait(Thread lockOwner) {
		hasBeenWaiting = true;
		waiting = true;
		executeWhenAboutToWait.run();
		return false;
	}

	/**
	 * Asserts that {@link #aboutToWait(Thread)} and {@link #aboutToRelease()} have
	 * been called in pairs or not at all.
	 *
	 * @param message The assertion message
	 */
	public synchronized void assertNotWaiting(String message) {
		Assert.assertTrue(message, !waiting);
	}

	/**
	 * Asserts that {@link #aboutToWait} has been called on this listener at least
	 * once.
	 *
	 * @param message The assertion message
	 */
	public synchronized void assertHasBeenWaiting(String message) {
		Assert.assertTrue(message, hasBeenWaiting);
	}

}
