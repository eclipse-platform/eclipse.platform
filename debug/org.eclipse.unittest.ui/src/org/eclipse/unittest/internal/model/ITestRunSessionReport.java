/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.model;

/**
 * An {@link ITestRunSessionReport} object
 */
public interface ITestRunSessionReport {

	/**
	 * Returns the name of the test run. The name is the name of the launch
	 * configuration use to run this test.
	 *
	 * @return returns the test run name
	 */
	String getTestRunName();

	/**
	 * Indicates if the test run session is starting
	 *
	 * @return <code>true</code> in case of the test session is starting, otherwise
	 *         returns <code>false</code>
	 */
	boolean isStarting();

	/**
	 * Indicates if the test run session is running
	 *
	 * @return <code>true</code> in case of the test session is running, otherwise
	 *         returns <code>false</code>
	 */
	boolean isRunning();

	/**
	 * Indicates if the test run session has been stopped or terminated
	 *
	 * @return <code>true</code> if the session has been stopped or terminated,
	 *         otherwise returns <code>false</code>
	 */
	boolean isStopped();

}
