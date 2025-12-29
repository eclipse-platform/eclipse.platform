/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness.session;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.tests.harness.session.customization.SessionCustomization;
import org.eclipse.core.tests.session.Setup;
import org.eclipse.core.tests.session.SetupManager;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * The implementation of the {@link SessionTestExtension} to be instantiated on
 * the host that is executing the session tests. It executes the test methods
 * remotely in a dedicated session.
 */
class SessionTestExtensionHost implements SessionTestExtension {
	public static final String CORE_TEST_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$

	private final RemoteTestExecutor testExecutor;

	private final Setup setup;

	private final Set<SessionCustomization> sessionCustomizations = new HashSet<>();

	SessionTestExtensionHost(String pluginId, String applicationId) {
		try {
			this.setup = SetupManager.getInstance().getDefaultSetup();
			setup.setSystemProperty("org.eclipse.update.reconcile", "false");
			testExecutor = new RemoteTestExecutor(setup, applicationId, pluginId);
		} catch (SetupException e) {
			throw new IllegalStateException("unable to create setup", e);
		}
	}

	void addSessionCustomization(SessionCustomization sessionCustomization) {
		this.sessionCustomizations.add(sessionCustomization);
	}

	/**
	 * Sets the given Eclipse program argument to the given value for sessions
	 * executed with this extension.
	 *
	 * @param key   the Eclipse argument key, must not be {@code null}
	 * @param value the Eclipse argument value to set, may be {@code null} to remove
	 *              the key
	 */
	@Override
	public void setEclipseArgument(String key, String value) {
		setup.setEclipseArgument(key, value);
	}

	/**
	 * Sets the given system property to the given value for sessions executed with
	 * this extension.
	 *
	 * @param key   the system property key, must not be {@code null}
	 * @param value the system property value to set, may be {@code null} to remove
	 *              the key
	 */
	@Override
	public void setSystemProperty(String key, String value) {
		setup.setSystemProperty(key, value);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		if (!skipIfNotExecuteInHost(invocation, invocationContext)) {
			return;
		}

		Class<?> testClass = extensionContext.getTestClass().get();
		Method testMethod = extensionContext.getTestMethod().get();

		boolean shouldFail = extensionContext.getTestMethod().get().getAnnotation(SessionShouldError.class) != null;
		PerformanceSessionTest performanceSessionTestAnnotation = extensionContext.getTestMethod().get()
				.getAnnotation(PerformanceSessionTest.class);
		if (performanceSessionTestAnnotation != null) {
			executePerformanceSessionTest(testClass, testMethod, shouldFail,
					performanceSessionTestAnnotation.repetitions());
		} else {
			exeuteSession(testClass, testMethod, shouldFail);
		}

	}

	private void executePerformanceSessionTest(Class<?> testClass, Method testMethod, boolean shouldFail,
			int repetitions) throws Exception, Throwable {
		try {
			setSystemProperty("eclipse.perf.dbloc", System.getProperty("eclipse.perf.dbloc"));
			setSystemProperty("eclipse.perf.config", System.getProperty("eclipse.perf.config"));
			for (int i = 0; i < repetitions - 1; i++) {
				exeuteSession(testClass, testMethod, shouldFail);
			}
			setSystemProperty("eclipse.perf.assertAgainst", System.getProperty("eclipse.perf.assertAgainst"));
			exeuteSession(testClass, testMethod, shouldFail);
		} finally {
			setSystemProperty("eclipse.perf.assertAgainst", null);
			setSystemProperty("eclipse.perf.dbloc", null);
			setSystemProperty("eclipse.perf.config", null);
		}
	}

	private void exeuteSession(Class<?> testClass, Method testMethod, boolean shouldFail) throws Exception, Throwable {
		try {
			prepareSession();
			testExecutor.executeRemotely(testClass.getName(), testMethod.getName(), shouldFail);
		} finally {
			cleanupSession();
		}
	}

	private boolean skipIfNotExecuteInHost(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext) throws Throwable {
		boolean shouldExecuteInHost = invocationContext.getExecutable().getAnnotation(ExecuteInHost.class) != null;
		if (!shouldExecuteInHost) {
			invocation.skip();
			return true;
		}
		invocation.proceed();
		return false;
	}

	private void prepareSession() throws Exception {
		for (SessionCustomization customization : sessionCustomizations) {
			customization.prepareSession(setup);
		}
	}

	private void cleanupSession() throws Exception {
		for (SessionCustomization customization : sessionCustomizations) {
			customization.cleanupSession(setup);
		}
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfNotExecuteInHost(invocation, invocationContext);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfNotExecuteInHost(invocation, invocationContext);
	}

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfNotExecuteInHost(invocation, invocationContext);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfNotExecuteInHost(invocation, invocationContext);
	}

}
