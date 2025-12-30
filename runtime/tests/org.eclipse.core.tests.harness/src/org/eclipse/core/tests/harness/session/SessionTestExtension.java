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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.session.customization.CustomSessionConfigurationDummy;
import org.eclipse.core.tests.harness.session.customization.CustomSessionConfigurationImpl;
import org.eclipse.core.tests.harness.session.customization.CustomSessionWorkspaceDummy;
import org.eclipse.core.tests.harness.session.customization.CustomSessionWorkspaceImpl;
import org.eclipse.core.tests.harness.session.customization.SessionCustomization;
import org.eclipse.core.tests.harness.session.samples.SampleSessionTests;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.InvocationInterceptor;

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
public interface SessionTestExtension extends InvocationInterceptor {
	public static final String CORE_TEST_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$
	public static final String UI_TEST_APPLICATION = "org.eclipse.pde.junit.runtime.uitestapplication"; //$NON-NLS-1$

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
		 * Sets the given session configuration that uses a custom "config.ini" file
		 * with a defined set of bundles to be used. The customization can be
		 * instantiated via
		 * {@link SessionTestExtension#createCustomConfiguration()}. By default,
		 * a temporary folder is used as a configuration directory.
		 * <p>
		 * <b>Example usage with default configuration directory:</b>
		 *
		 * <pre>
		 * &#64;RegisterExtension
		 * SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin("")
		 * 		.withConfiguration(SessionTestExtension.createCustomConfiguration()).create();
		 * </pre>
		 *
		 * <b>Example usage with custom configuration directory:</b>
		 *
		 * <pre>
		 * &#64;TempDir
		 * Path configurationDirectory;
		 *
		 * &#64;RegisterExtension
		 * SessionTestExtension extension = SessionTestExtension.forPlugin("")
		 * 		.withConfiguration(
		 * 				SessionTestExtension.createCustomConfiguration().setConfigurationDirectory(configurationDirectory))
		 * 		.create();
		 * </pre>
		 *
		 * @param sessionConfiguration the custom configuration to use for the session
		 *                             tests
		 */
		public SessionTestExtensionBuilder withCustomization(CustomSessionConfiguration sessionConfiguration) {
			Objects.requireNonNull(sessionConfiguration);
			sessionConfiguration.addBundle(Platform.getBundle(storedPluginId));
			this.storedSessionCustomizations.add(sessionConfiguration);
			return this;
		}

		/**
		 * {@return a <code>SessionTestExtension</code> created with the information in
		 * this builder}
		 */
		public SessionTestExtension create() {
			if (RemoteTestExecutor.isRemoteExecution()) {
				return new SessionTestExtensionRemote();
			}
			SessionTestExtensionHost extension = new SessionTestExtensionHost(storedPluginId, storedApplicationId);
			storedSessionCustomizations.forEach(customization -> extension.addSessionCustomization(customization));
			return extension;
		}
	}

	/**
	 * {@return a custom workspace configuration that, by default, uses a temporary
	 * folder to store the workspace files}
	 */
	public static CustomSessionWorkspace createCustomWorkspace() {
		if (RemoteTestExecutor.isRemoteExecution()) {
			return new CustomSessionWorkspaceDummy();
		}
		return new CustomSessionWorkspaceImpl();
	}

	/**
	 * {@return a custom Eclipse instance configuration that, by default, uses a
	 * temporary folder to store the configuration files}
	 */
	public static CustomSessionConfiguration createCustomConfiguration() {
		if (RemoteTestExecutor.isRemoteExecution()) {
			return new CustomSessionConfigurationDummy();
		}
		return new CustomSessionConfigurationImpl();
	}

	/**
	 * Sets the given Eclipse program argument to the given value for sessions
	 * executed with this extension.
	 *
	 * @param key   the Eclipse argument key, must not be {@code null}
	 * @param value the Eclipse argument value to set, may be {@code null} to remove
	 *              the key
	 */
	public void setEclipseArgument(String key, String value);

	/**
	 * Sets the given system property to the given value for sessions executed with
	 * this extension.
	 *
	 * @param key   the system property key, must not be {@code null}
	 * @param value the system property value to set, may be {@code null} to remove
	 *              the key
	 */
	public void setSystemProperty(String key, String value);

}
