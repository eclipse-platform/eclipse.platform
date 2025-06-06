/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.StackWalker.Option;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import junit.framework.Test;
import junit.framework.TestResult;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.eclipse.osgi.service.datalocation.Location;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class ConfigurationSessionTestSuite extends SessionTestSuite {

	private static final String PROP_CONFIG_AREA_READ_ONLY = InternalPlatform.PROP_CONFIG_AREA + ".readOnly";
	private static final String PROP_CONFIG_CASCADED = "osgi.configuration.cascaded";
	private static final String PROP_SHARED_CONFIG_AREA = "osgi.sharedConfiguration.area";
	private final Collection<String> bundles = new ArrayList<>();
	private final Map<String, String> configIniValues = new HashMap<>();
	private boolean cascaded;

	// by default we clean-up after ourselves
	private boolean cleanUp = true;

	private IPath configurationPath = FileSystemHelper.getRandomLocation(FileSystemHelper.getTempDir());
	private boolean prime = true;
	private boolean readOnly;

	public ConfigurationSessionTestSuite(String pluginId, Class<?> theClass) {
		super(pluginId, theClass);
	}

	public ConfigurationSessionTestSuite(String pluginId, String name) {
		super(pluginId, name);
	}

	@SuppressWarnings("deprecation")
	public void addMinimalBundleSet() {
		// Just use any class from the bundles we want to add as minimal bundle set

		addBundle(org.eclipse.core.runtime.FileLocator.class, "@2:start"); // org.eclipse.equinox.common
		addBundle(org.eclipse.core.runtime.Platform.class, "@:start"); // org.eclipse.core.runtime
		addBundle(org.eclipse.core.runtime.jobs.Job.class); // org.eclipse.core.jobs
		addBundle(org.eclipse.core.runtime.IExtension.class); // org.eclipse.equinox.registry
		addBundle(org.eclipse.core.runtime.preferences.IEclipsePreferences.class); // org.eclipse.equinox.preferences
		addBundle(org.osgi.service.prefs.Preferences.class); // org.osgi.service.prefs
		addBundle(org.eclipse.core.runtime.content.IContentType.class); // org.eclipse.core.contenttype
		addBundle(org.eclipse.equinox.app.IApplication.class); // org.eclipse.equinox.app

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
			for (Bundle bundle : FrameworkUtil.getBundle(ConfigurationSessionTestSuite.class).getBundleContext().getBundles()) {
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

	public void addBundle(String id) {
		String suffix = "";
		int atIndex = id.indexOf('@');
		if (atIndex >= 0) {
			suffix = id.substring(atIndex);
			id = id.substring(0, atIndex);
		}
		Bundle[] allVersions = Platform.getBundles(id, null);
		Assert.assertNotNull("No bundles found in test runtime with id: " + id, allVersions);
		String refSuffix = suffix;
		List<String> urLs = Arrays.stream(allVersions).map(b -> getBundleReference(b, refSuffix)).collect(Collectors.toList());
		bundles.addAll(urLs);
	}

	private static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	public void addThisBundle() {
		addBundle(STACK_WALKER.getCallerClass());
	}

	public void addBundle(Class<?> classFromBundle) {
		addBundle(classFromBundle, null);
	}

	public void addBundle(Class<?> classFromBundle, String suffix) {
		Bundle bundle = FrameworkUtil.getBundle(classFromBundle);
		Assert.assertNotNull("Class is not from a bundle: " + classFromBundle, bundle);
		addBundle(bundle, suffix);
	}

	public void addBundle(Bundle bundle, String suffix) {
		String url = getBundleReference(bundle, suffix);
		bundles.add(url);
	}

	public void setConfigIniValue(String key, String value) {
		configIniValues.put(key, value);
	}

	private void createConfigINI() throws IOException {
		Assert.assertTrue("1.0", !bundles.isEmpty());
		Properties contents = new Properties();
		StringBuilder osgiBundles = new StringBuilder();
		for (String string : this.bundles) {
			osgiBundles.append(string);
			osgiBundles.append(',');
		}
		osgiBundles.deleteCharAt(osgiBundles.length() - 1);
		contents.put("osgi.bundles", osgiBundles.toString());
		Bundle osgiFrameworkBundle = FrameworkUtil.getBundle(ConfigurationSessionTestSuite.class).getBundleContext()
				.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		String osgiFramework = getBundleReference(osgiFrameworkBundle, null);
		contents.put("osgi.framework", osgiFramework);
		contents.put("osgi.bundles.defaultStartLevel", "4");
		contents.put("osgi.install.area", Platform.getInstallLocation().getURL().toExternalForm());
		contents.put(PROP_CONFIG_CASCADED, Boolean.toString(cascaded));
		if (cascaded) {
			contents.put(PROP_SHARED_CONFIG_AREA, Platform.getConfigurationLocation().getURL().toExternalForm());
		}
		contents.put(PROP_CONFIG_AREA_READ_ONLY, Boolean.toString(readOnly));
		for (Map.Entry<String, String> entry : configIniValues.entrySet()) {
			contents.put(entry.getKey(), entry.getValue());
		}
		// save the properties
		File configINI = configurationPath.append("config.ini").toFile();
		try (OutputStream out = new FileOutputStream(configINI)) {
			contents.store(out, null);
		}
	}

	@Override
	protected void fillTestDescriptor(TestDescriptor test) throws SetupException {
		super.fillTestDescriptor(test);
		if (prime) {
			test.getSetup().setSystemProperty(PROP_CONFIG_AREA_READ_ONLY, Boolean.FALSE.toString());
			prime = false;
		}
	}

	public IPath getConfigurationPath() {
		return configurationPath;
	}

	private String getBundleReference(Bundle bundle, String suffix) {
		Optional<File> location = FileLocator.getBundleFileLocation(bundle);
		assertTrue("Unable to locate bundle with id: " + bundle.getSymbolicName(), location.isPresent());
		String externalForm;
		try {
			externalForm = location.get().toURI().toURL().toExternalForm();
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to convert file to URL string:" + location.get(), e);
		}
		// workaround for bug 88070
		return "reference:" + externalForm + (suffix != null ? suffix : "");
	}

	public boolean isCascaded() {
		return cascaded;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Ensures setup uses this suite's instance location.
	 */
	@Override
	protected Setup newSetup() throws SetupException {
		Setup base = super.newSetup();
		// the base implementation will have set this to the host configuration
		base.setEclipseArgument(Setup.CONFIGURATION, null);
		base.setSystemProperty(InternalPlatform.PROP_CONFIG_AREA, configurationPath.toOSString());
		return base;
	}

	/**
	 * Ensures workspace location is empty before running the first test, and after
	 * running the last test. Also sorts the test cases to be run if this suite was
	 * created by reifying a test case class.
	 */
	@Override
	public void run(TestResult result) {
		configurationPath.toFile().mkdirs();
		try {
			if (prime) {
				try {
					createConfigINI();
				} catch (IOException e) {
					fail(e);
				}
			}
			// we have to sort the tests cases
			Test[] allTests = getTests(true);
			// now run the tests in order
			for (int i = 0; i < allTests.length && !result.shouldStop(); i++) {
				// KLUDGE: this is a  work around update.configurator's
				// class PlatformConfiguration.initializeCurrent(). That method will overwrite
				// config.ini for shared configurations. As there is no switch to alter
				// that behavior and update.configurator is close to be being retired,
				// the kludge here is to generate new config.ini for every test run.
				if (cascaded)
				 {
					try {
						createConfigINI();
					} catch (IOException e) {
						fail(e);
					}
				// end of KLUDGE
				}

				runTest(allTests[i], result);
			}
		} finally {
			if (cleanUp) {
				FileSystemHelper.clear(configurationPath.toFile());
			}
		}

	}

	public void setCascaded(boolean cascaded) {
		this.cascaded = cascaded;
	}

	public void setCleanup(boolean cleanUp) {
		this.cleanUp = cleanUp;
	}

	public void setConfigurationPath(IPath configurationPath) {
		this.configurationPath = configurationPath;
	}

	public void setPrime(boolean prime) {
		this.prime = prime;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public static File getConfigurationDir() {
		Location configurationLocation = Platform.getConfigurationLocation();
		URL configurationURL = configurationLocation.getURL();
		if (!"file".equals(configurationURL.getProtocol())) {
			// only works if configuration is file: based
			throw new IllegalStateException();
		}
		return new File(configurationURL.getFile());
	}

}
