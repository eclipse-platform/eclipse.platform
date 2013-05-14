/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class WorkerJob extends Job {

	private final Worker worker;
	
	public WorkerJob(String name) {
		super(name);
		worker = new Worker(name);
	}

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
		List statii = new ArrayList();
		for (int i = 0; i < errors.length; i++) {
			Throwable throwable = errors[i];
			statii.add(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, errors[0].getMessage(), throwable));
		}
		return new MultiStatus(CompareUIPlugin.PLUGIN_ID, 0, (IStatus[]) statii.toArray(new IStatus[statii.size()]), CompareMessages.WorkerJob_0, null);
	}
	
	public boolean shouldRun() {
		return worker.hasWork();
	}
	
	public void add(IRunnableWithProgress runnable) {
		worker.add(runnable);
		schedule();
	}

}
