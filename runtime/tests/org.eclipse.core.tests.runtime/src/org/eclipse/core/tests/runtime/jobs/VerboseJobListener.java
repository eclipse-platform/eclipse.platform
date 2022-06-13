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

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;

/**
 *
 */
public class VerboseJobListener implements IJobChangeListener {
	@Override
	public void aboutToRun(IJobChangeEvent event) {
		System.out.println("[" + Thread.currentThread() + "] aboutToRun: " + event.getJob());
	}

	@Override
	public void scheduled(IJobChangeEvent event) {
		System.out.println("[" + Thread.currentThread() + "] scheduled: " + event.getJob());
	}

	@Override
	public void done(IJobChangeEvent event) {
		System.out.println("[" + Thread.currentThread() + "] finished: " + event.getJob());
	}

	@Override
	public void running(IJobChangeEvent event) {
		System.out.println("[" + Thread.currentThread() + "] running: " + event.getJob());
	}

	@Override
	public void sleeping(IJobChangeEvent event) {
		System.out.println("[" + Thread.currentThread() + "] sleeping: " + event.getJob());
	}

	@Override
	public void awake(IJobChangeEvent event) {
		System.out.println("[" + Thread.currentThread() + "] awake: " + event.getJob());
	}
}
