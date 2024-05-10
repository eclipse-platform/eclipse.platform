/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.session;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.eclipse.core.tests.session.SetupManager.SetupException;

public class SessionTestSuite extends TestSuite {
	public static final String CORE_TEST_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$
	public static final String UI_TEST_APPLICATION = "org.eclipse.pde.junit.runtime.uitestapplication"; //$NON-NLS-1$
	protected String applicationId = CORE_TEST_APPLICATION;
	private final Set<TestCase> crashTests = new HashSet<>();
	// the id for the plug-in whose classloader ought to be used to load the test case class
	protected String pluginId;
	private Setup setup;
	protected SessionTestRunner testRunner;

	public SessionTestSuite(String pluginId) {
		super();
		this.pluginId = pluginId;
	}

	public SessionTestSuite(String pluginId, Class<?> theClass) {
		super(theClass);
		this.pluginId = pluginId;
	}

	public SessionTestSuite(String pluginId, Class<? extends TestCase> theClass, String name) {
		super(theClass, name);
		this.pluginId = pluginId;
	}

	public SessionTestSuite(String pluginId, String name) {
		super(name);
		this.pluginId = pluginId;
	}

	/**
	 * Crash tests are not expected to complete (they fail if they do).
	 */
	public void addCrashTest(TestCase test) {
		crashTests.add(test);
		super.addTest(test);
	}

	protected void fillTestDescriptor(TestDescriptor test) throws SetupException {
		if (test.getApplicationId() == null) {
			test.setApplicationId(applicationId);
		}
		if (test.getPluginId() == null) {
			test.setPluginId(pluginId);
		}
		if (test.getSetup() == null) {
			test.setSetup(getSetup());
		}
		if (!test.isCrashTest() && crashTests.contains(test.getTest())) {
			test.setCrashTest(true);
		}
		test.setTestRunner(getTestRunner());
	}

	public String getApplicationId() {
		return applicationId;
	}

	public Setup getSetup() throws SetupException {
		if (setup == null) {
			setup = newSetup();
		}
		return setup;
	}

	protected SessionTestRunner getTestRunner() {
		if (testRunner == null) {
			testRunner = new SessionTestRunner();
		}
		return testRunner;
	}

	protected Test[] getTests(boolean sort) {
		Test[] allTests = new Test[testCount()];
		Enumeration<Test> e = tests();
		for (int i = 0; i < allTests.length; i++) {
			allTests[i] = e.nextElement();
		}
		if (sort) {
			Arrays.sort(allTests, (o1, o2) -> ((TestCase) o1).getName().compareTo(((TestCase) o2).getName()));
		}
		return allTests;
	}

	protected Setup newSetup() throws SetupException {
		Setup base =  SetupManager.getInstance().getDefaultSetup();
		base.setSystemProperty("org.eclipse.update.reconcile", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		return base;
	}

	protected void runSessionTest(TestDescriptor test, TestResult result) {
		try {
			fillTestDescriptor(test);
			test.run(result);
		} catch (SetupException e) {
			Throwable cause = e.getCause();
			result.addError(test.getTest(), cause != null ? cause : e);
		}
	}

	@Override
	public final void runTest(Test test, TestResult result) {
		if (test instanceof TestDescriptor) {
			runSessionTest((TestDescriptor) test, result);
		} else if (test instanceof TestCase) {
			runSessionTest(new TestDescriptor((TestCase) test), result);
		} else if (test instanceof TestSuite) {
			// find and run the test cases that make up the suite
			runTestSuite((TestSuite) test, result);
		} else {
			// we don't support session tests for things that are not TestCases
			// or TestSuites (e.g. TestDecorators)
			test.run(result);
		}
	}

	/*
	 * Traverses the test suite to find individual test cases to be run with the SessionTestRunner.
	 */
	protected void runTestSuite(TestSuite suite, TestResult result) {
		for (Enumeration<Test> e = suite.tests(); e.hasMoreElements();) {
			if (result.shouldStop()) {
				break;
			}
			Test test = e.nextElement();
			runTest(test, result);
		}
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	void setSetup(Setup setup) {
		this.setup = setup;
	}

}
