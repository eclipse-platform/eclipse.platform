/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.internal.model.*;

/**
 * This class manages the configurations.
 */

public class SiteLocal extends SiteLocalModel implements ILocalSite{

	private ListenersList listeners = new ListenersList();
	private SiteStatusAnalyzer siteStatusAnalyzer;
	private boolean isTransient = false;

	/*
	 * Have new features been found during reconciliation
	 */
	public static boolean newFeaturesFound = false;

	/*
	 * initialize the configurations from the persistent model.
	 * Set the reconciliation as non optimistic
	 */
	public static ILocalSite getLocalSite() throws CoreException {
		return internalGetLocalSite(false);
	}

	/*
	 *Internal call is reconciliation needs to be optimistic
	 */
	public static ILocalSite internalGetLocalSite(boolean isOptimistic) throws CoreException {
	
		SiteLocal localSite = new SiteLocal();
	
		// obtain platform configuration
		IPlatformConfiguration currentPlatformConfiguration = ConfiguratorUtils.getCurrentPlatformConfiguration();
		localSite.isTransient(currentPlatformConfiguration.isTransient());
	
		try {
			URL configXML = currentPlatformConfiguration.getConfigurationLocation();
			localSite.setLocationURLString(configXML.toExternalForm());
			localSite.resolve(configXML, null);
	
			// Attempt to read previous state
			parseLocalSiteFile(currentPlatformConfiguration, localSite);

		} catch (MalformedURLException exception) {
			throw Utilities.newCoreException(Policy.bind("SiteLocal.UnableToCreateURLFor", localSite.getLocationURLString() + " & " + CONFIG_FILE), exception); //$NON-NLS-1$ //$NON-NLS-2$
		}
	
		return localSite;
	}

