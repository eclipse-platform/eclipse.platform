package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginDescriptorModel;
import org.eclipse.core.runtime.model.PluginFragmentModel;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.model.*;

/**
 * Manages ConfiguredSites
 * 
 */

public class InstallConfiguration extends InstallConfigurationModel implements IInstallConfiguration, IWritable {

	private ListenersList listeners = new ListenersList();

	/*
	 * default constructor. 
	 */
	public InstallConfiguration() {
	}

	/*
	 * default constructor. 
	 */
	public InstallConfiguration(URL newLocation, String label) throws MalformedURLException {
		setLocationURLString(newLocation.toExternalForm());
		setLabel(label);
		setCurrent(false);
		resolve(newLocation, null);
	}

	/*
	 * copy constructor
	 */
	public InstallConfiguration(IInstallConfiguration config, URL newLocation, String label) throws MalformedURLException {
		setLocationURLString(newLocation.toExternalForm());
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
		// set dummy date as caller can call set date if the
		// date on the URL string has to be the same 
		setCreationDate(new Date());
		setCurrent(false);
		resolve(newLocation, null);
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
		return IPlatformConfiguration.ISitePolicy.USER_EXCLUDE;
	}

	/**
	 * Creates a Configuration Site and a new Site
	 * The policy is from <code> org.eclipse.core.boot.IPlatformConfiguration</code>
	 */
	public IConfiguredSite createConfiguredSite(File file) throws CoreException {

		ISite site = InternalSiteManager.createSite(file);

		//create a config site around the site
		// even if the site == null
		BaseSiteLocalFactory factory = new BaseSiteLocalFactory();
		ConfiguredSite configSite = (ConfiguredSite) factory.createConfigurationSiteModel((SiteModel) site, getDefaultPolicy());

		if (site != null && configSite.verifyUpdatableStatus().isOK()) {
			configSite.setPlatformURLString(site.getURL().toExternalForm());

			// obtain the list of plugins
			IPlatformConfiguration runtimeConfiguration = BootLoader.getCurrentPlatformConfiguration();
			ConfigurationPolicy configurationPolicy = (ConfigurationPolicy) configSite.getConfigurationPolicy();
			String[] pluginPath = new String[0];
			if (configurationPolicy.getPolicy() == IPlatformConfiguration.ISitePolicy.USER_INCLUDE)
				pluginPath = configurationPolicy.getPluginPath(site, null);

			// create new Site in configuration
			IPlatformConfiguration.ISitePolicy sitePolicy = runtimeConfiguration.createSitePolicy(configurationPolicy.getPolicy(), pluginPath);

			// change runtime					
			IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.createSiteEntry(site.getURL(), sitePolicy);
			runtimeConfiguration.configureSite(siteEntry);

			// if the privatre marker doesn't already exist create it
			configSite.createPrivateSiteMarker();
		}

		return configSite;
	}

	/**
	 * Creates a Configuration Site and a new Site as a private link site
	 * The policy is from <code> org.eclipse.core.boot.IPlatformConfiguration</code>
	 */
	public IConfiguredSite createLinkedConfiguredSite(File file) throws CoreException {

		ISite site = InternalSiteManager.createSite(file);

		//create a config site around the site
		// even if the site == null
		BaseSiteLocalFactory factory = new BaseSiteLocalFactory();
		ConfiguredSite configSite = (ConfiguredSite) factory.createConfigurationSiteModel((SiteModel) site, getDefaultPolicy());

		if (!configSite.isExtensionSite()) {
			String msg = Policy.bind("InstallConfiguration.NotAnExtensionSite");
			throw Utilities.newCoreException(msg, null);
		}

		if (configSite.isNativelyLinked()) {
			throw Utilities.newCoreException("InstallConfiguration.AlreadyNativelyLinked", null);
		}

		if (site != null) {
			configSite.setPlatformURLString(site.getURL().toExternalForm());

			// obtain the list of plugins
			IPlatformConfiguration runtimeConfiguration = BootLoader.getCurrentPlatformConfiguration();
			ConfigurationPolicy configurationPolicy = (ConfigurationPolicy) configSite.getConfigurationPolicy();
			String[] pluginPath = new String[0];
			if (configurationPolicy.getPolicy() == IPlatformConfiguration.ISitePolicy.USER_INCLUDE)
				pluginPath = configurationPolicy.getPluginPath(site, null);

			// create new Site in configuration
			IPlatformConfiguration.ISitePolicy sitePolicy = runtimeConfiguration.createSitePolicy(configurationPolicy.getPolicy(), pluginPath);

			// change runtime					
			IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.createSiteEntry(site.getURL(), sitePolicy);
			runtimeConfiguration.configureSite(siteEntry);

		}

		// configure all features as enable
		configure(configSite);

		return configSite;
	}

