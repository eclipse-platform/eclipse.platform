/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.filesystem.local;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class provides a simulation of progress. This is useful
 * for situations where computing the amount of work to do in advance
 * is too costly.  The monitor will accept any number of calls to
 * {@link #worked()}, and will scale the actual reported work appropriately
 * so that the progress never quite completes.
 */
public class InfiniteProgress {
	private final int MAX_TICKS = 172; // will be reached after ~ 1 Billion #worked()
	private int worked;
	private int nextTickAfter = 4;
	private int workReported;
	private final IProgressMonitor monitor;

	protected InfiniteProgress(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	public void beginTask(String name) {
		monitor.beginTask(name, MAX_TICKS);
	}

	public synchronized void subTask(String name) {
		monitor.subTask(name);
	}

	public synchronized void worked() {
		worked += 1;
		if (worked > nextTickAfter) {
			worked = 0;
			// starting with linear progress converging to asymptotic logarithmic progress:
			nextTickAfter = 1 + (int) (nextTickAfter * 1.1f);
			if (workReported < MAX_TICKS) {
				workReported++;
				monitor.worked(1);
			}
		}
	}

	public boolean isCanceled() {
		return monitor.isCanceled();
	}
}
