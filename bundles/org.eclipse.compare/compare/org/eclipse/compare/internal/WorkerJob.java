/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class WorkerJob extends Job {

	private final Worker worker;

	public WorkerJob(String name) {
		super(name);
		worker = new Worker(name);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		worker.run(monitor);
		// reschedule to ensure we don't miss a task
		IStatus result = getResult(worker);
		schedule();
		return result;
	}

	private IStatus getResult(Worker w) {
		Throwable[] errors = w.getErrors();
		if (errors.length == 0)
			return Status.OK_STATUS;
		if (errors.length == 1)
			return new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, errors[0].getMessage(), errors[0]);
		List<IStatus> statii = new ArrayList<>();
		for (Throwable throwable : errors) {
			statii.add(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, errors[0].getMessage(), throwable));
		}
		return new MultiStatus(CompareUIPlugin.PLUGIN_ID, 0, statii.toArray(new IStatus[statii.size()]), CompareMessages.WorkerJob_0, null);
	}

	@Override
	public boolean shouldRun() {
		return worker.hasWork();
	}

	public void add(IRunnableWithProgress runnable) {
		worker.add(runnable);
		schedule();
	}

}
