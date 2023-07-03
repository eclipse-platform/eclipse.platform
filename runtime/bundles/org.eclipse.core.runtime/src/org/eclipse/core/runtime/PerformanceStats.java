/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@gmail.com> - Adds generic type arguments https://bugs.eclipse.org/412836
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.PrintWriter;
import java.util.*;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.runtime.PerformanceStatsProcessor;

/**
 * PerformanceStats collects and aggregates timing data about events such as
 * a builder running, an editor opening, etc.  This data is collected for the
 * purpose of performance analysis, and is not intended to be used as
 * a generic event tracking and notification system.
 * <p>
 * Each performance event can have an associated maximum acceptable
 * duration that is specified in the platform debug options file (.options).
 * Events that take longer than this maximum are logged as errors.  Along
 * with option file entries for each debug event, there are some global debug
 * options for enabling or disabling performance event gathering and reporting.
 * See the "org.eclipse.core.runtime/perf*" debug options in the .options file
 * for the org.eclipse.core.runtime plugin for more details.
 * </p><p>
 * A performance event can optionally have additional context information
 * ({@link #getContext}).  This information is only stored in the case
 * of a performance failure, and can be used to provide further diagnostic
 * information that can help track down the cause of the failure.
 * </p><p>
 * Performance events and performance failures are batched up and periodically
 * sent to interested performance event listeners.
 * </p><p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 * @since 3.1
 */
public class PerformanceStats {
	/**
	 * A performance listener is periodically notified after performance events occur
	 * or after events fail.
	 * <p>
	 * This class is intended to be subclassed.
	 * </p>
	 *
	 * @see PerformanceStats#addListener(PerformanceStats.PerformanceListener)
	 */
	public static abstract class PerformanceListener {
		/**
		 * Creates a new listener.
		 */
		protected PerformanceListener() {
			super();
		}

		/**
		 * Notifies than an event exceeded the maximum duration for that event type.
		 * <p>
		 * This default implementation does nothing. Subclasses may override.
		 * </p>
		 *
		 * @param event The event that failed
		 * @param duration The duration of the failed event, in milliseconds
		 */
		public void eventFailed(PerformanceStats event, long duration) {
			//default implementation does nothing
		}

		/**
		 * Notifies that an event occurred.  Notification might not occur
		 * in the same thread or near the time of the actual event.
		 * <p>
		 * This default implementation does nothing. Subclasses may override.
		 * </p>
		 *
		 * @param event The event that occurred
		 */
		public void eventsOccurred(PerformanceStats[] event) {
			//default implementation does nothing
		}
	}

	/**
	 * An empty stats object that is returned when tracing is turned off
	 */
	private static final PerformanceStats EMPTY_STATS = new PerformanceStats("", ""); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Constant indicating whether or not tracing is enabled
	 */
	public static final boolean ENABLED;

	/**
	 * A constant indicating that the timer has not been started.
	 */
	private static final long NOT_STARTED = -1;

	/**
	 * All known event statistics.
	 */
	private final static Map<PerformanceStats, PerformanceStats> statMap =
				Collections.synchronizedMap(new HashMap<>());

	/**
	 * Maximum allowed durations for each event.
	 * Maps String (event name) -&gt; Long (threshold)
	 */
	private final static Map<String, Long> thresholdMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Whether non-failure statistics should be retained.
	 */
	private static final boolean TRACE_SUCCESS;

	/**
	 * An identifier that can be used to figure out who caused the event. This is
	 * typically a string representation of the object whose code was running when
	 * the event occurred or a <code>String</code> describing the event.
	 */
	private String blame;

	/**
	 * The id of the plugin that defined the blame object for this event, or
	 * <code>null</code> if it could not be determined.
	 */
	private String blamePluginId;

	/**
	 * An optional context for the event (may be <code>null</code>).
	 * The context can provide extra information about an event, such as the
	 * name of a project being built, or the input of an editor being opened.
	 */
	private String context;

	/**
	 * The starting time of the current occurrence of this event.
	 */
	private long currentStart = NOT_STARTED;

	/**
	 * The symbolic name of the event that occurred. This is usually the name of
	 * the debug option for this event.
	 */
	private String event;

	/**
	 * Whether this is a performance failure event
	 */
	private boolean isFailure;

	/**
	 * The total number of times this event has occurred.
	 */
	private int runCount = 0;

	/**
	 * The total time in milliseconds taken by all occurrences of this event.
	 */
	private long runningTime = 0;

	static {
		ENABLED = InternalPlatform.getDefault().getBooleanOption(Platform.PI_RUNTIME + "/perf", false);//$NON-NLS-1$
		//turn these on by default if the global trace flag is turned on
		TRACE_SUCCESS = InternalPlatform.getDefault().getBooleanOption(Platform.PI_RUNTIME + "/perf/success", ENABLED); //$NON-NLS-1$
	}

