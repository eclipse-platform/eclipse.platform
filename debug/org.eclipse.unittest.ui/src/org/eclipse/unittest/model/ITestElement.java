/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import java.util.Objects;

import org.eclipse.unittest.internal.model.ProgressState;

/**
 * Common protocol for test elements. This set consists of
 * {@link ITestCaseElement}, {@link ITestSuiteElement} and
 * {@link ITestRunSession}
 *
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITestElement {

	/**
	 * Result states of a test.
	 */
	public enum Result {
		UNDEFINED("Undefined"), //$NON-NLS-1$
		OK("OK"), //$NON-NLS-1$
		ERROR("Error"), //$NON-NLS-1$
		FAILURE("Failure"), //$NON-NLS-1$
		IGNORED("Ignored"); //$NON-NLS-1$

		private String fName;

		private Result(String name) {
			fName = name;
		}

		@Override
		public String toString() {
			return fName;
		}
	}

	/**
	 * A failure trace of a test.
	 *
	 * This class is not intended to be instantiated or extended by clients.
	 */
	public static final class FailureTrace {
		private final String fActual;
		private final String fExpected;
		private final String fTrace;

		public FailureTrace(String trace, String expected, String actual) {
			fActual = actual;
			fExpected = expected;
			fTrace = trace;
		}

		/**
		 * Returns the failure stack trace.
		 *
		 * @return the failure stack trace
		 */
		public String getTrace() {
			return fTrace;
		}

		/**
		 * Returns the expected result or <code>null</code> if the trace is not a
		 * comparison failure.
		 *
		 * @return the expected result or <code>null</code> if the trace is not a
		 *         comparison failure.
		 */
		public String getExpected() {
			return fExpected;
		}

		/**
		 * Returns the actual result or <code>null</code> if the trace is not a
		 * comparison failure.
		 *
		 * @return the actual result or <code>null</code> if the trace is not a
		 *         comparison failure.
		 */
		public String getActual() {
			return fActual;
		}

		/**
		 * Returns <code>true</code> in case of comparison failure.
		 *
		 * @return <code>true</code> in case of comparison failure, otherwise returns
		 *         <code>false</code>
		 */
		public boolean isComparisonFailure() {
			return (fExpected != null || fActual != null) && !Objects.equals(fActual, fExpected);
		}
	}

	/**
	 * Returns an identifier of the test element
	 *
	 * @return a test element identifier
	 */
	String getId();

	/**
	 * Returns some runner-specific data, such as complete test description or other
	 * data allowing further operations not covered by the generic test model.
	 *
	 * @return some runner-specific data, such as complete test description or other
	 *         data allowing further operations not covered by the generic test
	 *         model.
	 */
	String getData();

	/**
	 * Returns the test run session.
	 *
	 * @return the parent test run session.
	 */
	ITestRunSession getTestRunSession();

	/**
	 * Returns the estimated total time elapsed while executing this test element.
	 * The total time for a test suite includes the time used for all tests in that
	 * suite. The total time for a test session includes the time used for all tests
	 * in that session.
	 * <p>
	 * <strong>Note:</strong> The elapsed time is only valid for
	 * {@link ProgressState#COMPLETED} test elements.
	 * </p>
	 *
	 * @return total execution duration for the test element, or <code>null</code>
	 *         if the state of the element is not {@link ProgressState#COMPLETED}
	 */
	Duration getDuration();

	/**
	 * Returns the failure trace of this test element or <code>null</code> if the
	 * test has not resulted in an error or failure.
	 *
	 * @return the failure trace of this test or <code>null</code>.
	 */
	FailureTrace getFailureTrace();

	/**
	 * Returns parent test suite element of this test element
	 *
	 * @return a parent test suite element
	 */
	ITestSuiteElement getParent();

	/**
	 * Returns the name of the test element
	 *
	 * @return a name of test element
	 */
	String getTestName();

	/**
	 * Returns the display name of the test. Can be <code>null</code>. In that case,
	 * use {@link ITestElement#getTestName() getTestName()}.
	 *
	 * @return the test display name, can be <code>null</code>
	 */
	String getDisplayName();

}
