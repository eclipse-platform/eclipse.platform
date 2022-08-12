/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.PerformanceStats;

/**
 * An ResourceStats collects and aggregates timing data about an event such as
 * a builder running, an editor opening, etc.
 */
public class ResourceStats {
	/**
	 * The event that is currently occurring, maybe <code>null</code>
	 */
	private static PerformanceStats currentStats;
	//performance event names
	public static final String EVENT_BUILDERS = ResourcesPlugin.PI_RESOURCES + "/perf/builders"; //$NON-NLS-1$
	public static final String EVENT_LISTENERS = ResourcesPlugin.PI_RESOURCES + "/perf/listeners"; //$NON-NLS-1$
	public static final String EVENT_SAVE_PARTICIPANTS = ResourcesPlugin.PI_RESOURCES + "/perf/save.participants"; //$NON-NLS-1$
	public static final String EVENT_SNAPSHOT = ResourcesPlugin.PI_RESOURCES + "/perf/snapshot"; //$NON-NLS-1$

	//performance event enablement
	public static boolean TRACE_BUILDERS = PerformanceStats.isEnabled(ResourceStats.EVENT_BUILDERS);
	public static boolean TRACE_LISTENERS = PerformanceStats.isEnabled(ResourceStats.EVENT_LISTENERS);
	public static boolean TRACE_SAVE_PARTICIPANTS = PerformanceStats.isEnabled(ResourceStats.EVENT_SAVE_PARTICIPANTS);
	public static boolean TRACE_SNAPSHOT = PerformanceStats.isEnabled(ResourceStats.EVENT_SNAPSHOT);

	public static void endBuild() {
		if (currentStats != null)
			currentStats.endRun();
		currentStats = null;
	}

	public static void endNotify() {
		if (currentStats != null)
			currentStats.endRun();
		currentStats = null;
	}

	public static void endSave() {
		if (currentStats != null)
			currentStats.endRun();
		currentStats = null;
	}

	public static void endSnapshot() {
		if (currentStats != null)
			currentStats.endRun();
		currentStats = null;
	}

	/**
	 * Notifies the stats tool that a resource change listener has been added.
	 */
	public static void listenerAdded(IResourceChangeListener listener) {
		if (listener != null)
			PerformanceStats.getStats(EVENT_LISTENERS, listener.getClass().getName());
	}

	/**
	 * Notifies the stats tool that a resource change listener has been removed.
	 */
	public static void listenerRemoved(IResourceChangeListener listener) {
		if (listener != null)
			PerformanceStats.removeStats(EVENT_LISTENERS, listener.getClass().getName());
	}

	public static void startBuild(IncrementalProjectBuilder builder) {
		currentStats = PerformanceStats.getStats(EVENT_BUILDERS, builder);
		currentStats.startRun(builder.getProject().getName());
	}

	public static void startNotify(IResourceChangeListener listener) {
		currentStats = PerformanceStats.getStats(EVENT_LISTENERS, listener);
		currentStats.startRun();
	}

	public static void startSnapshot() {
		currentStats = PerformanceStats.getStats(EVENT_SNAPSHOT, ResourcesPlugin.getWorkspace());
		currentStats.startRun();
	}

	public static void startSave(ISaveParticipant participant) {
		currentStats = PerformanceStats.getStats(EVENT_SAVE_PARTICIPANTS, participant);
		currentStats.startRun();
	}
}