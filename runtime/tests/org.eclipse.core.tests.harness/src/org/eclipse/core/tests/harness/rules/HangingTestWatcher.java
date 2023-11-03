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
package org.eclipse.core.tests.harness.rules;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import org.eclipse.core.tests.harness.TestUtil;

/**
 * Logs a thread dump to the console and sends an interrupt to the calling
 * thread after the specified timeout. Is initialized and started via
 * {@link #createAndStart(Duration, String)} and can be stopped before the
 * timeout occurs via {@link #stop()}.
 *
 * This class is supposed to be used by the {@link HangingTestRule}, but
 * may also be useful to emulate the rule in JUnit 3 tests until they are
 * migrated to a newer JUnit versions.
 */
public class HangingTestWatcher {
	private final Duration timeout;

	private Timer timer;

	private String testName;

	private HangingTestWatcher(Duration timeout, String testName) {
		this.timeout = timeout;
		this.testName = testName;
		this.timer = new Timer();
	}

	private void start() {
		final Thread originalThread = Thread.currentThread();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logHangingThread();
				originalThread.interrupt();
			}
		}, timeout.toMillis());
	}

	private void logHangingThread() {
		System.out.println(getTimeoutMessage());
	}

	private String getTimeoutMessage() {
		return """
				%s ran into a timeout (%s ms) with the following thread dump:
				%s
				""".formatted(testName, timeout.toMillis(), TestUtil.createThreadDump());
	}

	/**
	 * Stops this logger such that
	 */
	public void stop() {
		timer.cancel();
	}

	public static HangingTestWatcher createAndStart(Duration timeout, String testName) {
		HangingTestWatcher watcher = new HangingTestWatcher(timeout, testName);
		watcher.start();
		return watcher;
	}
}
