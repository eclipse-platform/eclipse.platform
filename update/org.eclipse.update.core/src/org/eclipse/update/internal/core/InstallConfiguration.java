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

import org.eclipse.update.internal.model.SiteLocalModel;

import org.eclipse.core.runtime.ListenerList;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.IActivity;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.IInstallConfigurationChangedListener;
import org.eclipse.update.configuration.IProblemHandler;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.FeatureContentProvider;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteContentProvider;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.core.Site;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.configurator.ConfigurationActivator;
import org.eclipse.update.internal.configurator.FeatureEntry;
import org.eclipse.update.internal.configurator.PlatformConfiguration;
import org.eclipse.update.internal.configurator.PluginEntry;
import org.eclipse.update.internal.configurator.SiteEntry;
import org.eclipse.update.internal.model.ConfigurationActivityModel;
import org.eclipse.update.internal.model.ConfiguredSiteModel;
import org.eclipse.update.internal.model.InstallConfigurationModel;
import org.osgi.framework.Bundle;

/**
 * Manages ConfiguredSites
 *
 */

public class InstallConfiguration extends InstallConfigurationModel implements IInstallConfiguration {
	private static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$
	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

	/*
	 * default constructor.
	 */
	public InstallConfiguration() {
	}

	/*
	 * Copy constructor 
	 * @since 3.0
	 */
	public InstallConfiguration(IInstallConfiguration config) throws MalformedURLException, CoreException {
		this(config, null, null);
	}
	
	/*
	 * copy constructor
	 */
	public InstallConfiguration(IInstallConfiguration config, URL newLocation, String label) throws CoreException, MalformedURLException {
		// set current date and timeline as caller can call setDate if the
		// date on the URL string has to be the same
		Date now = new Date();
		setCreationDate(now);
		setCurrent(false);
		
		if (newLocation == null) {
			String newFileName = UpdateManagerUtils.getLocalRandomIdentifier(SiteLocalModel.CONFIG_FILE, now);
			newLocation = UpdateManagerUtils.getURL(((LocalSite)SiteManager.getLocalSite()).getLocationURL(), newFileName, null);
		}
		setLocationURLString(newLocation.toExternalForm());
		
		if (label == null)
			label = Utilities.format(now);
		setLabel(label);

		// do not copy list of listeners nor activities
		// make a copy of the siteConfiguration object
		if (config != null) {
			IConfiguredSite[] csites = config.getConfiguredSites();
			if (csites != null) {
				for (int i = 0; i < csites.length; i++) {
					ConfiguredSite configSite = new ConfiguredSite(csites[i]);
					addConfigurationSiteModel(configSite);
				}
			}
		}

		resolve(newLocation, null);
		// no need to parse file, all data are initialized
		initialized = true;
	}
	

	/*
	 * Returns the list of configured sites or an empty array
	 */
	public IConfiguredSite[] getConfiguredSites() {
		ConfiguredSiteModel[] result = getConfigurationSitesModel();
		if (result.length == 0)
			return new IConfiguredSite[0];
		else
			return (IConfiguredSite[]) result;
	}

	/*
	 * Returns the default site policy
	 */
	private int getDefaultPolicy() {
		return PlatformConfiguration.getDefaultPolicy();
	}

	/**
	 * Creates a Configuration Site and a new Site
	 * The policy is from <code> org.eclipse.core.boot.IPlatformConfiguration</code>
	 */
	public IConfiguredSite createConfiguredSite(File file) throws CoreException {

		if (!file.getName().equals("eclipse")) { //$NON-NLS-1$
			file = new File(file, "eclipse"); //$NON-NLS-1$
			file.mkdirs();
		}
		
		if (isDuplicateSite(file))
			throw Utilities.newCoreException(NLS.bind(Messages.InstallConfiguration_location_exists, (new String[] { file.getPath() })),null);
		ISite site = InternalSiteManager.createSite(file);

		//create a config site around the site
		// even if the site == null
		BaseSiteLocalFactory factory = new BaseSiteLocalFactory();
		ConfiguredSite configSite = (ConfiguredSite) factory.createConfigurationSiteModel((SiteModel) site, getDefaultPolicy());

		if (configSite.isNativelyLinked()) {
			throw Utilities.newCoreException(Messages.InstallConfiguration_AlreadyNativelyLinked, null); 
		}
		
		if (configSite.isProductSite()) {
			throw Utilities.newCoreException(Messages.InstallConfiguration_AlreadyProductSite, null); 
		}
		
		if (site != null) {
			configSite.setPlatformURLString(site.getURL().toExternalForm());

			// obtain the list of plugins
			IPlatformConfiguration runtimeConfiguration = ConfiguratorUtils.getCurrentPlatformConfiguration();
			ConfigurationPolicy configurationPolicy = configSite.getConfigurationPolicy();
			String[] pluginPath = new String[0];
			if (configurationPolicy.getPolicy() == IPlatformConfiguration.ISitePolicy.USER_INCLUDE)
				pluginPath = configurationPolicy.getPluginPath(site);

			// create new Site in configuration
			IPlatformConfiguration.ISitePolicy sitePolicy = runtimeConfiguration.createSitePolicy(configurationPolicy.getPolicy(), pluginPath);

			// change runtime
			IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.createSiteEntry(site.getURL(), sitePolicy);
			runtimeConfiguration.configureSite(siteEntry);

			// if the privatre marker doesn't already exist create it
			configSite.createPrivateSiteMarker();
			((SiteModel)site).setConfiguredSiteModel(configSite);
		}
		// configure all features as enable
		configure(configSite);
		
		return configSite;
	}

