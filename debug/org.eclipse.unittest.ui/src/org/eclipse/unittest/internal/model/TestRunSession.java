/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import java.text.DateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.launcher.TestViewSupportRegistry;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ISourceLocator;

/**
 * A test run session holds all information about a test run, i.e. launch
 * configuration, launch, test tree (including results).
 */
public class TestRunSession extends TestSuiteElement implements ITestRunSession, ITestRunSessionReport {

	/**
	 * The launch, or <code>null</code> iff this session was run externally.
	 */
	private final ILaunch fLaunch;
	private final String fTestRunName;
	private final ITestViewSupport fTestRunnerSupport;

	/**
	 * Test runner client or <code>null</code>.
	 */
	private ITestRunnerClient fTestRunnerClient;

	private final ListenerList<ITestSessionListener> fSessionListeners;
	private final TestSessionNotifier fSessionNotifier = new TestSessionNotifier();

	/**
	 * The test run session's cached result, or <code>null</code> if
	 * <code>fTestRoot != null</code>.
	 */
	private Result fTestResult;

	/**
	 * Map from testId to testElement.
	 */
	private HashMap<String, TestElement> fIdToTest;

	volatile Instant fStartTime;
	volatile Integer fPredefinedTestCount;

	volatile boolean fIsAborted;
	private Integer predefinedTestCount;
	private boolean completedOrAborted;

	/**
	 * Constructs a test run session object.
	 *
	 * @param testRunName         name of the test run
	 * @param startTime           a start time of a test run
	 * @param launchConfiguration a launch configuration for a test run
	 */
	public TestRunSession(String testRunName, Instant startTime, ILaunchConfiguration launchConfiguration) {
		super(null, "-1", testRunName, null, null, null); //$NON-NLS-1$
		// TODO: check assumptions about non-null fields

		fLaunch = new NoopLaunch(launchConfiguration, ILaunchManager.RUN_MODE, null);
		fTestRunnerSupport = TestViewSupportRegistry.newTestRunnerViewSupport(launchConfiguration).orElse(null);

		Assert.isNotNull(testRunName);
		fTestRunName = testRunName;

		fIdToTest = new HashMap<>();

		fTestRunnerClient = null;
		fStartTime = startTime;

		fSessionListeners = new ListenerList<>();
	}