	/*
	 *Configure all features as Enable Check we only enable highest version 
	 */
	private void configure(ConfiguredSite linkedSite) throws CoreException {
		ISite site = linkedSite.getSite();
		IFeatureReference[] newFeaturesRef = site.getFeatureReferences();

		for (int i = 0; i < newFeaturesRef.length; i++) {
			// TRACE
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER) {
				String reconciliationType = "enable (optimistic)";
				UpdateManagerPlugin.debug("New Linked Site:New Feature: " + newFeaturesRef[i].getURL() + " as " + reconciliationType);
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

		if (removeConfigurationSiteModel((ConfiguredSiteModel) site)) {
			// notify listeners
			Object[] configurationListeners = listeners.getListeners();
			for (int i = 0; i < configurationListeners.length; i++) {
				IInstallConfigurationChangedListener listener = ((IInstallConfigurationChangedListener) configurationListeners[i]);
				listener.installSiteAdded(site);
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
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/*
	 * @see IInstallConfiguration#removeInstallConfigurationChangedListener(IInstallConfigurationChangedListener)
	 */
	public void removeInstallConfigurationChangedListener(IInstallConfigurationChangedListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/*
	 * write the Configuration.xml file
	 */
	private void export(File exportFile) throws CoreException {
		try {
			Writer writer = new Writer(exportFile, "UTF8"); //$NON-NLS-1$
			writer.write(this);
		} catch (FileNotFoundException e) {
			throw Utilities.newCoreException(Policy.bind("InstallConfiguration.UnableToSaveConfiguration", exportFile.getAbsolutePath()), e);
			//$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw Utilities.newCoreException(Policy.bind("InstallConfiguration.UnableToEncodeConfiguration", exportFile.getAbsolutePath()), e);
			//$NON-NLS-1$
		}
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

	/*
	 * Saves the configuration into its URL/location
	 * and changes the platform configuration.
	 * The runtime site entries from platform.cfg are updated as required
	 * (cannot recreate these because must preserve other runtime state) [18520]
	 */
	public void save(boolean isTransient) throws CoreException {

		// save the configuration.xml file
		saveConfigurationFile(isTransient);

		// Write info  into platform for the next runtime
		IPlatformConfiguration runtimeConfiguration = BootLoader.getCurrentPlatformConfiguration();
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
			tempConfig = BootLoader.getPlatformConfiguration(null);
			IPlatformConfiguration.ISiteEntry[] tmpSites = runtimeConfiguration.getConfiguredSites();
			for (int i = 0; i < tmpSites.length; i++) {
				tempConfig.configureSite(tmpSites[i]);
			}
		} catch (IOException e) {
			// assume no currently configured sites
		}

		//check sites
		checkSites(configurationSites, runtimeConfiguration);

		// Write the plugin path, primary feature and platform
		// into platform.cfg
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
					feature = configuredFeaturesRef[j].getFeature();
				} catch (CoreException e) {
					UpdateManagerPlugin.warn(null, e);
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
		} catch (IOException e) {
			throw Utilities.newCoreException(Policy.bind("InstallConfiguration.UnableToSavePlatformConfiguration", runtimeConfiguration.getConfigurationLocation().toExternalForm()), e);
			//$NON-NLS-1$
		}
		
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
		String[] pluginPath = configurationPolicy.getPluginPath(cSite.getSite(), cSite.getPreviousPluginPath());
		IPlatformConfiguration.ISitePolicy sitePolicy = runtimeConfiguration.createSitePolicy(configurationPolicy.getPolicy(), pluginPath);

		// get the URL of the site that matches the one platform.cfg gave us
		URL urlToCheck = null;
		try {
			urlToCheck = new URL(cSite.getPlatformURLString());
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(Policy.bind("InstallConfiguration.UnableToCreateURL", cSite.getPlatformURLString()), e);
			//$NON-NLS-1$
		} catch (ClassCastException e) {
			throw Utilities.newCoreException(Policy.bind("InstallConfiguration.UnableToCast"), e);
			//$NON-NLS-1$
		}
		
		// update runtime configuration [18520]
		// Note: we must not blindly replace the site entries because they
		//       contain additional runtime state that needs to be preserved.
		IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.findConfiguredSite(urlToCheck);
		if (siteEntry == null)
			siteEntry = runtimeConfiguration.createSiteEntry(urlToCheck, sitePolicy);
		else {
			siteEntry.setSitePolicy(sitePolicy);
			if (tempConfig != null) // [19958] remove reused entries from list
				tempConfig.unconfigureSite(siteEntry);
		}
		runtimeConfiguration.configureSite(siteEntry, true /*replace if exists*/
		);
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
			}
		}
		String pluginVersion = null;
		if (featurePlugin != null)
			pluginVersion = featurePlugin.getVersionedIdentifier().getVersion().toString();

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

			// save information in runtime platform state
			String version = feature.getVersionedIdentifier().getVersion().toString();
			String application = feature.getApplication();
			IPlatformConfiguration.IFeatureEntry featureEntry = runtimeConfiguration.createFeatureEntry(id, version, pluginVersion, true, application, roots);
			runtimeConfiguration.configureFeatureEntry(featureEntry);
		} else {
			// write non-primary feature entries
			String version = feature.getVersionedIdentifier().getVersion().toString();
			IPlatformConfiguration.IFeatureEntry featureEntry = runtimeConfiguration.createFeatureEntry(id, version, pluginVersion, false, null, null);
			runtimeConfiguration.configureFeatureEntry(featureEntry);
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
				UpdateManagerPlugin.warn(Policy.bind("InstallConfiguration.UnableToCreateURL", configurationSites[i].getPlatformURLString()), e);
				//$NON-NLS-1$
			} catch (ClassCastException e) {
				UpdateManagerPlugin.warn(Policy.bind("InstallConfiguration.UnableToCast"), e);
				//$NON-NLS-1$
			}

			// if the URL doesn't exits log it
			IPlatformConfiguration.ISiteEntry siteEntry = runtimeConfiguration.findConfiguredSite(urlToCheck);
			if (siteEntry == null) {
				UpdateManagerPlugin.warn(Policy.bind("Unable to find site {0} in platform configuration {1}.", urlToCheck.toExternalForm(), runtimeConfiguration.getConfigurationLocation().toExternalForm()));
				//$NON-NLS-1$
			}
		}
	}

