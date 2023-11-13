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
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class PlatformURLLocalTest {

	public static void assertEquals(String tag, URL expected, URL actual, boolean external)
			throws MalformedURLException {
		if (external) {
			Assert.assertEquals(tag, expected, actual);
		} else {
			Assert.assertEquals(tag + ".1",
				new URL(expected.getProtocol(), expected.getHost(), expected.getPort(), expected.getFile()),
				new URL(actual.getProtocol(), actual.getHost(), actual.getPort(), actual.getFile()));
		}
	}

	@Test
	public void testPlatformURLConfigResolution() throws IOException {
		// create a fake URL
		URL platformURL = new URL("platform:/config/x");
		URL resolvedURL = FileLocator.resolve(platformURL);
		assertNotEquals("3.0", platformURL, resolvedURL);
		URL expected = new URL(Platform.getConfigurationLocation().getURL(), "x");
		assertEquals("5.0", expected, resolvedURL, false);
	}

	@Test
	public void testPlatformURLMetaResolution() throws Exception {
		// create a fake URL
		URL platformURL = new URL("platform:/meta/" + RuntimeTestsPlugin.PI_RUNTIME_TESTS + "/x");
		URL resolvedURL = FileLocator.resolve(platformURL);
		assertNotEquals("3.0", platformURL, resolvedURL);
		URL expected = new URL(RuntimeTestsPlugin.getPlugin().getStateLocation().toFile().toURI().toURL(), "x");
		assertEquals("5.0", expected, resolvedURL, false);
	}

	@Test
	public void testBug155081() throws IOException, BundleException {
		Bundle bundle = null;
		try {
			bundle = BundleTestingHelper.installBundle("0.1", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "platformURL/platform.test.underscore");
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
			URL test = new URL("platform:/plugin/platform.test.underscore_1.1.0.r321_v20060816/test.txt");
			try (InputStream in = test.openStream()) {
			}
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	@Test
	public void testBug300197_01() throws IOException, BundleException {
		Bundle bundle = null;
		try {
			bundle = BundleTestingHelper.installBundle("0.1", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "platformURL/platform_test_underscore");
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
			URL test = new URL("platform:/plugin/platform_test_underscore/test.txt");
			try (InputStream in = test.openStream()) {
			}
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	@Test
	public void testBug300197_02() throws IOException, BundleException {
		Bundle bundle = null;
		try {
			bundle = BundleTestingHelper.installBundle("0.1", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "platformURL/platform_test_underscore_2.0.0");
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
			URL test = new URL("platform:/plugin/platform_test_underscore_2.0.0/test.txt");
			try (InputStream in = test.openStream()) {
			}
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}
}
