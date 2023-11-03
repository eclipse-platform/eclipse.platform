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
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

/**
 * A test rule that watches for a hanging test. It logs a thread dump in case a
 * test runs longer than a given timeout and sends an interrupt to the thread
 * that executes this rule. In contrast to the JUnit {@link Timeout} rule, it
 * still executes the test in the original thread.
 */
public class HangingTestRule extends TestWatcher {

	private final Duration timeout;

	private HangingTestWatcher hangingTestWatcher;

	public HangingTestRule(Duration timeout) {
		this.timeout = timeout;
	}

	@Override
	protected void starting(Description description) {
		hangingTestWatcher = HangingTestWatcher.createAndStart(timeout, description.getDisplayName());
	}

	@Override
	protected void finished(Description description) {
		hangingTestWatcher.stop();
	}

}