	/**
	 * Creates a Configuration Site and a new Site as a private link site
	 * The policy is from <code> org.eclipse.core.boot.IPlatformConfiguration</code>
	 */
	public IConfiguredSite createLinkedConfiguredSite(File file) throws CoreException {
		return createConfiguredSite(file);
//		if (isDuplicateSite(file))
//			throw Utilities.newCoreException(UpdateUtils.getFormattedMessage("InstallConfiguration.location.exists", file.getPath()),null);
//		
//		ISite site = InternalSiteManager.createSite(file);
//
//		//create a config site around the site
//		// even if the site == null
//		BaseSiteLocalFactory factory = new BaseSiteLocalFactory();
//		ConfiguredSite configSite = (ConfiguredSite) factory.createConfigurationSiteModel((SiteModel) site, getDefaultPolicy());
//
//		if (!configSite.isExtensionSite()) {
//			String msg = Policy.bind("InstallConfiguration.NotAnExtensionSite");
//			throw Utilities.newCoreException(msg, null);
//		}
//
//		if (configSite.isNativelyLinked()) {
//			throw Utilities.newCoreException("InstallConfiguration.AlreadyNativelyLinked", null);
//		}
//
//		if (site != null) {
//			configSite.setPlatformURLString(site.getURL().toExternalForm());
//
//			// obtain the list of plugins
//			IPlatformConfiguration runtimeConfiguration = ConfiguratorUtils.getCurrentPlatformConfiguration();
//			ConfigurationPolicy configurationPolicy = configSite.getConfigurationPolicy();
//			String[] pluginPath = new String[0];
//			if (configurationPolicy.getPolicy() == IPlatformConfiguration.ISitePolicy.USER_INCLUDE)
//				pluginPath = configurationPolicy.getPluginPath(site);
//
//			// create new Site in configuration
//			IPlatformConfiguration.ISitePolicy sitePolicy = runtimeConfiguration.createSitePolicy(configurationPolicy.getPolicy(), pluginPath);
//
//			// change runtime
//			IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.createSiteEntry(site.getURL(), sitePolicy);
//			runtimeConfiguration.configureSite(siteEntry);
//
//		}
//
//		// configure all features as enable
//		configure(configSite);
//
//		return configSite;
	}