	/**
	 * Adds a listener that is notified when performance events occur.  If
	 * an equal listener is already installed, it will be replaced.
	 *
	 * @param listener The listener to be added
	 * @see #removeListener(PerformanceStats.PerformanceListener)
	 */
	public static void addListener(PerformanceListener listener) {
		if (ENABLED)
			PerformanceStatsProcessor.addListener(listener);
	}

	/**
	 * Discards all known performance event statistics.
	 */
	public static void clear() {
		statMap.clear();
	}

	/**
	 * Returns all performance event statistics.
	 *
	 * @return An array of known performance event statistics.  The array
	 * will be empty if there are no recorded statistics.
	 */
	public static PerformanceStats[] getAllStats() {
		return statMap.values().toArray(new PerformanceStats[statMap.size()]);
	}

	/**
	 * Returns the stats object corresponding to the given parameters.
	 * A stats object is created and added to the global list of events if it did not
	 * already exist.
	 *
	 * @param eventName A symbolic event name.  This is usually the name of
	 * the debug option for this event. An example event name from
	 * the org.eclipse.core.resources plugin describing a build event might look like:
	 * 		<code>"org.eclipse.core.resources/perf/building"</code>"
	 * @param blameObject The blame for the event.  This is typically the object
	 * whose code was running when the event occurred.  If a blame object cannot
	 * be obtained, a <code>String</code> describing the event should be supplied
	 */
	public static PerformanceStats getStats(String eventName, Object blameObject) {
		if (!ENABLED || eventName == null || blameObject == null)
			return EMPTY_STATS;
		PerformanceStats newStats = new PerformanceStats(eventName, blameObject);
		if (!TRACE_SUCCESS)
			return newStats;
		//use existing stats object if available
		PerformanceStats oldStats = statMap.get(newStats);
		if (oldStats != null)
			return oldStats;
		statMap.put(newStats, newStats);
		return newStats;
	}

