/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model;

import java.util.HashMap;
import java.util.Map;

/**
 * This package maintains the mapping between extension ids in the registry and extension ids
 * as defined in extension files. It also allows a configurer to change the page which will be
 * displayed when the welcome screen is shown.
 */

public class ExtensionMap {

	private static ExtensionMap instance;
	private String startPage;
	private Map<String, String> extensions = new HashMap<>();

	private ExtensionMap() {

	}

	/**
	 * Get the one and only instance of this class
	 * @return
	 */
	static public ExtensionMap getInstance() {
		if (instance == null) {
			instance = new ExtensionMap();
		}
		return instance;
	}

	/**
	 * Save an association beteen an anchorId and pluginId
	 * @param anchorId the id of an anchor
	 * @param pluginId the plugin which contributed that anchor
	 */
	public void putPluginId(String anchorId, String pluginId) {
		if (anchorId != null) {
			extensions.put(anchorId, pluginId);
		}
	}

	/**
	 * Lookup in which plugin
	 * @param anchorId
	 * @return the plugin which contributed that anchor
	 */
	public String getPluginId(String anchorId) {
		return extensions.get(anchorId);
	}

	/**
	 * Clear the map and content page
	 */
	public void clear() {
		extensions = new HashMap<>();
		startPage = null;
	}

	/**
	 * called to determine if the configurer has overriden the start page
	 * @return the new start page or null.
	 */
	public String getStartPage() {
		return startPage;
	}

	/**
	 * Allows a configurer to override the page which is displayed when
	 * the welcome screen is first shown
	 * @param contentPage
	 */
	public void setStartPage(String contentPage) {
		startPage = contentPage;
	}

}