	/*
	 * 
	 */
	public void saveConfigurationFile(boolean isTransient) throws CoreException {
		// save the configuration
		if ("file".equalsIgnoreCase(getURL().getProtocol())) { //$NON-NLS-1$
			// the location points to a file
			File file = new File(getURL().getFile());
			if (!file.exists()) {
				//log + 24642 [works for all activities]
				UpdateManagerPlugin.log(this);	
			}
			if (isTransient)
				file.deleteOnExit();
			export(file);
		}
	}

	/*
	 * @see IWritable#write(int, PrintWriter)
	 */
	public void write(int indent, PrintWriter w) {
		String gap = ""; //$NON-NLS-1$
		for (int i = 0; i < indent; i++)
			gap += " "; //$NON-NLS-1$
		String increment = ""; //$NON-NLS-1$
		for (int i = 0; i < IWritable.INDENT; i++)
			increment += " "; //$NON-NLS-1$

		//CONFIGURATION	
		w.print(gap + "<" + InstallConfigurationParser.CONFIGURATION + " ");
		//$NON-NLS-1$ //$NON-NLS-2$
		long time = (getCreationDate() != null) ? getCreationDate().getTime() : 0L;
		w.print("date=\"" + time + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		w.println(">"); //$NON-NLS-1$

		// site configurations
		if (getConfigurationSitesModel() != null) {
			ConfiguredSiteModel[] sites = getConfigurationSitesModel();
			for (int i = 0; i < sites.length; i++) {
				ConfiguredSite element = (ConfiguredSite) sites[i];
				((IWritable) element).write(indent + IWritable.INDENT, w);
			}
		}

		// activities
		if (getActivityModel() != null) {
			ConfigurationActivityModel[] activities = getActivityModel();
			for (int i = 0; i < activities.length; i++) {
				ConfigurationActivity element = (ConfigurationActivity) activities[i];
				((IWritable) element).write(indent + IWritable.INDENT, w);
			}
		}

		// end
		w.println(gap + "</" + InstallConfigurationParser.CONFIGURATION + ">");
		//$NON-NLS-1$ //$NON-NLS-2$
		w.println(""); //$NON-NLS-1$		
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
					IFeatureReference[] featuresToUnconfigure = nowConfigSites[i].getSite().getFeatureReferences();
					for (int j = 0; j < featuresToUnconfigure.length; j++) {
						IFeature featureToUnconfigure = null;
						try {
							featureToUnconfigure = featuresToUnconfigure[j].getFeature();
						} catch (CoreException e) {
							UpdateManagerPlugin.warn(null, e);
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
			if (!rootString.startsWith("platform")) {
				// DEBUG:
				if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION)
					UpdateManagerPlugin.debug("getRuntimeConfiguration Plugin Entry Full URL:" + pluginEntryfullURL + " Platform String:" + rootString + " [NON PLATFORM URL].");
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
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION)
				UpdateManagerPlugin.debug("getRuntimeConfiguration plugin Entry Full URL:" + pluginEntryfullURL + " Platform String:" + rootString + " Site URL:" + pluginEntryRootURL + " Relative:" + relativeString);

			// verify we are about to write a valid file URL
			// check with fullURL as it is not resolved to platform:base/
			if (pluginEntryfullURL != null) {
				if ("file".equals(pluginEntryfullURL.getProtocol())) {
					String fileString = pluginEntryfullURL.getFile();
					if (!new File(fileString).exists()) {
						UpdateManagerPlugin.warn("The URL:" + result + " doesn't point to a valid platform plugin.The URL will not be written in the platform configuration", new Exception());
						return null;
					}
				}
			}

			return result;
		} catch (IOException e) {
			throw Utilities.newCoreException(Policy.bind("InstallConfiguration.UnableToCreateURL", rootString), e);
			//$NON-NLS-1$
		}
	}

	/*
	 * Return URLs for any fragments that are associated with the specified plugin entry
	 */
	private URL[] getRuntimeFragmentURLs(IPluginEntry entry) throws CoreException {

		// get the identifier associated with the entry
		VersionedIdentifier vid = entry.getVersionedIdentifier();

		// get the plugin descriptor from the registry
		IPluginRegistry reg = Platform.getPluginRegistry();
		IPluginDescriptor desc = reg.getPluginDescriptor(vid.getIdentifier());
		ArrayList list = new ArrayList();
		if (desc != null) {
			try {
				// get all of the fragments
				PluginDescriptorModel descModel = (PluginDescriptorModel) desc;
				PluginFragmentModel[] frags = descModel.getFragments();

				for (int i = 0; frags != null && i < frags.length; i++) {
					String location = frags[i].getLocation();
					try {
						URL locationURL = new URL(location);
						locationURL = Platform.resolve(locationURL);
						list.add(asInstallRelativeURL(locationURL));
					} catch (IOException e) {
						// skip bad fragments
					}
				}

			} catch (ClassCastException e) {
				// cannot determine fragments
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
		URL install = BootLoader.getInstallURL();

		// try to determine if supplied URL can be recast as install-relative
		if (install.getProtocol().equals(url.getProtocol())) {
			if (install.getProtocol().equals("file")) {
				String installS = new File(install.getFile()).getAbsolutePath().replace(File.separatorChar, '/');
				if (!installS.endsWith("/"))
					installS += "/";
				String urlS = new File(url.getFile()).getAbsolutePath().replace(File.separatorChar, '/');
				if (!urlS.endsWith("/"))
					urlS += "/";
				int ix = installS.lastIndexOf("/");
				if (ix != -1) {
					installS = installS.substring(0, ix + 1);
					if (urlS.startsWith(installS)) {
						try {
							return new URL("platform:/base/" + urlS.substring(installS.length()));
						} catch (MalformedURLException e) {
						}
					}
				}
			}
		}
		return url;
	}
}