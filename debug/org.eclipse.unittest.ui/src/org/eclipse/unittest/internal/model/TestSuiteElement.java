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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestSuiteElement;

/**
 * A test suite element. Holds all information about a test case
 */
public class TestSuiteElement extends TestElement implements ITestSuiteElement {

	private final List<TestElement> fChildren;
	private Status fChildrenStatus;
	private Integer expectedTestCount;

	/**
	 * Constructs a test suite object
	 *
	 * @param parent                a parent {@link TestSuiteElement} object
	 * @param id                    an identifier of a test suite
	 * @param testName              a name of test suite
	 * @param expectedChildrenCount an expected children count
	 * @param displayName           a display name of test suite
	 * @param data                  an optional additional data for a test suite
	 */
	public TestSuiteElement(TestSuiteElement parent, String id, String testName, Integer expectedChildrenCount,
			String displayName, String data) {
		super(parent, id, testName, displayName, data);
		this.expectedTestCount = expectedChildrenCount;
		fChildren = new ArrayList<>(expectedChildrenCount == null ? 0 : expectedChildrenCount.intValue());
	}

	@Override
	public Result getTestResult(boolean includeChildren) {
		if (includeChildren) {
			return getStatus().convertToResult();
		} else {
			return super.getStatus().convertToResult();
		}
	}

	@Override
	public List<TestElement> getChildren() {
		return Collections.unmodifiableList(fChildren);
	}

	/**
	 * Adds a child {@link ITestElement} to this test suite element
	 *
	 * @param child a child {@link ITestElement}
	 */
	public void addChild(TestElement child) {
		fChildren.add(child);
	}

	/**
	 * Removes a child {@link ITestElement} from this test suite element
	 *
	 * @param child a child {@link ITestElement}
	 */
	public void removeChild(TestElement child) {
		fChildren.remove(child);
	}

	@Override
	public Status getStatus() {
		Status suiteStatus = getSuiteStatus();
		if (fChildrenStatus != null) {
			// must combine children and suite status here, since failures can occur e.g. in
			// @AfterClass
			return combineStatus(fChildrenStatus, suiteStatus);
		} else {
			return suiteStatus;
		}
	}

	private Status getCumulatedStatus() {
		TestElement[] children = fChildren.toArray(new TestElement[fChildren.size()]); // copy list to avoid concurreny
																						// problems
		if (children.length == 0)
			return getSuiteStatus();

		Status cumulated = children[0].getStatus();

		for (int i = 1; i < children.length; i++) {
			Status childStatus = children[i].getStatus();
			cumulated = combineStatus(cumulated, childStatus);
		}
		// not necessary, see special code in Status.combineProgress()
//		if (suiteStatus.isErrorOrFailure() && cumulated.isNotRun())
//			return suiteStatus; //progress is Done if error in Suite and no children run
		return cumulated;
	}

	/**
	 * Returns a test suite execution status
	 *
	 * @return a test suite execution status
	 */
	public Status getSuiteStatus() {
		return super.getStatus();
	}

	/**
	 * Notifies on the status changes in a specified child {@link ITestElement}
	 * element
	 *
	 * @param child       a child {@link ITestElement} element
	 * @param childStatus a new status value
	 */
	public void childChangedStatus(ITestElement child, Status childStatus) {
		int childCount = fChildren.size();
		if (child == fChildren.get(0) && childStatus.isRunning()) {
			// is first child, and is running -> copy status
			internalSetChildrenStatus(childStatus);
			return;
		}
		TestElement lastChild = fChildren.get(childCount - 1);
		if (child == lastChild) {
			if (childStatus.isDone()) {
				// all children done, collect cumulative status
				internalSetChildrenStatus(getCumulatedStatus());
				return;
			}
			// go on (child could e.g. be a TestSuiteElement with RUNNING_FAILURE)

		} else if (!lastChild.getStatus().isNotRun()) {
			// child is not last, but last child has been run -> child has been rerun or is
			// rerunning
			internalSetChildrenStatus(getCumulatedStatus());
			return;
		}

		// finally, set RUNNING_FAILURE/ERROR if child has failed but suite has not
		// failed:
		if (childStatus.isFailure()) {
			if (fChildrenStatus == null || !fChildrenStatus.isErrorOrFailure()) {
				internalSetChildrenStatus(Status.RUNNING_FAILURE);
				return;
			}
		} else if (childStatus.isError()) {
			if (fChildrenStatus == null || !fChildrenStatus.isError()) {
				internalSetChildrenStatus(Status.RUNNING_ERROR);
				return;
			}
		}
	}

	private void internalSetChildrenStatus(Status status) {
		if (fChildrenStatus == status)
			return;

		if (status == Status.RUNNING) {
			if (fDuration != null) {
				// re-running child: ignore change
			} else {
				testStartedInstant = Instant.now();
			}
		} else if (status.convertToProgressState() == ProgressState.COMPLETED && fDuration == null
				&& testStartedInstant != null) {
			fDuration = Duration.between(testStartedInstant, Instant.now());
		}

		fChildrenStatus = status;

		TestSuiteElement parent = getParent();
		if (parent != null) {
			parent.childChangedStatus(this, getStatus());
		}
	}

	@Override
	public String toString() {
		return "TestSuite: " + getTestName() + " : " + super.toString() + " (" + fChildren.size() + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	private static Status combineStatus(Status one, Status two) {
		Status progress = combineProgress(one, two);
		Status error = combineError(one, two);
		return combineProgressAndErrorStatus(progress, error);
	}

	private static Status combineProgress(Status one, Status two) {
		if (one.isNotRun() && two.isNotRun()) {
			return Status.NOT_RUN;
		}
		if (one.isDone() && two.isDone()) {
			return Status.OK;
		}
		if (!one.isRunning() && !two.isRunning()) {
			return Status.OK; // one done, one not-run -> a parent failed and its children are not run
		}
		return Status.RUNNING;
	}

	private static Status combineError(Status one, Status two) {
		if (one.isError() || two.isError()) {
			return Status.ERROR;
		}
		if (one.isFailure() || two.isFailure()) {
			return Status.FAILURE;
		}
		return Status.OK;
	}

	private static Status combineProgressAndErrorStatus(Status progress, Status error) {
		if (progress.isDone()) {
			if (error.isError()) {
				return Status.ERROR;
			}
			if (error.isFailure()) {
				return Status.FAILURE;
			}
			return Status.OK;
		}

		if (progress.isNotRun()) {
			return Status.NOT_RUN;
		}

		if (error.isError()) {
			return Status.RUNNING_ERROR;
		}
		if (error.isFailure()) {
			return Status.RUNNING_FAILURE;
		}
		return Status.RUNNING;
	}

	@Override
	Integer getFinalTestCaseCount() {
		if (expectedTestCount != null) {
			return expectedTestCount;
		}
		if (getStatus().isDone()) {
			return Integer.valueOf(getChildren().stream().map(TestElement::getFinalTestCaseCount)
					.filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
		}
		return null;
	}

	@Override
	public int countStartedTestCases() {
		return getChildren().stream().mapToInt(TestElement::countStartedTestCases).sum();
	}

	@Override
	int getCurrentFailureCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentFailureCount).sum();
	}

	@Override
	int getCurrentAssumptionFailureCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentAssumptionFailureCount).sum();
	}

	@Override
	int getCurrentIgnoredCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentIgnoredCount).sum();
	}

	@Override
	int getCurrentErrorCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentErrorCount).sum();
	}

}
