/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.unittest.model;

import java.time.Duration;

import org.eclipse.debug.core.ILaunch;

/**
 * Represents a test run session.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITestRunSession extends ITestSuiteElement {

	/**
	 * Returns the {@link ILaunch} from which this test run session has been
	 * started, or <code>null</code> if not available.
	 *
	 * @return the {@link ILaunch} object instance, or <code>null</code> is not
	 *         available.
	 */
	ILaunch getLaunch();

	/**
	 * Returns a test element by its identifier
	 *
	 * @param id a test element identifier
	 * @return a {@link ITestElement} found or <code>null</code>
	 */
	ITestElement getTestElement(String id);

	/**
	 * Creates a new Test Case and adds it to the model
	 *
	 * @param testId      a unique id for the test
	 * @param testName    the name of the test
	 * @param parent      the parent, can be <code>null</code>
	 * @param displayName the display name of the test
	 * @param data        runner specific data
	 * @return the new test case element
	 */
	ITestCaseElement newTestCase(String testId, String testName, ITestSuiteElement parent, String displayName,
			String data);

	/**
	 * Creates a new Test Suite and adds it to the model
	 *
	 * @param testId      a unique id for the test
	 * @param testName    the name of the test
	 * @param testCount   the number of tests this suite will run, <code>null</code>
	 *                    if unknown.
	 * @param parent      the parent
	 * @param displayName the display name of the test
	 * @param data        runner specific data
	 * @return the new test case element
	 */
	ITestSuiteElement newTestSuite(String testId, String testName, Integer testCount, ITestSuiteElement parent,
			String displayName, String data);

	/**
	 * Notifies on a test run ended normally. Individual test success don't matter.
	 * If the test session failed to complete for some reason, use
	 * {@link #notifyTestSessionAborted(Duration, Exception)}.
	 *
	 * @param duration the total elapsed time of the test run, can be
	 *                 <code>null</code>.
	 * @see #notifyTestSessionAborted(Duration, Exception) notifyTestRunAborted to
	 *      use for abnormal termination of the test session.
	 */
	void notifyTestSessionCompleted(final Duration duration);

	/**
	 * Notifies on a test run aborted, abnormally.
	 *
	 * @param duration duration of the test run session until abortion, can be
	 *                 <code>null</code>.
	 * @param cause    the cause of the abortion, can be shown in log or to user,
	 *                 can be <code>null</code>.
	 * @see #notifyTestSessionCompleted(Duration) notifyTestRunAborted to use for
	 *      normal completion.
	 */
	void notifyTestSessionAborted(final Duration duration, final Exception cause);

	/**
	 * Notifies on an individual test ended.
	 *
	 * @param test      a unique Id identifying the test
	 * @param isIgnored <code>true</code> indicates that the specified test was
	 *                  ignored, otherwise - <code>false</code>
	 */
	void notifyTestEnded(ITestElement test, boolean isIgnored);

	/**
	 * Notifies on an individual test started.
	 *
	 * @param test the test
	 */
	void notifyTestStarted(ITestElement test);

	/**
	 * Notifies on a test run started.
	 *
	 * @param count the number of individual tests that will be run,
	 *              <code>null</code> if unknown
	 */
	void notifyTestSessionStarted(final Integer count);

	/**
	 * Notifies on an individual test failed with a stack trace.
	 *
	 * @param test               the test
	 * @param status             the outcome of the test; one of
	 *                           {@link org.eclipse.unittest.model.ITestElement.Result#ERROR}
	 *                           or
	 *                           {@link org.eclipse.unittest.model.ITestElement.Result#FAILURE}.
	 *                           An exception is thrown otherwise
	 * @param isAssumptionFailed indicates that an assumption is failed
	 * @param failureTrace       The failure trace
	 * @throws IllegalArgumentException if status doesn't indicate ERROR or FAILURE.
	 */
	void notifyTestFailed(ITestElement test, Result status, boolean isAssumptionFailed, FailureTrace failureTrace)
			throws IllegalArgumentException;

}
