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

import java.net.*;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.internal.configurator.branding.*;
import org.osgi.framework.*;
import org.w3c.dom.*;


/**
 * 
 * Feature information
 */
public class FeatureEntry
		implements
			IPlatformConfiguration.IFeatureEntry,
			IConfigurationConstants,
			IBundleGroup,
			IBundleGroupConstants,
			IProductConstants {
	private final String id;
	private final String version;
	private final String pluginVersion;
	private final String application;
	private final URL[] root;
	private final boolean primary;
	private final String pluginIdentifier;
	private String url;
	private String description;
	private String licenseURL;
	private ArrayList<PluginEntry> plugins;
	private AboutInfo branding;
	private SiteEntry site;
	private ResourceBundle resourceBundle;
	private boolean fullyParsed;

	public FeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
		if (id == null)
			throw new IllegalArgumentException();
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
		if (plugins == null)
			plugins = new ArrayList<>();
		plugins.add(plugin);
	}
	
	public PluginEntry[] getPluginEntries() {
		if (plugins == null)
			fullParse();
		return plugins.toArray(new PluginEntry[plugins.size()]);
	}
	
	/**
	 * Sets the url string (relative to the site url)
	 * @param url
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
		if (id != null)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_ID, id); 
		if (primary)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PRIMARY, "true"); //$NON-NLS-1$
		if (version != null)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_VERSION, version); 
		if (pluginVersion != null && !pluginVersion.equals(version) && pluginVersion.length() > 0)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PLUGIN_VERSION, pluginVersion); 
		if (pluginIdentifier != null && !pluginIdentifier.equals(id) && pluginIdentifier.length() > 0)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER, pluginIdentifier);
		if (application != null)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_APPLICATION, application);
		if (url != null)
			// make externalized URL install relative
			featureElement.setAttribute(CFG_URL, Utils.makeRelative(installURL, url));
		
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
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public Bundle[] getBundles() {
		if (plugins == null)
			fullParse();
		
		ArrayList<Bundle> bundles = new ArrayList<>(plugins.size());
		for (PluginEntry plugin : plugins) {
			// get the highest version for the plugin
			Bundle bundle = Utils.getBundle(plugin.getPluginIdentifier());
			if (bundle != null)
				bundles.add(bundle);
		}
		return bundles.toArray(new Bundle[bundles.size()]);
	}

	@Override
	public String getDescription() {
		if (description == null)
			fullParse();
		return description;
	}

	@Override
	public String getIdentifier() {
		return id;
	}

	@Override
	public String getName() {
		if (branding == null)
			branding = AboutInfo.readFeatureInfo(id, version, getFeaturePluginIdentifier());
		return branding.getProductName();
	}

	@Override
	public String getProperty(String key) {
		if (key == null)
			return null;
		
		if (branding == null)
			branding = AboutInfo.readFeatureInfo(id, version, getFeaturePluginIdentifier());
		
		// IBundleGroupConstants
		if (key.equals(FEATURE_IMAGE))
			return branding.getFeatureImageURL() == null ? null : branding.getFeatureImageURL().toExternalForm();
		else if (key.equals(TIPS_AND_TRICKS_HREF)) 
			return branding.getTipsAndTricksHref();
		else if (key.equals(IBundleGroupConstants.WELCOME_PAGE)) // same value is used by product and bundle group
			return branding.getWelcomePageURL() == null ? null : branding.getWelcomePageURL().toExternalForm();
		else if (key.equals(WELCOME_PERSPECTIVE))
			return branding.getWelcomePerspectiveId();
		else if (key.equals(BRANDING_BUNDLE_ID))
			return pluginIdentifier;
		else if (key.equals(BRANDING_BUNDLE_VERSION))
			return pluginVersion;
		// IProductConstants
		else if (key.equals(APP_NAME)) 
			return branding.getAppName();
		else if (key.equals(ABOUT_TEXT))
			return branding.getAboutText();
		else if (key.equals(ABOUT_IMAGE))
			return branding.getAboutImageURL() == null ? null : branding.getAboutImageURL().toExternalForm();
		else if (key.equals(WINDOW_IMAGE))
			return branding.getWindowImageURL()== null ? null : branding.getWindowImageURL().toExternalForm();
		else if (key.equals(WINDOW_IMAGES)) {
			URL[] urls = branding.getWindowImagesURLs();
			if (urls == null)
				return null;
			StringBuilder windowImagesURLs = new StringBuilder();
			for (int i=0; i<urls.length; i++){
				windowImagesURLs.append(urls[i].toExternalForm());
				if (i != urls.length-1)
					windowImagesURLs.append(',');
			}
			return windowImagesURLs.toString();
		} else if (key.equals(LICENSE_HREF))
			return getLicenseURL();
		
		return null;
	}
	
	@Override
	public String getProviderName() {
		if (branding == null)
			branding = AboutInfo.readFeatureInfo(id, version, getFeaturePluginIdentifier());
		return branding.getProviderName();
	}
	
	@Override
	public String getVersion() {
		return version;
	}

	public String getApplication() {
		return application;
	}

	public String getId() {
		return id;
	}
	
	public ResourceBundle getResourceBundle(){
		if (resourceBundle != null)
			return resourceBundle;
		
		// Determine the properties file location
		if (site == null)
			return null;
		
		ResourceBundle bundle = null;
		try {
			URL propertiesURL = new URL(site.getResolvedURL(), getURL());
			ClassLoader l = new URLClassLoader(new URL[] { propertiesURL }, null);
			bundle = ResourceBundle.getBundle(IConfigurationConstants.CFG_FEATURE_ENTRY, Utils.getDefaultLocale(), l);
		} catch (MissingResourceException e) {
			Utils.log(e.getLocalizedMessage()); 
		} catch (MalformedURLException e) {
			Utils.log(e.getLocalizedMessage()); 
		}
		return bundle;
	}
	
	public void setLicenseURL(String licenseURL) {
		this.licenseURL = licenseURL;
	}
	
	public String getLicenseURL() {
		if (licenseURL == null)
			fullParse();
		if (licenseURL == null)
			return null;
		
		String resolvedURL = Utils.getResourceString(getResourceBundle(), licenseURL);
		if (resolvedURL.startsWith("http://") || resolvedURL.startsWith("https://")) //$NON-NLS-1$ //$NON-NLS-2$
			return resolvedURL;
		try {
			return new URL(getSite().getResolvedURL(), getURL() + resolvedURL).toExternalForm();
		} catch (MalformedURLException e) {
			return resolvedURL;
		}
	}
	
	private void fullParse() {
		if (fullyParsed)
			return;
		fullyParsed = true;
		if (plugins == null) 
			plugins = new ArrayList<>();
		FullFeatureParser parser = new FullFeatureParser(this);
		parser.parse();
	}
	
	public Bundle getDefiningBundle() {
		return Utils.getBundle(getFeaturePluginIdentifier());
	}
	
	public boolean hasBranding() {
		String bundleId = getFeaturePluginIdentifier();
		return bundleId != null && Utils.getBundle(bundleId) != null;
	}
}
