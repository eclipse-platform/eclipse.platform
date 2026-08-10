/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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

/*
 * Represents a feature entry in a platform.xml file.
 */
public class Feature {

	private String id;
	private String url;
	private String version;
	private String pluginIdentifier;
	private String pluginVersion;

	public String getId() {
		return id;
	}

	public String getPluginIdentifier() {
		return pluginIdentifier;
	}

	public String getPluginVersion() {
		return pluginVersion;
	}

	public String getUrl() {
		return url;
	}

	public String getVersion() {
		return version;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setPluginIdentifier(String pluginIdentifier) {
		this.pluginIdentifier = pluginIdentifier;
	}

	public void setPluginVersion(String pluginVersion) {
		this.pluginVersion = pluginVersion;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
