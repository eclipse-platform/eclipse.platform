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

import org.eclipse.core.runtime.jobs.LockListener;
import org.junit.Assert;

/**
 * A lock listener used for testing that ensures wait/release calls are correctly paired.
 */
public class TestLockListener extends LockListener {
	private boolean waiting;

	@Override
	public synchronized void aboutToRelease() {
		waiting = false;
	}

	@Override
	public synchronized boolean aboutToWait(Thread lockOwner) {
		waiting = true;
		return false;
	}

	public synchronized void assertNotWaiting(String message) {
		Assert.assertTrue(message, !waiting);
	}

}
