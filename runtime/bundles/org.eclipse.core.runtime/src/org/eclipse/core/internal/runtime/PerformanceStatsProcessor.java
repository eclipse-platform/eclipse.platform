/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
 *     Christoph Läubrich - remove reference to InternalPlatform.getDefault().log
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.PrintWriter;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.PerformanceStats.PerformanceListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Processes, records, and performs notification of performance events
 * that occur in the system.
 */
public class PerformanceStatsProcessor extends Job {
	private static final PerformanceStatsProcessor instance = new PerformanceStatsProcessor();

	private static final long SCHEDULE_DELAY = 2000;

	/**
	 * Events that have occurred but have not yet been broadcast.
	 */
	private final ArrayList<PerformanceStats> changes = new ArrayList<>();

	/**
	 * Event failures that have occurred but have not yet been broadcast.
	 * Maps (PerformanceStats -&gt; Long).
	 */
	private final HashMap<PerformanceStats,Long> failures = new HashMap<>();

	/**
	 * Event listeners.
	 */
	private final ListenerList<PerformanceListener> listeners = new ListenerList<>();

	private FrameworkLog log;

	/*
	 * @see PerformanceStats#addListener
	 */
	public static void addListener(PerformanceListener listener) {
		instance.listeners.add(listener);
	}

	/**
	 * Records the fact that an event occurred.
	 *
	 * @param stats The event that occurred
	 */
	public static void changed(PerformanceStats stats) {
		synchronized (instance) {
			instance.changes.add(stats);
		}
		instance.schedule(SCHEDULE_DELAY);
	}

	/**
	 * Records the fact that an event failed.
	 *
	 * @param stats The event that occurred
	 * @param pluginId The id of the plugin that declared the blame object, or
	 * <code>null</code>
	 * @param elapsed The elapsed time for this failure
	 */
	public static void failed(PerformanceStats stats, String pluginId, long elapsed) {
		synchronized (instance) {
			instance.failures.put(stats, Long.valueOf(elapsed));
		}
		instance.schedule(SCHEDULE_DELAY);
		instance.logFailure(stats, pluginId, elapsed);
	}

	/*
	 * @see PerformanceStats#printStats(PrintWriter)
	 */
	public static void printStats(PrintWriter out) {
		/* gather totals */
		long totalTime = 0;
		int totalCount = 0;
		PerformanceStats[] allStats = PerformanceStats.getAllStats();
		for (PerformanceStats stats : allStats) {
			totalTime += stats.getRunningTime();
			totalCount += stats.getRunCount();
		}
		//dump stats
		out.println("---------------------------------------------------------------"); //$NON-NLS-1$
		for (PerformanceStats stats : allStats) {
			out.print("Event: "); //$NON-NLS-1$
			out.print(stats.getEvent());
			out.print(" Blame: "); //$NON-NLS-1$
			out.print(stats.getBlameString());
			if (stats.getContext() != null) {
				out.print(" Context: "); //$NON-NLS-1$
				out.print(stats.getContext());
			}
			out.println();

			int runCount = stats.getRunCount();
			if (runCount > 0) {
				out.print("Run count: "); //$NON-NLS-1$
				out.print(Integer.toString(runCount));
				out.print(" ("); //$NON-NLS-1$
				out.print(Integer.toString((int) (runCount * 100.0 / totalCount)));
				out.println(" % of total)"); //$NON-NLS-1$
			}

			long runTime = stats.getRunningTime();
			if (runTime > 0) {
				out.print("Duration (ms): "); //$NON-NLS-1$
				out.print(Long.toString(runTime));
				out.print(" ("); //$NON-NLS-1$
				out.print(Integer.toString((int) (runTime * 100.0 / totalTime)));
				out.println(" % of total)"); //$NON-NLS-1$
			}
			out.println(""); //$NON-NLS-1$
		}
	}

	/*
	 * @see PerformanceStats#removeListener
	 */
	public static void removeListener(PerformanceListener listener) {
		instance.listeners.remove(listener);
	}

	/**
	 * Private constructor to enforce singleton usage.
	 */
	private PerformanceStatsProcessor() {
		super("Performance Stats"); //$NON-NLS-1$
		setSystem(true);
		setPriority(DECORATE);
		BundleContext context = PlatformActivator.getContext();
		String filter = '(' + FrameworkLog.SERVICE_PERFORMANCE + '=' + Boolean.TRUE + ')';
		Collection<ServiceReference<FrameworkLog>> references;
		FrameworkLog perfLog = null;
		try {
			references = context.getServiceReferences(FrameworkLog.class, filter);
			if (references != null && !references.isEmpty()) {
				//just take the first matching service
				perfLog = context.getService(references.iterator().next());
				//make sure correct location is set
				IPath logLocation = Platform.getLogFileLocation();
				logLocation = logLocation.removeLastSegments(1).append("performance.log"); //$NON-NLS-1$
				perfLog.setFile(logLocation.toFile(), false);
			}
		} catch (Exception e) {
			IStatus error = new Status(IStatus.ERROR, Platform.PI_RUNTIME, 1, "Error loading performance log", e); //$NON-NLS-1$
			RuntimeLog.log(error);
		}
		//use the platform log if we couldn't create the performance log
		if (perfLog == null)
			perfLog = InternalPlatform.getDefault().getFrameworkLog();
		log = perfLog;
	}

	/**
	 * Logs performance event failures to the platform's performance log
	 */
	private void logFailure(PerformanceStats stats, String pluginId, long elapsed) {
		//may have failed to get the performance log service
		if (log == null)
			return;
		if (pluginId == null)
			pluginId = Platform.PI_RUNTIME;
		String msg = "Performance failure: " + stats.getEvent() + " blame: " + stats.getBlameString() + " context: " + stats.getContext() + " duration: " + elapsed; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		Status status = new Status(IStatus.WARNING, pluginId, 1, msg, new RuntimeException());
		log.log(new FrameworkLogEntry(status, status.getPlugin(), status.getSeverity(), status.getCode(), status.getMessage(), 0, status.getException(), null));

	}

	/*
	 * @see Job#run(IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		PerformanceStats[] events;
		PerformanceStats[] failedEvents;
		Long[] failedTimes;
		synchronized (this) {
			events = changes.toArray(new PerformanceStats[changes.size()]);
			changes.clear();
			failedEvents = failures.keySet().toArray(new PerformanceStats[failures.size()]);
			failedTimes = failures.values().toArray(new Long[failures.size()]);
			failures.clear();
		}

		//notify performance listeners
		for (PerformanceListener listener : listeners) {
			if (events.length > 0)
				listener.eventsOccurred(events);
			for (int j = 0; j < failedEvents.length; j++)
				listener.eventFailed(failedEvents[j], failedTimes[j].longValue());
		}
		schedule(SCHEDULE_DELAY);
		return Status.OK_STATUS;
	}

	/*
	 * @see Job#shouldRun()
	 */
	@Override
	public boolean shouldRun() {
		return !changes.isEmpty() || !failures.isEmpty();
	}
}