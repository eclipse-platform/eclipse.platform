/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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

import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.harness.TestProgressMonitor;

/**
 * A test progress monitor that sends a signal to a barrier object when it
 * becomes blocked.
 */
class TestBlockingMonitor extends TestProgressMonitor implements IProgressMonitor {
	private TestBarrier2 barrier;
	private boolean cancelled;

	public TestBlockingMonitor(AtomicIntegerArray status, int index) {
		this(new TestBarrier2(status, index));
	}

	public TestBlockingMonitor(TestBarrier2 barrier) {
		this.barrier = barrier;
	}

	@Override
	public void clearBlocked() {
		//leave empty for now
	}

	@Override
	public boolean isCanceled() {
		return cancelled;
	}

	@Override
	public void setBlocked(IStatus reason) {
		barrier.setStatus(TestBarrier2.STATUS_BLOCKED);
	}

	@Override
	public void setCanceled(boolean b) {
		cancelled = true;
	}
}
