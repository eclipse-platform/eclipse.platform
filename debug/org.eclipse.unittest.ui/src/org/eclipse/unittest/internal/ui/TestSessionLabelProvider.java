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

package org.eclipse.unittest.internal.ui;

import java.text.MessageFormat;
import java.time.Duration;

import org.eclipse.unittest.internal.model.Status;
import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.ui.TestRunnerViewPart.TestResultsLayout;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;

/**
 * A Test Session label provider implementation.
 */
class TestSessionLabelProvider extends LabelProvider implements IStyledLabelProvider {

	private final TestRunnerViewPart fTestRunnerPart;
	private final TestResultsLayout fLayoutMode;

	private boolean fShowTime;

	/**
	 * Constructs Test Session Provider object.
	 *
	 * @param testRunnerPart a test runner view part object
	 * @param layoutMode     a layout mode
	 */
	public TestSessionLabelProvider(TestRunnerViewPart testRunnerPart, TestResultsLayout layoutMode) {
		fTestRunnerPart = testRunnerPart;
		fLayoutMode = layoutMode;
		fShowTime = true;
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (!(element instanceof ITestElement)) {
			return new StyledString(element.toString());
		}
		TestElement testElement = (TestElement) element;
		StyledString text = new StyledString(testElement.getDisplayName());
		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL) {
			if (testElement.getParentContainer() instanceof ITestRunSession) {
				String displayName = fTestRunnerPart.getDisplayName();
				if (displayName != null) {
					String decorated = MessageFormat.format(Messages.TestSessionLabelProvider_testName_RunnerVersion,
							text.getString(), displayName);
					text = StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, text);
				}
			}

		} else {
			if (element instanceof TestCaseElement) {
				String decorated = getTextForFlatLayout((TestCaseElement) testElement, text.getString());
				text = StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, text);
			}
		}
		return addElapsedTime(text, testElement.getDuration());
	}

	private String getTextForFlatLayout(TestCaseElement testCaseElement, String label) {
		String parentName;
		String parentDisplayName = testCaseElement.getParent().getDisplayName();
		if (parentDisplayName != null) {
			parentName = parentDisplayName;
		} else {
			parentName = testCaseElement.getTestName();
		}
		return MessageFormat.format(Messages.TestSessionLabelProvider_testMethodName_className, label,
				BasicElementLabels.getJavaElementName(parentName));
	}

	private StyledString addElapsedTime(StyledString styledString, Duration duration) {
		String string = styledString.getString();
		String decorated = addElapsedTime(string, duration);
		return StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.COUNTER_STYLER, styledString);
	}

	private String addElapsedTime(String string, Duration duration) {
		if (!fShowTime || duration == null) {
			return string;
		}
		return MessageFormat.format(Messages.TestSessionLabelProvider_testName_elapsedTimeInSeconds, string,
				Double.valueOf(duration.toNanos() / 1.0e9));
	}

	@Override
	public String getText(Object element) {
		if (!(element instanceof ITestElement)) {
			return element.toString();
		}
		TestElement testElement = (TestElement) element;
		String label = testElement.getDisplayName();
		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL) {
			if (testElement instanceof ITestRunSession || (testElement.getParent() instanceof ITestRunSession
					&& testElement.getParent().getChildren().size() <= 1)) {
				String displayName = fTestRunnerPart.getDisplayName();
				if (displayName != null) {
					label = MessageFormat.format(Messages.TestSessionLabelProvider_testName_RunnerVersion, label,
							displayName);
				}
			}
		} else {
			if (element instanceof TestCaseElement) {
				label = getTextForFlatLayout((TestCaseElement) testElement, label);
			}
		}
		return addElapsedTime(label, testElement.getDuration());
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof TestElement && ((TestElement) element).isAssumptionFailure())
			return fTestRunnerPart.fTestAssumptionFailureIcon;

		if (element instanceof TestCaseElement) {
			TestCaseElement testCaseElement = ((TestCaseElement) element);
			if (testCaseElement.isIgnored())
				return fTestRunnerPart.fTestIgnoredIcon;

			Status status = testCaseElement.getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fTestIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fTestRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fTestErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fTestFailIcon;
			else if (status.isOK())
				return fTestRunnerPart.fTestOkIcon;
			else
				throw new IllegalStateException(element.toString());
		} else if (element instanceof TestElement) { // suite or session
			Status status = ((TestElement) element).getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fSuiteIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fSuiteRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fSuiteErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fSuiteFailIcon;
			else if (status.isOK())
				return fTestRunnerPart.fSuiteOkIcon;
			else
				throw new IllegalStateException(element.toString());
		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}

	/**
	 * Makes the label provider to show time on the generated labels
	 *
	 * @param showTime <code>true</code> in case a time value is to be shown,
	 *                 otherwise - <code>false</code>
	 */
	public void setShowTime(boolean showTime) {
		fShowTime = showTime;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

}
