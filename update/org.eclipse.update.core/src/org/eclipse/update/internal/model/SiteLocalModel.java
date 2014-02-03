/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.model;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.MissingResourceException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.update.core.model.ModelObject;
import org.eclipse.update.internal.core.BaseSiteLocalFactory;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * This class manages the configurations.
 */

public class SiteLocalModel extends ModelObject {
	public static final String CONFIG_FILE = "platform.xml"; //$NON-NLS-1$
	private long stamp;
	private String label;
	private URL location;
	private String locationURLString;
	private int history = UpdateCore.DEFAULT_HISTORY;
	private List /* of InstallConfigurationModel */configurations;
	private List /* of InstallConfigurationModel */preservedConfigurations;
	private InstallConfigurationModel currentConfiguration;

	/**
	 * Constructor for LocalSite
	 */
	public SiteLocalModel(){
		super();
	}

	/**
	 * @since 2.0
	 */
	public InstallConfigurationModel getCurrentConfigurationModel() {
		return currentConfiguration;
	}

	/**
	 * @since 2.0
	 */
	public InstallConfigurationModel[] getConfigurationHistoryModel() {
		if (configurations==null)
			// initialize history
			processHistory();
		
		if (configurations == null || configurations.size() == 0)
			return new InstallConfigurationModel[0];
		else
			return (InstallConfigurationModel[])configurations.toArray(arrayTypeFor(configurations));
	}

	/**
	 * adds a new configuration to the LocalSite
	 *  the newly added configuration is teh current one
	 */
	public void addConfigurationModel(InstallConfigurationModel config) {
		if (config != null) {
			if (configurations == null)
				configurations = new ArrayList();
			if (!configurations.contains(config))
				configurations.add(config);
		}
	}

	/**
	 * adds a new configuration to the LocalSite
	 *  the newly added configuration is teh current one
	 */
	public boolean removeConfigurationModel(InstallConfigurationModel config) {
		if (config != null) {
			return configurations.remove(config);
		}
		return false;
	}
	/**
	 * Gets the location of the local site.
	 * @return Returns a URL
	 */
	public URL getLocationURL() {
		return location;
	}

	/**
	 * Gets the locationURLString.
	 * @return Returns a String
	 */
	public String getLocationURLString() {
		return locationURLString;
	}


	/**
	 * Sets the locationURLString.
	 * @param locationURLString The locationURLString to set
	 */
	public void setLocationURLString(String locationURLString) {
		assertIsWriteable();
		this.locationURLString = locationURLString;
		this.location=null;
	}


	/**
	 * @since 2.0
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label.
	 * @param label The label to set
	 */
	public void setLabel(String label) {
		assertIsWriteable();
		this.label = label;
	}

	
	/**
	 * @since 2.0
	 */
	public int getMaximumHistoryCount() {
		return history;
	}

	/**
	 * @since 2.0
	 */
	public void setMaximumHistoryCount(int history) {
		assertIsWriteable();
		this.history = history;
	}

	
	/**
	 * Adds a preserved configuration into teh collection
	 * do not save the configuration
	 * @since 2.0
	 */
	public void addPreservedInstallConfigurationModel(InstallConfigurationModel configuration) {
		if (preservedConfigurations == null)
			preservedConfigurations = new ArrayList();

		preservedConfigurations.add(configuration);
	}

	/**
	 * @since 2.0
	 */
	public boolean removePreservedConfigurationModel(InstallConfigurationModel configuration) {
		if (preservedConfigurations != null) {
			return preservedConfigurations.remove(configuration);
		}
		return false;
	}

	/**
	 * @since 2.0
	 */
	public InstallConfigurationModel[] getPreservedConfigurationsModel() {
		if (preservedConfigurations==null || preservedConfigurations.isEmpty())
			return new InstallConfigurationModel[0];
		return (InstallConfigurationModel[])preservedConfigurations.toArray(arrayTypeFor(preservedConfigurations));
	}


	/**
	 * Sets the currentConfiguration.
	 * @param currentConfiguration The currentConfiguration to set
	 */
	public void setCurrentConfigurationModel(InstallConfigurationModel currentConfiguration) {
		assertIsWriteable();
		this.currentConfiguration = currentConfiguration;
		
		//2.0.2 set the configuredSite of sites
		ConfiguredSiteModel[] confSites = currentConfiguration.getConfigurationSitesModel();
		for (int i = 0; i < confSites.length; i++) {
			confSites[i].getSiteModel().setConfiguredSiteModel(confSites[i]);
		}
	}

	/*
	 * @see ModelObject#resolve(URL)
	 */
	public void resolve(URL base,URL bundleURL) throws MalformedURLException {
		// local
		location = resolveURL(base,bundleURL,getLocationURLString());
		
		// delegate
		resolveListReference(getConfigurationHistoryModel(),base,bundleURL);
		resolveListReference(getPreservedConfigurationsModel(),base,bundleURL);
		resolveReference(getCurrentConfigurationModel(),base,bundleURL);
	}
	

	/**
	 * Gets the stamp.
	 * @return Returns a long
	 */
	public long getStamp() {
		return stamp;
	}

	/**
	 * Sets the stamp.
	 * @param stamp The stamp to set
	 */
	public void setStamp(long stamp) {
		this.stamp = stamp;
	}

	/**
	 * @see org.eclipse.update.core.model.ModelObject#getPropertyName()
	 */
	protected String getPropertyName() {
		return "platform"; //$NON-NLS-1$
	}

	/*
	 * reads the configuration/history directory
	 */
	private void processHistory() {
		try {
			URL historyURL = new URL(getLocationURL(), "history"); //$NON-NLS-1$
			historyURL = FileLocator.toFileURL(historyURL);
			File historyDir = new File(historyURL.getFile());
			if (historyDir.exists()) {
				File[] backedConfigs = historyDir.listFiles();
				BaseSiteLocalFactory factory = new BaseSiteLocalFactory();
				for (int i=0; i<backedConfigs.length; i++) {
					String name = backedConfigs[i].getName();
					if (name.endsWith(".xml")) //$NON-NLS-1$
						name = name.substring(0, name.length()-4);
					else 
						continue;
					Date date = new Date(Long.parseLong(name));
					InstallConfigurationModel config = factory.createInstallConfigurationModel();
					config.setLocationURLString(backedConfigs[i].getAbsolutePath().replace('\\', '/'));
					config.setLabel(date.toString());
					config.setCreationDate(date);
					config.resolve(backedConfigs[i].toURL(), getResourceBundleURL());
	
					// add the config
					addConfigurationModel(config);
				}
			}
		} catch (Exception e) {
			UpdateCore.warn("Error processing history: ", e); //$NON-NLS-1$
		}
	}

	/**
	 * return the appropriate resource bundle for this sitelocal
	 */
	URL getResourceBundleURL() throws CoreException {
		URL url = null;
		try {
			url = UpdateManagerUtils.asDirectoryURL(getLocationURL());
		} catch (MissingResourceException e) {
			UpdateCore.warn(e.getLocalizedMessage() + ":" + url.toExternalForm()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			UpdateCore.warn(e.getLocalizedMessage()); 
		}
		return url;
	}

}
