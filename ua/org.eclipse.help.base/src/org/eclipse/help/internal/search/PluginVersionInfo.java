/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;
import org.eclipse.help.internal.base.util.*;
import org.osgi.framework.*;

/**
 * Table of plugins. Records all plugins, their version, corresponding fragments
 * versions The values are String in format:
 * pluginID\npluginVersion\nfragment1ID\nfragment1Version\nfragment2ID\nfragment2Version
 */
public class PluginVersionInfo extends HelpProperties {
	private static final long serialVersionUID = 1L;

	// Separates plugins and versions in value strings
	protected static final String SEPARATOR = "\n"; //$NON-NLS-1$

	File dir;

	boolean doComparison = true;

	boolean hasChanged = false;

	boolean ignoreSavedVersions;

	Collection<String> added = new ArrayList<>();

	Collection<String> removed = new ArrayList<>();

	/**
	 * Creates table of current contributing plugins and their fragments with
	 * versions.
	 *
	 * @param name
	 *            the name of the file to serialize the data to
	 * @param docBundleIds
	 *            Collection of String
	 * @param dir
	 *            location to store the data
	 * @param ignoreSavedVersions
	 *            if true, will cause detect change to ignore saved plugin
	 *            version and behave like there was nothing saved
	 */
	public PluginVersionInfo(String name, Collection<String> docBundleIds, File dir,
			boolean ignoreSavedVersions) {
		super(name, dir);
		this.dir = dir;
		this.ignoreSavedVersions = ignoreSavedVersions;
		createTable(docBundleIds);
	}

	protected void createTable(Collection<String> docBundleIds) {
		// create table of current contributions
		for (String bundleId : docBundleIds) {
			Bundle pluginBundle = Platform.getBundle(bundleId);
			if (pluginBundle == null) {
				continue;
			}
			StringBuilder pluginVersionAndFragments = new StringBuilder();
			appendBundleInformation(pluginVersionAndFragments, bundleId,
					pluginBundle.getHeaders().get(
							Constants.BUNDLE_VERSION));
			Bundle[] fragmentBundles = Platform.getFragments(pluginBundle);
			if (fragmentBundles != null) {
				for (Bundle fragmentBundle : fragmentBundles) {
					if (fragmentBundle.getState() == Bundle.INSTALLED
							|| fragmentBundle.getState() == Bundle.UNINSTALLED)
						continue;
					appendBundleInformation(pluginVersionAndFragments,
							fragmentBundle.getSymbolicName(),
							fragmentBundle.getHeaders().get(
									Constants.BUNDLE_VERSION));
				}
			}
			this.put(bundleId, pluginVersionAndFragments.toString());
		}
	}

	protected void appendBundleInformation(StringBuilder buffer, String id,
			String version) {
		if (buffer.length()>0)
			buffer.append(SEPARATOR);
		buffer.append(id);
		buffer.append(SEPARATOR);
		buffer.append(version);
	}

	/**
	 * Detects changes in contributions or their version since last time the
	 * contribution table was saved.
	 *
	 * @return true if contributions have changed
	 */
	public boolean detectChange() {
		if (!doComparison)
			return hasChanged;
		// Create table of contributions present before last save()
		HelpProperties oldContrs = new HelpProperties(this.name, dir);
		if (!ignoreSavedVersions) {
			oldContrs.restore();
		}
		// check if contributions changed
		hasChanged = false;
		for (Enumeration<Object> keysEnum = this.keys(); keysEnum.hasMoreElements();) {
			String oneContr = (String) keysEnum.nextElement();
			if (!oldContrs.containsKey(oneContr)) {
				// plugin has been added
				added.add(oneContr);
			} else {
				String versions = (String) this.get(oneContr);
				String oldVersions = (String) oldContrs.get(oneContr);
				if (!compare(versions, oldVersions)) {
					// plugin version changed or fragments changed
					added.add(oneContr);
					removed.add(oneContr);
				}
			}
		}
		for (Enumeration<?> keysEnum = oldContrs.keys(); keysEnum
				.hasMoreElements();) {
			String oneContr = (String) keysEnum.nextElement();
			if (!this.containsKey(oneContr)) {
				// plugin has been removed
				removed.add(oneContr);
			}
		}
		hasChanged = added.size() > 0 || removed.size() > 0;
		doComparison = false;
		return hasChanged;
	}

	/**
	 * @return String - Collection of IDs of contributions that were added or
	 *         upgraded
	 */
	public Collection<String> getAdded() {
		if (doComparison)
			detectChange();
		return added;
	}

	/**
	 * @return String - Collection of IDs of contributions that were removed or
	 *         upgraded
	 */
	public Collection<String> getRemoved() {
		if (doComparison)
			detectChange();
		return removed;
	}

	/**
	 * Saves contributions to a file. After this method is called, calls to
	 * detectChange() will return false.
	 *
	 * @return true if operation was successful
	 */
	@Override
	public boolean save() {
		if (super.save()) {
			doComparison = false;
			hasChanged = false;
			ignoreSavedVersions = false;
			added = new ArrayList<>();
			removed = new ArrayList<>();
			return true;
		}
		return false;
	}

	/**
	 * Compares plugins and versions represented as a string for equality String
	 * have form id1\nverison1\nid2\nversion2 String are equal of they contain
	 * the same set of IDs and their corresponding version equal
	 *
	 * @return true if plugins and versions match
	 */
	private boolean compare(String versions, String oldVersions) {
		Map<String, String> versionMap = new HashMap<>();
		for (StringTokenizer t = new StringTokenizer(versions, SEPARATOR, false); t
				.hasMoreTokens();) {
			String pluginOrFragment = t.nextToken();
			if (t.hasMoreTokens()) {
				versionMap.put(pluginOrFragment, t.nextToken());
			}
		}
		Map<String, String> oldVersionMap = new HashMap<>();
		for (StringTokenizer t = new StringTokenizer(oldVersions, SEPARATOR,
				false); t.hasMoreTokens();) {
			String pluginOrFragment = t.nextToken();
			if (t.hasMoreTokens()) {
				oldVersionMap.put(pluginOrFragment, t.nextToken());
			}
		}
		return versionMap.equals(oldVersionMap);
	}
}
