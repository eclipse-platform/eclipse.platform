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
import java.util.Objects;
import org.eclipse.core.tests.harness.session.samples.SampleSessionTests;
import org.eclipse.core.tests.session.Setup;
import org.eclipse.core.tests.session.SetupManager;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * A JUnit 5 extension that will execute every test method in a class in its own
 * session, i.e., in an own Eclipse application instance. It is instantiate via
 * a builder created with {@link SessionTestExtension#forPlugin(String)}. For an
 * example, see the {@link SampleSessionTests} class.
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * &#64;RegisterExtension
 * static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_HARNESS).create();
 * </pre>
 *
 * <b>Note:</b> This does not enforce an execution order of test cases. If a
 * specific execution order is required, it has to be ensured by different
 * means, such as using a test method order annotation like
 * {@link TestMethodOrder}.
 *
 * @see SessionShouldError
 *
 */
public class SessionTestExtension implements InvocationInterceptor {
	public static final String CORE_TEST_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$

	private final RemoteTestExecutor testExecutor;

	private SessionTestExtension(String pluginId, String applicationId) {
		try {
			Setup setup = SetupManager.getInstance().getDefaultSetup();
			setup.setSystemProperty("org.eclipse.update.reconcile", "false");
			testExecutor = new RemoteTestExecutor(setup, applicationId, pluginId);
		} catch (SetupException e) {
			throw new IllegalStateException("unable to create setup", e);
		}
	}

	/**
	 * Creates a builder for the session test extension. Make sure to finally call
	 * {@link SessionTestExtensionBuilder#create()} to create a
	 * {@link SessionTestExtension} out of the builder.
	 *
	 * @param pluginId the id of the plug-in in which the test class is placed
	 * @return the builder to create a session test extension with
	 */
	public static SessionTestExtensionBuilder forPlugin(String pluginId) {
		return new SessionTestExtensionBuilder(pluginId);
	}

	public static class SessionTestExtensionBuilder {
		private final String storedPluginId;
		private String storedApplicationId = CORE_TEST_APPLICATION;

		private SessionTestExtensionBuilder(String pluginId) {
			Objects.requireNonNull(pluginId);
			this.storedPluginId = pluginId;
		}

		public SessionTestExtensionBuilder withApplicationId(String applicationId) {
			storedApplicationId = applicationId;
			return this;
		}

		public SessionTestExtension create() {
			return new SessionTestExtension(storedPluginId, storedApplicationId);
		}
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		/**
		 * Ensure that we do not recursively make a remote call if we are already in
		 * remote execution
		 */
		if (RemoteTestExecutor.isRemoteExecution()) {
			invocation.proceed();
			return;
		}
		String testClass = extensionContext.getTestClass().get().getName();
		String testMethod = extensionContext.getTestMethod().get().getName();

		boolean shouldFail = extensionContext.getTestMethod().get().getAnnotation(SessionShouldError.class) != null;
		invocation.skip();
		testExecutor.executeRemotely(testClass, testMethod, shouldFail);
	}

}
