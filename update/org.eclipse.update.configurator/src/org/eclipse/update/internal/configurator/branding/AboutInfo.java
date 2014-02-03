/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator.branding;

import java.net.*;
import java.util.Hashtable;

import org.eclipse.core.runtime.*;


/**
 * The information within this object is obtained from the about INI file.
 * This file resides within an install configurations directory and must be a 
 * standard java property file.  
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 */
public final class AboutInfo {
	private final static String INI_FILENAME = "about.ini"; //$NON-NLS-1$
	private final static String PROPERTIES_FILENAME = "about.properties"; //$NON-NLS-1$
	private final static String MAPPINGS_FILENAME = "about.mappings"; //$NON-NLS-1$

	private String featureId;
	private String versionId = ""; //$NON-NLS-1$
	private String featurePluginLabel;
	private String providerName;
	private String appName;
	private URL windowImageURL;
	private URL[] windowImagesURLs;
	private URL aboutImageURL;
	private URL featureImageURL;
	private URL welcomePageURL;
	private String aboutText;
	private String welcomePerspective;
	private String tipsAndTricksHref;


	/*
	 * Create a new about info for a feature with the given id.
	 */
	/* package */ AboutInfo(String featureId) {
		super();
		this.featureId = featureId;
	}

	/**
	 * Returns the configuration information for the feature with the 
	 * given id.
	 * 
	 * @param featureId the feature id
	 * @param versionId the version id (of the feature)
	 * @param pluginId the plug-in id
	 * @return the configuration information for the feature
	 */
	public static AboutInfo readFeatureInfo(String featureId, String versionId, String pluginId) {
//		Assert.isNotNull(featureId);
//		Assert.isNotNull(versionId);
//		Assert.isNotNull(pluginId);
		IniFileReader reader = new IniFileReader(featureId, pluginId, INI_FILENAME, PROPERTIES_FILENAME, MAPPINGS_FILENAME);
		reader.load();
// bug 78031
//		if (!status.isOK()) {
//			//return null;
//			return new AboutInfo(featureId); // dummy about info
//		}
		
		AboutInfo info = new AboutInfo(featureId);
		Hashtable runtimeMappings  = new Hashtable();
		runtimeMappings.put("{featureVersion}", versionId); //$NON-NLS-1$
		info.versionId = versionId;
		info.featurePluginLabel = reader.getFeaturePluginLabel();
		info.providerName = reader.getProviderName();
		info.appName = reader.getString("appName", true, runtimeMappings); //$NON-NLS-1$
		info.aboutText = reader.getString("aboutText", true, runtimeMappings); //$NON-NLS-1$
		info.windowImageURL = reader.getURL("windowImage"); //$NON-NLS-1$
		// look for the newer array, but if its not there then use the older,
		// single image definition
		info.windowImagesURLs = reader.getURLs("windowImages"); //$NON-NLS-1$
		info.aboutImageURL = reader.getURL("aboutImage"); //$NON-NLS-1$
		info.featureImageURL = reader.getURL("featureImage"); //$NON-NLS-1$
		info.welcomePageURL = reader.getURL("welcomePage"); //$NON-NLS-1$
		info.welcomePerspective = reader.getString("welcomePerspective", false, runtimeMappings); //$NON-NLS-1$
		info.tipsAndTricksHref = reader.getString("tipsAndTricksHref", false, runtimeMappings); //$NON-NLS-1$
		return info;
	}
	
	/**
	 * Returns the URL for an image which can be shown in an "about" dialog 
	 * for this product. Products designed to run "headless" typically would not 
	 * have such an image.
	 * 
	 * @return the URL for an about image, or <code>null</code> if none
	 */
	public URL getAboutImageURL() {
		return aboutImageURL;
	}

	/**
	 * Returns the URL for an image which can be shown in an "about features" 
	 * dialog. Products designed to run "headless" typically would not have such an image.
	 * 
	 * @return the URL for a feature image, or <code>null</code> if none
	 */
	public URL getFeatureImageURL() {
		return featureImageURL;
	}

	/**
	 * Returns the simple name of the feature image file.
	 * 
	 * @return the simple name of the feature image file,
	 * or <code>null</code> if none
	 */
	public String getFeatureImageName() {
		if (featureImageURL != null) {
			IPath path = new Path(featureImageURL.getPath());
			return path.lastSegment();
		} 
		return null;
	}

		
	/**
	 * Returns a label for the feature plugn, or <code>null</code>.
	 */
	public String getFeatureLabel() {
		return featurePluginLabel;
	}

	/**
	 * Returns the id for this feature.
	 * 
	 * @return the feature id
	 */
	public String getFeatureId() {
		return featureId;
	}
	
	/**
	 * Returns the text to show in an "about" dialog for this product.
	 * Products designed to run "headless" typically would not have such text.
	 * 
	 * @return the about text, or <code>null</code> if none
	 */
	public String getAboutText() {
		return aboutText;
	}

	/**
	 * Returns the application name or <code>null</code>.
	 * Note this is never shown to the user.
	 * It is used to initialize the SWT Display.
	 * <p>
	 * On Motif, for example, this can be used
	 * to set the name used for resource lookup.
	 * </p>
	 *
	 * @return the application name, or <code>null</code>
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * Returns the product name or <code>null</code>.
	 * This is shown in the window title and the About action.
	 *
	 * @return the product name, or <code>null</code>
	 */
	public String getProductName() {
		return featurePluginLabel;
	}

	/**
	 * Returns the provider name or <code>null</code>.
	 *
	 * @return the provider name, or <code>null</code>
	 */
	public String getProviderName() {
		return providerName;
	}

	/**
	 * Returns the feature version id.
	 *
	 * @return the version id of the feature
	 */
	public String getVersionId() {
		return versionId;
	}

	/**
	 * Returns a <code>URL</code> for the welcome page.
	 * Products designed to run "headless" typically would not have such an page.
	 * 
	 * @return the welcome page, or <code>null</code> if none
	 */
	public URL getWelcomePageURL() {
		return welcomePageURL;
	}

	/**
	 * Returns the ID of a perspective in which to show the welcome page.
	 * May be <code>null</code>.
	 * 
	 * @return the welcome page perspective id, or <code>null</code> if none
	 */
	public String getWelcomePerspectiveId() {
		return welcomePerspective;
	}

	/**
	 * Returns a <code>String</code> for the tips and trick href.
	 * 
	 * @return the tips and tricks href, or <code>null</code> if none
	 */
	public String getTipsAndTricksHref() {
		return tipsAndTricksHref;
	}

	/**
	 * Returns the image url for the window image to use for this product.
	 * Products designed to run "headless" typically would not have such an image.
	 * 
	 * @return the image url for the window image, or <code>null</code> if none
	 */
	public URL getWindowImageURL() {
		return windowImageURL;
	}
	
	/**
	 * Return an array of image URLs for the window images to use for
	 * this product. The expectations is that the elements will be the same
	 * image rendered at different sizes. Products designed to run "headless"
	 * typically would not have such images.
	 * 
	 * @return an array of the image descriptors for the window images, or
	 *         <code>null</code> if none
	 * @since 3.0
	 */
	public URL[] getWindowImagesURLs() {
		return windowImagesURLs;
	}
}
