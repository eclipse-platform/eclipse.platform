/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.unittest.internal.launcher.TestListenerRegistry;
import org.eclipse.unittest.internal.launcher.TestRunListener;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;

import org.eclipse.core.runtime.ListenerList;

/**
 * Notifier for the callback listener API {@link TestRunListener}.
 */
public class TestRunListenerAdapter implements ITestSessionListener {

	private final TestRunSession fSession;

	/**
	 * Constructs a {@link TestRunListenerAdapter} object
	 *
	 * @param session a {@link TestRunSession} object
	 */
	public TestRunListenerAdapter(TestRunSession session) {
		fSession = session;
	}

	private ListenerList<TestRunListener> getListenerList() {
		return TestListenerRegistry.getDefault().getUnitTestRunListeners();
	}

	private void fireSessionStarted() {
		for (TestRunListener listener : getListenerList()) {
			listener.sessionStarted(fSession);
		}
	}

	private void fireSessionFinished() {
		for (TestRunListener listener : getListenerList()) {
			listener.sessionFinished(fSession);
		}
	}

	private void fireTestCaseStarted(ITestCaseElement testCaseElement) {
		for (TestRunListener listener : getListenerList()) {
			listener.testCaseStarted(testCaseElement);
		}
	}

	private void fireTestCaseFinished(ITestCaseElement testCaseElement) {
		for (TestRunListener listener : getListenerList()) {
			listener.testCaseFinished(testCaseElement);
		}
	}

	@Override
	public void sessionStarted() {
		// wait until all test are added
	}

	@Override
	public void sessionCompleted(Duration duration) {
		fireSessionFinished();
	}

	@Override
	public void sessionAborted(Duration duration) {
		fireSessionFinished();
	}

	@Override
	public void testAdded(ITestElement testElement) {
		// do nothing
	}

	@Override
	public void runningBegins() {
		fireSessionStarted();
	}

	@Override
	public void testStarted(ITestCaseElement testCaseElement) {
		fireTestCaseStarted(testCaseElement);
	}

	@Override
	public void testEnded(ITestCaseElement testCaseElement) {
		fireTestCaseFinished(testCaseElement);
	}

	@Override
	public void testFailed(ITestElement testElement, Result status, FailureTrace trace) {
		// ignore
	}
}
