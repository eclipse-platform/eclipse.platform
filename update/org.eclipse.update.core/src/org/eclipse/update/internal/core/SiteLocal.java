package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved. 
 */
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.model.*;
import org.xml.sax.SAXException;
import org.eclipse.update.internal.core.Policy;

/**
 * This class manages the configurations.
 */

public class SiteLocal
	extends SiteLocalModel
	implements ILocalSite, IWritable {

	private static IPluginEntry[] allRunningPluginEntry;
	private ListenersList listeners = new ListenersList();
	private boolean isTransient = false;

	private static final String UPDATE_STATE_SUFFIX = ".metadata";

	/**
	 * initialize the configurations from the persistent model.
	 * The configurations are per user, so we save the data in the 
	 * user path, not the .metadata of any workspace, so the data
	 * is shared between the workspaces.
	 */
	public static ILocalSite getLocalSite() throws CoreException {

		SiteLocal localSite = new SiteLocal();

		// obtain platform configuration
		IPlatformConfiguration currentPlatformConfiguration =
			BootLoader.getCurrentPlatformConfiguration();
		localSite.isTransient(currentPlatformConfiguration.isTransient());

		try {

			// obtain LocalSite.xml location
			URL location;
			try {
				location = getUpdateStateLocation(currentPlatformConfiguration);
			} catch (IOException exception) {
				throw Utilities.newCoreException(
					Policy.bind(Policy.bind("SiteLocal.UnableToRetrieveRWArea")), //$NON-NLS-1$
					exception);
			}

			URL configXML = UpdateManagerUtils.getURL(location, SITE_LOCAL_FILE, null);
			localSite.setLocationURLString(configXML.toExternalForm());
			localSite.resolve(configXML, null);

			parseLocalSiteFile(localSite, currentPlatformConfiguration, configXML);

			// check if we have to reconcile, if the timestamp has changed
			long bootStamp = currentPlatformConfiguration.getChangeStamp();
			if (localSite.getStamp() != bootStamp) {
				if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
					UpdateManagerPlugin.getPlugin().debug(
						"Reconcile platform stamp:"
							+ bootStamp
							+ " is different from LocalSite stamp:"
							+ localSite.getStamp());
					//$NON-NLS-1$ //$NON-NLS-2$
				}

				localSite.reconcile();

			} else {
				// no reconciliation, preserve the list of plugins from the platform anyway
				localSite.preserveRuntimePluginPath();
			}
		} catch (MalformedURLException exception) {
			throw Utilities.newCoreException(
				Policy.bind(
					"SiteLocal.UnableToCreateURLFor",
					localSite.getLocationURLString() + " & " + SITE_LOCAL_FILE),
				exception);
			//$NON-NLS-1$ //$NON-NLS-2$
		}

		return localSite;
	}

	/**
	 * Create the localSite object either by parsing, recovering from the file system, or reconciling with the platform configuration
	 */
	private static void parseLocalSiteFile(
		SiteLocal localSite,
		IPlatformConfiguration currentPlatformConfiguration,
		URL configXML)
		throws MalformedURLException, CoreException {

		//attempt to parse the LocalSite.xml	
		URL resolvedURL = URLEncoder.encode(configXML);
		try {
			new SiteLocalParser(resolvedURL.openStream(), localSite);
		} catch (FileNotFoundException exception) {
			// file SITE_LOCAL_FILE doesn't exist, ok, log it 
			// and reconcile with platform configuration
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
				UpdateManagerPlugin.getPlugin().debug(
					localSite.getLocationURLString()
						+ " does not exist, there is no previous state or install history we can recover from, we shall use default from platform configuration.");
				//$NON-NLS-1$
			}

			localSite.reconcile();

		} catch (SAXException exception) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
				Utilities.logException(
					Policy.bind(
						"SiteLocal.ErrorParsingSavedState",
						localSite.getLocationURLString()),
					exception);
				//$NON-NLS-1$
			}
			recoverSiteLocal(resolvedURL, localSite);

		} catch (IOException exception) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
				Utilities.logException(
					Policy.bind("SiteLocal.UnableToAccessFile", configXML.toExternalForm()),
					exception);
				//$NON-NLS-1$
			}

			recoverSiteLocal(resolvedURL, localSite);
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

			// check if we have to remove a configuration
			// the first added is #0
			while (getConfigurationHistory().length > getMaximumHistoryCount()) {
				InstallConfigurationModel removedConfig = getConfigurationHistoryModel()[0];
				if (removeConfigurationModel(removedConfig)) {

					// DEBUG:
					if (UpdateManagerPlugin.DEBUG
						&& UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION) {
						UpdateManagerPlugin.getPlugin().debug(
							"Removed configuration :" + removedConfig.getLabel());
						//$NON-NLS-1$
					}

					// notify listeners
					Object[] siteLocalListeners = listeners.getListeners();
					for (int i = 0; i < siteLocalListeners.length; i++) {
						(
							(ILocalSiteChangedListener) siteLocalListeners[i]).installConfigurationRemoved(
							(IInstallConfiguration) removedConfig);
					}

					//remove files
					URL url = removedConfig.getURL();
					UpdateManagerUtils.removeFromFileSystem(new File(url.getFile()));
				}

			}

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
				(
					(
						ILocalSiteChangedListener) siteLocalListeners[i])
							.currentInstallConfigurationChanged(
					config);
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
	 * Saves the site into the config file
	 */
	public void save() throws CoreException {

		// Save the current configuration as
		// the other are already saved
		// and set runtim info for next startup
		 ((InstallConfiguration) getCurrentConfiguration()).save(isTransient());

		// save the local site
		if ("file".equalsIgnoreCase(getLocationURL().getProtocol())) { //$NON-NLS-1$
			File file = null;
			try {
				URL newURL = UpdateManagerUtils.getURL(getLocationURL(), SITE_LOCAL_FILE, null);
				file = new File(newURL.getFile());
				if (isTransient())
					file.deleteOnExit();
				Writer writer = new Writer(file, "UTF8");
				writer.write(this);
			} catch (FileNotFoundException e) {
				throw Utilities.newCoreException(
					Policy.bind("SiteLocal.UnableToSaveStateIn", file.getAbsolutePath()),
					e);
				//$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				throw Utilities.newCoreException(
					Policy.bind(
						"SiteLocal.UnableToEncodeConfiguration",
						 file.getAbsolutePath()),
					e);
				//$NON-NLS-1$
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(
					Policy.bind("SiteLocal.UnableToCreateURLFor")
						+ getLocationURL().toExternalForm()
						+ " : "
						+ SITE_LOCAL_FILE,
					e);
				//$NON-NLS-2$ //$NON-NLS-1$
			}
		}
	}
	/*
	 * @see IWritable#write(int, PrintWriter)
	 */
	public void write(int indent, PrintWriter w) {

		// force the recalculation to avoid reconciliation
		IPlatformConfiguration platformConfig =
			BootLoader.getCurrentPlatformConfiguration();
		platformConfig.refresh();
		long changeStamp = platformConfig.getChangeStamp();
		this.setStamp(changeStamp);

		String gap = ""; //$NON-NLS-1$
		for (int i = 0; i < indent; i++)
			gap += " "; //$NON-NLS-1$
		String increment = ""; //$NON-NLS-1$
		for (int i = 0; i < IWritable.INDENT; i++)
			increment += " "; //$NON-NLS-1$

		w.print(gap + "<" + SiteLocalParser.SITE + " "); //$NON-NLS-1$ //$NON-NLS-2$
		if (getLabel() != null) {
			w.print("label=\"" + Writer.xmlSafe(getLabel()) + "\" ");
			//$NON-NLS-1$ //$NON-NLS-2$
		}
		w.print("history=\"" + getMaximumHistoryCount() + "\" ");
		//$NON-NLS-1$ //$NON-NLS-2$
		w.print("stamp=\"" + changeStamp + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		w.println(">"); //$NON-NLS-1$
		w.println(""); //$NON-NLS-1$

		// the last one is the current configuration
		InstallConfigurationModel[] configurations = getConfigurationHistoryModel();
		for (int index = 0; index < configurations.length; index++) {
			InstallConfigurationModel element = configurations[index];
			if (!element.isCurrent()) {
				writeConfig(gap + increment, w, element);
			}
		}
		// write current configuration last
		writeConfig(
			gap + increment,
			w,
			(InstallConfigurationModel) getCurrentConfiguration());
		w.println(""); //$NON-NLS-1$

		if (getPreservedConfigurations() != null) {
			// write preserved configurations
			w.print(gap + increment + "<" + SiteLocalParser.PRESERVED_CONFIGURATIONS + ">");
			//$NON-NLS-1$ //$NON-NLS-2$

			InstallConfigurationModel[] preservedConfig = getPreservedConfigurationsModel();
			for (int index = 0; index < preservedConfig.length; index++) {
				InstallConfigurationModel element = preservedConfig[index];
				writeConfig(gap + increment + increment, w, element);
			}
			w.println(""); //$NON-NLS-1$
			w.print(
				gap + increment + "</" + SiteLocalParser.PRESERVED_CONFIGURATIONS + ">");
			//$NON-NLS-1$ //$NON-NLS-2$
		}
		// end
		w.println("</" + SiteLocalParser.SITE + ">"); //$NON-NLS-1$ //$NON-NLS-2$

		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
			UpdateManagerPlugin.getPlugin().debug("Saved change stamp:" + changeStamp);
			//$NON-NLS-1$
		}

	}

	/**
	 * @since 2.0
	 */
	private void writeConfig(
		String gap,
		PrintWriter w,
		InstallConfigurationModel config) {
		w.print(gap + "<" + SiteLocalParser.CONFIG + " "); //$NON-NLS-1$ //$NON-NLS-2$
		String URLInfoString =
			UpdateManagerUtils.getURLAsString(getLocationURL(), config.getURL());
		w.print("url=\"" + Writer.xmlSafe(URLInfoString) + "\" ");
		//$NON-NLS-1$ //$NON-NLS-2$

		if (config.getLabel() != null) {
			w.print("label=\"" + Writer.xmlSafe(config.getLabel()) + "\"");
			//$NON-NLS-1$ //$NON-NLS-2$
		}

		w.println("/>"); //$NON-NLS-1$
	}

	/**
	 * @since 2.0
	 */
	private IInstallConfiguration cloneConfigurationSite(
		IInstallConfiguration installConfig,
		URL newFile,
		String name)
		throws CoreException {

		// save previous current configuration
		if (getCurrentConfiguration() != null)
			((InstallConfiguration) getCurrentConfiguration()).saveConfigurationFile(
				isTransient());

		InstallConfiguration result = null;
		Date currentDate = new Date();

		String newFileName =
			UpdateManagerUtils.getLocalRandomIdentifier(DEFAULT_CONFIG_FILE, currentDate);
		try {
			if (newFile == null)
				newFile = UpdateManagerUtils.getURL(getLocationURL(), newFileName, null);
			// pass the date onto the name
			if (name == null)
				name = Utilities.format(currentDate);
			result = new InstallConfiguration(installConfig, newFile, name);
			// set teh same date in the installConfig
			result.setCreationDate(currentDate);
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteLocal.UnableToCreateURLFor") + newFileName,
				e);
			//$NON-NLS-1$
		}
		return result;
	}

	/**
	 * @since 2.0
	 */
	public IInstallConfiguration cloneCurrentConfiguration() throws CoreException {
		return cloneConfigurationSite(getCurrentConfiguration(), null, null);
	}

	/**
	 * @since 2.0
	 */
	public void revertTo(
		IInstallConfiguration configuration,
		IProgressMonitor monitor,
		IProblemHandler handler)
		throws CoreException {

		// create the activity 
		//Start UOW ?
		ConfigurationActivity activity =
			new ConfigurationActivity(IActivity.ACTION_REVERT);
		activity.setLabel(configuration.getLabel());
		activity.setDate(new Date());
		IInstallConfiguration newConfiguration = null;

		try {
			// create a configuration
			newConfiguration = cloneCurrentConfiguration();
			newConfiguration.setLabel(configuration.getLabel());

			// process delta
			// the Configured featuresConfigured are the same as the old configuration
			// the unconfigured featuresConfigured are the rest...
			((InstallConfiguration) newConfiguration).revertTo(
				configuration,
				monitor,
				handler);

			// add to the stack which will set up as current
			addConfiguration(newConfiguration);

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
				((InstallConfiguration) newConfiguration).addActivityModel(
					(ConfigurationActivityModel) activity);
		}

	}

	/**
	 * @since 2.0
	 */
	public void addToPreservedConfigurations(IInstallConfiguration configuration)
		throws CoreException {
		if (configuration != null) {

			// create new configuration based on the one to preserve
			InstallConfiguration newConfiguration = null;
			String newFileName =
				UpdateManagerUtils.getLocalRandomIdentifier(
					DEFAULT_PRESERVED_CONFIG_FILE,
					new Date());
			try {
				URL newFile = UpdateManagerUtils.getURL(getLocationURL(), newFileName, null);
				// pass the date onto teh name
				Date currentDate = configuration.getCreationDate();
				String name = configuration.getLabel();
				newConfiguration = new InstallConfiguration(configuration, newFile, name);
				// set teh same date in the installConfig
				newConfiguration.setCreationDate(currentDate);

			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(
					Policy.bind("SiteLocal.UnableToCreateURLFor") + newFileName,
					e);
				//$NON-NLS-1$
			}

			// activity
			ConfigurationActivity activity =
				new ConfigurationActivity(IActivity.ACTION_ADD_PRESERVED);
			activity.setLabel(configuration.getLabel());
			activity.setDate(new Date());
			activity.setStatus(IActivity.STATUS_OK);
			((InstallConfiguration) newConfiguration).addActivityModel(activity);
			((InstallConfiguration) newConfiguration).saveConfigurationFile(isTransient());

			// add to the list			
			addPreservedInstallConfigurationModel(
				(InstallConfigurationModel) newConfiguration);
		}
	}

	/*
	 * @see ILocalSite#getPreservedConfigurationFor(IInstallConfiguration)
	 */
	public IInstallConfiguration findPreservedConfigurationFor(IInstallConfiguration configuration) {

		// based on time stamp for now
		InstallConfigurationModel preservedConfig = null;
		if (configuration != null) {
			InstallConfigurationModel[] preservedConfigurations =
				getPreservedConfigurationsModel();
			if (preservedConfigurations != null) {
				for (int indexPreserved = 0;
					indexPreserved < preservedConfigurations.length;
					indexPreserved++) {
					if (configuration
						.getCreationDate()
						.equals(preservedConfigurations[indexPreserved].getCreationDate())) {
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
		if (
			removePreservedConfigurationModel((InstallConfigurationModel) configuration))
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

	/*
	 * Reconciliation is the comparison between the old preserved state and the new one from platform.cfg
	 * 
	 * If the old state contained sites that are not in the new state, the old sites are not added to the state
	 * 
	 * If the new state contains sites that were not in the old state, configure the site and configure all the found features
	 * 
	 * If the sites are in both states, verify the features
	 * 
	 * if the old site contained features that are not in the new site, the features are not added to the site
	 * 
	 * if the new site contains feature that were not in the old site, configure the new feature
	 * 
	 * if the feature is in both site (old and new), use old feature state
	 * 
	 * When adding a feature to a site, we will check if the feature is broken or not. 
	 * A feature is broken when at least one of its plugin is not installed on the site.
	 * 
	 * At the end, go over all the site, get the configured features and make sure that if we find duplicates
	 * only one feature is configured
	 */
	public void reconcile() throws CoreException {

		IPlatformConfiguration platformConfig =
			BootLoader.getCurrentPlatformConfiguration();
		IPlatformConfiguration.ISiteEntry[] newSiteEntries =
			platformConfig.getConfiguredSites();
		IInstallConfiguration newDefaultConfiguration =
			cloneConfigurationSite(null, null, null);
		IConfiguredSite[] oldConfiguredSites = new IConfiguredSite[0];

		// sites from the current configuration
		if (getCurrentConfiguration() != null)
			oldConfiguredSites = getCurrentConfiguration().getConfiguredSites();

		// check if sites from the platform are new sites or modified sites
		// if they are new add them, if they are modified, compare them with the old
		// one and add them
		for (int siteIndex = 0; siteIndex < newSiteEntries.length; siteIndex++) {
			IPlatformConfiguration.ISiteEntry currentSiteEntry = newSiteEntries[siteIndex];
			URL resolvedURL = resolveSiteEntry(currentSiteEntry);
			boolean found = false;
			IConfiguredSite currentConfigurationSite = null;

			// check if SiteEntry has been possibly modified
			// if it was part of the previously known configuredSite
			for (int index = 0; index < oldConfiguredSites.length && !found; index++) {
				currentConfigurationSite = oldConfiguredSites[index];
				if (currentConfigurationSite.getSite().getURL().equals(resolvedURL)) {
					found = true;
					ConfiguredSite reconciledConfiguredSite = reconcile(currentConfigurationSite);
					reconciledConfiguredSite.setPreviousPluginPath(
						currentSiteEntry.getSitePolicy().getList());
					newDefaultConfiguration.addConfiguredSite(reconciledConfiguredSite);
				}
			}

			// old site not found, this is a new site, create it
			if (!found) {
				ISite site = SiteManager.getSite(resolvedURL);

				//site policy
				IPlatformConfiguration.ISitePolicy sitePolicy =
					currentSiteEntry.getSitePolicy();
				ConfiguredSite configSite =
					(ConfiguredSite) new BaseSiteLocalFactory().createConfigurationSiteModel(
						(SiteModel) site,
						sitePolicy.getType());
				configSite.setPlatformURLString(currentSiteEntry.getURL().toExternalForm());
				configSite.setPreviousPluginPath(currentSiteEntry.getSitePolicy().getList());

				//the site may not be read-write
				configSite.isUpdatable(newSiteEntries[siteIndex].isUpdateable());

				// Add the features as configured
				IFeatureReference[] newFeaturesRef = site.getFeatureReferences();
				for (int i = 0; i < newFeaturesRef.length; i++) {
					FeatureReferenceModel newFeatureRefModel =
						(FeatureReferenceModel) newFeaturesRef[i];
					configSite.getConfigurationPolicy().addConfiguredFeatureReference(
						newFeatureRefModel);
				}

				newDefaultConfiguration.addConfiguredSite(configSite);
			}
		}

		// verify we do not have 2 features with different version that
		// are configured
		checkConfiguredFeatures(newDefaultConfiguration);

		// add Activity reconciliation
		BaseSiteLocalFactory siteLocalFactory = new BaseSiteLocalFactory();
		ConfigurationActivityModel activity =
			siteLocalFactory.createConfigurationAcivityModel();
		activity.setAction(IActivity.ACTION_RECONCILIATION);
		activity.setDate(new Date());
		activity.setLabel(getLocationURLString());
		((InstallConfiguration) newDefaultConfiguration).addActivityModel(activity);

		// add the configuration as the currentConfig
		this.addConfiguration(newDefaultConfiguration);
		this.save();
	}

	/**
	 * 
	 */
	private URL resolveSiteEntry(IPlatformConfiguration.ISiteEntry newSiteEntry)
		throws CoreException {
		URL resolvedURL = null;
		try {
			resolvedURL = Platform.resolve(newSiteEntry.getURL());
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind(
					"SiteLocal.UnableToResolve",
					newSiteEntry.getURL().toExternalForm()),
				e);
			//$NON-NLS-1$
		}
		return resolvedURL;
	}

	/**
	 * Compare the old state of ConfiguredSite with
	 * the 'real' features we found in Site
	 * 
	 * getSite of ConfiguredSite contains the real features found
	 * 
	 * So if ConfiguredSite.getPolicy has feature A and D as configured and C as unconfigured
	 * And if the Site contains features A,B and C
	 * We have to remove D and Configure B
	 * 
	 * We copy the oldConfig without the Features
	 * Then we loop through the features we found on teh real site
	 * If they didn't exist before we add them as configured
	 * Otherwise we use the old policy and add them to teh new configuration site
	 */
	private ConfiguredSite reconcile(IConfiguredSite oldConfiguredSite)
		throws CoreException {

		ConfiguredSite newConfiguredSite = createNewConfigSite(oldConfiguredSite);
		ConfigurationPolicy newSitePolicy = newConfiguredSite.getConfigurationPolicy();
		ConfigurationPolicy oldSitePolicy =
			((ConfiguredSite) oldConfiguredSite).getConfigurationPolicy();

		// check the Features that are still on the new version of the Config Site
		// and the new one. Add the new Features as Configured
		List toCheck = new ArrayList();
		ISite site = oldConfiguredSite.getSite();
		IFeatureReference[] foundFeatures = site.getFeatureReferences();
		IFeatureReference[] oldConfiguredFeaturesRef =
			oldConfiguredSite.getFeatureReferences();

		for (int i = 0; i < foundFeatures.length; i++) {
			boolean newFeatureFound = false;
			FeatureReferenceModel currentFeatureRefModel =
				(FeatureReferenceModel) foundFeatures[i];
			for (int j = 0; j < oldConfiguredFeaturesRef.length; j++) {
				IFeatureReference oldFeatureRef = oldConfiguredFeaturesRef[j];
				if (oldFeatureRef != null && oldFeatureRef.equals(currentFeatureRefModel)) {
					toCheck.add(oldFeatureRef);
					newFeatureFound = true;
				}
			}

			// new fature found: add as configured if site is Exclude
			// otherwise add as unconfigured (bug 13695)
			if (!newFeatureFound) {
				if (oldSitePolicy.getPolicy()
					== IPlatformConfiguration.ISitePolicy.USER_EXCLUDE) {
					newSitePolicy.addConfiguredFeatureReference(currentFeatureRefModel);
				} else {
					newSitePolicy.addUnconfiguredFeatureReference(currentFeatureRefModel);
				}
			}
		}

		// if a feature has been found in new and old state use old state (configured/unconfigured)
		Iterator featureIter = toCheck.iterator();
		while (featureIter.hasNext()) {
			IFeatureReference oldFeatureRef = (IFeatureReference) featureIter.next();
			if (oldSitePolicy.isConfigured(oldFeatureRef)) {
				newSitePolicy.addConfiguredFeatureReference(oldFeatureRef);
			} else {
				newSitePolicy.addUnconfiguredFeatureReference(oldFeatureRef);
			}
		}

		return newConfiguredSite;
	}

	/**
	 * Validate we have only one configured feature across the different sites
	 * even if we found multiples
	 * 
	 * If we find 2 features, the one with a higher version is configured
	 * If they have teh same version, the first feature is configured
	 * 
	 * This is a double loop comparison
	 * One look goes from 0 to numberOfConfiguredSites -1
	 * the other from the previous index to numberOfConfiguredSites
	 * 
	 */
	private void checkConfiguredFeatures(IInstallConfiguration newDefaultConfiguration)
		throws CoreException {

		IConfiguredSite[] configuredSites =
			newDefaultConfiguration.getConfiguredSites();

		// each configured site
		for (int indexConfiguredSites = 0;
			indexConfiguredSites < configuredSites.length;
			indexConfiguredSites++) {
			checkConfiguredFeatures(configuredSites[indexConfiguredSites]);
		}

		// Check configured sites between them
		if (configuredSites.length > 1) {
			for (int indexConfiguredSites = 0;
				indexConfiguredSites < configuredSites.length - 1;
				indexConfiguredSites++) {
				IFeatureReference[] configuredFeatures =
					configuredSites[indexConfiguredSites].getConfiguredFeatures();
				for (int indexConfiguredFeatures = 0;
					indexConfiguredFeatures < configuredFeatures.length;
					indexConfiguredFeatures++) {
					IFeatureReference featureToCompare =
						configuredFeatures[indexConfiguredFeatures];

					// compare with the rest of the configurations
					for (int i = indexConfiguredSites + 1; i < configuredSites.length; i++) {
						IFeatureReference[] possibleFeatureReference =
							configuredSites[i].getConfiguredFeatures();
						for (int j = 0; j < possibleFeatureReference.length; j++) {
							int result = compare(featureToCompare, possibleFeatureReference[j]);
							if (result != 0) {
								if (result == 1) {
									FeatureReferenceModel featureRefModel =
										(FeatureReferenceModel) possibleFeatureReference[j];
									((ConfiguredSite) configuredSites[i])
										.getConfigurationPolicy()
										.addUnconfiguredFeatureReference(featureRefModel);
								};
								if (result == 2) {
									FeatureReferenceModel featureRefModel =
										(FeatureReferenceModel) featureToCompare;
									((ConfiguredSite) configuredSites[indexConfiguredSites])
										.getConfigurationPolicy()
										.addUnconfiguredFeatureReference(featureRefModel);
									// do not break, we can continue, even if the feature is unconfigured
									// if we find another feature in another site, we may unconfigure it.
									// but it would have been unconfigured anyway because we confured another version of the same feature
									// so if teh version we find is lower than our version, by transition, it is lower then the feature that unconfigured us
								}
							}
						}
					}
				}

			}
		}
	}

	/**
	 * Validate we have only one configured feature per configured site
	 * 
	 */
	private void checkConfiguredFeatures(IConfiguredSite configuredSite)
		throws CoreException {

		ConfiguredSite cSite = (ConfiguredSite) configuredSite;
		IFeatureReference[] configuredFeatures = cSite.getConfiguredFeatures();
		ConfigurationPolicy cPolicy = cSite.getConfigurationPolicy();

		for (int indexConfiguredFeatures = 0;
			indexConfiguredFeatures < configuredFeatures.length - 1;
			indexConfiguredFeatures++) {

			IFeatureReference featureToCompare =
				configuredFeatures[indexConfiguredFeatures];

			// within the configured site
			// compare with the other configured features of this site
			for (int restOfConfiguredFeatures = indexConfiguredFeatures + 1;
				restOfConfiguredFeatures < configuredFeatures.length;
				restOfConfiguredFeatures++) {
				int result =
					compare(featureToCompare, configuredFeatures[restOfConfiguredFeatures]);
				if (result != 0) {
					if (result == 1) {
						cPolicy.addUnconfiguredFeatureReference(
							(FeatureReferenceModel) configuredFeatures[restOfConfiguredFeatures]);
					};
					if (result == 2) {
						cPolicy.addUnconfiguredFeatureReference(
							(FeatureReferenceModel) featureToCompare);
					}
				}
			}
		}
	}

	/**
	 * compare two feature references
	 * returns 0 if the feature are different
	 * returns 1 if the version of feature 1 is greater than version of feature 2
	 * returns 2 if opposite
	 */
	private int compare(
		IFeatureReference featureRef1,
		IFeatureReference featureRef2)
		throws CoreException {
		if (featureRef1 == null)
			return 0;

		IFeature feature1 = featureRef1.getFeature();
		IFeature feature2 = featureRef2.getFeature();

		if (feature1 == null || feature2 == null) {
			return 0;
		}

		VersionedIdentifier id1 = feature1.getVersionedIdentifier();
		VersionedIdentifier id2 = feature2.getVersionedIdentifier();

		if (id1 == null || id2 == null) {
			return 0;
		}

		if (id1.getIdentifier() != null
			&& id1.getIdentifier().equals(id2.getIdentifier())) {
			PluginVersionIdentifier version1 = id1.getVersion();
			PluginVersionIdentifier version2 = id2.getVersion();
			if (version1 != null) {
				boolean greaterOrEqual = (version1.isGreaterOrEqualTo(version2));
				if (greaterOrEqual) {
					return 1;
				} else {
					return 2;
				}
			} else {
				return 2;
			}
		}
		return 0;
	};

	/**
	 * Add the list of plugins the platform found for each site. This list will be preserved in 
	 * a transient way. 
	 * 
	 * We do not lose explicitly set plugins found in platform.cfg.
	 */
	private void preserveRuntimePluginPath() throws CoreException {

		IPlatformConfiguration platformConfig =
			BootLoader.getCurrentPlatformConfiguration();
		IPlatformConfiguration.ISiteEntry[] siteEntries =
			platformConfig.getConfiguredSites();

		// sites from the current configuration
		IConfiguredSite[] configured = new IConfiguredSite[0];
		if (getCurrentConfiguration() != null)
			configured = getCurrentConfiguration().getConfiguredSites();

		// sites from the platform			
		for (int siteIndex = 0; siteIndex < siteEntries.length; siteIndex++) {
			URL resolvedURL = resolveSiteEntry(siteEntries[siteIndex]);

			boolean found = false;
			for (int index = 0; index < configured.length && !found; index++) {

				// the array may have hole as we set found site to null
				if (configured[index] != null) {
					if (configured[index].getSite().getURL().equals(resolvedURL)) {
						found = true;
						String[] listOfPlugins = siteEntries[siteIndex].getSitePolicy().getList();
						((ConfiguredSite) configured[index]).setPreviousPluginPath(listOfPlugins);
						configured[index] = null;
					}
				}
			}
		}

	}

	private ConfiguredSite createNewConfigSite(IConfiguredSite oldConfiguredSiteToReconcile)
		throws CoreException {
		// create a copy of the ConfigSite based on old ConfigSite
		// this is not a clone, do not copy any features
		ConfiguredSite cSiteToReconcile = (ConfiguredSite) oldConfiguredSiteToReconcile;
		SiteModel siteModel = cSiteToReconcile.getSiteModel();
		int policy = cSiteToReconcile.getConfigurationPolicy().getPolicy();

		// copy values of the old ConfigSite that should be preserved except Features
		ConfiguredSite newConfigurationSite =
			(ConfiguredSite) new BaseSiteLocalFactory().createConfigurationSiteModel(
				siteModel,
				policy);
		newConfigurationSite.isUpdatable(cSiteToReconcile.isUpdatable());
		newConfigurationSite.setPlatformURLString(
			cSiteToReconcile.getPlatformURLString());

		return newConfigurationSite;
	}

	/*
	 * Get update state location relative to platform configuration
	 */
	private static URL getUpdateStateLocation(IPlatformConfiguration config)
		throws IOException {
		// Create a directory location for update state files. This
		// directory name is constructed by adding a well-known suffix
		// to the name of the corresponding platform  configuration. This
		// way, we can have multiple platform configuration files in
		// the same directory without ending up with update state conflicts.
		// For example: platform configuration file:C:/platform.cfg results
		// in update state location file:C:/platform.cfg.update/
		URL configLocation = Platform.resolve(config.getConfigurationLocation());
		String temp = configLocation.toExternalForm();
		temp += UPDATE_STATE_SUFFIX + "/";
		URL updateLocation = new URL(temp);
		if ("file".equalsIgnoreCase(updateLocation.getProtocol())) {
			// ensure path exists. Handle transient configurations
			ArrayList list = new ArrayList();
			File path = new File(updateLocation.getFile());
			while (path != null) { // walk up to first dir that exists
				if (!path.exists()) {
					list.add(path);
					path = path.getParentFile();
				} else
					path = null;
			}
			for (int i = list.size() - 1;
				i >= 0;
				i--) { // walk down to create missing dirs
				path = (File) list.get(i);
				path.mkdir();
				if (config.isTransient())
					path.deleteOnExit();
			}
		}
		return updateLocation;
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
	 * if we are unable to parse the SiteLoca we will attempt to parse the file system
	 */
	private static void recoverSiteLocal(URL url, SiteLocal site)
		throws CoreException, MalformedURLException {

		if (url == null)
			throw Utilities.newCoreException(
				Policy.bind("SiteLocal.SiteUrlIsNull"), //$NON-NLS-1$
				null);

		// parse site information
		site.setLabel(url.toExternalForm());

		//stamp
		long stamp = 0L;
		site.setStamp(stamp);

		// retrieve XML files
		File localXml = new File(url.getFile());
		File dir = localXml.getParentFile();
		File[] configFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith("Config") && name.endsWith("xml"));
			}
		});
		File[] preservedFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith("PreservedConfig") && name.endsWith("xml"));
			}
		});

		// history
		int history = 0;
		if (configFiles != null && configFiles.length > 0) {
			history = configFiles.length;
		} else {
			history = SiteLocalModel.DEFAULT_HISTORY;
		}
		site.setMaximumHistoryCount(history);

		// parse configuration information

		for (int i = 0; i < configFiles.length; i++) {
			URL configURL = configFiles[i].toURL();
			InstallConfigurationModel config =
				new BaseSiteLocalFactory().createInstallConfigurationModel();
			config.setLocationURLString(configURL.toExternalForm());
			config.resolve(configURL, getResourceBundle(url));
			try {
				config.initialize();
				config.setLabel(Utilities.format(config.getCreationDate()));
				site.addConfigurationModel(config);
			} catch (CoreException e) {
				UpdateManagerPlugin.getPlugin().getLog().log(e.getStatus());
			}

		}

		// parse preserved configuration information
		for (int i = 0; i < preservedFiles.length; i++) {
			URL configURL = configFiles[i].toURL();
			InstallConfigurationModel config =
				new BaseSiteLocalFactory().createInstallConfigurationModel();
			config.setLocationURLString(configURL.toExternalForm());
			config.resolve(configURL, getResourceBundle(url));
			try {
				config.initialize();
				config.setLabel(Utilities.format(config.getCreationDate()));
				site.addPreservedInstallConfigurationModel(config);
			} catch (CoreException e) {
				UpdateManagerPlugin.getPlugin().getLog().log(e.getStatus());
			}
		}
	}

	/**
	 * return the appropriate resource bundle for this sitelocal
	 */
	private static ResourceBundle getResourceBundle(URL url) throws CoreException {
		ResourceBundle bundle = null;
		try {
			ClassLoader l = new URLClassLoader(new URL[] { url }, null);
			bundle =
				ResourceBundle.getBundle(
					SiteLocalModel.SITE_LOCAL_FILE,
					Locale.getDefault(),
					l);
		} catch (MissingResourceException e) {
			//ok, there is no bundle, keep it as null
			//DEBUG:
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
				UpdateManagerPlugin.getPlugin().debug(
					e.getLocalizedMessage() + ":" + url.toExternalForm());
				//$NON-NLS-1$
			}
		}
		return bundle;
	}

}