/*******************************************************************************
 * Copyright (c) 2019, 2020 Torbjörn Svensson and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Torbjörn Svensson <azoff@svenskalinuxforeningen.se> - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.update.internal.configurator.FeatureEntry;
import org.eclipse.update.internal.configurator.SiteEntry;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class FeatureEntryTests {

	@Test
	public void testLicenseUrl() throws MalformedURLException {
		FeatureEntry featureEntry = new FeatureEntry("org.eclipse.platform", "4.13.0.v20190916-1323", null, null, false, null, null);
		featureEntry.setSite(new SiteEntry(new URL("platform:/base/")));
		featureEntry.setURL("features/org.eclipse.platform_4.13.0.v20190916-1323/");

		featureEntry.setLicenseURL("http://www.example.org");
		assertEquals("http://www.example.org", featureEntry.getLicenseURL());

		featureEntry.setLicenseURL("https://www.example.org");
		assertEquals("https://www.example.org", featureEntry.getLicenseURL());

		featureEntry.setLicenseURL("license.html");
		assertEquals("platform:/base/features/org.eclipse.platform_4.13.0.v20190916-1323/license.html", featureEntry.getLicenseURL());
	}
}