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

import java.util.ArrayList;
import java.util.List;

/*
 * Represents a site in a platform.xml file.
 */
public class Site {

	private String url;
	private final List<Feature> features = new ArrayList<>();

	public void addFeature(Feature feature) {
		this.features.add(feature);
	}

	public List<Feature> getFeatures() {
		return features;
	}

	/**
	 * Note the string that we are returning is an <em>ENCODED</em> URI string.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Note that the string should be an <em>ENCODED</em> URI string.
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}
