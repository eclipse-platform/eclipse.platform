/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.launcher;

import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.ui.UITestRunListener;

import org.eclipse.debug.core.ILaunch;

/**
 * An interface to be implemented by a Test Runner Client. Its implementation
 * should takes care of placing the right listeners to a given
 * {@link TestRunSession} (usually received in the constructor) and to react to
 * the various test engine events (can be some notifications via some network,
 * reading standard output, etc. depending on design of a specified test runner)
 * by sending notifications to the {@link UITestRunListener}s.
 */
public interface ITestRunnerClient {

	/**
	 * Starts monitoring test execution.
	 *
	 * @see #stopMonitoring()
	 */
	void startMonitoring();

	/**
	 * Requests to stop the tests execution. Usually requested by user; so it should
	 * stop the test runner client (usually calling {@link #stopMonitoring()} and
	 * also related test specific closable objects like an underlying
	 * {@link ILaunch} (unless launch is configured to be kept alive).
	 */
	void stopTest();

	/**
	 * Stops monitoring and disconnects this test runner client; this is typically
	 * happening when a test run session is marked as terminated.
	 */
	void stopMonitoring();

}
