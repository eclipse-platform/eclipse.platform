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
import java.util.Objects;
import java.util.Set;
import org.eclipse.core.tests.harness.session.customization.CustomSessionWorkspaceImpl;
import org.eclipse.core.tests.harness.session.customization.SessionCustomization;
import org.eclipse.core.tests.harness.session.samples.SampleSessionTests;
import org.eclipse.core.tests.session.Setup;
import org.eclipse.core.tests.session.SetupManager;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * A JUnit 5 extension that will execute every test method in a class in its own
 * session, i.e., in an own Eclipse application instance. It is instantiated via
 * a builder created with {@link SessionTestExtension#forPlugin(String)}. For an
 * example, see the {@link SampleSessionTests} class.
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * &#64;RegisterExtension
 * SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_HARNESS).create();
 * </pre>
 *
 * <b>Execution Order:</b> Note that this does not enforce an execution order of
 * test cases. If a specific execution order is required, it has to be ensured
 * by different means, such as using a test method order annotation like
 * {@link TestMethodOrder}.
 * <p>
 * <b>Execution Scope:</b> Note that by default a test class is instantiated
 * with {@link Lifecycle#PER_METHOD}, so a new extension with a new
 * configuration will be created for each test method. Often different test
 * methods are supposed to share a common workspace or configuration. Then, to
 * use the same extension across all test methods, it must either be defined
 * {@code static} or the lifecycle of the test class has to be set to
 * {@link Lifecycle#PER_CLASS}:
 *
 * @see SessionShouldError
 *
 */
public class SessionTestExtension implements InvocationInterceptor {
	public static final String CORE_TEST_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$

	private final RemoteTestExecutor testExecutor;

	private final Setup setup;

	private final Set<SessionCustomization> sessionCustomizations = new HashSet<>();

	private SessionTestExtension(String pluginId, String applicationId) {
		try {
			this.setup = SetupManager.getInstance().getDefaultSetup();
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
		private Set<SessionCustomization> storedSessionCustomizations = new HashSet<>();

		private SessionTestExtensionBuilder(String pluginId) {
			Objects.requireNonNull(pluginId);
			this.storedPluginId = pluginId;
		}

		/**
		 * Sets the ID of the Eclipse application to start for running the session
		 * tests.
		 *
		 * @param applicationId the ID of the Eclipse application to run the tests in,
		 *                      must not be {@code null}
		 *
		 * @return this
		 */
		public SessionTestExtensionBuilder withApplicationId(String applicationId) {
			Objects.requireNonNull(applicationId);
			storedApplicationId = applicationId;
			return this;
		}

		/**
		 * Sets the given session workspace by using the path of the passed
		 * customization for the "data" property of the Eclipse instance. The
		 * customization can be instantiated via
		 * {@link SessionTestExtension#createCustomWorkspace()}. By default, a temporary
		 * folder is used for the workspace.
		 * <p>
		 * <b>Example usage with default workspace directory:</b>
		 *
		 * <pre>
		 * &#64;RegisterExtension
		 * SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin("")
		 * 		.withCustomization(SessionTestExtension.createCustomWorkspace()).create();
		 * </pre>
		 *
		 * <b>Example usage with custom workspace directory:</b>
		 *
		 * <pre>
		 * &#64;TempDir
		 * Path workspaceDirectory;
		 *
		 * &#64;RegisterExtension
		 * SessionTestExtension extension = SessionTestExtension.forPlugin("")
		 * 		.withCustomization(SessionTestExtension.createCustomWorkspace().setWorkspaceDirectory(workspaceDirectory))
		 * 		.create();
		 * </pre>
		 *
		 * @param sessionWorkspace the customization object for specifying the workspace
		 *                         directory, must not be {@code null}
		 *
		 * @return this
		 */
		public SessionTestExtensionBuilder withCustomization(CustomSessionWorkspace sessionWorkspace) {
			Objects.requireNonNull(sessionWorkspace);
			this.storedSessionCustomizations.add(sessionWorkspace);
			return this;
		}

		/**
		 * {@return a <code>SessionTestExtension</code> created with the information in
		 * this builder}
		 */
		public SessionTestExtension create() {
			SessionTestExtension extension = new SessionTestExtension(storedPluginId, storedApplicationId);
			storedSessionCustomizations.forEach(customization -> extension.addSessionCustomization(customization));
			return extension;
		}
	}

	private void addSessionCustomization(SessionCustomization sessionCustomization) {
		this.sessionCustomizations.add(sessionCustomization);
	}

	/**
	 * {@return a custom workspace configuration that, by default, uses a temporary
	 * folder to store the workspace files}
	 */
	public static CustomSessionWorkspace createCustomWorkspace() {
		return new CustomSessionWorkspaceImpl();
	}

	public void setEclipseArgument(String key, String value) {
		setup.setEclipseArgument(key, value);
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
		try {
			prepareSession();
			testExecutor.executeRemotely(testClass, testMethod, shouldFail);
		} finally {
			cleanupSession();
		}
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

}