	/*
	 *Configure all features as Enable Check we only enable highest version
	 */
	private void configure(ConfiguredSite linkedSite) throws CoreException {
		ISite site = linkedSite.getSite();
		ISiteFeatureReference[] newFeaturesRef = site.getFeatureReferences();

		for (int i = 0; i < newFeaturesRef.length; i++) {
			// TRACE
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				String reconciliationType = "enable (optimistic)"; //$NON-NLS-1$
				UpdateCore.debug("New Linked Site:New Feature: " + newFeaturesRef[i].getURL() + " as " + reconciliationType); //$NON-NLS-1$ //$NON-NLS-2$
			}
			ConfigurationPolicy policy = linkedSite.getConfigurationPolicy();
			policy.configure(newFeaturesRef[i], true, false);
		}
		SiteReconciler.checkConfiguredFeatures(linkedSite);
	}

	/*
	 *
	 */
	public void addConfiguredSite(IConfiguredSite site) {
		if (!isCurrent() && isReadOnly())
			return;

		ConfigurationActivity activity = new ConfigurationActivity(IActivity.ACTION_SITE_INSTALL);
		activity.setLabel(site.getSite().getURL().toExternalForm());
		activity.setDate(new Date());
		ConfiguredSiteModel configSiteModel = (ConfiguredSiteModel) site;
		addConfigurationSiteModel(configSiteModel);
		configSiteModel.setInstallConfigurationModel(this);

		// notify listeners
		Object[] configurationListeners = listeners.getListeners();
		for (int i = 0; i < configurationListeners.length; i++) {
			IInstallConfigurationChangedListener listener = ((IInstallConfigurationChangedListener) configurationListeners[i]);
			listener.installSiteAdded(site);
		}

		// everything done ok
		activity.setStatus(IActivity.STATUS_OK);
		this.addActivity(activity);
	}

	/**
	 * Method addActivity.
	 * @param activity
	 */
	public void addActivity(IActivity activity) {
		addActivityModel((ConfigurationActivityModel)activity);
	}

	/*
	 *
	 */
	public void removeConfiguredSite(IConfiguredSite site) {
		if (!isCurrent() && isReadOnly())
			return;

		if (removeConfigurationSiteModel((ConfiguredSiteModel) site)) {
			// notify listeners
			Object[] configurationListeners = listeners.getListeners();
			for (int i = 0; i < configurationListeners.length; i++) {
				IInstallConfigurationChangedListener listener = ((IInstallConfigurationChangedListener) configurationListeners[i]);
				listener.installSiteRemoved(site);
			}
			
			//activity
			ConfigurationActivity activity = new ConfigurationActivity(IActivity.ACTION_SITE_REMOVE);
			activity.setLabel(site.getSite().getURL().toExternalForm());
			activity.setDate(new Date());
			activity.setStatus(IActivity.STATUS_OK);
			this.addActivity(activity);
		}
	}

	/*
	 * @see IInstallConfiguration#addInstallConfigurationChangedListener(IInstallConfigurationChangedListener)
	 */
	public void addInstallConfigurationChangedListener(IInstallConfigurationChangedListener listener) {
		listeners.add(listener);
	}

	/*
	 * @see IInstallConfiguration#removeInstallConfigurationChangedListener(IInstallConfigurationChangedListener)
	 */
	public void removeInstallConfigurationChangedListener(IInstallConfigurationChangedListener listener) {
		listeners.remove(listener);
	}


	/*
	 * Deletes the configuration from its URL/location
	 */
	public void remove() {
		// save the configuration
		if ("file".equalsIgnoreCase(getURL().getProtocol())) { //$NON-NLS-1$
			// the location points to a file
			File file = new File(getURL().getFile());
			UpdateManagerUtils.removeFromFileSystem(file);
		}
	}

	/**
	 * Saves the configuration into its URL/location
	 * and changes the platform configuration.
	 * The runtime site entries from platform.xml are updated as required
	 * (cannot recreate these because must preserve other runtime state) [18520]
	 * @return true if restart is needed
	 */
	public boolean save() throws CoreException {
		
		// Write info  into platform for the next runtime
		IPlatformConfiguration runtimeConfiguration = ConfiguratorUtils.getCurrentPlatformConfiguration();
		ConfiguredSiteModel[] configurationSites = getConfigurationSitesModel();

		// clean configured Entries from platform runtime
		IPlatformConfiguration.IFeatureEntry[] configuredFeatureEntries = runtimeConfiguration.getConfiguredFeatureEntries();
		for (int i = 0; i < configuredFeatureEntries.length; i++) {
			runtimeConfiguration.unconfigureFeatureEntry(configuredFeatureEntries[i]);
		}

		// [19958] remember sites currently configured by runtime (use
		// temp configuration object rather than a straight list to ensure
		// correct lookup)
		IPlatformConfiguration tempConfig = null;
		try {
			tempConfig = ConfiguratorUtils.getPlatformConfiguration(null);
			IPlatformConfiguration.ISiteEntry[] tmpSites = runtimeConfiguration.getConfiguredSites();
			for (int i = 0; i < tmpSites.length; i++) {
				tempConfig.configureSite(tmpSites[i]);
			}
		} catch (IOException e) {
			// assume no currently configured sites
		}

		//check sites
		checkSites(configurationSites, runtimeConfiguration);

		// Save the plugin path, primary feature and platform
		for (int i = 0; i < configurationSites.length; i++) {
			ConfiguredSite cSite = ((ConfiguredSite) configurationSites[i]);
			ConfigurationPolicy configurationPolicy = cSite.getConfigurationPolicy();

			savePluginPath(cSite, runtimeConfiguration, tempConfig);

			// IF primary feature URL or platform feature URL that we need to pass to runtime config
			// is part of platform:base:, write it as platform:base: URL
			IFeatureReference[] configuredFeaturesRef = configurationPolicy.getConfiguredFeatures();
			for (int j = 0; j < configuredFeaturesRef.length; j++) {
				IFeature feature = null;
				try {
					feature = configuredFeaturesRef[j].getFeature(null);
				} catch (CoreException e) {
					UpdateCore.warn(null, e);
				}
				saveFeatureEntry(cSite, feature, runtimeConfiguration);
			}
		}

		// [19958] remove any extra site entries from runtime configuration
		// (site entries that no longer exist in this configuration)
		if (tempConfig != null) {
			IPlatformConfiguration.ISiteEntry[] tmpSites = tempConfig.getConfiguredSites();
			for (int i = 0; i < tmpSites.length; i++) {
				runtimeConfiguration.unconfigureSite(tmpSites[i]);
			}
		}

		try {
			runtimeConfiguration.save();
			// log configuration and activities
			this.date = new Date(runtimeConfiguration.getChangeStamp());
			if ("file".equalsIgnoreCase(getURL().getProtocol())) //$NON-NLS-1$
				UpdateCore.log(this);
			resetActivities();
			return isRestartNeeded(runtimeConfiguration);
		} catch (IOException e) {
			CoreException exc = Utilities.newCoreException(NLS.bind(Messages.InstallConfiguration_UnableToSavePlatformConfiguration, (new String[] { runtimeConfiguration.getConfigurationLocation().toExternalForm() })), e);
			UpdateCore.warn("",exc); //$NON-NLS-1$
		}
		return true;
	}

	/*
	 * Write the plugin path for each site
	 * Do not check if the site already existed before [16696].
	 * Reuse any runtime site objects in platform.cfg (to preserve state) [18520].
	 */
	private void savePluginPath(ConfiguredSite cSite, IPlatformConfiguration runtimeConfiguration, IPlatformConfiguration tempConfig) // [19958]
	throws CoreException {

		ConfigurationPolicy configurationPolicy = cSite.getConfigurationPolicy();

		// create a ISitePolicy (policy, pluginPath)
		// for the site
		String[] pluginPath = configurationPolicy.getPluginPath(cSite.getSite());
		IPlatformConfiguration.ISitePolicy sitePolicy = runtimeConfiguration.createSitePolicy(configurationPolicy.getPolicy(), pluginPath);

		// get the URL of the site that matches the one platform.cfg gave us
		URL urlToCheck = null;
		try {
			urlToCheck = new URL(cSite.getPlatformURLString());
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(NLS.bind(Messages.InstallConfiguration_UnableToCreateURL, (new String[] { cSite.getPlatformURLString() })), e);
		} catch (ClassCastException e) {
			throw Utilities.newCoreException(Messages.InstallConfiguration_UnableToCast, e);	
		}

		// update runtime configuration [18520]
		// Note: we must not blindly replace the site entries because they
		//       contain additional runtime state that needs to be preserved.
		IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.findConfiguredSite(urlToCheck);
		if (siteEntry == null)
			siteEntry = runtimeConfiguration.createSiteEntry(urlToCheck, sitePolicy);
		else {
			siteEntry.setSitePolicy(sitePolicy);
			((SiteEntry)siteEntry).refreshPlugins();
			if (tempConfig != null) // [19958] remove reused entries from list
				tempConfig.unconfigureSite(siteEntry);
		}
		((SiteEntry)siteEntry).setEnabled(cSite.isEnabled());
		runtimeConfiguration.configureSite(siteEntry, true /*replace if exists*/);
	}

	/*
	 * Save the Feature entry
	 * The feature can be a primary feature and/or a platform feature
	 */
	private void saveFeatureEntry(ConfiguredSite cSite, IFeature feature, IPlatformConfiguration runtimeConfiguration) throws CoreException {
		if (feature == null)
			return;

		// get the URL of the plugin that corresponds to the feature (pluginid = featureid)
		String id = feature.getVersionedIdentifier().getIdentifier();
		IPluginEntry[] entries = feature.getPluginEntries();
		URL url = null;
		IPluginEntry featurePlugin = null;
		for (int k = 0; k < entries.length; k++) {
			if (id.equalsIgnoreCase(entries[k].getVersionedIdentifier().getIdentifier())) {
				url = getRuntimeConfigurationURL(entries[k], cSite);
				featurePlugin = entries[k];
				break;
			}
		}
		String pluginVersion = null;
		if (featurePlugin != null)
			pluginVersion = featurePlugin.getVersionedIdentifier().getVersion().toString();

        // Find the site
        SiteEntry siteEntry = null;
        try {
            URL featureUrl = new URL(cSite.getPlatformURLString());
            siteEntry = (SiteEntry)runtimeConfiguration.findConfiguredSite(featureUrl);
        } catch (MalformedURLException e) {
            throw Utilities.newCoreException(NLS.bind(Messages.InstallConfiguration_UnableToCreateURL, (new String[] { cSite.getPlatformURLString() })), e);
        } catch (ClassCastException e) {
            throw Utilities.newCoreException(Messages.InstallConfiguration_UnableToCast, e);    
        }

        // if the URL doesn't exist throw a CoreException
        if (siteEntry == null) {
            throw new CoreException(
                    new Status(IStatus.ERROR, UpdateCore.getPlugin().getBundle().getSymbolicName(),
                            NLS.bind(Messages.InstallConfiguration_unableToFindSite, (new String[] { cSite.getSite().getURL().toExternalForm(), runtimeConfiguration.getConfigurationLocation().toExternalForm() }))));
        }

		// write the primary features
		if (feature.isPrimary()) {
			// get any fragments for the feature plugin
			ArrayList list = new ArrayList();
			if (url != null)
				list.add(url);
			if (featurePlugin != null) {
				URL[] fragments = getRuntimeFragmentURLs(featurePlugin);
				list.addAll(Arrays.asList(fragments));
			}
			URL[] roots = (URL[]) list.toArray(new URL[0]);
			String pluginIdentifier = feature.getPrimaryPluginID();

			// save information in runtime platform state
			String version = feature.getVersionedIdentifier().getVersion().toString();
			String application = feature.getApplication();
			FeatureEntry featureEntry = (FeatureEntry)runtimeConfiguration.createFeatureEntry(id, version, pluginIdentifier, pluginVersion, true, application, roots);
			featureEntry.setURL(getFeatureRelativeURL(feature));
			siteEntry.addFeatureEntry(featureEntry);
		} else {
			// write non-primary feature entries
			String version = feature.getVersionedIdentifier().getVersion().toString();
			String pluginIdentifier = feature.getPrimaryPluginID();
			FeatureEntry featureEntry = (FeatureEntry)runtimeConfiguration.createFeatureEntry(id, version, pluginIdentifier, pluginVersion, false, null, null);
			featureEntry.setURL(getFeatureRelativeURL(feature));
			siteEntry.addFeatureEntry(featureEntry);
		}

		// write the platform features (features that contain special platform plugins)
		IPluginEntry[] platformPlugins = getPlatformPlugins(feature, runtimeConfiguration);
		for (int k = 0; k < platformPlugins.length; k++) {
			id = platformPlugins[k].getVersionedIdentifier().getIdentifier();
			url = getRuntimeConfigurationURL(platformPlugins[k], cSite);
			if (url != null) {
				runtimeConfiguration.setBootstrapPluginLocation(id, url);
			}
		}
	}

	/*
	 * Log if we are about to create a site that didn't exist before
	 * in platform.cfg [16696].
	 */
	private void checkSites(ConfiguredSiteModel[] configurationSites, IPlatformConfiguration runtimeConfiguration) throws CoreException {

		// check all the sites we are about to write already existed
		// they should have existed either because they were created by
		// updateManager or because we read them from platform.cfg
		for (int i = 0; i < configurationSites.length; i++) {
			// get the URL of the site that matches the one platform.cfg gave us
			URL urlToCheck = null;
			try {
				urlToCheck = new URL(configurationSites[i].getPlatformURLString());
			} catch (MalformedURLException e) {
				UpdateCore.warn(NLS.bind(Messages.InstallConfiguration_UnableToCreateURL, (new String[] { configurationSites[i].getPlatformURLString() })), e);
			} catch (ClassCastException e) {
				UpdateCore.warn(Messages.InstallConfiguration_UnableToCast, e);
			}

			// if the URL doesn't exits log it
			IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.findConfiguredSite(urlToCheck);
			if (siteEntry == null) {
				UpdateCore.warn(NLS.bind(Messages.InstallConfiguration_unableToFindSite, (new String[] { urlToCheck.toExternalForm(), runtimeConfiguration.getConfigurationLocation().toExternalForm() }))); 
			}
		}
	}


	/*
	 * reverts this configuration to the match the new one
	 *
	 * Compare the oldSites with the currentOne. the old state is the state we want to revert to.
	 *
	 * If a site was in old state, but not in the currentOne, keep it in the hash.
	 * If a site is in the currentOne but was not in the old state, unconfigure all features and add it in the hash
	 * If a site was in baoth state, calculate the 'delta' and re-set it in the hash map
	 *
	 * At the end, set the configured site from the new sites hash map
	 *
	 */
	public void revertTo(IInstallConfiguration configuration, IProgressMonitor monitor, IProblemHandler handler) throws CoreException, InterruptedException {

		IConfiguredSite[] oldConfigSites = configuration.getConfiguredSites();
		IConfiguredSite[] nowConfigSites = this.getConfiguredSites();

		// create a hashtable of the *old* and *new* sites
		Map oldSitesMap = new Hashtable(0);
		Map newSitesMap = new Hashtable(0);
		for (int i = 0; i < oldConfigSites.length; i++) {
			IConfiguredSite element = oldConfigSites[i];
			oldSitesMap.put(element.getSite().getURL().toExternalForm(), element);
			newSitesMap.put(element.getSite().getURL().toExternalForm(), element);
		}
		// create list of all the sites that map the *old* sites
		// we want the intersection between the old sites and the current sites
		if (nowConfigSites != null) {
			String key = null;

			for (int i = 0; i < nowConfigSites.length; i++) {
				key = nowConfigSites[i].getSite().getURL().toExternalForm();
				IConfiguredSite oldSite = (IConfiguredSite) oldSitesMap.get(key);
				if (oldSite != null) {
					// the Site existed before, calculate the delta between its current state and the
					// state we are reverting to and put it back into the map
					 ((ConfiguredSite) nowConfigSites[i]).revertTo(oldSite, monitor, handler);
				} else {
					// the site didn't exist in the InstallConfiguration we are reverting to
					// unconfigure everything from this site so it is still present
					ISiteFeatureReference[] featuresToUnconfigure = nowConfigSites[i].getSite().getFeatureReferences();
					for (int j = 0; j < featuresToUnconfigure.length; j++) {
						IFeature featureToUnconfigure = null;
						try {
							featureToUnconfigure = featuresToUnconfigure[j].getFeature(null);
						} catch (CoreException e) {
							UpdateCore.warn(null, e);
						}
						if (featureToUnconfigure != null)
							nowConfigSites[i].unconfigure(featureToUnconfigure);
					}
				}
				newSitesMap.put(key,nowConfigSites[i]);
			}

			// the new configuration has the exact same sites as the old configuration
			// the old configuration in the Map are either as-is because they don't exist
			// in the current one, or they are the delta from the current one to the old one
			Collection sites = newSitesMap.values();
			if (sites != null && !sites.isEmpty()) {
				ConfiguredSiteModel[] sitesModel = new ConfiguredSiteModel[sites.size()];
				sites.toArray(sitesModel);
				setConfigurationSiteModel(sitesModel);
			}
		}
	}

	/*
	 * @see IInstallConfiguration#getActivities()
	 */
	public IActivity[] getActivities() {
		if (getActivityModel().length == 0)
			return new IActivity[0];
		return (IActivity[]) getActivityModel();
	}

	/*
	 * returns the list of platform plugins of the feature or an empty list
	 * if the feature doesn't contain any platform plugins
	 */
	private IPluginEntry[] getPlatformPlugins(IFeature feature, IPlatformConfiguration runtimeConfiguration) {
		Map featurePlatformPlugins = new HashMap();
		String[] platformPluginID = runtimeConfiguration.getBootstrapPluginIdentifiers();
		IPluginEntry[] featurePlugins = feature.getPluginEntries();

		for (int i = 0; i < platformPluginID.length; i++) {
			String featurePluginId = null;
			for (int j = 0; j < featurePlugins.length; j++) {
				featurePluginId = featurePlugins[j].getVersionedIdentifier().getIdentifier();
				if (platformPluginID[i].equals(featurePluginId)) {
					featurePlatformPlugins.put(platformPluginID[i], featurePlugins[j]);
				}
			}
		}

		Collection values = featurePlatformPlugins.values();
		if (values == null || values.size() == 0)
			return new IPluginEntry[0];

		IPluginEntry[] result = new IPluginEntry[values.size()];
		Iterator iter = values.iterator();
		int index = 0;
		while (iter.hasNext()) {
			result[index] = ((IPluginEntry) iter.next());
			index++;
		}
		return result;
	}

	/*
	 * returns the URL of the pluginEntry on the site
	 * Transform the URL to use platform: protocol if needed
	 * return null if the URL to write is not valid
	 */
	private URL getRuntimeConfigurationURL(IPluginEntry entry, ConfiguredSite cSite) throws CoreException {

		String rootString = cSite.getPlatformURLString();
		String pluginPathID = getPathID(entry);
		try {
			ISiteContentProvider siteContentProvider = cSite.getSite().getSiteContentProvider();
			URL pluginEntryfullURL = siteContentProvider.getArchiveReference(pluginPathID);

			//
			if (!rootString.startsWith("platform")) { //$NON-NLS-1$
				// DEBUG:
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
					UpdateCore.debug("getRuntimeConfiguration Plugin Entry Full URL:" + pluginEntryfullURL + " Platform String:" + rootString + " [NON PLATFORM URL]."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return pluginEntryfullURL;
			}

			//URL pluginEntryRootURL = Platform.resolve(new URL(rootString));
			// Do not resolve [16507], just use platform:base/ as a root
			// rootString = platform:base
			// pluginRoot = /home/a
			// pluginFull = /home/a/c/boot.jar
			// relative = platform:/base/c/boot.jar
			URL pluginEntryRootURL = cSite.getSite().getURL();
			String relativeString = UpdateManagerUtils.getURLAsString(pluginEntryRootURL, pluginEntryfullURL);
			URL result = new URL(new URL(rootString), relativeString);

			// DEBUG:
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.debug("getRuntimeConfiguration plugin Entry Full URL:" + pluginEntryfullURL + " Platform String:" + rootString + " Site URL:" + pluginEntryRootURL + " Relative:" + relativeString); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			// verify we are about to write a valid file URL
			// check with fullURL as it is not resolved to platform:base/
			if (pluginEntryfullURL != null) {
				if ("file".equals(pluginEntryfullURL.getProtocol())) { //$NON-NLS-1$
					String fileString = pluginEntryfullURL.getFile();
					if (!new File(fileString).exists()) {
						UpdateCore.warn("The URL:" + result + " doesn't point to a valid platform plugin.The URL will not be written in the platform configuration", new Exception()); //$NON-NLS-1$ //$NON-NLS-2$
						return null;
					}
				}
			}

			return result;
		} catch (IOException e) {
			throw Utilities.newCoreException(NLS.bind(Messages.InstallConfiguration_UnableToCreateURL, (new String[] { rootString })), e);
		}
	}

	/*
	 * Return URLs for any fragments that are associated with the specified plugin entry
	 */
	private URL[] getRuntimeFragmentURLs(IPluginEntry entry) throws CoreException {

		// get the identifier associated with the entry
		VersionedIdentifier vid = entry.getVersionedIdentifier();

		// get the plugin descriptor from the registry
		Bundle bundle = Platform.getBundle(vid.getIdentifier());
		ArrayList list = new ArrayList();
		if (bundle != null && bundle.getState() != Bundle.UNINSTALLED && bundle.getState() != Bundle.INSTALLED) {
			FragmentEntry[] fragments = UpdateManagerUtils.getFragments(bundle);
			for (int i = 0; fragments != null && i < fragments.length; i++) {
				String location = fragments[i].getLocation();
				try {
					URL locationURL = new URL(location);
					locationURL = FileLocator.toFileURL(FileLocator.resolve(locationURL));
					list.add(asInstallRelativeURL(locationURL));
				} catch (IOException e) {
					// skip bad fragments
				}
			}
		}
		return (URL[]) list.toArray(new URL[0]);
	}

	/**
	 * Returns the path identifier for a plugin entry.
	 * <code>plugins/&lt;pluginId>_&lt;pluginVersion>.jar</code>
	 * @return the path identifier
	 */
	private String getPathID(IPluginEntry entry) {
		return Site.DEFAULT_PLUGIN_PATH + entry.getVersionedIdentifier().toString() + FeatureContentProvider.JAR_EXTENSION;
	}

	/**
	 * Try to recast URL as platform:/base/
	 */
	private URL asInstallRelativeURL(URL url) {
		// get location of install
		URL install = ConfiguratorUtils.getInstallURL();

		// try to determine if supplied URL can be recast as install-relative
		if (install.getProtocol().equals(url.getProtocol())) {
			if (install.getProtocol().equals("file")) { //$NON-NLS-1$
				String installS = new File(install.getFile()).getAbsolutePath().replace(File.separatorChar, '/');
				if (!installS.endsWith("/")) //$NON-NLS-1$
					installS += "/"; //$NON-NLS-1$
				String urlS = new File(url.getFile()).getAbsolutePath().replace(File.separatorChar, '/');
				if (!urlS.endsWith("/")) //$NON-NLS-1$
					urlS += "/"; //$NON-NLS-1$
				int ix = installS.lastIndexOf("/"); //$NON-NLS-1$
				if (ix != -1) {
					installS = installS.substring(0, ix + 1);
					if (urlS.startsWith(installS)) {
						try {
							return new URL("platform:/base/" + urlS.substring(installS.length())); //$NON-NLS-1$
						} catch (MalformedURLException e) {
						}
					}
				}
			}
		}
		return url;
	}
	
	private boolean isDuplicateSite(File siteDirectory) {
		IConfiguredSite[] sites = getConfiguredSites();
		URL fileURL;
		try {
			fileURL = siteDirectory.toURL();
		} catch (MalformedURLException e) {
			return false;
		}
		for (int i = 0; i < sites.length; i++) {
			URL url = sites[i].getSite().getURL();
			if (UpdateManagerUtils.sameURL(fileURL, url))
				return true;
		}
		return false;
	}
	
	/*
	 * Returns the feature url relative to the site.
	 */
	private String getFeatureRelativeURL(IFeature feature) {
		String url = feature.getURL().toExternalForm();
		String siteURL = feature.getSite().getURL().toExternalForm();
		// TODO fix this. toURL() returns file:/d:/eclipse/etc... wheareas the 
		// platform.asLocalURL() returns file:d:/eclipse/etc... (no leading / )
//		if (url.startsWith("file:/") && Platform.getOS().equals("win32"))
//			url = "file:" + url.substring(6);
		
		if (url.startsWith(siteURL))
			return url.substring(siteURL.length());
		else
			return url;
	}
	
	/**
	 * @return true if restart is needed
	 */
	private boolean isRestartNeeded(IPlatformConfiguration runtimeConfig) {

		// First, create a map for faster lookups
		Set newPluginsSet = null;
		if (runtimeConfig instanceof PlatformConfiguration) {
			newPluginsSet = ((PlatformConfiguration)runtimeConfig).getPluginPaths();
			// On windows, we will be doing case insensitive search as well, so lower it now
			if (isWindows) {
				String[] newPluginsSetArray = (String[])newPluginsSet.toArray( new String[newPluginsSet.size()]);
				for (int i = 0; i < newPluginsSetArray.length; i++) {
					newPluginsSet.add(newPluginsSetArray[i].toLowerCase());
				}
			}
		} else {
			URL[] newBundlePaths = runtimeConfig.getPluginPath();
			newPluginsSet = new HashSet(newBundlePaths.length);
			for (int i=0; i<newBundlePaths.length; i++) {
				
				String pluginLocation = newBundlePaths[i].getFile();
				newPluginsSet.add(pluginLocation);
				// On windows, we will be doing case insensitive search as well, so lower it now
				if (isWindows)
					newPluginsSet.add(pluginLocation.toLowerCase());
			}
		}
		
		
		
		Bundle[] oldBundles = UpdateCore.getPlugin().getBundleContext().getBundles();

		int offset = ConfigurationActivator.UPDATE_PREFIX.length();
		for (int i=0; i<oldBundles.length; i++) {
			if (oldBundles[i].getBundleId() == 0)
				continue; // skip the system bundle
			String oldBundleLocation = oldBundles[i].getLocation();
			// Don't worry about bundles we did not install
			if (!oldBundleLocation.startsWith(ConfigurationActivator.UPDATE_PREFIX))
				continue;
			oldBundleLocation = oldBundleLocation.substring(offset);
			
			if (newPluginsSet.contains(oldBundleLocation))
				continue;
			if (isWindows && newPluginsSet.contains(oldBundleLocation.toLowerCase()))
				continue;
			
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.debug("Bundle " + oldBundleLocation + " has been removed"); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		
		if (runtimeConfig instanceof PlatformConfiguration) {
			return areThereNewVersionOfOldPlugins( ((PlatformConfiguration)runtimeConfig).getPlugins(), oldBundles);
		}

		return false;
	}
	
	/**
	 * The method should only be called if all old plug-ins exist in the new
	 * configuration. Determines if a new version was added to any plugin ID, so
	 * more versions are enabled for any given plug-in id then in old
	 * configuration.
	 * 
	 * @param newConfigurationPlugins
	 * @param oldConfigurationBundles
	 * @return
	 */
	private boolean areThereNewVersionOfOldPlugins(PluginEntry[] newConfigurationPlugins, Bundle[] oldConfigurationBundles) {

		
		for ( int i = 0; i < oldConfigurationBundles.length; i++) {
			if (oldConfigurationBundles[i].getBundleId() == 0)
				continue; // skip the system bundle
			if ( getNumberOfPlugins(oldConfigurationBundles[i].getSymbolicName(), oldConfigurationBundles) != getNumberOfPlugins(oldConfigurationBundles[i].getSymbolicName(), newConfigurationPlugins)) {
				return true;
			}
		}
		return false;				
	}

	private int getNumberOfPlugins(String symbolicName, PluginEntry[] newConfigurationPlugins) {
		
		int numberOfPlugins = 0;
		
		for ( int i = 0; i < newConfigurationPlugins.length; i++) {
			if ( symbolicName.equals(newConfigurationPlugins[i].getPluginIdentifier())) {
				numberOfPlugins++;
			}
		}
		
		return numberOfPlugins;
	}

	private int getNumberOfPlugins(String symbolicName, Bundle[] oldConfigurationBundles) {

		int numberOfPlugins = 0;
		
		for ( int i = 0; i < oldConfigurationBundles.length; i++) {
			if ( symbolicName.equals(oldConfigurationBundles[i].getSymbolicName())) {
				numberOfPlugins++;
			}
		}
		
		return numberOfPlugins;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof InstallConfiguration))
			return false;
		
		InstallConfiguration config = (InstallConfiguration)obj;
	
		return getCreationDate().equals(config.getCreationDate()) && 
				getLabel().equals(config.getLabel()) &&
				getLocationURLString().equals(config.getLocationURLString());
	}
}
