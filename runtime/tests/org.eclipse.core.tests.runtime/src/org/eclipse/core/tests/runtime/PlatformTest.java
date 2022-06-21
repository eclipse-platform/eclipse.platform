/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime;

import static java.util.Collections.emptyMap;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.junit.Test;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test cases for the Platform API
 */
public class PlatformTest extends RuntimeTest {
	private FrameworkLog logService;
	private ServiceReference<FrameworkLog> logRef;
	private java.io.File originalLocation;

	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public PlatformTest() {
		super(null);
	}

	public PlatformTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		//ensure platform locations are initialized
		Platform.getLogFileLocation();

		//setup reference to log service, and remember original log location
		logRef = RuntimeTestsPlugin.getContext().getServiceReference(FrameworkLog.class);
		logService = RuntimeTestsPlugin.getContext().getService(logRef);
		originalLocation = logService.getFile();
	}

	@Override
	protected void tearDown() throws Exception {
		//undo any damage done by log location test
		super.tearDown();
		logService.setFile(originalLocation, true);
		RuntimeTestsPlugin.getContext().ungetService(logRef);
	}

	public void testGetSystemCharset() {
		Charset encoding = Platform.getSystemCharset();
		assertNotNull(encoding);
		String vmSpec = System.getProperty("java.vm.specification.version");
		int version = Integer.parseInt(vmSpec);
		String property;
		if (version >= 18) {
			property = System.getProperty("native.encoding");
		} else {
			property = System.getProperty("sun.jnu.encoding");
		}
		assertEquals(Charset.forName(property), encoding);
	}

	public void testGetCommandLine() {
		assertNotNull("1.0", Platform.getCommandLineArgs());
	}

	public void testGetLocation() {
		assertNotNull("1.0", Platform.getLocation());
	}

	/**
	 * API test for {@link Platform#getResourceBundle(org.osgi.framework.Bundle)}
	 */
	public void testGetResourceBundle() {
		//ensure it returns non-null for bundle with a resource bundle
		ResourceBundle bundle = Platform.getResourceBundle(Platform.getBundle("org.eclipse.core.runtime"));
		assertNotNull(bundle);
		//ensure it throws exception for bundle with no resource bundle
		boolean failed = false;
		try {
			bundle = Platform.getResourceBundle(Platform.getBundle("org.eclipse.core.tests.runtime"));
		} catch (MissingResourceException e) {
			//expected
			failed = true;
		}
		assertTrue(failed);
	}

	public void testGetLogLocation() throws IOException {
		IPath initialLocation = Platform.getLogFileLocation();
		System.out.println(Platform.getLogFileLocation());
		Platform.getStateLocation(Platform.getBundle("org.eclipse.equinox.common"));//causes DataArea to be initialzed
		System.out.println(Platform.getLogFileLocation());

		assertNotNull("1.0", initialLocation);

		//ensure result is same as log service
		IPath logPath = new Path(logService.getFile().getAbsolutePath());
		assertEquals("2.0", logPath, initialLocation);

		//changing log service location should change log location
		File newLocation = File.createTempFile("testGetLogLocation", null);
		logService.setFile(newLocation, true);
		assertEquals("3.0", new Path(newLocation.getAbsolutePath()), Platform.getLogFileLocation());

		//when log is non-local, should revert to default location
		logService.setWriter(new StringWriter(), true);
		assertEquals("4.0", initialLocation, Platform.getLogFileLocation());
	}

	public void testRunnable() {
		final List<Throwable> exceptions = new ArrayList<>();

		final List<IStatus> collected = new ArrayList<>();

		// add a log listener to ensure that we report using the right plug-in id
		ILogListener logListener = new ILogListener() {
			@Override
			public void logging(IStatus status, String plugin) {
				collected.add(status);
			}
		};
		Platform.addLogListener(logListener);

		final Exception exception = new Exception("PlatformTest.testRunnable: this exception is thrown on purpose as part of the test.");
		ISafeRunnable runnable = new ISafeRunnable() {
			@Override
			public void handleException(Throwable t) {
				exceptions.add(t);
			}

			@Override
			public void run() throws Exception {
				throw exception;
			}
		};

		SafeRunner.run(runnable);

		Platform.removeLogListener(logListener);

		assertEquals("1.0", exceptions.size(), 1);
		assertEquals("1.1", exception, exceptions.get(0));

		// ensures the status object produced has the right plug-in id (bug 83614)
		assertEquals("2.0", collected.size(), 1);
		assertEquals("2.1", RuntimeTest.PI_RUNTIME_TESTS, collected.get(0).getPlugin());
	}

	/**
	 * Test for method {@link Platform#isFragment(Bundle)}.
	 * <p>
	 * This test creates a simple bundle and a fragment for it, packages them to
	 * jars and installs them into the OSGi container.
	 * </p>
	 * <p>
	 * The test checks that {@link Platform#isFragment(Bundle)} returns
	 * <code>false</code> for the host bundle and <code>true</code> for the fragment
	 * bundle.
	 * </p>
	 */
	public void testIsFragment() throws Exception {
		String bundleName = getName();
		File config = RuntimeTestsPlugin.getContext().getDataFile(bundleName);
		Files.createDirectories(config.toPath());

		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_SYMBOLICNAME, bundleName);
		headers.put(Constants.BUNDLE_VERSION, "1.0.0");
		File testBundleJarFile = createBundle(config, bundleName, headers, emptyMap());
		Bundle hostBundle = getContext().installBundle(testBundleJarFile.getName(),
				new FileInputStream(testBundleJarFile));
		assertNotNull(hostBundle);

		assertFalse(Platform.isFragment(hostBundle));

		String fragmentName = bundleName + "_fragment";
		headers.put(Constants.BUNDLE_SYMBOLICNAME, fragmentName);
		headers.put(Constants.BUNDLE_VERSION, "1.0.0");
		headers.put(Constants.FRAGMENT_HOST, bundleName);
		testBundleJarFile = createBundle(config, fragmentName, headers, emptyMap());
		Bundle fragmentBundle = getContext().installBundle(testBundleJarFile.getName(),
				new FileInputStream(testBundleJarFile));
		assertNotNull(fragmentBundle);

		assertTrue(Platform.isFragment(fragmentBundle));
	}

	/**
	 * Test for method {@link Platform#getBundle(String)}.
	 * <p>
	 * This test creates 2 bundles with the same symbolic name in 2 versions,
	 * packages them to jars and installs them into the OSGi container.
	 * </p>
	 * <p>
	 * After installing the bundles they are in {@link Bundle#INSTALLED} state,
	 * which are filtered out when querying the Platform for bundles. The test
	 * checks that the bundles are not returned by the method yet.
	 * </p>
	 * <p>
	 * Next, the test starts the bundles which makes them visible. It is checked
	 * that {@link Platform#getBundle(String)} returns the bundle with the highest
	 * version.
	 * </p>
	 * <p>
	 * The bundle with the highest version is uninstalled then, and afterwards the
	 * bundle with the lower version will be returned by the Platform.
	 * </p>
	 * <p>
	 * Last, the second bundle is also uninstalled, and the test checks that no
	 * bundle is returned by Platform.
	 * </p>
	 */
	public void testGetBundle() throws Exception {
		Map<String, Bundle> bundles = createSimpleTestBundles("1.0.0", "2.0.0");
		Bundle bundle;

		bundle = Platform.getBundle(getName());
		assertNull(getName() + " bundle just installed, but not started => expect null result", bundle);
		for (Bundle b : bundles.values()) {
			b.start();
		}

		// now get it from Platform
		// 2 versions installed, highest version should be returned
		bundle = Platform.getBundle(getName());
		assertNotNull("bundle must be available", bundle);
		assertEquals("2.0.0", bundle.getVersion().toString());

		// uninstall it; now lower version will be returned
		bundle.uninstall();
		bundle = Platform.getBundle(getName());
		assertNotNull("bundle must be available", bundle);
		assertEquals("1.0.0", bundle.getVersion().toString());

		// uninstall it; no bundle available
		bundle.uninstall();
		bundle = Platform.getBundle(getName());
		assertNull(getName() + " bundle => expect null result", bundle);
	}

	@Test
	public void testILogFactory() {
		ILog staticLog = RuntimeTestsPlugin.getPlugin().getLog();
		assertNotNull(staticLog);
		ServiceTracker<ILog, ILog> tracker = new ServiceTracker<>(
				RuntimeTestsPlugin.getPlugin().getBundle().getBundleContext(), ILog.class, null);
		tracker.open();
		try {
			ILog serviceLog = tracker.getService();
			assertEquals(staticLog, serviceLog);
		} finally {
			tracker.close();
		}
	}

	/**
	 * Test for method {@link Platform#getBundles(String, String)}.
	 * <p>
	 * This test creates 3 bundles with the same symbolic name in 3 versions,
	 * packages them to jars and installs them into the OSGi container.
	 * </p>
	 * <p>
	 * First, the test checks that the method returns no result, since the bundles
	 * are in {@link Bundle#INSTALLED} state, which are filtered out by the method.
	 * The bundles are started then to become visible.
	 * </p>
	 * <p>
	 * Next, it is tested that the method returns all bundle in descending version
	 * order when no version constraint is given.
	 * </p>
	 * <p>
	 * Then different version constraints are given and it is tested that the
	 * expected number of bundles in the expected order are returned by
	 * {@link Platform#getBundles(String, String)}.
	 * </p>
	 */
	public void testGetBundles() throws Exception {
		Map<String, Bundle> bundles = createSimpleTestBundles("1.0.0", "3.0.0", "2.0.0");
		Bundle bundle;

		bundle = Platform.getBundle(getName());
		assertNull(getName() + " bundle just installed, but not started => expect null result", bundle);
		for (Bundle b : bundles.values()) {
			b.start();
		}

		Bundle[] result = Platform.getBundles(getName(), null); // no version constraint => get all 3
		assertNotNull(getName() + " bundle not available", bundles);
		assertEquals(3, result.length);
		assertEquals(3, result[0].getVersion().getMajor()); // 3.0.0 version first
		assertEquals(1, result[2].getVersion().getMajor()); // 1.0.0 version last

		result = Platform.getBundles(getName(), "2.0.0");
		assertEquals(2, result.length);
		assertEquals(3, result[0].getVersion().getMajor()); // 3.0.0 version first
		assertEquals(2, result[1].getVersion().getMajor()); // 2.0.0 version last

		result = Platform.getBundles(getName(), "[1.0.0,2.0.0)");
		assertEquals(1, result.length);
		assertEquals(1, result[0].getVersion().getMajor()); // 1.0.0 version

		result = Platform.getBundles(getName(), "[1.1.0,2.0.0)");
		assertNull("no match => null result", result);
	}

	public void testGetSystemBundle() {
		Bundle expectedSystem = RuntimeTestsPlugin.getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		Bundle actualSystem = Platform.getBundle(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		assertEquals("Wrong system bundle.", expectedSystem, actualSystem);

		Bundle[] actualSystems = Platform.getBundles(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null);
		assertEquals("Wrong number of system bundles.", 1, actualSystems.length);
		assertEquals("Wrong system bundle.", expectedSystem, actualSystems[0]);
	}

	/**
	 * Helper method to create empty bundles with just name and version given. The
	 * bundles are packaged to jars and installed into the container. The jars are
	 * marked for deletion on exit.
	 */
	private Map<String, Bundle> createSimpleTestBundles(String... versions) throws BundleException, IOException {
		Map<String, Bundle> bundles = new HashMap<>();
		String bundleName = getName();
		File config = RuntimeTestsPlugin.getContext().getDataFile(bundleName);
		Files.createDirectories(config.toPath());

		for (String v : versions) {
			Map<String, String> headers = new HashMap<>();
			headers.put(Constants.BUNDLE_SYMBOLICNAME, bundleName);
			headers.put(Constants.BUNDLE_VERSION, v);
			File testBundleJarFile = createBundle(config, bundleName + "_" + v, headers, emptyMap());
			Bundle testBundle = getContext().installBundle(testBundleJarFile.getName(),
					new FileInputStream(testBundleJarFile));
			assertNotNull(testBundle);
			bundles.put(v, testBundle);
		}
		return bundles;
	}

	// copied from
	// org.eclipse.osgi.tests.bundles.SystemBundleTests.createBundle(File, String,
	// Map<String, String>, Map<String, String>...)
	@SafeVarargs
	static File createBundle(File outputDir, String bundleName, Map<String, String> headers,
			Map<String, String>... entries) throws IOException {
		Manifest m = new Manifest();
		Attributes attributes = m.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			attributes.putValue(entry.getKey(), entry.getValue());
		}
		File file = new File(outputDir, "bundle" + bundleName + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), m)) {
			if (entries != null) {
				for (Map<String, String> entryMap : entries) {
					for (Map.Entry<String, String> entry : entryMap.entrySet()) {
						jos.putNextEntry(new JarEntry(entry.getKey()));
						if (entry.getValue() != null) {
							jos.write(entry.getValue().getBytes());
						}
						jos.closeEntry();
					}
				}
			}
			jos.flush();
		}
		file.deleteOnExit();
		return file;
	}

	public void testDebugOption() {
		assertNull(Platform.getDebugOption("Missing Option"));
		assertFalse(Platform.getDebugBoolean("Missing Option"));
		String option = Platform.getDebugOption("org.eclipse.core.runtime/debug-test");
		assertEquals("true".equalsIgnoreCase(option), Platform.getDebugBoolean("org.eclipse.core.runtime/debug-test"));
	}
}