	/**
	 * Constructs a test run session object from a launch.
	 *
	 * @param launch an {@link ILaunch} object
	 */
	public TestRunSession(ILaunch launch) {
		super(null, "-1", "<TestRunSession>", null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.isNotNull(launch);

		fLaunch = launch;

		ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
		if (launchConfiguration != null) {
			fTestRunName = launchConfiguration.getName();
			fTestRunnerSupport = TestViewSupportRegistry.newTestRunnerViewSupport(launchConfiguration).orElse(null);
		} else {
			fTestRunName = "<TestRunSession>"; //$NON-NLS-1$
			fTestRunnerSupport = null;
		}

		fIdToTest = new HashMap<>();

		if (fTestRunnerSupport != null) {
			fTestRunnerClient = fTestRunnerSupport.newTestRunnerClient(this);
		}

		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(new ILaunchesListener2() {
			@Override
			public void launchesTerminated(ILaunch[] launches) {
				if (Arrays.asList(launches).contains(fLaunch)) {
					launchManager.removeLaunchListener(this);
				}
			}

			@Override
			public void launchesRemoved(ILaunch[] launches) {
				if (Arrays.asList(launches).contains(fLaunch)) {
					launchManager.removeLaunchListener(this);
				}
			}

			@Override
			public void launchesChanged(ILaunch[] launches) {
				// do nothing
			}

			@Override
			public void launchesAdded(ILaunch[] launches) {
				// do nothing
			}
		});
		fSessionListeners = new ListenerList<>();
		setStatus(Status.RUNNING);
		addTestSessionListener(new TestRunListenerAdapter(this));
		fTestRunnerClient.startMonitoring();
	}

	/**
	 * Resets a test run session object (used when re-creating a test run session
	 * from an XML file(
	 *
	 * TODO Consider removal as it's only use in XML parsing
	 */
	public void reset() {
		fTestResult = null;
		fIdToTest = new HashMap<>();
	}

	@Override
	public ProgressState getProgressState() {
		if (isRunning()) {
			return ProgressState.RUNNING;
		}
		if (isStopped()) {
			return ProgressState.ABORTED;
		}
		return ProgressState.COMPLETED;
	}

	@Override
	public FailureTrace getFailureTrace() {
		return null;
	}

	@Override
	public TestSuiteElement getParentContainer() {
		return null;
	}

	@Override
	public TestRunSession getTestRunSession() {
		return this;
	}

	/**
	 * Returns the Test Runner View Support for which this test run session has been
	 * launched, or <code>null</code> if not available.
	 *
	 * @return the test runner view support, or <code>null</code> is not available.
	 */
	public ITestViewSupport getTestViewSupport() {
		return fTestRunnerSupport;
	}

	/**
	 * @return the launch, or <code>null</code> iff this session was run externally
	 */
	@Override
	public ILaunch getLaunch() {
		return fLaunch;
	}

	@Override
	public String getTestRunName() {
		return fTestRunName;
	}

	@Override
	public int getCurrentErrorCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentErrorCount).sum();
	}

	@Override
	public int getCurrentFailureCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentFailureCount).sum();
	}

	@Override
	public int getCurrentAssumptionFailureCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentAssumptionFailureCount).sum();
	}

	@Override
	public int getCurrentIgnoredCount() {
		return getChildren().stream().mapToInt(TestElement::getCurrentIgnoredCount).sum();
	}

	/**
	 * Returns start time for a run session
	 *
	 * @return an {@link Instant} object indicating a test run session start time
	 */
	public Instant getStartTime() {
		return fStartTime;
	}

	/**
	 * Indicates if the test run session has been stopped or terminated
	 *
	 * @return <code>true</code> if the session has been stopped or terminated,
	 *         otherwise returns <code>false</code>
	 */
	@Override
	public boolean isStopped() {
		return fIsAborted;
	}

	/**
	 * Adds an {@link ITestSessionListener} listener
	 *
	 * @param listener an {@link ITestSessionListener} object
	 */
	public synchronized void addTestSessionListener(ITestSessionListener listener) {
		fSessionListeners.add(listener);
	}

	/**
	 * Removes an {@link ITestSessionListener} listener
	 *
	 * @param listener an {@link ITestSessionListener} object
	 */
	public void removeTestSessionListener(ITestSessionListener listener) {
		fSessionListeners.remove(listener);
	}

	@Override
	public boolean isStarting() {
		return getStartTime() == null && fLaunch != null && !fLaunch.isTerminated();
	}

	/**
	 * Forces a test run session to abort its execution
	 */
	public void abortTestRun() {
		fIsAborted = true;
		if (fTestRunnerClient != null) {
			fTestRunnerClient.stopTest();
			fTestRunnerClient.stopMonitoring();
		}
	}

	@Override
	public boolean isRunning() {
		return getStartTime() != null && fTestRunnerClient != null && !completedOrAborted;
	}

	@Override
	public TestElement getTestElement(String id) {
		return fIdToTest.get(id);
	}

	private TestElement addTreeEntry(String id, String testName, boolean isSuite, Integer testCount,
			boolean isDynamicTest, TestSuiteElement parent, String displayName, String data) {
		return createTestElement(parent != null ? parent : this, id, testName, isSuite, testCount, isDynamicTest,
				displayName, data);
	}

	/**
	 * Creates a test element, either {@link ITestSuiteElement} or
	 * {@link ITestCaseElement} instance, depending on the arguments.
	 *
	 * @param parent        a parent test suite element
	 * @param id            an identifier of the test element
	 * @param testName      a name of the test element
	 * @param isSuite       a flag indicating if the test element should be
	 *                      represented by a test suite element
	 * @param testCount     a number of predefined test cases in case of test suite
	 *                      element
	 * @param isDynamicTest a flag indicating that test suite is dynamic (that
	 *                      doesn't have predefined tests)
	 * @param displayName   a display name for the test element
	 * @param data          some test runner specific data, can be <code>null</code>
	 * @return a created {@link ITestSuiteElement} or {@link ITestCaseElement}
	 *         instance
	 */
	public TestElement createTestElement(TestSuiteElement parent, String id, String testName, boolean isSuite,
			Integer testCount, boolean isDynamicTest, String displayName, String data) {
		TestElement testElement;
		if (isSuite) {
			TestSuiteElement testSuiteElement = new TestSuiteElement(parent != null ? parent : this, id, testName,
					testCount, displayName, data);
			testElement = testSuiteElement;
		} else {
			testElement = new TestCaseElement(parent != null ? parent : this, id, testName, displayName, isDynamicTest,
					data);
		}
		fIdToTest.put(id, testElement);
		return testElement;
	}

	private final class NoopLaunch extends Launch {
		private NoopLaunch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator) {
			super(launchConfiguration, mode, locator);
		}

		@Override
		public boolean isTerminated() {
			return true;
		}

		@Override
		public boolean isDisconnected() {
			return true;
		}
	}

	/**
	 * Listens to events from the and translates {@link ITestRunnerClient} them into
	 * high-level model events (broadcasted to {@link ITestSessionListener}s).
	 */
	private class TestSessionNotifier {

		private boolean firstStart;

		/**
		 * Notifies on a test run started
		 *
		 * @param testCount number of tests in this run
		 */
		public void testRunStarted(Integer testCount) {
			fStartTime = Instant.now();
			fPredefinedTestCount = testCount;

			for (ITestSessionListener listener : fSessionListeners) {
				listener.sessionStarted();
			}
		}

		/**
		 * Notifies on a test run ended
		 *
		 * @param duration a duration of this test run
		 */
		public void testRunEnded(Duration duration) {
			for (ITestSessionListener listener : fSessionListeners) {
				listener.sessionCompleted(duration);
			}
		}

		/**
		 * Notifies on a test run stopped (aborted)
		 *
		 * @param duration a duration of this test run
		 */
		public void testRunStopped(Duration duration) {
			fIsAborted = true;

			for (ITestSessionListener listener : fSessionListeners) {
				listener.sessionAborted(duration);
			}
		}

		/**
		 * Notifies on a test tree entry is created
		 *
		 * @param testId        an identifier of a test entry
		 * @param testName      a name of test
		 * @param isSuite       <code>true</code> indicates that a test entry is a test
		 *                      suite, <code>false</code> - a test case
		 * @param testCount     a number of tests in a test suite or <code>null</code>
		 * @param isDynamicTest indicates if a test is a dynamic test (doesn't have a
		 *                      predefined number of tests)
		 * @param parent        a parent test suite element
		 * @param displayName   a display name for the test
		 * @param uniqueId      an unique identifier of test entry or <code>null</code>
		 * @return an {@link ITestElement} object instance
		 */
		public ITestElement testTreeEntry(String testId, String testName, boolean isSuite, Integer testCount,
				boolean isDynamicTest, ITestSuiteElement parent, String displayName, String uniqueId) {
			ITestElement testElement = addTreeEntry(testId, testName, isSuite, testCount, isDynamicTest,
					(TestSuiteElement) parent, displayName, uniqueId);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testAdded(testElement);
			}
			return testElement;
		}

		/**
		 * Notifies on a test started
		 *
		 * @param test a test element object
		 */
		public void testStarted(ITestElement test) {
			if (!(test instanceof TestCaseElement)) {
				return;
			}
			if (firstStart) {
				for (ITestSessionListener listener : fSessionListeners) {
					listener.runningBegins();
				}
				firstStart = false;
			}
			setStatus(test, Status.RUNNING);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testStarted((ITestCaseElement) test);
			}
		}

		/**
		 * Notifies on a test ended
		 *
		 * @param testElement a test element object
		 * @param isIgnored   indicates a skipped (not run) test element
		 */
		public void testEnded(ITestElement testElement, boolean isIgnored) {
			if (testElement == null) {
				return;
			}
			if (!(testElement instanceof TestCaseElement)) {
				if (isIgnored) {
					((TestElement) testElement).setAssumptionFailed(true);
					setStatus(testElement, Status.OK);
				} else {
					logUnexpectedTest(testElement.getId(), testElement);
				}
				return;
			}
			TestCaseElement testCaseElement = (TestCaseElement) testElement;
			if (isIgnored) {
				testCaseElement.setIgnored(true);
			}

			if (testCaseElement.getStatus() == Status.RUNNING)
				setStatus(testCaseElement, Status.OK);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testEnded(testCaseElement);
			}
		}

		/**
		 * Notifies on a failed test element
		 *
		 * @param testElement        a failed test element
		 * @param status             a result status of test execution
		 * @param isAssumptionFailed indicates if the failure is an assumption failure
		 * @param trace              a failure trace
		 */
		public void testFailed(ITestElement testElement, Result status, boolean isAssumptionFailed,
				FailureTrace trace) {
			if (testElement == null) {
				return;
			}

			if (isAssumptionFailed) {
				((TestElement) testElement).setAssumptionFailed(true);
				status = Result.OK;
			}

			registerTestFailureStatus((TestElement) testElement, status, trace);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testFailed(testElement, status, trace);
			}
		}

		private void logUnexpectedTest(String testId, ITestElement testElement) {
			UnitTestPlugin
					.log(new Exception("Unexpected TestElement type for testId '" + testId + "': " + testElement)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Registers a failure status for a test element
	 *
	 * @param testElement  a test element
	 * @param status       a result of test execution
	 * @param failureTrace a failure trace
	 */
	public void registerTestFailureStatus(TestElement testElement, Result status, FailureTrace failureTrace) {
		testElement.setStatus(Status.fromResult(status), failureTrace);
	}

	/**
	 * Registers an ended test element
	 *
	 * @param testElement a test element
	 * @param completed   <code>true</code> indicates that the test was completed.
	 *                    <code>false</code> otherwise
	 */
	public void registerTestEnded(TestElement testElement, boolean completed) {
		if (testElement instanceof TestCaseElement) {
			if (!completed) {
				return;
			}
			TestCaseElement testCaseElement = (TestCaseElement) testElement;
			if (!testCaseElement.getStatus().isErrorOrFailure())
				setStatus(testElement, Status.OK);
		}
	}

	/**
	 * Sets a {@link Status} for a test element
	 *
	 * @param testElement a test element
	 * @param status      an execution status
	 */
	private void setStatus(ITestElement testElement, Status status) {
		((TestElement) testElement).setStatus(status);
	}

	/**
	 * Returns an array of all failed {@link ITestElement}s
	 *
	 * @return an array of failed {@link ITestElement}s
	 */
	public List<TestElement> getAllFailedTestElements() {
		List<TestElement> failures = new ArrayList<>();
		addFailures(failures, this);
		return Collections.unmodifiableList(failures);
	}

	private void addFailures(Collection<TestElement> failures, TestElement testElement) {
		Result testResult = testElement.getTestResult(true);
		if (testResult == Result.ERROR || testResult == Result.FAILURE) {
			failures.add(testElement);
		}
		if (testElement instanceof TestSuiteElement) {
			TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;
			for (TestElement child : testSuiteElement.getChildren()) {
				addFailures(failures, child);
			}
		}
	}

	@Override
	public String toString() {
		return fTestRunName + " " + DateFormat.getDateTimeInstance().format(new Date(fStartTime.toEpochMilli())); //$NON-NLS-1$
	}

	@Override
	public TestSuiteElement getParent() {
		return null;
	}

	@Override
	public String getTestName() {
		return getTestRunName();
	}

	/**
	 * An abstract base class for a listener safe runnable
	 */
	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			UnitTestPlugin.log(exception);
		}
	}

	@Override
	public TestCaseElement newTestCase(String testId, String testName, ITestSuiteElement parent, String displayName,
			String data) {
		return (TestCaseElement) fSessionNotifier.testTreeEntry(testId, testName, false, Integer.valueOf(1), false,
				parent, displayName, data);
	}

	@Override
	public TestSuiteElement newTestSuite(String testId, String testName, Integer testCount, ITestSuiteElement parent,
			String displayName, String data) {
		return (TestSuiteElement) fSessionNotifier.testTreeEntry(testId, testName, true, testCount, testCount == null,
				parent, displayName, data);
	}

	@Override
	public void notifyTestSessionAborted(final Duration reportDuration, Exception cause) {
		if (isStopped()) {
			return;
		}
		if (reportDuration != null) {
			setDuration(reportDuration);
		}
		fTestRunnerClient.stopMonitoring();
		this.completedOrAborted = true;
		SafeRunner.run(new ListenerSafeRunnable() {
			@Override
			public void run() {
				fSessionNotifier.testRunStopped(fDuration);
			}
		});
	}

	@Override
	public void notifyTestSessionCompleted(final Duration reportDuration) {
		if (isStopped()) {
			return;
		}
		if (reportDuration != null) {
			setDuration(reportDuration);
		}
		fTestRunnerClient.stopMonitoring();
		this.completedOrAborted = true;
		SafeRunner.run(new ListenerSafeRunnable() {
			@Override
			public void run() {
				fSessionNotifier.testRunEnded(fDuration);
			}
		});
	}

	@Override
	public void notifyTestEnded(ITestElement test, boolean isIgnored) {
		if (isStopped()) {
			return;
		}
		SafeRunner.run(new ListenerSafeRunnable() {
			@Override
			public void run() {
				fSessionNotifier.testEnded(test, isIgnored);
			}
		});
	}

	@Override
	public void notifyTestStarted(ITestElement test) {
		if (isStopped()) {
			return;
		}
		SafeRunner.run(new ListenerSafeRunnable() {
			@Override
			public void run() {
				fSessionNotifier.testStarted(test);
			}
		});
	}

	@Override
	public void notifyTestSessionStarted(final Integer count) {
		if (isStopped()) {
			return;
		}
		this.predefinedTestCount = count;
		SafeRunner.run(new ListenerSafeRunnable() {
			@Override
			public void run() {
				fSessionNotifier.testRunStarted(count);
			}
		});
	}

	@Override
	public void notifyTestFailed(ITestElement test, Result status, boolean isAssumptionFailed,
			FailureTrace failureTrace) {
		if (isStopped()) {
			return;
		}
		if (status != Result.FAILURE && status != Result.ERROR) {
			throw new IllegalArgumentException("Status has to be FAILURE or ERROR"); //$NON-NLS-1$
		}
		SafeRunner.run(new ListenerSafeRunnable() {
			@Override
			public void run() {
				fSessionNotifier.testFailed(test, status, isAssumptionFailed, failureTrace);
			}
		});
	}

	@Override
	public Integer getFinalTestCaseCount() {
		if (predefinedTestCount != null) {
			return predefinedTestCount;
		}
		if (getChildren().isEmpty()) {
			return null;
		}
		if (!isRunning()) {
			int res = 0;
			for (TestElement child : getChildren()) {
				Integer childCount = child.getFinalTestCaseCount();
				if (childCount == null) {
					return null;
				}
				res += childCount.intValue();
			}
			return Integer.valueOf(res);
		}
		return null;
	}

	@Override
	public Result getTestResult(boolean includeChildren) {
		return this.fTestResult != null ? this.fTestResult : super.getTestResult(includeChildren);
	}
}
