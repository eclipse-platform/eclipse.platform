/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/

package org.eclipse.help.internal.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.HelpPlugin;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Handles unresolved toc place holders as well as situations where remote help
 * is unavailable
 */

public class MissingContentManager {

	private static final String HELP_PROTOCOL = "help:"; //$NON-NLS-1$
	private static final String EXTENSION_POINT_ID_TOC = HelpPlugin.PLUGIN_ID + ".toc"; //$NON-NLS-1$
	private static final String ELEMENT_NAME_PLACEHOLDER = "placeholder"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_PLACEHOLDER_PAGE = "placeholderPage"; //$NON-NLS-1$
	public static final String  IGNORE_MISSING_PLACEHOLDER_PREFERENCE = "ignorePlaceholders"; //$NON-NLS-1$

	// Hrefs which are processed by org.eclipse.help.internal.webapp.StatusProducer
	public static final String REMOTE_STATUS_HREF = "NetworkHelpStatus.html"; //$NON-NLS-1$
	public static final String REMOTE_STATUS_HELP_VIEW_HREF = "NetworkHelpStatusHV.html"; //$NON-NLS-1$
	public static final String MISSING_TOPIC_HREF = "MissingTopicStatus.html"; //$NON-NLS-1$
	public static final String MISSING_TOPIC_PATH = "missingTopic/"; //$NON-NLS-1$
	public static final String MISSING_BOOKS_HREF = "MissingBooks.html"; //$NON-NLS-1$
	public static final String MISSING_BOOKS_HELP_VIEW_HREF = "MissingBooksHV.html"; //$NON-NLS-1$

	/*
	 * A place holder defines a page to be shown when a documentation page
	 * which matches the specified path not installed
	 */
	public static class Placeholder implements Comparable<Placeholder> {
		public String path;
		public String bundle;
		public String placeholderPage;

		public Placeholder(String path, String bundle, String placeholderPage) {
			this.path = path;
			this.bundle = bundle;
			this.placeholderPage = placeholderPage;
		}

		@Override
		public int compareTo(Placeholder o) {
			return o.path.compareTo(path);
		}
	}

	private static MissingContentManager instance;
	private List<Placeholder> placeholders;
	private Set<String> bundlesToIgnore; // A set of bundles the user does not want to see reference to

	public static MissingContentManager getInstance() {
		if ( instance == null ) {
			instance = new MissingContentManager();
		}
		return instance;
	}

