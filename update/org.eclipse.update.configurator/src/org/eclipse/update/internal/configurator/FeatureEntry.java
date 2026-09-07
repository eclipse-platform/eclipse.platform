/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.update.internal.configurator;

import java.net.URL;
import java.util.ArrayList;

import org.eclipse.update.configurator.IPlatformConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 *
 * Feature information
 */
public class FeatureEntry implements IPlatformConfiguration.IFeatureEntry, IConfigurationConstants {
	private final String id;
	private final String version;
	private final String pluginVersion;
	private final String application;
	private final URL[] root;
	private final boolean primary;
	private final String pluginIdentifier;
	private String url;
	private ArrayList<PluginEntry> plugins;
	private SiteEntry site;
	private boolean fullyParsed;

	public FeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
		if (id == null) {
			throw new IllegalArgumentException();
		}
		this.id = id;
		this.version = version;
		this.pluginVersion = pluginVersion;
		this.pluginIdentifier = pluginIdentifier;
		this.primary = primary;
		this.application = application;
		this.root = (root == null ? new URL[0] : root);
	}

	public FeatureEntry( String id, String version, String pluginVersion, boolean primary, String application, URL[] root) {
		this(id, version, id, pluginVersion, primary, application, root);
	}

	public void setSite(SiteEntry site) {
		this.site = site;
	}

	public SiteEntry getSite() {
		return this.site;
	}

	public void addPlugin(PluginEntry plugin) {
		if (plugins == null) {
			plugins = new ArrayList<>();
		}
		plugins.add(plugin);
	}

	public PluginEntry[] getPluginEntries() {
		if (plugins == null) {
			fullParse();
		}
		return plugins.toArray(new PluginEntry[plugins.size()]);
	}

	/**
	 * Sets the url string (relative to the site url)
	 */
	public void setURL(String url) {
		this.url = url;
	}

	/**
	 * @return the feature url (relative to the site): features/org.eclipse.platform/
	 */
	public String getURL() {
//		if (url == null)
//			url = FEATURES + "/" + id + "_" + version + "/";
		return url;
	}

	@Override
	public String getFeatureIdentifier() {
		return id;
	}

	@Override
	public String getFeatureVersion() {
		return version;
	}

	@Override
	public String getFeaturePluginVersion() {
		return pluginVersion != null && pluginVersion.length() > 0 ? pluginVersion : null;
	}

	@Override
	public String getFeaturePluginIdentifier() {
		// if no plugin is specified, use the feature id
		return pluginIdentifier != null && pluginIdentifier.length() > 0 ? pluginIdentifier : id;
	}

	@Override
	public String getFeatureApplication() {
		return application;
	}

	@Override
	public URL[] getFeatureRootURLs() {
		return root;
	}

	@Override
	public boolean canBePrimary() {
		return primary;
	}

	public Element toXML(Document doc) {
		URL installURL = getSite().getConfig().getInstallURL();

		Element featureElement = doc.createElement(CFG_FEATURE_ENTRY);
		// write out feature entry settings
		if (id != null) {
			featureElement.setAttribute(CFG_FEATURE_ENTRY_ID, id);
		}
		if (primary) {
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PRIMARY, "true"); //$NON-NLS-1$
		}
		if (version != null) {
			featureElement.setAttribute(CFG_FEATURE_ENTRY_VERSION, version);
		}
		if (pluginVersion != null && !pluginVersion.equals(version) && pluginVersion.length() > 0) {
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PLUGIN_VERSION, pluginVersion);
		}
		if (pluginIdentifier != null && !pluginIdentifier.equals(id) && pluginIdentifier.length() > 0) {
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER, pluginIdentifier);
		}
		if (application != null) {
			featureElement.setAttribute(CFG_FEATURE_ENTRY_APPLICATION, application);
		}
		if (url != null) {
			// make externalized URL install relative
			featureElement.setAttribute(CFG_URL, Utils.makeRelative(installURL, url));
		}

		for (URL url : getFeatureRootURLs()) {
			// make externalized URL install relative
			String root = Utils.makeRelative(installURL, url).toExternalForm();
			if (root.trim().length() > 0){
				Element rootElement = doc.createElement(CFG_FEATURE_ENTRY_ROOT);
				rootElement.appendChild(doc.createTextNode(root));
				featureElement.appendChild(rootElement);
			}
		}

		return featureElement;
	}

	public String getApplication() {
		return application;
	}

	public String getId() {
		return id;
	}

	private void fullParse() {
		if (fullyParsed) {
			return;
		}
		fullyParsed = true;
		if (plugins == null) {
			plugins = new ArrayList<>();
		}
		FullFeatureParser parser = new FullFeatureParser(this);
		parser.parse();
	}
}
