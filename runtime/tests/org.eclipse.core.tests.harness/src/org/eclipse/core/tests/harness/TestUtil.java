/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.core.tests.harness;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public final class TestUtil {
	private TestUtil() {
	}

	/**
	 * Creates a multi-line string representing a current thread dump consisting of
	 * all thread's stacks.
	 *
	 * @return a multi-line string containing a current thread dump
	 */
	public static String createThreadDump() {
		return ThreadDump.create();
	}

	private static final class ThreadDump {

		public static String create() {
			StringBuilder out = new StringBuilder();
			String staticIndent = " ";
			String indentPerLevel = "  ";
			out.append(staticIndent + "[ThreadDump taken from thread '" + Thread.currentThread().getName() + "' at "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())) + ":"
					+ System.lineSeparator());
			List<Entry<Thread, StackTraceElement[]>> stackTraces = getStacksOfAllThreads();
			for (Entry<Thread, StackTraceElement[]> entry : stackTraces) {
				Thread thread = entry.getKey();
				out.append(staticIndent + indentPerLevel).append("Thread \"").append(thread.getName()).append("\" ") //
						.append("#").append(thread.getId()).append(" ") //
						.append("prio=").append(thread.getPriority()).append(" ") //
						.append(thread.getState()).append(System.lineSeparator());
				StackTraceElement[] stack = entry.getValue();
				for (StackTraceElement stackEntry : stack) {
					out.append(staticIndent + indentPerLevel + indentPerLevel).append("at ").append(stackEntry)
							.append(System.lineSeparator());
				}
			}
			out.append(staticIndent).append("] // ThreadDump end").append(System.lineSeparator());
			return out.toString();
		}

		private static List<Entry<Thread, StackTraceElement[]>> getStacksOfAllThreads() {
			Comparator<Entry<Thread, StackTraceElement[]>> threadIdComparator = Comparator
					.comparing(e -> e.getKey().getId());
			Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
			return stackTraces.entrySet().stream().sorted(threadIdComparator).collect(Collectors.toList());
		}
	}

}
