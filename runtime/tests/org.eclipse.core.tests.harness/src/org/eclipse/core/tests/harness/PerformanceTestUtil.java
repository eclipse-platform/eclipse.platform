/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness;

import java.util.List;
import java.util.Locale;

/**
 * Times test work and prints the distribution of the measured samples.
 */
public final class PerformanceTestUtil {

	@FunctionalInterface
	public interface ThrowingRunnable {
		void run() throws Exception;
	}

	private PerformanceTestUtil() {
	}

	/**
	 * Prints min, median, 90th percentile and maximum of the given nanosecond
	 * samples in milliseconds.
	 */
	public static void reportTimings(String label, List<Long> nanos) {
		if (nanos.isEmpty()) {
			System.out.println(label + ": no measurements");
			return;
		}
		long[] sorted = nanos.stream().mapToLong(Long::longValue).sorted().toArray();
		System.out.printf(Locale.ROOT, "%-48s n=%-4d min=%8.2f  p50=%8.2f  p90=%8.2f  max=%8.2f (ms)%n", label,
				sorted.length, sorted[0] / 1e6, sorted[sorted.length / 2] / 1e6,
				sorted[(int) (sorted.length * 0.9)] / 1e6, sorted[sorted.length - 1] / 1e6);
	}

	/**
	 * Runs the given runnable at least {@code minIterations} times and then until
	 * either {@code maxIterations} or {@code maxTimeMs} is reached.
	 */
	public static void exercise(ThrowingRunnable runnable, int minIterations, int maxIterations, int maxTimeMs)
			throws Exception {
		long start = System.currentTimeMillis();
		for (int i = 0; i < maxIterations; i++) {
			runnable.run();
			if (i >= minIterations - 1 && System.currentTimeMillis() - start > maxTimeMs) {
				break;
			}
		}
	}
}
