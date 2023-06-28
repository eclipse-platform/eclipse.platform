/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.unittest.internal.model;

import java.time.Duration;

import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;

/**
 * A listener interface for observing the execution of a test session (initial
 * run and reruns).
 */
public interface ITestSessionListener {
	/**
	 * A test run has started.
	 */
	void sessionStarted();

	/**
	 * A test run has ended.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	void sessionCompleted(Duration duration);

	/**
	 * A test run has been stopped prematurely.
	 *
	 * @param duration the time elapsed before the test run was stopped
	 */
	void sessionAborted(Duration duration);

	/**
	 * A test has been added to the plan.
	 *
	 * @param testElement the test
	 */
	void testAdded(ITestElement testElement);

	/**
	 * All test have been added and running begins
	 */
	void runningBegins();

	/**
	 * An individual test has started.
	 *
	 * @param testCaseElement the test
	 */
	void testStarted(ITestCaseElement testCaseElement);

	/**
	 * An individual test has ended.
	 *
	 * @param testCaseElement the test
	 */
	void testEnded(ITestCaseElement testCaseElement);

	/**
	 * An individual test has failed with a stack trace.
	 *
	 * @param testElement the test
	 * @param status      the outcome of the test; one of
	 *                    {@link org.eclipse.unittest.model.ITestElement.Result#ERROR}
	 *                    or
	 *                    {@link org.eclipse.unittest.model.ITestElement.Result#FAILURE}
	 * @param trace       the stack trace
	 */
	void testFailed(ITestElement testElement, Result status, FailureTrace trace);

}
