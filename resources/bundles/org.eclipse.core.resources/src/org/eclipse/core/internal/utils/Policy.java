/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.Bundle;

public class Policy {
	static DebugTrace DEBUG_TRACE;

	public static final DebugOptionsListener RESOURCES_DEBUG_OPTIONS_LISTENER = options -> {
		DEBUG_TRACE = options.newDebugTrace(ResourcesPlugin.PI_RESOURCES);
		Policy.DEBUG = options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/debug", false); //$NON-NLS-1$

		Policy.DEBUG_AUTO_REFRESH = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/refresh", false); //$NON-NLS-1$

		Policy.DEBUG_BUILD_DELTA = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/delta", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_CYCLE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/cycle", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_FAILURE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/failure", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_INTERRUPT = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/interrupt", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_INVOKING = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/invoking", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_NEEDED = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/needbuild", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_NEEDED_DELTA = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/needbuilddelta", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_NEEDED_STACK = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/needbuildstack", false); //$NON-NLS-1$
		Policy.DEBUG_BUILD_STACK = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/build/stacktrace", false); //$NON-NLS-1$

		Policy.DEBUG_TREE_IMMUTABLE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/tree/immutable", false); //$NON-NLS-1$
		Policy.DEBUG_TREE_IMMUTABLE_STACK = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/tree/immutablestack", false); //$NON-NLS-1$

		Policy.DEBUG_CONTENT_TYPE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/contenttype", false); //$NON-NLS-1$
		Policy.DEBUG_CONTENT_TYPE_CACHE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/contenttype/cache", false); //$NON-NLS-1$
		Policy.DEBUG_HISTORY = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/history", false); //$NON-NLS-1$
		Policy.DEBUG_NATURES = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/natures", false); //$NON-NLS-1$
		Policy.DEBUG_NOTIFICATIONS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/notifications", false); //$NON-NLS-1$
		Policy.DEBUG_PREFERENCES = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/preferences", false); //$NON-NLS-1$

		Policy.DEBUG_RESTORE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_MARKERS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/markers", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_MASTERTABLE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/mastertable", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_METAINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/metainfo", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_SNAPSHOTS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/snapshots", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_SYNCINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/syncinfo", false); //$NON-NLS-1$
		Policy.DEBUG_RESTORE_TREE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/restore/tree", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_MARKERS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/markers", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_MASTERTABLE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/mastertable", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_METAINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/metainfo", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_SYNCINFO = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/syncinfo", false); //$NON-NLS-1$
		Policy.DEBUG_SAVE_TREE = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/save/tree", false); //$NON-NLS-1$

		Policy.DEBUG_STRINGS = Policy.DEBUG && options.getBooleanOption(ResourcesPlugin.PI_RESOURCES + "/strings", false); //$NON-NLS-1$
	};

	public static final boolean buildOnCancel = false;
	//general debug flag for the plugin
	public static boolean DEBUG = false;

	public static boolean DEBUG_AUTO_REFRESH = false;

	//debug constants
	public static boolean DEBUG_BUILD_DELTA = false;
	public static boolean DEBUG_BUILD_CYCLE = false;
	public static boolean DEBUG_BUILD_FAILURE = false;
	public static boolean DEBUG_BUILD_INTERRUPT = false;
	public static boolean DEBUG_BUILD_INVOKING = false;
	public static boolean DEBUG_BUILD_NEEDED = false;
	public static boolean DEBUG_BUILD_NEEDED_DELTA = false;
	public static boolean DEBUG_BUILD_NEEDED_STACK = false;
	public static boolean DEBUG_BUILD_STACK = false;

	public static boolean DEBUG_TREE_IMMUTABLE_STACK = false;
	public static boolean DEBUG_TREE_IMMUTABLE = false;

	public static boolean DEBUG_CONTENT_TYPE = false;
	public static boolean DEBUG_CONTENT_TYPE_CACHE = false;
	public static boolean DEBUG_HISTORY = false;
	public static boolean DEBUG_NATURES = false;
	public static boolean DEBUG_NOTIFICATIONS = false;
	public static boolean DEBUG_PREFERENCES = false;
	// Get timing information for restoring data
	public static boolean DEBUG_RESTORE = false;
	public static boolean DEBUG_RESTORE_MARKERS = false;
	public static boolean DEBUG_RESTORE_MASTERTABLE = false;

	public static boolean DEBUG_RESTORE_METAINFO = false;
	public static boolean DEBUG_RESTORE_SNAPSHOTS = false;
	public static boolean DEBUG_RESTORE_SYNCINFO = false;
	public static boolean DEBUG_RESTORE_TREE = false;
	// Get timing information for save and snapshot data
	public static boolean DEBUG_SAVE = false;
	public static boolean DEBUG_SAVE_MARKERS = false;
	public static boolean DEBUG_SAVE_MASTERTABLE = false;

	public static boolean DEBUG_SAVE_METAINFO = false;
	public static boolean DEBUG_SAVE_SYNCINFO = false;
	public static boolean DEBUG_SAVE_TREE = false;
	public static boolean DEBUG_STRINGS = false;
	public static final long MAX_BUILD_DELAY = 1000;

	public static final long MIN_BUILD_DELAY = 100;
	public static int opWork = 100;
	public static final int totalWork = 100;

	public static void checkCanceled(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			throw new OperationCanceledException();
	}

	/**
	 * Print a debug message to the console.
	 * Prepend the message with the current date, the name of the current thread and the current job if present.
	 */
	public static void debug(String message) {
		StringBuilder output = new StringBuilder();
		Job currentJob = Job.getJobManager().currentJob();
		if (currentJob != null) {
			output.append(currentJob.getClass().getName());
			output.append("("); //$NON-NLS-1$
			output.append(currentJob.getName());
			output.append("): "); //$NON-NLS-1$
		}
		output.append(message);
		DEBUG_TRACE.trace(null, output.toString());
	}

	/**
	 * Print a debug throwable to the console.
	 */
	public static void debug(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		String str = writer.toString();
		if (str.endsWith("\n")) //$NON-NLS-1$
			str = str.substring(0, str.length() - 2);
		debug(str);
	}

	public static void log(int severity, String message, Throwable t) {
		if (message == null)
			message = ""; //$NON-NLS-1$
		log(new Status(severity, ResourcesPlugin.PI_RESOURCES, 1, message, t));
	}

	public static void log(IStatus status) {
		final Bundle bundle = Platform.getBundle(ResourcesPlugin.PI_RESOURCES);
		if (bundle == null)
			return;
		ILog.of(bundle).log(status);
	}

	/**
	 * Logs a throwable, assuming severity of error
	 */
	public static void log(Throwable t) {
		log(IStatus.ERROR, "Internal Error", t); //$NON-NLS-1$
	}

	public static IProgressMonitor monitorFor(IProgressMonitor monitor) {
		return monitor == null ? new NullProgressMonitor() : monitor;
	}

	public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks) {
		return SubMonitor.convert(monitor, ticks);
	}
}
