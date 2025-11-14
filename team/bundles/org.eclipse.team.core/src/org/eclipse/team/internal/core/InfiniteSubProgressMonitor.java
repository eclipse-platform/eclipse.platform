/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Provides an infinite progress monitor by subdividing by half repeatedly.
 *
 * The ticks parameter represents the number of ticks shown in the progress dialog
 * (or propogated up to a parent IProgressMonitor). The totalWork parameter provided
 * in actually a hint used to determine how work is translated into ticks.
 * The number of totalWork that can actually be worked is n*totalWork/2 where
 * 2^n = totalWork. What this means is that if you provide a totalWork of 32 (2^5) than
 * the maximum number of ticks is 5*32/2 = 80.
 */
public class InfiniteSubProgressMonitor implements IProgressMonitor {

	private final SubMonitor delegate;
	int totalWork;
	int halfWay;
	int currentIncrement;
	int nextProgress;
	int worked;

	/**
	 * Constructor for InfiniteSubProgressMonitor.
	 */
	public InfiniteSubProgressMonitor(IProgressMonitor monitor, int ticks) {
		this(monitor, ticks, SubMonitor.SUPPRESS_NONE);
	}

	/**
	 * Constructor for InfiniteSubProgressMonitor.
	 */
	public InfiniteSubProgressMonitor(IProgressMonitor monitor, int ticks, int style) {
		if (monitor instanceof SubMonitor subMonitor) {
			this.delegate = subMonitor.split(ticks, style);
		} else {
			// For non-SubMonitor, convert it first then split
			SubMonitor converted = SubMonitor.convert(monitor);
			this.delegate = converted.split(ticks, style);
		}
	}

	@Override
	public void beginTask(String name, int totalWork) {
		delegate.setTaskName(name);
		delegate.setWorkRemaining(totalWork);
		this.totalWork = totalWork;
		this.halfWay = totalWork / 2;
		this.currentIncrement = 1;
		this.nextProgress = currentIncrement;
		this.worked = 0;
	}

	@Override
	public void worked(int work) {
		if (worked >= totalWork) {
			return;
		}
		if (--nextProgress <= 0) {
			delegate.worked(1);
			worked++;
			if (worked >= halfWay) {
				// we have passed the current halfway point, so double the
				// increment and reset the halfway point.
				currentIncrement *= 2;
				halfWay += (totalWork - halfWay) / 2;
			}
			// reset the progress counter to another full increment
			nextProgress = currentIncrement;
		}
	}

	/**
	 * Don't allow clearing of the subtask. This will stop the flickering
	 * of the subtask in the progress dialogs.
	 *
	 * @see IProgressMonitor#subTask(String)
	 */
	@Override
	public void subTask(String name) {
		if(name != null && ! name.isEmpty()) {
			delegate.subTask(name);
		}
	}

	@Override
	public void done() {
		// SubMonitor doesn't require explicit done() calls, but delegate for compatibility
		delegate.done();
	}

	@Override
	public void internalWorked(double work) {
		delegate.internalWorked(work);
	}

	@Override
	public boolean isCanceled() {
		return delegate.isCanceled();
	}

	@Override
	public void setCanceled(boolean value) {
		delegate.setCanceled(value);
	}

	@Override
	public void setTaskName(String name) {
		delegate.setTaskName(name);
	}
}
