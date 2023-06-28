/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.core.externaltools.internal.launchConfigurations;


import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.IProcess;

/**
 * Refreshes resources as specified by a launch configuration, when
 * an associated process terminates.
 */
public class BackgroundResourceRefresher implements IDebugEventSetListener  {

	private ILaunchConfiguration fConfiguration;
	private IProcess fProcess;



	public BackgroundResourceRefresher(ILaunchConfiguration configuration, IProcess process) {
		fConfiguration = configuration;
		fProcess = process;
	}

	/**
	 * If the process has already terminated, resource refreshing is done
	 * immediately in the current thread. Otherwise, refreshing is done when the
	 * process terminates.
	 */
	public void startBackgroundRefresh() {
		synchronized (fProcess) {
			if (fProcess.isTerminated()) {
				refresh();
			} else {
				DebugPlugin.getDefault().addDebugEventListener(this);
			}
		}
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getSource() == fProcess && event.getKind() == DebugEvent.TERMINATE) {
				DebugPlugin.getDefault().removeDebugEventListener(this);
				refresh();
				break;
			}
		}
	}

	/**
	 * Submits a job to do the refresh
	 */
	protected void refresh() {
		Job.create(ExternalToolsProgramMessages.BackgroundResourceRefresher_0, monitor -> {
			RefreshUtil.refreshResources(fConfiguration, monitor);
		}).schedule();
	}
}
