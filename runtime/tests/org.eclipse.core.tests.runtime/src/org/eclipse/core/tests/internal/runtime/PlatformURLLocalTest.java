/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.core.tests.internal.runtime;

import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.runtime.RuntimeTest;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class PlatformURLLocalTest extends RuntimeTest {

	public static void assertEquals(String tag, URL expected, URL actual, boolean external) {
		if (external) {
			assertEquals(tag, expected, actual);
			return;
		}
		try {
			assertEquals(tag + ".1", new URL(expected.getProtocol(), expected.getHost(), expected.getPort(), expected.getFile()), new URL(actual.getProtocol(), actual.getHost(), actual.getPort(), actual.getFile()));
		} catch (MalformedURLException e) {
			fail(tag + ".2", e);
		}
	}

	public PlatformURLLocalTest(String name) {
		super(name);
	}

	public void testPlatformURLConfigResolution() {
		URL platformURL = null;
		try {
			// 	create a fake URL
			platformURL = new URL("platform:/config/x");
		} catch (MalformedURLException e) {
			fail("1.0", e);
		}
		URL resolvedURL = null;
		try {
			resolvedURL = FileLocator.resolve(platformURL);
		} catch (IOException e) {
			fail("2.0", e);
		}
		assertNotEquals("3.0", platformURL, resolvedURL);
		URL expected = null;
		try {
			expected = new URL(Platform.getConfigurationLocation().getURL(), "x");
		} catch (MalformedURLException e) {
			fail("4.0", e);
		}
		assertEquals("5.0", expected, resolvedURL, false);
	}

	public void testPlatformURLMetaResolution() {
		URL platformURL = null;
		try {
			// 	create a fake URL
			platformURL = new URL("platform:/meta/" + PI_RUNTIME_TESTS + "/x");
		} catch (MalformedURLException e) {
			fail("1.0", e);
		}
		URL resolvedURL = null;
		try {
			resolvedURL = FileLocator.resolve(platformURL);
		} catch (IOException e) {
			fail("2.0", e);
		}
		assertNotEquals("3.0", platformURL, resolvedURL);
		URL expected = null;
		try {
			expected = new URL(RuntimeTestsPlugin.getPlugin().getStateLocation().toFile().toURI().toURL(), "x");
		} catch (MalformedURLException e) {
			fail("4.0", e);
		}
		assertEquals("5.0", expected, resolvedURL, false);
	}

	public void testBug155081() throws IOException, BundleException {
		Bundle bundle = null;
		try {
			bundle = BundleTestingHelper.installBundle("0.1", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "platformURL/platform.test.underscore");
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
			URL test = new URL("platform:/plugin/platform.test.underscore_1.1.0.r321_v20060816/test.txt");
			InputStream in = test.openStream();
			in.close();
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	public void testBug300197_01() throws IOException, BundleException {
		Bundle bundle = null;
		try {
			bundle = BundleTestingHelper.installBundle("0.1", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "platformURL/platform_test_underscore");
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
			URL test = new URL("platform:/plugin/platform_test_underscore/test.txt");
			InputStream in = test.openStream();
			in.close();
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	public void testBug300197_02() throws IOException, BundleException {
		Bundle bundle = null;
		try {
			bundle = BundleTestingHelper.installBundle("0.1", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "platformURL/platform_test_underscore_2.0.0");
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
			URL test = new URL("platform:/plugin/platform_test_underscore_2.0.0/test.txt");
			InputStream in = test.openStream();
			in.close();
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}
}
