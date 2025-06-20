/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

public class Policy {

	//debug constants
	public static boolean DEBUG = false;
	public static boolean DEBUG_STREAMS = false;
	public static boolean DEBUG_REFRESH_JOB = true;
	public static boolean DEBUG_BACKGROUND_EVENTS = false;
	public static boolean DEBUG_THREADING = false;

	static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = options -> {
		DEBUG = options.getBooleanOption(TeamPlugin.ID + "/debug", false); //$NON-NLS-1$
		DEBUG_STREAMS = DEBUG && options.getBooleanOption(TeamPlugin.ID + "/streams", false); //$NON-NLS-1$
		DEBUG_REFRESH_JOB = DEBUG && options.getBooleanOption(TeamPlugin.ID + "/refreshjob", false); //$NON-NLS-1$
		DEBUG_BACKGROUND_EVENTS = DEBUG && options.getBooleanOption(TeamPlugin.ID + "/backgroundevents", false); //$NON-NLS-1$
		DEBUG_THREADING = DEBUG && options.getBooleanOption(TeamPlugin.ID + "/threading", false); //$NON-NLS-1$
	};

	/**
	 * Progress monitor helpers.
	 *
	 * @param monitor the progress monitor
	 */
	public static void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
	public static IProgressMonitor monitorFor(IProgressMonitor monitor) {
		if (monitor == null) {
			return new NullProgressMonitor();
		}
		return monitor;
	}

	public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null) {
			return new NullProgressMonitor();
		}
		if (monitor instanceof NullProgressMonitor) {
			return monitor;
		}
		return SubMonitor.convert(monitor, ticks);
	}

	public static IProgressMonitor infiniteSubMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null) {
			return new NullProgressMonitor();
		}
		if (monitor instanceof NullProgressMonitor) {
			return monitor;
		}
		return new InfiniteSubProgressMonitor(monitor, ticks);
	}
}
