/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;

public class CheatSheetStopWatch {
	private static CheatSheetStopWatch stopWatch = null;

	private Map<String, Entry> table;

	private CheatSheetStopWatch() {

	}

	public static CheatSheetStopWatch getInstance() {
		if(stopWatch == null) {
			stopWatch = new CheatSheetStopWatch();
		}

		return stopWatch;
	}

	public void start(String key) {

		Entry entry = getEntry(key);
		if (entry == null) {
			entry = new Entry();
			putEntry(key, entry);
		} else {
			resetEntry(entry);
		}

		entry.start = System.currentTimeMillis();
	}

	public void stop(String key) {
		Entry entry = getEntry(key);
		entry.stop = System.currentTimeMillis();
	}

	public long totalElapsedTime(String key) {
		Entry entry = getEntry(key);
		return entry.stop - entry.start;
	}

	public void lapTime(String key) {
		Entry entry = getEntry(key);
		if(entry.currentLap == -1) {
			entry.previousLap = entry.start;
		} else {
			entry.previousLap = entry.currentLap;
		}
		entry.currentLap = System.currentTimeMillis();
	}

	public long elapsedTime(String key) {
		Entry entry = getEntry(key);
		return entry.currentLap - entry.previousLap;
	}

	/**
	 * Contains the data for an entry in the stopwatch.
	 */
	private static class Entry {
		protected long start = -1;
		protected long stop = -1;
		protected long currentLap = -1;
		protected long previousLap = -1;
	}

	private Entry getEntry(String key) {
		return getTable().get(key);
	}

	private void putEntry(String key, Entry entry) {
		getTable().put(key, entry);
	}

	private void resetEntry(Entry entry) {
		entry.start = -1;
		entry.stop = -1;
		entry.currentLap = -1;
		entry.previousLap = -1;
	}

	private Map<String, Entry> getTable() {
		if (table == null) {
			table = new HashMap<>(10);
		}
		return table;
	}


	public static boolean isTracing() {
		if (CheatSheetPlugin.getPlugin().isDebugging()) {
			String traceTimes = Platform.getDebugOption("org.eclipse.ui.cheatsheets/trace/creation/times"); //$NON-NLS-1$
			if (traceTimes != null && traceTimes.equalsIgnoreCase("true")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	public static void startStopWatch(String key) {
		if(isTracing()) {
			getInstance().start(key);
		}
	}

	public static void printTotalTime(String key, String message) {
		if(isTracing()) {
			getInstance().stop(key);
			System.out.print(message);
			System.out.println(getInstance().totalElapsedTime(key));
		}
	}

	public static void printLapTime(String key, String message) {
		if(isTracing()) {
			getInstance().lapTime(key);
			System.out.print(message);
			System.out.println(getInstance().elapsedTime(key));
		}
	}
}
