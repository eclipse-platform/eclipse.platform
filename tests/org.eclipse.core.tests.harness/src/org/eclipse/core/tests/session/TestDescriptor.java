/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * A test descriptor represents a test case. It is used by the session
 * test framework to run tests remotely. Using test descriptors, one
 * can run any test case provided by any plug-in by just providing the
 * plug-in id, the test class and the test case name.
 */
public class TestDescriptor extends TestCase {
	private String applicationId;
	private boolean crashTest;
	private String method;
	private String pluginId;
	private Setup setup;
	private Test test;
	private String testClass;
	private SessionTestRunner testRunner;

	public TestDescriptor(String testClass, String method) {
		this.testClass = testClass;
		this.method = method;
	}

	public TestDescriptor(TestCase test) {
		this.testClass = test.getClass().getName();
		this.method = test.getName();
		this.test = test;
	}

	@Override
	public int countTestCases() {
		return 1;
	}

	public String getApplicationId() {
		return applicationId;
	}

	@Override
	public String getName() {
		return getTestMethod();
	}

	public String getPluginId() {
		return pluginId;
	}

	public Setup getSetup() {
		return setup;
	}

	public Test getTest() {
		return test == null ? this : test;
	}

	public String getTestClass() {
		return testClass;
	}

	public String getTestMethod() {
		return method;
	}

	public SessionTestRunner getTestRunner() {
		return testRunner;
	}

	public boolean isCrashTest() {
		return crashTest;
	}

	@Override
	public void run(TestResult result) {
		Setup localSetup = (Setup) setup.clone();
		localSetup.setEclipseArgument(Setup.APPLICATION, applicationId);
		localSetup.setEclipseArgument("testpluginname", pluginId);
		localSetup.setEclipseArgument("test", testClass + ':' + method);
		getTestRunner().run(getTest(), result, localSetup, crashTest);
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public void setCrashTest(boolean crashTest) {
		this.crashTest = crashTest;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public void setSetup(Setup setup) {
		this.setup = setup == null ? null : (Setup) setup.clone();
	}

	public void setTestRunner(SessionTestRunner testRunner) {
		this.testRunner = testRunner;
	}

	@Override
	public String toString() {
		return getName() + "(" + getTestClass() + ")";
	}
}
