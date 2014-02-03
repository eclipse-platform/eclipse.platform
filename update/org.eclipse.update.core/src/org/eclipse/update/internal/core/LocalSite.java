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
package org.eclipse.update.internal.core;

import org.eclipse.core.runtime.ListenerList;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.IActivity;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.configuration.ILocalSiteChangedListener;
import org.eclipse.update.configuration.IProblemHandler;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.model.InstallConfigurationModel;
import org.eclipse.update.internal.model.SiteLocalModel;
import org.eclipse.update.internal.model.SiteLocalParser;

/**
 * This class manages the configurations.
 */

public class LocalSite extends SiteLocalModel implements ILocalSite{

	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);
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
		return internalGetLocalSite();
	}

	/*
	 *Internal call is reconciliation needs to be optimistic
	 */
	public static ILocalSite internalGetLocalSite() throws CoreException {
	
		LocalSite localSite = new LocalSite();
	
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
			throw Utilities.newCoreException(NLS.bind(Messages.SiteLocal_UnableToCreateURLFor, (new String[] { localSite.getLocationURLString() + " & " + CONFIG_FILE })), exception); //$NON-NLS-1$
		}
	
		return localSite;
	}

	/**
	 * Create the localSite object
	 */
	private static boolean parseLocalSiteFile(IPlatformConfiguration platformConfig, LocalSite localSite ) throws CoreException, MalformedURLException {

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
	protected LocalSite() {
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
					UpdateCore.debug("Removed configuration :" + removedConfig.getLabel()); //$NON-NLS-1$
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
		listeners.add(listener);
	}

	/*
	 * @see ILocalSite#removeLocalSiteChangedListener(ILocalSiteChangedListener)
	 */
	public void removeLocalSiteChangedListener(ILocalSiteChangedListener listener) {
		listeners.add(listener);
	}

	/**
	 * Saves the site into the config file.
	 * @return true if changes restart is needed
	 */
	public boolean save() throws CoreException {

		// Save the current configuration as
		// the other are already saved
		// and set runtim info for next startup
		return ((InstallConfiguration) getCurrentConfiguration()).save();
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
			return new InstallConfiguration(getCurrentConfiguration());
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(Messages.SiteLocal_cloneConfig, e); 
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
