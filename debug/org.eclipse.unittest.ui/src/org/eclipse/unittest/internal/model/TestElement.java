/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
import java.time.Instant;

import org.eclipse.unittest.model.ITestElement;

import org.eclipse.core.runtime.Assert;

/**
 * A test element n holds basic information about a test case or a test suite
 * element
 */
public abstract class TestElement implements ITestElement {
	private final TestSuiteElement fParent;
	private final String fId;
	private final String fTestName;
	/**
	 * Extra (runner-specific) data, can be <code>null</code>
	 */
	private final String fData;

	/**
	 * The display name of the test element, can be <code>null</code>. In that case,
	 * use {@link TestElement#fTestName fTestName}.
	 */
	private final String fDisplayName;

	private Status fStatus;
	protected FailureTrace fTrace;

	private boolean fAssumptionFailed;

	protected Instant testStartedInstant = null;
	protected Duration fDuration = null;

	/**
	 * Constructs the test element object
	 *
	 * @param parent      the parent, can be <code>null</code>
	 * @param id          the test id
	 * @param testName    the test name
	 * @param displayName the test display name, can be <code>null</code>
	 * @param data        some runner-specific data, can be <code>null</code>
	 */
	public TestElement(TestSuiteElement parent, String id, String testName, String displayName, String data) {
		Assert.isNotNull(id);
		Assert.isNotNull(testName);
		fParent = parent;
		fId = id;
		fTestName = testName;
		fDisplayName = displayName;
		fData = data;
		fStatus = Status.NOT_RUN;
		if (parent != null) {
			parent.addChild(this);
		} else if (!(this instanceof TestRunSession)) {
			throw new IllegalArgumentException("Test elements must have a parent"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the progress state of this test element.
	 * <ul>
	 * <li>{@link ProgressState#NOT_STARTED}: the test has not yet started</li>
	 * <li>{@link ProgressState#RUNNING}: the test is currently running</li>
	 * <li>{@link ProgressState#ABORTED}: the test has stopped before being
	 * completed</li>
	 * <li>{@link ProgressState#COMPLETED}: the test (and all its children) has
	 * completed</li>
	 * </ul>
	 *
	 * @return returns one of {@link ProgressState#NOT_STARTED},
	 *         {@link ProgressState#RUNNING}, {@link ProgressState#ABORTED} or
	 *         {@link ProgressState#COMPLETED}.
	 */
	public ProgressState getProgressState() {
		return getStatus().convertToProgressState();
	}

	/**
	 * Returns the result of the test element.
	 * <ul>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#UNDEFINED}: the
	 * result is not yet evaluated</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#OK}: the test has
	 * succeeded</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#ERROR}: the test
	 * has returned an error</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#FAILURE}: the test
	 * has returned an failure</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#IGNORED}: the test
	 * has been ignored (skipped)</li>
	 * </ul>
	 *
	 * @param includeChildren if <code>true</code>, the returned result is the
	 *                        combined result of the test and its children (if it
	 *                        has any). If <code>false</code>, only the test's
	 *                        result is returned.
	 *
	 * @return returns one of
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#UNDEFINED},
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#OK},
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#ERROR},
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#FAILURE} or
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#IGNORED}.
	 *         Clients should also prepare for other, new values.
	 */
	public Result getTestResult(boolean includeChildren) {
		if (fAssumptionFailed) {
			return Result.IGNORED;
		}
		return getStatus().convertToResult();
	}

	/**
	 * Returns the parent test element container or <code>null</code> if the test
	 * element is the test run session.
	 *
	 * @return the parent test suite
	 */
	public TestSuiteElement getParentContainer() {
		return fParent;
	}

	@Override
	public FailureTrace getFailureTrace() {
		Result testResult = getTestResult(false);
		if ((testResult == Result.ERROR || testResult == Result.FAILURE
				|| (testResult == Result.IGNORED) && fTrace != null)) {
			return fTrace;
		}
		return null;
	}

	@Override
	public TestSuiteElement getParent() {
		return fParent;
	}

	@Override
	public String getId() {
		return fId;
	}

	@Override
	public String getTestName() {
		return fTestName;
	}

	/**
	 * Sets the current test element status
	 *
	 * @param status one of {@link Status#NOT_RUN}, {@link Status#OK},
	 *               {@link Status#ERROR} or {@link Status#FAILURE}.
	 */
	public void setStatus(Status status) {
		if (status == Status.RUNNING) {
			testStartedInstant = Instant.now();
		} else if (status.convertToProgressState() == ProgressState.COMPLETED && testStartedInstant != null) {
			this.fDuration = Duration.between(testStartedInstant, Instant.now());
		}

		fStatus = status;
		TestSuiteElement parent = getParent();
		if (parent != null) {
			parent.childChangedStatus(this, status);
		}
	}

	/**
	 * Sets the extended status for this test element
	 *
	 * @param status       one of {@link Status#NOT_RUN}, {@link Status#OK},
	 *                     {@link Status#ERROR} or {@link Status#FAILURE}.
	 * @param failureTrace stacktracee/error message or null
	 */
	public void setStatus(Status status, FailureTrace failureTrace) {
		if (failureTrace != null && fTrace != null) {
			// don't overwrite first trace if same test run logs multiple errors
			fTrace = new FailureTrace(fTrace.getTrace() + failureTrace.getTrace(), fTrace.getExpected(),
					fTrace.getActual());
		} else {
			fTrace = failureTrace;
		}
		setStatus(status);
	}

	/**
	 * Returns the status of this test element
	 * <ul>
	 * <li>{@link Status#NOT_RUN}: the test has not executed</li>
	 * <li>{@link Status#OK}: the test is successful</li>
	 * <li>{@link Status#ERROR}: the test had an error</li>
	 * <li>{@link Status#FAILURE}: the test had an assertion failure</li>
	 * </ul>
	 *
	 * @return returns one of {@link Status#NOT_RUN}, {@link Status#OK},
	 *         {@link Status#ERROR} or {@link Status#FAILURE}.
	 */
	public Status getStatus() {
		return fStatus;
	}

	/**
	 * Sets a duration value for a test element
	 *
	 * @param duration a duration value
	 */
	public void setDuration(Duration duration) {
		this.fDuration = duration;
	}

	@Override
	public Duration getDuration() {
		return this.fDuration;
	}

	/**
	 * Sets up the assumption failure flag for this test
	 *
	 * @param assumptionFailed a flag indicating the assumption failure
	 */
	public void setAssumptionFailed(boolean assumptionFailed) {
		fAssumptionFailed = assumptionFailed;
	}

	/**
	 * Indicates if there was an assumption failure
	 *
	 * @return true if there was a comparison failure, otherwise return false
	 */
	public boolean isAssumptionFailure() {
		return fAssumptionFailed;
	}

	@Override
	public String toString() {
		return getTestName() + '[' + getProgressState() + " - " + getTestResult(true) + ']'; //$NON-NLS-1$
	}

	@Override
	public String getDisplayName() {
		return fDisplayName != null ? fDisplayName : getTestName();
	}

	@Override
	public String getData() {
		return fData;
	}

	@Override
	public TestRunSession getTestRunSession() {
		return getParent().getTestRunSession();
	}

	/**
	 * Returns the total number of expected test case elements, or the total number
	 * of ran test case elements if completed, or <code>null</null> if tests are
	 * still running and we can't know the final count.
	 *
	 * @return a total number of test cases
	 */
	abstract Integer getFinalTestCaseCount();

	/**
	 * Returns the number of started test case elements
	 *
	 * @return a number of started test cases
	 */
	abstract int countStartedTestCases();

	/**
	 * Returns the number of failed test case elements
	 *
	 * @return a number of failed test cases
	 */
	abstract int getCurrentFailureCount();

	/**
	 * Returns the number of assumption failures
	 *
	 * @return a number of assumption failures
	 */
	abstract int getCurrentAssumptionFailureCount();

	/**
	 * Returns the number of ignored test case elements
	 *
	 * @return a number of ignored test cases
	 */
	abstract int getCurrentIgnoredCount();

	/**
	 * Returns the number of test case elements with errors
	 *
	 * @return a number of test cases with errors
	 */
	abstract int getCurrentErrorCount();
}
