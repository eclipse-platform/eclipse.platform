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
package org.eclipse.unittest.internal.launcher;

import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestRunSession;

/**
 * A test run listener default implementation.
 *
 * <p>
 * A test run starts with the call to {@link #sessionLaunched(ITestRunSession)}
 * and {@link #sessionStarted(ITestRunSession)}, followed by calls to
 * {@link #testCaseStarted(ITestCaseElement)} and
 * {@link #testCaseFinished(ITestCaseElement)} for all test cases contained in
 * the tree.
 * </p>
 * <p>
 * A test run session is ended with the call to
 * {@link #sessionFinished(ITestRunSession)}. After that call, no references
 * must be kept to the session or any of the test cases or suites.
 * </p>
 */
public class TestRunListener {

	/**
	 * A test run session has been launched. The test tree is not available yet.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the
	 * session element after {@link #sessionFinished(ITestRunSession)} has finished.
	 * </p>
	 *
	 * @param session the session that has just been launched
	 */
	public void sessionLaunched(ITestRunSession session) {
		// does nothing
	}

	/**
	 * A test run session has started. The test tree can be accessed through the
	 * session element.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the
	 * session element after {@link #sessionFinished(ITestRunSession)} has finished.
	 * </p>
	 *
	 * @param session the session that has just started.
	 */
	public void sessionStarted(ITestRunSession session) {
		// does nothing
	}

	/**
	 * A test run session has finished. The test tree can be accessed through the
	 * session element.
	 *
	 * <p>
	 * Important: The implementor of this method must not keep the session element
	 * when the method is finished.
	 * </p>
	 *
	 * @param session the test
	 */
	public void sessionFinished(ITestRunSession session) {
		// does nothing
	}

	/**
	 * A test case has started. The result can be accessed from the element.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the
	 * test case element after {@link #sessionFinished(ITestRunSession)} has
	 * finished.
	 * </p>
	 *
	 * @param testCaseElement the test that has started to run
	 */
	public void testCaseStarted(ITestCaseElement testCaseElement) {
		// does nothing
	}

	/**
	 * A test case has ended. The result can be accessed from the element.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the
	 * test case element after {@link #sessionFinished(ITestRunSession)} has
	 * finished.
	 * </p>
	 *
	 * @param testCaseElement the test that has finished running
	 */
	public void testCaseFinished(ITestCaseElement testCaseElement) {
		// does nothing
	}
}
