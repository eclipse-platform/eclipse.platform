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
package org.eclipse.unittest.ui;

import java.util.Collection;
import java.util.List;

import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.text.StringMatcher;

import org.eclipse.jface.action.IAction;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Interface to be implemented by a Test View Support to be returned by
 * org.org.eclipse.unittest.unittestViewSupport extension.
 */
public interface ITestViewSupport {
	/**
	 * Returns a Test Runner Client.
	 *
	 * @param session the test session. ⚠️ The session may not be fully initialized
	 *                at that point, however {@link ITestRunSession#getLaunch()} is
	 *                supposed to return the proper launch.
	 *
	 * @return returns a Test Runner Client
	 */
	ITestRunnerClient newTestRunnerClient(ITestRunSession session);

	/**
	 * Returns filter patterns to exclude lines from stack trace or an error message
	 *
	 * @return filter patterns, matching lines will be hidden in the UI
	 */
	Collection<StringMatcher> getTraceExclusionFilterPatterns();

	/**
	 * Returns an action to open a specified test elements
	 *
	 * @param shell    a parent {@link Shell} instance
	 * @param testCase a test case element
	 * @return an action to open a specified test case element, or <code>null</code>
	 */
	IAction getOpenTestAction(Shell shell, ITestCaseElement testCase);

	/**
	 * Returns an action to open a specified test suite element
	 *
	 * @param shell     a parent {@link Shell} instance
	 * @param testSuite a test suite element
	 * @return an action to open a specified test suite element, or
	 *         <code>null</code>
	 */
	IAction getOpenTestAction(Shell shell, ITestSuiteElement testSuite);

	/**
	 * Returns an action to open a failure trace element
	 *
	 * @param shell     a parent {@link Shell} instance
	 * @param failure   a test element that is failed
	 * @param traceLine a stack trace or an error message text
	 * @return an action to open a failure trace element, or <code>null</code>
	 */
	IAction createOpenEditorAction(Shell shell, ITestElement failure, String traceLine);

	/**
	 * Returns an action to copy an existing stack trace/error message into a
	 * console view
	 *
	 * @param failedTest the failed test
	 * @return an {@link Runnable} if it can be created, otherwise -
	 *         <code>null</code>
	 */
	Runnable createShowStackTraceInConsoleViewActionDelegate(ITestElement failedTest);

	/**
	 * Returns a Rerun launch configuration for the given element
	 *
	 * @param testElements the tests to rerun
	 * @return a {@link ILaunchConfiguration}, derived from current test session and
	 *         selected element.
	 */
	ILaunchConfiguration getRerunLaunchConfiguration(List<ITestElement> testElements);

	/**
	 * Returns a Test View Support display name
	 *
	 * @return returns a display name
	 */
	String getDisplayName();
}