	/**
	 * Create the localSite object
	 */
	private static boolean parseLocalSiteFile(IPlatformConfiguration platformConfig, SiteLocal localSite ) throws CoreException, MalformedURLException {

		//attempt to parse the LocalSite.xml	
//		URL resolvedURL = URLEncoder.encode(configXML);
		try {
//			InputStream in = UpdateCore.getPlugin().get(resolvedURL).getInputStream();
			new SiteLocalParser(platformConfig, localSite);
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	/**
	 * 
	 */
	private SiteLocal() {
	}

	/**
	 * adds a new configuration to the LocalSite
	 *  the newly added configuration is teh current one
	 */
	public void addConfiguration(IInstallConfiguration config) {
		if (config != null) {
			addConfigurationModel((InstallConfigurationModel) config);

			trimHistoryToCapacity();

			// set configuration as current		
			if (getCurrentConfigurationModel() != null)
				getCurrentConfigurationModel().setCurrent(false);
			if (config instanceof InstallConfiguration)
				 ((InstallConfiguration) config).setCurrent(true);

			setCurrentConfigurationModel((InstallConfigurationModel) config);
			((InstallConfigurationModel) config).markReadOnly();

			// notify listeners
			Object[] siteLocalListeners = listeners.getListeners();
			for (int i = 0; i < siteLocalListeners.length; i++) {
				((ILocalSiteChangedListener) siteLocalListeners[i]).currentInstallConfigurationChanged(config);
			}
		}

	}

	/*
	 * 
	 */
	private void trimHistoryToCapacity() {
		// check if we have to remove a configuration
		// the first added is #0
		while (getConfigurationHistory().length > getMaximumHistoryCount() &&
				getConfigurationHistory().length > 1) {
			// do not remove the first element in history, this is the original config
			InstallConfigurationModel removedConfig = getConfigurationHistoryModel()[1];
			if (removeConfigurationModel(removedConfig)) {

				// DEBUG:
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION) {
					UpdateCore.debug("Removed configuration :" + removedConfig.getLabel());
					//$NON-NLS-1$
				}

				// notify listeners
				Object[] siteLocalListeners = listeners.getListeners();
				for (int i = 0; i < siteLocalListeners.length; i++) {
					((ILocalSiteChangedListener) siteLocalListeners[i]).installConfigurationRemoved((IInstallConfiguration) removedConfig);
				}

				//remove files
				URL url = removedConfig.getURL();
				UpdateManagerUtils.removeFromFileSystem(new File(url.getFile()));
			}
		}
	}
	/*
	 * @see ILocalSite#addLocalSiteChangedListener(ILocalSiteChangedListener)
	 */
	public void addLocalSiteChangedListener(ILocalSiteChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/*
	 * @see ILocalSite#removeLocalSiteChangedListener(ILocalSiteChangedListener)
	 */
	public void removeLocalSiteChangedListener(ILocalSiteChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Saves the site into the config file.
	 * @return true if changes restart is needed
	 */
	public boolean save() throws CoreException {

		// Save the current configuration as
		// the other are already saved
		// and set runtim info for next startup
		return ((InstallConfiguration) getCurrentConfiguration()).save(isTransient());
	}
	
//	/**
//	 * Method createNewInstallConfiguration.
//	 * @return IInstallConfiguration
//	 */
//	private IInstallConfiguration createNewInstallConfiguration() throws CoreException {
//		InstallConfiguration newInstallConfig = createConfigurationSite(null);
//		newInstallConfig.setTimeline(newInstallConfig.getCreationDate().getTime());
//		return newInstallConfig;
//	}


	/**
	 * @since 2.0
	 * @deprecated This method should not be used. The current install configuration is to be used.
	 */
	public IInstallConfiguration cloneCurrentConfiguration() throws CoreException {
		try {
			// This method should be deprecated
//		return getCurrentConfiguration();
			return new InstallConfiguration(getCurrentConfiguration());
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException("Clonning current configuration", e);
		}
	}

	/**
	 * @since 2.0
	 */
	public void revertTo(IInstallConfiguration configuration, IProgressMonitor monitor, IProblemHandler handler) throws CoreException {

		// create the activity 
		//Start UOW ?
		ConfigurationActivity activity = new ConfigurationActivity(IActivity.ACTION_REVERT);
		activity.setLabel(configuration.getLabel());
		activity.setDate(new Date());
		IInstallConfiguration newConfiguration = null;

		try {
			// create a configuration
			newConfiguration = cloneCurrentConfiguration();
			newConfiguration.setLabel(configuration.getLabel());

			// add to the stack which will set up as current
			addConfiguration(newConfiguration);

			// process delta
			// the Configured featuresConfigured are the same as the old configuration
			// the unconfigured featuresConfigured are the rest...
			 ((InstallConfiguration) newConfiguration).revertTo(configuration, monitor, handler);

			// everything done ok
			activity.setStatus(IActivity.STATUS_OK);
		} catch (CoreException e) {
			// error
			activity.setStatus(IActivity.STATUS_NOK);
			throw e;
		} catch (InterruptedException e) {
			//user decided not to revert, do nothing
			// because we didn't add the configuration to the history
		} finally {
			if (newConfiguration != null)
				 ((InstallConfiguration) newConfiguration).addActivity(activity);
		}

	}

	/**
	 * @since 2.0
	 * @deprecated
	 */
	public IInstallConfiguration addToPreservedConfigurations(IInstallConfiguration configuration) throws CoreException {
		return null;
//		InstallConfiguration newConfiguration = null;
//		if (configuration != null) {
//
//			// create new configuration based on the one to preserve
//			String newFileName = UpdateManagerUtils.getLocalRandomIdentifier(CONFIG_FILE, new Date());
//			try {
//				URL newFile = UpdateManagerUtils.getURL(getLocationURL(), newFileName, null);
//				// pass the date onto teh name
//				Date currentDate = configuration.getCreationDate();
//				String name = configuration.getLabel();
//				newConfiguration = new InstallConfiguration(configuration, newFile, name);
//				// set the same date in the installConfig
//				newConfiguration.setCreationDate(currentDate);
//				newConfiguration.setTimeline(configuration.getTimeline());
//			} catch (MalformedURLException e) {
//				throw Utilities.newCoreException(Policy.bind("SiteLocal.UnableToCreateURLFor") + newFileName, e);
//				//$NON-NLS-1$
//			}
//
//			// activity
//			ConfigurationActivity activity = new ConfigurationActivity(IActivity.ACTION_ADD_PRESERVED);
//			activity.setLabel(configuration.getLabel());
//			activity.setDate(new Date());
//			activity.setStatus(IActivity.STATUS_OK);
//			((InstallConfiguration) newConfiguration).addActivity(activity);
////			((InstallConfiguration) newConfiguration).saveConfigurationFile(isTransient());
//
//			// add to the list			
//			addPreservedInstallConfigurationModel(newConfiguration);
//		}
//		return newConfiguration;
	}

	/*
	 * @see ILocalSite#getPreservedConfigurationFor(IInstallConfiguration)
	 */
	public IInstallConfiguration findPreservedConfigurationFor(IInstallConfiguration configuration) {

		// based on time stamp for now
		InstallConfigurationModel preservedConfig = null;
		if (configuration != null) {
			InstallConfigurationModel[] preservedConfigurations = getPreservedConfigurationsModel();
			if (preservedConfigurations != null) {
				for (int indexPreserved = 0; indexPreserved < preservedConfigurations.length; indexPreserved++) {
					if (configuration.getCreationDate().equals(preservedConfigurations[indexPreserved].getCreationDate())) {
						preservedConfig = preservedConfigurations[indexPreserved];
						break;
					}
				}
			}
		}

		return (IInstallConfiguration) preservedConfig;
	}

	/*
	 * @see ILocalSite#getCurrentConfiguration()
	 * LocalSiteModel#getCurrentConfigurationModel() may return null if
	 * we just parsed LocalSite.xml
	 */
	public IInstallConfiguration getCurrentConfiguration() {
		if (getCurrentConfigurationModel() == null) {
			int index = 0;
			if ((index = getConfigurationHistoryModel().length) == 0) {
				return null;
			} else {
				InstallConfigurationModel config = getConfigurationHistoryModel()[index - 1];
				config.setCurrent(true);
				setCurrentConfigurationModel(config);
			}
		}
		return (IInstallConfiguration) getCurrentConfigurationModel();
	}

	/*
	 * @see ILocalSite#getPreservedConfigurations()
	 */
	public IInstallConfiguration[] getPreservedConfigurations() {
		if (getPreservedConfigurationsModel().length == 0)
			return new IInstallConfiguration[0];
		return (IInstallConfiguration[]) getPreservedConfigurationsModel();
	}

	/*
	 * @see ILocalSite#removeFromPreservedConfigurations(IInstallConfiguration)
	 */
	public void removeFromPreservedConfigurations(IInstallConfiguration configuration) {
		if (removePreservedConfigurationModel((InstallConfigurationModel) configuration))
			 ((InstallConfiguration) configuration).remove();
	}

	/*
	 * @see ILocalSite#getConfigurationHistory()
	 */
	public IInstallConfiguration[] getConfigurationHistory() {
		if (getConfigurationHistoryModel().length == 0)
			return new IInstallConfiguration[0];
		return (IInstallConfiguration[]) getConfigurationHistoryModel();
	}


	/**
	 * Gets the isTransient.
	 * @return Returns a boolean
	 */
	public boolean isTransient() {
		return isTransient;
	}

	/**
	 * Sets the isTransient.
	 * @param isTransient The isTransient to set
	 */
	private void isTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	/**
	 * if we are unable to parse the SiteLocal.xml
	 * we will attempt to parse the file system
	 */
//	private static void recoverSiteLocal(URL url, SiteLocal site) throws CoreException, MalformedURLException {
//
//		if (url == null)
//			throw Utilities.newCoreException(Policy.bind("SiteLocal.SiteUrlIsNull"),
//			//$NON-NLS-1$
//			null);
//
//		// parse site information
//		site.setLabel(url.toExternalForm());
//
//		//stamp
//		long stamp = 0L;
//		site.setStamp(stamp);
//
//		// retrieve XML files
//		File localXml = new File(url.getFile());
//		if (localXml.exists()) {
//			try {
//				UpdateManagerUtils.removeFromFileSystem(localXml);
//				UpdateCore.warn("Removed bad LocalSite.xml file:" + localXml);
//			} catch (Exception e) {
//				UpdateCore.warn("Unable to remove bad LocalSite.xml file:" + localXml, e);
//			}
//		}
//
//		File dir = localXml.getParentFile();
//		File[] configFiles = dir.listFiles(new FilenameFilter() {
//			public boolean accept(File dir, String name) {
//				return (name.startsWith(DEFAULT_CONFIG_PREFIX) && name.endsWith("xml"));
//			}
//		});
//		if (configFiles == null)
//			configFiles = new File[0];
//
//		File[] preservedFiles = dir.listFiles(new FilenameFilter() {
//			public boolean accept(File dir, String name) {
//				return (name.startsWith(DEFAULT_PRESERVED_CONFIG_PREFIX) && name.endsWith("xml"));
//			}
//		});
//		if (preservedFiles == null)
//			preservedFiles = new File[0];
//
//		// history
//		int history = 0;
//		if (configFiles.length > 0) {
//			history = configFiles.length;
//		}
//
//		if (SiteLocalModel.DEFAULT_HISTORY > history)
//			history = SiteLocalModel.DEFAULT_HISTORY;
//		site.setMaximumHistoryCount(history);
//
//		// parse configuration information
//		List validConfig = new ArrayList();
//		for (int i = 0; i < configFiles.length; i++) {
//			URL configURL = configFiles[i].toURL();
//			InstallConfigurationModel config = new BaseSiteLocalFactory().createInstallConfigurationModel();
//			String relativeURL = UpdateManagerUtils.getURLAsString(url, configURL);
//			config.setLocationURLString(relativeURL);
//			config.resolve(configURL, url);
//			config.setLabel(Utilities.format(config.getCreationDate()));
//			validConfig.add(config);
//		}
//
//		// add the currentConfig last
//		// based on creation date
//		if (validConfig.size() > 0) {
//			Iterator iter = validConfig.iterator();
//			InstallConfigurationModel currentConfig = (InstallConfigurationModel) iter.next();
//			while (iter.hasNext()) {
//				InstallConfigurationModel element = (InstallConfigurationModel) iter.next();
//				Date currentConfigDate = currentConfig.getCreationDate();
//				Date elementDate = element.getCreationDate();
//				if (elementDate != null && elementDate.after(currentConfigDate)) {
//					site.addConfigurationModel(currentConfig);
//					currentConfig = element;
//				} else {
//					site.addConfigurationModel(element);
//				}
//			}
//			site.addConfigurationModel(currentConfig);
//		}
//
//		// parse preserved configuration information
//		for (int i = 0; i < preservedFiles.length; i++) {
//			URL configURL = preservedFiles[i].toURL();
//			InstallConfigurationModel config = new BaseSiteLocalFactory().createInstallConfigurationModel();
//			String relativeURL = UpdateManagerUtils.getURLAsString(url, configURL);
//			config.setLocationURLString(relativeURL);
//			config.resolve(configURL, url);
//			config.setLabel(Utilities.format(config.getCreationDate()));
//			site.addPreservedInstallConfigurationModel(config);
//		}
//	}

	/*
	 * 
	 */
	private SiteStatusAnalyzer getSiteStatusAnalyzer() {
		if (siteStatusAnalyzer == null)
			siteStatusAnalyzer = new SiteStatusAnalyzer(this);
		return siteStatusAnalyzer;
	}

	/*
	 *  check if the Plugins of the feature are on the plugin path
	 *  If all the plugins are on the plugin path, and the version match and there is no other version -> HAPPY
	 *  If all the plugins are on the plugin path, and the version match and there is other version -> AMBIGUOUS
	 *  If some of the plugins are on the plugin path, but not all -> UNHAPPY
	 * 	Check on all ConfiguredSites
	 */
	public IStatus getFeatureStatus(IFeature feature) throws CoreException {
		return getSiteStatusAnalyzer().getFeatureStatus(feature);
	}
	/**
	 * @see org.eclipse.update.internal.model.SiteLocalModel#setMaximumHistoryCount(int)
	 */
	public void setMaximumHistoryCount(int history) {
		super.setMaximumHistoryCount(history);
		trimHistoryToCapacity();
	}

}