	/**
	 * Returns whether monitoring of a given performance event is enabled.
	 * <p>
	 * For frequent performance events, the result of this method call should
	 * be cached by the caller to minimize overhead when performance monitoring
	 * is turned off.  It is not possible for enablement to change during the life
	 * of this invocation of the platform.
	 * </p>
	 *
	 * @param eventName The name of the event to determine enablement for
	 * @return <code>true</code>If the performance event with the given
	 * name is enabled, and <code>false</code> otherwise.
	 */
	public static boolean isEnabled(String eventName) {
		if (!ENABLED)
			return false;
		String option = Platform.getDebugOption(eventName);
		return option != null && !"false".equalsIgnoreCase(option) && !"-1".equalsIgnoreCase(option); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Prints all statistics to the standard output.
	 */
	public static void printStats() {
		if (!ENABLED)
			return;
		PrintWriter writer = new PrintWriter(System.out);
		PerformanceStatsProcessor.printStats(writer);
		writer.flush();
	}

	/**
	 * Writes all statistics using the provided writer
	 *
	 * @param out The writer to print stats to.
	 */
	public static void printStats(PrintWriter out) {
		if (!ENABLED)
			return;
		PerformanceStatsProcessor.printStats(out);
	}

	/**
	 * Removes an event listener. Has no effect if an equal
	 * listener object is not currently registered.
	 *
	 * @param listener The listener to remove
	 * @see #addListener(PerformanceStats.PerformanceListener)
	 */
	public static void removeListener(PerformanceListener listener) {
		if (ENABLED)
			PerformanceStatsProcessor.removeListener(listener);
	}

	/**
	 * Removes statistics for a given event and blame
	 *
	 * @param eventName The name of the event to remove
	 * @param blameObject The blame for the event to remove
	 */
	public static void removeStats(String eventName, Object blameObject) {
		synchronized (statMap) {
			for (Iterator<PerformanceStats> it = statMap.keySet().iterator(); it.hasNext();) {
				PerformanceStats stats = it.next();
				if (stats.getEvent().equals(eventName) && stats.getBlame().equals(blameObject))
					it.remove();
			}
		}
	}

	/**
	 * Creates a new PerformanceStats object.  Private to prevent client instantiation.
	 */
	private PerformanceStats(String event, Object blame) {
		this(event, blame, null);
	}

	/**
	 * Creates a new PerformanceStats object.  Private to prevent client instantiation.
	 */
	private PerformanceStats(String event, Object blameObject, String context) {
		this.event = event;
		this.blame = blameObject instanceof String ? (String) blameObject : blameObject.getClass().getName();
		this.blamePluginId = InternalPlatform.getDefault().getBundleId(blameObject);
		this.context = context;
	}

	/**
	 * Adds an occurrence of this event to the cumulative counters. This method
	 * can be used as an alternative to <code>startRun</code> and <code>endRun</code>
	 * for clients that want to track the context and execution time separately.
	 *
	 * @param elapsed The elapsed time of the new occurrence in milliseconds
	 * @param contextName The context for the event to return, or <code>null</code>.
	 * The context optionally provides extra information about an event, such as the
	 * name of a project being built, or the input of an editor being opened.
	 */
	public void addRun(long elapsed, String contextName) {
		if (!ENABLED)
			return;
		runCount++;
		runningTime += elapsed;
		if (elapsed > getThreshold(event))
			PerformanceStatsProcessor.failed(createFailureStats(contextName, elapsed), blamePluginId, elapsed);
		if (TRACE_SUCCESS)
			PerformanceStatsProcessor.changed(this);
	}

	/**
	 * Creates a stats object representing a performance failure
	 *
	 * @param contextName The failure context information.
	 * @param elapsed The elapsed time in milliseconds
	 * @return The failure stats
	 */
	private PerformanceStats createFailureStats(String contextName, long elapsed) {
		PerformanceStats failedStat = new PerformanceStats(event, blame, contextName);
		PerformanceStats old = statMap.get(failedStat);
		if (old == null)
			statMap.put(failedStat, failedStat);
		else
			failedStat = old;
		failedStat.isFailure = true;
		failedStat.runCount++;
		failedStat.runningTime += elapsed;
		return failedStat;
	}

	/**
	 * Stops timing the occurrence of this event that was started by the previous
	 * call to <code>startRun</code>.  The event is automatically added to
	 * the cumulative counters for this event and listeners are notified.
	 * <p>
	 * Note that this facility guards itself against runs that start but fail to stop,
	 * so it is not necessary to call this method from a finally block.  Tracking
	 * performance of failure cases is generally not of interest.
	 * </p>
	 *
	 * @see #startRun()
	 */
	public void endRun() {
		if (!ENABLED || currentStart == NOT_STARTED)
			return;
		addRun(System.currentTimeMillis() - currentStart, context);
		currentStart = NOT_STARTED;
	}

	@Override
	public boolean equals(Object obj) {
		//count and time are not considered part of equality
		if (!(obj instanceof PerformanceStats))
			return false;
		PerformanceStats that = (PerformanceStats) obj;
		return this.event.equals(that.event) && this.getBlameString().equals(that.getBlameString())
				&& Objects.equals(this.context, that.context);
	}

	/**
	 * Returns an object that can be used to figure out who caused the event,
	 * or a string describing the cause of the event.
	 *
	 * @return The blame for this event
	 */
	public Object getBlame() {
		return blame;
	}

	/**
	 * Returns a string describing the blame for this event.
	 *
	 * @return A string describing the blame.
	 */
	public String getBlameString() {
		return blame;
	}

	/**
	 * Returns the optional event context, such as the input of an editor, or the target project
	 * of a build event.
	 *
	 * @return The context, or <code>null</code> if there is none
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Returns the symbolic name of the event that occurred.
	 *
	 * @return The name of the event.
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * Returns the total number of times this event has occurred.
	 *
	 * @return The number of occurrences of this event.
	 */
	public int getRunCount() {
		return runCount;
	}

	/**
	 * Returns the total execution time in milliseconds for all occurrences
	 * of this event.
	 *
	 * @return The total running time in milliseconds.
	 */
	public long getRunningTime() {
		return runningTime;
	}

	/**
	 * Returns the performance threshold for this event.
	 */
	private long getThreshold(String eventName) {
		Long value = thresholdMap.get(eventName);
		if (value == null) {
			String option = InternalPlatform.getDefault().getOption(eventName);
			if (option != null) {
				try {
					value = Long.valueOf(option);
				} catch (NumberFormatException e) {
					//invalid option, just ignore
				}
			}
			if (value == null)
				value = Long.valueOf(Long.MAX_VALUE);
			thresholdMap.put(eventName, value);
		}
		return value.longValue();
	}

	@Override
	public int hashCode() {
		//count and time are not considered part of equality
		int hash = event.hashCode() * 37 + getBlameString().hashCode();
		if (context != null)
			hash = hash * 37 + context.hashCode();
		return hash;
	}

	/**
	 * Returns whether this performance event represents a performance failure.
	 *
	 * @return <code>true</code> if this is a performance failure, and
	 * <code>false</code> otherwise.
	 */
	public boolean isFailure() {
		return isFailure;
	}

	/**
	 * Resets count and running time for this particular stats event.
	 */
	public void reset() {
		runningTime = 0;
		runCount = 0;
	}

	/**
	 * Starts timing an occurrence of this event. This is a convenience method,
	 * fully equivalent to <code>startRun(null)</code>.
	 */
	public void startRun() {
		if (ENABLED)
			startRun(null);
	}

	/**
	 * Starts timing an occurrence of this event.  The event should be stopped
	 * by a subsequent call to <code>endRun</code>.
	 *
	 * @param contextName The context for the event to return, or <code>null</code>.
	 * The context optionally provides extra information about an event, such as the
	 * name of a project being built, or the input of an editor being opened.
	 * @see #endRun
	 */
	public void startRun(String contextName) {
		if (!ENABLED)
			return;
		this.context = contextName;
		this.currentStart = System.currentTimeMillis();
	}

	/**
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("PerformanceStats("); //$NON-NLS-1$
		result.append(event);
		result.append(',');
		result.append(blame);
		if (context != null) {
			result.append(',');
			result.append(context);
		}
		result.append(')');
		return result.toString();
	}
}