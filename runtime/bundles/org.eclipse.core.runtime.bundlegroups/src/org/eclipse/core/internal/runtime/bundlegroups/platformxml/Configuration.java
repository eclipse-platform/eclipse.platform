/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime.bundlegroups.platformxml;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.URIUtil;

/**
 * The contents of a platform.xml file.
 */
public class Configuration {

	private final List<Site> sites = new ArrayList<>();
	private String sharedUR;
	private URL osgiInstallArea;

	void setSharedUR(String value) {
		sharedUR = value;
	}

	void setOsgiInstallArea(URL osgiInstallArea) {
		this.osgiInstallArea = osgiInstallArea;
	}

	/**
	 * The sites of this configuration, followed by those of the shared
	 * configuration it links to, if any.
	 */
	public List<Site> getSites() {
		if (sharedUR == null) {
			return sites;
		}
		List<Site> result = new ArrayList<>(sites);
		Configuration parent = loadParent();
		if (parent != null) {
			result.addAll(parent.getSites());
		}
		return result;
	}

	private Configuration loadParent() {
		File location;
		try {
			location = URIUtil.toFile(new URI(sharedUR));
		} catch (URISyntaxException e) {
			return null;
		}
		if (location == null) {
			return null;
		}
		if (!location.isAbsolute()) {
			File installArea = toFile(osgiInstallArea);
			if (installArea == null) {
				return null;
			}
			location = new File(installArea, location.getPath());
		}
		return ConfigurationParser.parse(location, osgiInstallArea);
	}

	private static File toFile(URL url) {
		try {
			return url == null ? null : URIUtil.toFile(URIUtil.toURI(url));
		} catch (URISyntaxException e) {
			return null;
		}
	}

	void add(Site site) {
		sites.add(site);
	}
}
