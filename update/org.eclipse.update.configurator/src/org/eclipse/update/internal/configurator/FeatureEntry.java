/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.net.*;
import java.util.*;

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
			IProduct,
			IProductConstants {
	private String id;
	private String version;
	private String pluginVersion;
	private String application;
	private URL[] root;
	private boolean primary;
	private String pluginIdentifier;
	private String url;
	private String provider;
	private ArrayList plugins;
	private AboutInfo branding;

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
	
	/*
	 * @see IFeatureEntry#getFeatureIdentifier()
	 */
	public String getFeatureIdentifier() {
		return id;
	}

	/*
	 * @see IFeatureEntry#getFeatureVersion()
	 */
	public String getFeatureVersion() {
		return version;
	}

	/*
	 * @see IFeatureEntry#getFeaturePluginVersion()
	 */
	public String getFeaturePluginVersion() {
		return pluginVersion != null && pluginVersion.length() > 0 ? pluginVersion : null;
	}

	/*
	 * @see IFeatureEntry#getFeaturePluginIdentifier()
	 */
	public String getFeaturePluginIdentifier() {
		// if no plugin is specified, use the feature id
		return pluginIdentifier != null && pluginIdentifier.length() > 0 ? pluginIdentifier : id;
	}
	
	/*
	 * @see IFeatureEntry#getFeatureApplication()
	 */
	public String getFeatureApplication() {
		return application;
	}

	/*
	 * @see IFeatureEntry#getFeatureRootURLs()
	 */
	public URL[] getFeatureRootURLs() {
		return root;
	}

	/*
	 * @see IFeatureEntry#canBePrimary()
	 */
	public boolean canBePrimary() {
		return primary;
	}

	public Element toXML(Document doc) {
	
		Element featureElement = doc.createElement(CFG_FEATURE_ENTRY);		
		// write out feature entry settings
		if (id != null)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_ID, id); 
		if (primary)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PRIMARY, "true");
		if (version != null)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_VERSION, version); 
		if (pluginVersion != null && !pluginVersion.equals(version) && pluginVersion.length() > 0)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PLUGIN_VERSION, pluginVersion); 
		if (pluginIdentifier != null && pluginIdentifier.equals(id) && pluginIdentifier.length() > 0)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER, pluginIdentifier);
		if (application != null)
			featureElement.setAttribute(CFG_FEATURE_ENTRY_APPLICATION, application);
		if (url != null)
			featureElement.setAttribute(CFG_URL, url);
		
		URL[] roots = getFeatureRootURLs();
		for (int i=0; i<roots.length; i++) {
			String root = roots[i].toExternalForm();
			if (root != null && root.trim().length() > 0){
				Element rootElement = doc.createElement(CFG_FEATURE_ENTRY_ROOT);
				rootElement.appendChild(doc.createTextNode(root));
				featureElement.appendChild(rootElement);
			}
		}
		
		return featureElement;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroup#getBundles()
	 */
	public Bundle[] getBundles() {
		if (plugins == null) {
			plugins = new ArrayList();
		}
		
		return new Bundle[0];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroup#getDescription()
	 */
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroup#getIdentifier()
	 */
	public String getIdentifier() {
		return id;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroup#getName()
	 */
	public String getName() {
		if (branding == null)
			branding = AboutInfo.readFeatureInfo(id, version, getFeaturePluginIdentifier());
		return branding.getProductName();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroup#getProperty(java.lang.String)
	 */
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
		else if (key.equals(WELCOME_PAGE)) 
			return branding.getWelcomePageURL() == null ? null : branding.getWelcomePageURL().toExternalForm();
		else if (key.equals(WELCOME_PERSPECTIVE))
			return branding.getWelcomePerspectiveId();
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
			StringBuffer windowImagesURLs = new StringBuffer();
			for (int i=0; i<urls.length; i++){
				windowImagesURLs.append(urls[i].toExternalForm());
				if (i != urls.length-1)
					windowImagesURLs.append(',');
			}
			return windowImagesURLs.toString();
		}
		
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroup#getProviderName()
	 */
	public String getProviderName() {
		if (branding == null)
			branding = AboutInfo.readFeatureInfo(id, version, getFeaturePluginIdentifier());
		return branding.getProviderName();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroup#getVersion()
	 */
	public String getVersion() {
		return version;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getApplication()
	 */
	public String getApplication() {
		return application;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getId()
	 */
	public String getId() {
		return id;
	}
}