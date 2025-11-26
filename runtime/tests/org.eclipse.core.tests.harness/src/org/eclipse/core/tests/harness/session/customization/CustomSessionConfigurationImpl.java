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
package org.eclipse.core.tests.harness.session.customization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.harness.FileSystemHelper.deleteOnShutdownRecursively;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.core.tests.session.Setup;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class CustomSessionConfigurationImpl implements CustomSessionConfiguration {
	private static final String PROP_BUNDLES = "osgi.bundles";
	private static final String PROP_FRAMEWORK = "osgi.framework";
	private static final String PROP_BUNDLES_DEFAULT_START_LEVEL = "osgi.bundles.defaultStartLevel";
	private static final String PROP_INSTALL_AREA = "osgi.install.area";
	private static final String PROP_CONFIG_AREA_READ_ONLY = InternalPlatform.PROP_CONFIG_AREA + ".readOnly";
	private static final String PROP_CONFIG_CASCADED = "osgi.configuration.cascaded";
	private static final String PROP_SHARED_CONFIG_AREA = "osgi.sharedConfiguration.area";
	private static final String TEMP_DIR_PREFIX = "eclipse_session_configuration";

	private final Collection<BundleReference> bundleReferences = new LinkedHashSet<>();
	private Path configurationDirectory;
	private boolean readOnly = false;
	private boolean cascaded = false;
	private boolean firstExecutedSession = true;

	public CustomSessionConfigurationImpl() {
		addMinimalBundleSet();
	}

	@SuppressWarnings("deprecation")
	private void addMinimalBundleSet() {
		// Just use any class from the bundles we want to add as minimal bundle set

		addBundle(org.eclipse.core.runtime.FileLocator.class, "@2:start"); // org.eclipse.equinox.common
		addBundle(org.eclipse.core.runtime.Platform.class, "@:start"); // org.eclipse.core.runtime
		addBundle(org.eclipse.core.runtime.jobs.Job.class); // org.eclipse.core.jobs
		addBundle(org.eclipse.core.runtime.IExtension.class); // org.eclipse.equinox.registry
		addBundle(org.eclipse.core.runtime.preferences.IEclipsePreferences.class); // org.eclipse.equinox.preferences
		addBundle(org.osgi.service.prefs.Preferences.class); // org.osgi.service.prefs
		addBundle(org.eclipse.core.runtime.content.IContentType.class); // org.eclipse.core.contenttype
		addBundle(org.eclipse.equinox.app.IApplication.class); // org.eclipse.equinox.app

		// org.apache.felix.scr + (non-optional) dependencies
		addBundle(org.apache.felix.scr.info.ScrInfo.class); // org.apache.felix.scr
		addBundle(org.osgi.service.event.EventAdmin.class); // org.osgi.service.event
		addBundle(org.osgi.service.component.ComponentConstants.class); // org.osgi.service.component
		addBundle(org.osgi.util.promise.Promise.class); // org.osgi.util.promise
		addBundle(org.osgi.util.function.Function.class); // org.osgi.util.function

		addBundle(org.eclipse.core.tests.harness.TestHarnessPlugin.class); // org.eclipse.core.tests.harness
		addBundle(org.eclipse.test.performance.Performance.class); // org.eclipse.test.performance

		addBundle(org.eclipse.jdt.internal.junit.runner.ITestLoader.class); // org.eclipse.jdt.junit.runtime
		addBundle(org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader.class); // org.eclipse.jdt.junit4.runtime
		addBundle(org.eclipse.jdt.internal.junit5.runner.JUnit5TestLoader.class); // org.eclipse.jdt.junit5.runtime
		addBundle(org.eclipse.pde.internal.junit.runtime.CoreTestApplication.class); // org.eclipse.pde.junit.runtime

		addBundle(net.bytebuddy.ByteBuddy.class); // net.bytebuddy for org.assertj.core.api
		addBundle(org.assertj.core.api.Assertions.class); // org.assertj.core.api
		addBundle(org.hamcrest.CoreMatchers.class); // org.hamcrest.core

		// The org.junit bundle requires an org.hamcrest.core bundle, but as of version
		// 2.x, the org.hamcrest bundle above provides the actual classes. So we need to
		// ensure that the actual org.hamcrest.core bundle required by org.junit is
		// added too.
		if ("org.hamcrest".equals(FrameworkUtil.getBundle(org.hamcrest.CoreMatchers.class).getSymbolicName())) {
			Bundle maxHamcrestCoreBundle = null;
			Version maxHamcrestCoreVersion = null;
			for (Bundle bundle : FrameworkUtil.getBundle(ConfigurationSessionTestSuite.class).getBundleContext()
					.getBundles()) {
				if ("org.hamcrest.core".equals(bundle.getSymbolicName())) {
					Version version = bundle.getVersion();
					if (maxHamcrestCoreVersion == null || maxHamcrestCoreVersion.compareTo(version) < 0) {
						maxHamcrestCoreVersion = version;
						maxHamcrestCoreBundle = bundle;
					}
				}
			}
			if (maxHamcrestCoreBundle != null) {
				addBundle(maxHamcrestCoreBundle, null);
			}
		}

		addBundle(org.junit.Test.class); // org.junit
		addBundle(org.junit.jupiter.api.Test.class); // junit-jupiter-api
		addBundle(org.junit.jupiter.engine.JupiterTestEngine.class); // junit-jupiter-engine
		addBundle(org.junit.jupiter.migrationsupport.EnableJUnit4MigrationSupport.class); // junit-jupiter-migrationsupport
		addBundle(org.junit.jupiter.params.ParameterizedTest.class); // junit-jupiter-params
		addBundle(org.junit.vintage.engine.VintageTestEngine.class); // junit-vintage-engine
		addBundle(org.junit.platform.commons.JUnitException.class); // junit-platform-commons
		addBundle(org.junit.platform.engine.TestEngine.class); // junit-platform-engine
		addBundle(org.junit.platform.launcher.Launcher.class); // junit-platform-launcher
		addBundle(org.junit.platform.runner.JUnitPlatform.class); // junit-platform-runner
		addBundle(org.junit.platform.suite.api.Suite.class); // junit-platform-suite-api
		addBundle(org.junit.platform.suite.commons.SuiteLauncherDiscoveryRequestBuilder.class); // junit-platform-suite-commons
		addBundle(org.junit.platform.suite.engine.SuiteTestEngine.class); // junit-platform-suite-engine
		addBundle(org.apiguardian.api.API.class); // org.apiguardian.api
		addBundle(org.opentest4j.AssertionFailedError.class); // org.opentest4j
	}

	@Override
	public CustomSessionConfiguration setCascaded() {
		this.cascaded = true;
		return this;
	}

	@Override
	public CustomSessionConfiguration setReadOnly() {
		this.readOnly = true;
		return this;
	}

	@Override
	public CustomSessionConfiguration setConfigurationDirectory(Path configurationDirectory) {
		Objects.requireNonNull(configurationDirectory);
		this.configurationDirectory = configurationDirectory;
		deleteOnShutdownRecursively(configurationDirectory);
		return this;
	}

	@Override
	public Path getConfigurationDirectory() throws IOException {
		if (configurationDirectory == null) {
			setConfigurationDirectory(Files.createTempDirectory(TEMP_DIR_PREFIX));
		}
		return configurationDirectory;
	}

	@Override
	public void prepareSession(Setup setup) throws IOException {
		if (firstExecutedSession) {
			// configuration area needs to be written on first start
			overwriteConfigurationAreaWritability(setup);
		}
		setCustomConfigurationArea(setup);
		if (firstExecutedSession) {
			createOrRefreshConfigIni();
		}
		// Recreating the config.ini when "cascaded==true" is a work around
		// update.configurator's class PlatformConfiguration.initializeCurrent(). That
		// method will overwrite config.ini for shared configurations. As there is no
		// switch to alter that behavior, this workaround generates a new config.ini for
		// every test run. In 2007, this was introduced as a temporary workaround as
		// update.configurator was expected to be close to be being retired.
		if (cascaded) {
			createOrRefreshConfigIni();
		}
	}

	@Override
	public void cleanupSession(Setup setup) {
		if (firstExecutedSession) {
			// after first session, use configuration area's readability configuration
			removeConfigurationAreaWritabilityOverwrite(setup);
		}
		firstExecutedSession = false;
	}

	private void overwriteConfigurationAreaWritability(Setup setup) {
		setup.setSystemProperty(PROP_CONFIG_AREA_READ_ONLY, Boolean.FALSE.toString());
	}

	private void removeConfigurationAreaWritabilityOverwrite(Setup setup) {
		setup.setSystemProperty(PROP_CONFIG_AREA_READ_ONLY, null);
	}

	private void setCustomConfigurationArea(Setup setup) throws IOException {
		// the base implementation will have set this to the host configuration
		setup.setEclipseArgument(Setup.CONFIGURATION, null);
		setup.setSystemProperty(InternalPlatform.PROP_CONFIG_AREA, getConfigurationDirectory().toString());
	}

	private void createOrRefreshConfigIni() throws IOException {
		Properties contents = new Properties();
		contents.put(PROP_BUNDLES, String.join(",", getBundleUrls()));
		contents.put(PROP_FRAMEWORK, getOsgiFrameworkBundleUrl());
		contents.put(PROP_BUNDLES_DEFAULT_START_LEVEL, "4");
		contents.put(PROP_INSTALL_AREA, Platform.getInstallLocation().getURL().toExternalForm());
		contents.put(PROP_CONFIG_CASCADED, Boolean.valueOf(cascaded).toString());
		if (cascaded) {
			contents.put(PROP_SHARED_CONFIG_AREA, Platform.getConfigurationLocation().getURL().toExternalForm());
		}
		contents.put(PROP_CONFIG_AREA_READ_ONLY, Boolean.valueOf(readOnly).toString());
		// save the properties
		Path configINI = getConfigurationDirectory().resolve("config.ini");
		try (OutputStream out = Files.newOutputStream(configINI)) {
			contents.store(out, null);
		}
	}

	@Override
	public CustomSessionConfiguration addBundle(Class<?> classFromBundle) {
		Objects.requireNonNull(classFromBundle);
		addBundle(classFromBundle, null);
		return this;
	}

	private void addBundle(Class<?> classFromBundle, String suffix) {
		Bundle bundle = FrameworkUtil.getBundle(classFromBundle);
		Assert.assertNotNull("Class is not from a bundle: " + classFromBundle, bundle);
		addBundle(bundle, suffix);
	}

	@Override
	public CustomSessionConfiguration addBundle(Bundle bundle) {
		Objects.requireNonNull(bundle);
		addBundle(bundle, "");
		return this;
	}

	private void addBundle(Bundle bundle, String suffix) {
		bundleReferences.add(new BundleReference(bundle, suffix));
	}

	private Collection<String> getBundleUrls() {
		assertThat(bundleReferences).as("check bundles are not empty").isNotEmpty();
		return bundleReferences.stream().map(BundleReference::toURL).collect(Collectors.toList());
	}

	private static String getOsgiFrameworkBundleUrl() {
		Bundle osgiFrameworkBundle = FrameworkUtil.getBundle(CustomSessionConfigurationImpl.class).getBundleContext()
				.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		BundleReference osgiFrameworkBundleReference = new BundleReference(osgiFrameworkBundle);
		return osgiFrameworkBundleReference.toURL();
	}

	private record BundleReference(Bundle bundle, String suffix) {
		BundleReference(Bundle bundle, String suffix) {
			this.bundle = bundle;
			this.suffix = suffix != null ? suffix : "";
		}

		BundleReference(Bundle bundle) {
			this(bundle, null);
		}

		String toURL() {
			Optional<File> location = FileLocator.getBundleFileLocation(bundle);
			assertTrue("Unable to locate bundle with id: " + bundle.getSymbolicName(), location.isPresent());
			String externalForm;
			try {
				externalForm = location.get().toURI().toURL().toExternalForm();
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to convert file to URL string:" + location.get(), e);
			}
			// workaround for bug 88070
			return "reference:" + externalForm + suffix;
		}
	}
}
