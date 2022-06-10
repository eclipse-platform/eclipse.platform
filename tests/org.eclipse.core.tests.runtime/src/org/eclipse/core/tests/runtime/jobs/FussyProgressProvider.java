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

import java.util.ArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.core.tests.harness.FussyProgressMonitor;

/**
 * Dispatches fussy progress monitors, and sanity checks them when finished.
 */
public class FussyProgressProvider extends ProgressProvider {
	private ArrayList<FussyProgressMonitor> monitors = new ArrayList<>();

	@Override
	public IProgressMonitor createMonitor(Job job) {
		//only give a fussy monitor to jobs from runtime tests
		String name = job == null ? "" : job.getClass().getName();
		if (!name.contains("core.tests.runtime") && !name.contains("core.tests.internal.runtime") && !name.contains("core.tests.harness"))
			return new NullProgressMonitor();
		FussyProgressMonitor result = new FussyProgressMonitor(job);
		monitors.add(result);
		return result;
	}

	public void sanityCheck() {
		for (FussyProgressMonitor monitor : monitors)
			monitor.sanityCheck();
	}

	@Override
	public IProgressMonitor getDefaultMonitor() {
		return createMonitor(null);
	}
}
