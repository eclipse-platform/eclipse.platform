/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A job that runs the given number of times with a small delay between runs
 * Can report how many times it has run.
 */
public class RepeatingJob extends Job {
	final int repeats;
	int runCount = 0;
	private static final int DELAY = 1;
	private Object myFamily;

	public RepeatingJob(String name, int repeats) {
		super(name);
		this.repeats = repeats;
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == myFamily;
	}

	/**
	 * Returns the number of times this job has executed.
	 */
	public int getRunCount() {
		return runCount;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		schedule(DELAY);
		runCount++;
		return Status.OK_STATUS;
	}

	public void setFamily(Object family) {
		this.myFamily = family;
	}

	@Override
	public boolean shouldRun() {
		return runCount < repeats;
	}
}