	/*
	 * Read the extension registry
	 */
	private MissingContentManager() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		placeholders = new ArrayList<>();
		bundlesToIgnore = new HashSet<>();
		if ( BaseHelpSystem.getMode() == BaseHelpSystem.MODE_INFOCENTER ) {
			return; // Placeholders are not shown for infocenters
		}
		// Read the placeholders from the extension registry
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(EXTENSION_POINT_ID_TOC);
		for (IConfigurationElement elem : elements) {
			String pluginId = elem.getContributor().getName();
			if (elem.getName().equals(ELEMENT_NAME_PLACEHOLDER)) {
				try {
					String plugin = elem.getAttribute(ATTRIBUTE_NAME_PLUGIN);
					String path = HELP_PROTOCOL  + plugin + '/';
					String placeholder = elem
							.getAttribute(ATTRIBUTE_NAME_PLACEHOLDER_PAGE);
					placeholders.add(new Placeholder(path, plugin, placeholder));
				} catch (Exception e) {
					// log and skip
					String msg = "Exception reading " + ELEMENT_NAME_PLACEHOLDER + " extension in bundle" + pluginId; //$NON-NLS-1$ //$NON-NLS-2$
					Platform.getLog(getClass()).error(msg, e);
				}
			}
		}
		placeholders.sort(null);
		// Read the preferences to find any ignored placeholders
		String ignoredBundles = Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, IGNORE_MISSING_PLACEHOLDER_PREFERENCE, "", null); //$NON-NLS-1$
		if (ignoredBundles.length() > 0) {
			StringTokenizer tokenizer = new StringTokenizer(ignoredBundles, " ,"); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				bundlesToIgnore.add(tokenizer.nextToken());
			}
		}
	}

	/**
	 * Called when a page cannot be found
	 * @param path the path of the page that could not be loaded
	 * @return a place holder page if defined, otherwise an error page
	 */
	public String getPageNotFoundPage(String path, boolean showPlaceholderPage) {
		for (Placeholder placeholder : placeholders) {
			if (path.startsWith(placeholder.path) && Platform.getBundle(placeholder.bundle) == null) {
				if ( showPlaceholderPage) {
					return placeholder.placeholderPage;
				} else {
					return "/org.eclipse.help.webapp/" + MISSING_TOPIC_PATH + path.substring(HELP_PROTOCOL.length()); //$NON-NLS-1$
				}
			}
		}
		return Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, "page_not_found", null, null); //$NON-NLS-1$
	}

	/**
	 *
	 * @return true if there is an unresolved place holder and this is not an infocenter
	 */
	public boolean isUnresolvedPlaceholders() {
		if (BaseHelpSystem.getMode()==BaseHelpSystem.MODE_INFOCENTER) {
			return false;
		}
		Placeholder[] unresolvedPlaceHolders = getUnresolvedPlaceholders();
		return unresolvedPlaceHolders.length > 0;
	}

	/**
	 * If any help is missing returns an appropriate page
	 * @return null if no help is unavailable or an appropriate page if
	 *  the plug-in that corresponds to a place holder is not available.
	 *  The returned page will be in the format /plug-in/path.
	 */
	public String getHelpMissingPage(boolean isHelpView) {
		Placeholder[] unresolvedPlaceHolders = getUnresolvedPlaceholders();
		if (unresolvedPlaceHolders.length == 0) {
				return null;
		} else {
				String suffix = isHelpView ? MISSING_BOOKS_HELP_VIEW_HREF : MISSING_BOOKS_HREF;
				return "/org.eclipse.help.webapp" + '/'+ suffix; //$NON-NLS-1$
		}
	}

	/**
	 * Get the page to be shown when some remote help is known to be unavailable
	 */
	public String getRemoteHelpUnavailablePage(boolean isHelpView) {
		if ( BaseHelpSystem.getMode()!=BaseHelpSystem.MODE_INFOCENTER ) {
			String suffix = isHelpView ? REMOTE_STATUS_HELP_VIEW_HREF : REMOTE_STATUS_HREF;
			return "/org.eclipse.help.webapp/" + suffix; //$NON-NLS-1$
		}
		return null;
	}

	public Placeholder[] getUnresolvedPlaceholders() {
		List<Placeholder> unresolved;
		unresolved = new ArrayList<>();
		for (Placeholder ph : placeholders) {
			String bundle = ph.bundle;
			if (bundle != null && !bundlesToIgnore.contains(bundle) ) {
				if (Platform.getBundle(bundle) == null ) {
					unresolved.add(ph);
				}
			}
		}
		return unresolved.toArray(new Placeholder[unresolved.size()]);
	}

	// Modifies the preferences to ignore any bundles that are currently unresolved placeholders
	public void ignoreAllMissingPlaceholders() {
		Placeholder[] unresolved = getUnresolvedPlaceholders();
		String ignoredBundles = Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, IGNORE_MISSING_PLACEHOLDER_PREFERENCE, "", null); //$NON-NLS-1$
		for (Placeholder element : unresolved) {
			String bundle = element.bundle;
			bundlesToIgnore.add(bundle);
			if (ignoredBundles.length() > 0) {
				ignoredBundles = ignoredBundles + ',';
			}
			ignoredBundles = ignoredBundles + bundle;

		}
		IScopeContext instanceScope = InstanceScope.INSTANCE;
		IEclipsePreferences prefs = instanceScope.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.put(IGNORE_MISSING_PLACEHOLDER_PREFERENCE, ignoredBundles);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			Platform.getLog(getClass()).error("Cannot save preferences", e); //$NON-NLS-1$
		}
	}

}
