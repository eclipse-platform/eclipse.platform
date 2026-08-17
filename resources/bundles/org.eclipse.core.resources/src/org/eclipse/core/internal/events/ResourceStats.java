/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
package org.eclipse.core.internal.events;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.Platform;

/**
 * An ResourceStats collects and aggregates timing data about an event such as
 * a builder running, an editor opening, etc.
 */
public class ResourceStats {

	/**
	 * A single timed occurrence of a traced event. Returned by the
	 * <code>start...</code> methods and passed back to {@link ResourceStats#end(Run)}.
	 */
	public record Run(PerformanceStats stats, String context, long startTime) {
	}

	//performance event names
	public static final String EVENT_BUILDERS = ResourcesPlugin.PI_RESOURCES + "/perf/builders"; //$NON-NLS-1$
	public static final String EVENT_LISTENERS = ResourcesPlugin.PI_RESOURCES + "/perf/listeners"; //$NON-NLS-1$
	public static final String EVENT_SAVE_PARTICIPANTS = ResourcesPlugin.PI_RESOURCES + "/perf/save.participants"; //$NON-NLS-1$
	public static final String EVENT_SNAPSHOT = ResourcesPlugin.PI_RESOURCES + "/perf/snapshot"; //$NON-NLS-1$
	public static final String EVENT_REFRESH = ResourcesPlugin.PI_RESOURCES + "/perf/refresh"; //$NON-NLS-1$

	/*
	 * Whether the debug option of the event is set. These only track this bundle's
	 * own options: the global org.eclipse.core.runtime/perf flag belongs to another
	 * bundle, and changes to it are not reported to this bundle's debug options
	 * listener, so it has to be read separately through PerformanceStats.ENABLED.
	 */
	private static volatile boolean optionBuilders = isOptionSet(EVENT_BUILDERS);
	private static volatile boolean optionListeners = isOptionSet(EVENT_LISTENERS);
	private static volatile boolean optionSaveParticipants = isOptionSet(EVENT_SAVE_PARTICIPANTS);
	private static volatile boolean optionSnapshot = isOptionSet(EVENT_SNAPSHOT);
	private static volatile boolean optionRefresh = isOptionSet(EVENT_REFRESH);

	//durations above which an occurrence is reported to the log, in milliseconds
	public static volatile int TRACE_BUILDERS_THRESHOLD = threshold(EVENT_BUILDERS);
	public static volatile int TRACE_REFRESH_THRESHOLD = threshold(EVENT_REFRESH);

	/**
	 * Re-reads the tracing options so that tracing can be switched on and off at
	 * runtime. Called whenever the platform debug options change.
	 */
	public static void optionsChanged() {
		optionBuilders = isOptionSet(EVENT_BUILDERS);
		optionListeners = isOptionSet(EVENT_LISTENERS);
		optionSaveParticipants = isOptionSet(EVENT_SAVE_PARTICIPANTS);
		optionSnapshot = isOptionSet(EVENT_SNAPSHOT);
		optionRefresh = isOptionSet(EVENT_REFRESH);
		TRACE_BUILDERS_THRESHOLD = threshold(EVENT_BUILDERS);
		TRACE_REFRESH_THRESHOLD = threshold(EVENT_REFRESH);
	}

	public static boolean isTracingBuilders() {
		return PerformanceStats.ENABLED && optionBuilders;
	}

	public static boolean isTracingListeners() {
		return PerformanceStats.ENABLED && optionListeners;
	}

	public static boolean isTracingSaveParticipants() {
		return PerformanceStats.ENABLED && optionSaveParticipants;
	}

	public static boolean isTracingSnapshot() {
		return PerformanceStats.ENABLED && optionSnapshot;
	}

	public static boolean isTracingRefresh() {
		return PerformanceStats.ENABLED && optionRefresh;
	}

	private static boolean isOptionSet(String event) {
		String option = Platform.getDebugOption(event);
		return option != null && !"false".equalsIgnoreCase(option) && !"-1".equalsIgnoreCase(option); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static int threshold(String event) {
		String option = Platform.getDebugOption(event);
		if (option == null) {
			return 0;
		}
		try {
			return Integer.parseInt(option.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Run start(String event, Object blame, String context) {
		return new Run(PerformanceStats.getStats(event, blame), context, System.currentTimeMillis());
	}

	/**
	 * Starts a run that is only timed, without being recorded as a performance
	 * event. Used where just the duration is of interest, such as debug tracing.
	 */
	public static Run startTiming() {
		return new Run(null, null, System.currentTimeMillis());
	}

	/**
	 * Records the given run and returns its duration in milliseconds, or -1 if
	 * there was no run.
	 */
	public static long end(Run run) {
		if (run == null) {
			return -1;
		}
		long duration = System.currentTimeMillis() - run.startTime();
		if (run.stats() != null) {
			run.stats().addRun(duration, run.context());
		}
		return duration;
	}

	/**
	 * Notifies the stats tool that a resource change listener has been added.
	 */
	public static void listenerAdded(IResourceChangeListener listener) {
		if (listener != null) {
			PerformanceStats.getStats(EVENT_LISTENERS, listener.getClass().getName());
		}
	}

	/**
	 * Notifies the stats tool that a resource change listener has been removed.
	 */
	public static void listenerRemoved(IResourceChangeListener listener) {
		if (listener != null) {
			PerformanceStats.removeStats(EVENT_LISTENERS, listener.getClass().getName());
		}
	}

	public static Run startBuild(IncrementalProjectBuilder builder) {
		return start(EVENT_BUILDERS, builder, builder.getProject().getName());
	}

	public static Run startNotify(IResourceChangeListener listener) {
		return start(EVENT_LISTENERS, listener, null);
	}

	public static Run startSnapshot() {
		return start(EVENT_SNAPSHOT, ResourcesPlugin.getWorkspace(), null);
	}

	public static Run startSave(ISaveParticipant participant) {
		return start(EVENT_SAVE_PARTICIPANTS, participant, null);
	}

	public static Run startRefresh(IResource resource) {
		return start(EVENT_REFRESH, resource, null);
	}

}
