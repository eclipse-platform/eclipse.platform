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
package org.eclipse.update.internal.configurator;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.internal.boot.*;
import org.eclipse.update.configurator.*;

public class PlatformConfiguration implements IPlatformConfiguration {

	private static PlatformConfiguration currentPlatformConfiguration = null;

	private URL configLocation;
	private HashMap sites;
	private HashMap externalLinkSites; // used to restore prior link site state
	private HashMap cfgdFeatures;
	private HashMap bootPlugins;
	private String defaultFeature;
	private long changeStamp;
	private boolean changeStampIsValid = false;
	private long lastFeaturesChangeStamp;
	private long featuresChangeStamp;
	private boolean featuresChangeStampIsValid = false;
	private long lastPluginsChangeStamp;
	private long pluginsChangeStamp;
	private boolean pluginsChangeStampIsValid = false;
	private boolean transientConfig = false;
	private File cfgLockFile;
	private RandomAccessFile cfgLockFileRAF;
	private BootDescriptor runtimeDescriptor;

	private static String cmdFeature = null;
	private static String cmdApplication = null;
	private static boolean cmdInitialize = false;
	private static boolean cmdFirstUse = false;
	private static boolean cmdUpdate = false;
	private static boolean cmdNoUpdate = false;
	private static boolean cmdDev = false;

	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$

	private static final String CONFIG_DIR = ".config"; //$NON-NLS-1$
	private static final String CONFIG_NAME = "platform.cfg"; //$NON-NLS-1$
	private static final String CONFIG_FILE = CONFIG_DIR + "/" + CONFIG_NAME; //$NON-NLS-1$
	private static final String CONFIG_FILE_INIT = "install.ini"; //$NON-NLS-1$
	private static final String CONFIG_INI = "config.ini"; //NON-NLS-1$
	private static final String CONFIG_FILE_LOCK_SUFFIX = ".lock"; //$NON-NLS-1$
	private static final String CONFIG_FILE_TEMP_SUFFIX = ".tmp"; //$NON-NLS-1$
	private static final String CONFIG_FILE_BAK_SUFFIX = ".bak"; //$NON-NLS-1$
	private static final String CHANGES_MARKER = ".newupdates"; //$NON-NLS-1$
	private static final String LINKS = "links"; //$NON-NLS-1$

	private static final String RUNTIME_PLUGIN_ID = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String[] BOOTSTRAP_PLUGINS = { "org.eclipse.core.boot" }; //$NON-NLS-1$
	private static final String CFG_BOOT_PLUGIN = "bootstrap"; //$NON-NLS-1$
	private static final String CFG_SITE = "site"; //$NON-NLS-1$
	private static final String CFG_URL = "url"; //$NON-NLS-1$
	private static final String CFG_POLICY = "policy"; //$NON-NLS-1$
	private static final String[] CFG_POLICY_TYPE = { "USER-INCLUDE", "USER-EXCLUDE" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String CFG_POLICY_TYPE_UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
	private static final String CFG_LIST = "list"; //$NON-NLS-1$
	private static final String CFG_STAMP = "stamp"; //$NON-NLS-1$
	private static final String CFG_FEATURE_STAMP = "stamp.features"; //$NON-NLS-1$
	private static final String CFG_PLUGIN_STAMP = "stamp.plugins"; //$NON-NLS-1$
	private static final String CFG_UPDATEABLE = "updateable"; //$NON-NLS-1$
	private static final String CFG_LINK_FILE = "linkfile"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY = "feature"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_DEFAULT = "feature.default.id"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_ID = "id"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_PRIMARY = "primary"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_VERSION = "version"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER = "plugin-identifier"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_APPLICATION = "application"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_ROOT = "root"; //$NON-NLS-1$

	private static final String INIT_DEFAULT_FEATURE_ID = "feature.default.id"; //$NON-NLS-1$
	private static final String INIT_DEFAULT_PLUGIN_ID = "feature.default.plugin.id"; //$NON-NLS-1$
	private static final String INIT_DEFAULT_FEATURE_APPLICATION = "feature.default.application"; //$NON-NLS-1$
	private static final String DEFAULT_FEATURE_ID = "org.eclipse.platform"; //$NON-NLS-1$
	private static final String DEFAULT_FEATURE_APPLICATION = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

	private static final String CFG_VERSION = "version"; //$NON-NLS-1$
	private static final String CFG_TRANSIENT = "transient"; //$NON-NLS-1$
	private static final String VERSION = "2.1"; //$NON-NLS-1$
	private static final String EOF = "eof"; //$NON-NLS-1$
	private static final int CFG_LIST_LENGTH = 10;

	private static final int DEFAULT_POLICY_TYPE = ISitePolicy.USER_EXCLUDE;
	private static final String[] DEFAULT_POLICY_LIST = new String[0];

	private static final String LINK_PATH = "path"; //$NON-NLS-1$
	private static final String LINK_READ = "r"; //$NON-NLS-1$
	private static final String LINK_READ_WRITE = "rw"; //$NON-NLS-1$

	private static final String CMD_FEATURE = "-feature"; //$NON-NLS-1$
	private static final String CMD_APPLICATION = "-application"; //$NON-NLS-1$
	private static final String CMD_PLUGINS = "-plugins"; //$NON-NLS-1$
	private static final String CMD_UPDATE = "-update"; //$NON-NLS-1$
	private static final String CMD_INITIALIZE = "-initialize"; //$NON-NLS-1$
	private static final String CMD_FIRSTUSE = "-firstuse"; //$NON-NLS-1$
	private static final String CMD_NO_UPDATE = "-noupdate"; //$NON-NLS-1$
	private static final String CMD_NEW_UPDATES = "-newUpdates"; //$NON-NLS-1$
	private static final String CMD_DEV = "-dev"; // triggers -noupdate //$NON-NLS-1$

	public static final String RECONCILER_APP = "org.eclipse.update.core.reconciler"; //$NON-NLS-1$

	private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private static URL installURL;
	

	private PlatformConfiguration(String configPath) throws IOException {
		this.sites = new HashMap();
		this.externalLinkSites = new HashMap();
		this.cfgdFeatures = new HashMap();
		this.bootPlugins = new HashMap();

		// initialize configuration
		initializeCurrent(configPath);

		// pick up any first-time default settings (relative to install location)
		loadInitializationAttributes();

		// Detect external links. These are "soft link" to additional sites. The link
		// files are usually provided by external installation programs. They are located
		// relative to this configuration URL.
		configureExternalLinks();

		// Validate sites in the configuration. Causes any sites that do not exist to
		// be removed from the configuration
		validateSites();

		// compute differences between configuration and actual content of the sites
		// (base sites and link sites)
		computeChangeStamp();

		// determine which plugins we will use to start the rest of the "kernel"
		// (need to get core.runtime matching the executing core.boot and
		// xerces matching the selected core.runtime)
		//		locateDefaultPlugins();
	}

	PlatformConfiguration(URL url) throws IOException {
		this.sites = new HashMap();
		this.externalLinkSites = new HashMap();
		this.cfgdFeatures = new HashMap();
		this.bootPlugins = new HashMap();
		initialize(url);
	}

	/*
	 * @see IPlatformConfiguration#createSiteEntry(URL, ISitePolicy)
	 */
	public ISiteEntry createSiteEntry(URL url, ISitePolicy policy) {
		return new SiteEntry(url, policy, this);
	}

	/*
	 * @see IPlatformConfiguration#createSitePolicy(int, String[])
	 */
	public ISitePolicy createSitePolicy(int type, String[] list) {
		return new SitePolicy(type, list);
	}

	/*
	 * @see IPlatformConfiguration#createFeatureEntry(String, String, String, boolean, String, URL)
	 */
	public IFeatureEntry createFeatureEntry(String id, String version, String pluginVersion, boolean primary, String application, URL[] root) {
		return new FeatureEntry(id, version, pluginVersion, primary, application, root);
	}

	/*
	 * @see IPlatformConfiguration#createFeatureEntry(String, String, String,
	 * String, boolean, String, URL)
	 */
	public IFeatureEntry createFeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
		return new FeatureEntry(id, version, pluginIdentifier, pluginVersion, primary, application, root);
	}

	/*
	 * @see IPlatformConfiguration#configureSite(ISiteEntry)
	 */
	public void configureSite(ISiteEntry entry) {
		configureSite(entry, false);
	}

	/*
	 * @see IPlatformConfiguration#configureSite(ISiteEntry, boolean)
	 */
	public synchronized void configureSite(ISiteEntry entry, boolean replace) {

		if (entry == null)
			return;

		URL url = entry.getURL();
		if (url == null)
			return;
		String key = url.toExternalForm();

		if (sites.containsKey(key) && !replace)
			return;

		sites.put(key, entry);
	}

	/*
	 * @see IPlatformConfiguration#unconfigureSite(ISiteEntry)
	 */
	public synchronized void unconfigureSite(ISiteEntry entry) {
		if (entry == null)
			return;

		URL url = entry.getURL();
		if (url == null)
			return;
		String key = url.toExternalForm();

		sites.remove(key);
	}

	/*
	 * @see IPlatformConfiguration#getConfiguredSites()
	 */
	public ISiteEntry[] getConfiguredSites() {
		if (sites.size() == 0)
			return new ISiteEntry[0];

		return (ISiteEntry[]) sites.values().toArray(new ISiteEntry[0]);
	}

	/*
	 * @see IPlatformConfiguration#findConfiguredSite(URL)
	 */
	public ISiteEntry findConfiguredSite(URL url) {
		if (url == null)
			return null;
		String key = url.toExternalForm();

		ISiteEntry result = (ISiteEntry) sites.get(key);
		try {
			key = URLDecoder.decode(key, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		
		if (result == null) // retry with decoded URL string
			result = (ISiteEntry) sites.get(key);
			
		return result;
	}

	/*
	 * @see IPlatformConfiguration#configureFeatureEntry(IFeatureEntry)
	 */
	public synchronized void configureFeatureEntry(IFeatureEntry entry) {
		if (entry == null)
			return;

		String key = entry.getFeatureIdentifier();
		if (key == null)
			return;

		cfgdFeatures.put(key, entry);
	}

	/*
	 * @see IPlatformConfiguration#unconfigureFeatureEntry(IFeatureEntry)
	 */
	public synchronized void unconfigureFeatureEntry(IFeatureEntry entry) {
		if (entry == null)
			return;

		String key = entry.getFeatureIdentifier();
		if (key == null)
			return;

		cfgdFeatures.remove(key);
	}

	/*
	 * @see IPlatformConfiguration#getConfiguredFeatureEntries()
	 */
	public IFeatureEntry[] getConfiguredFeatureEntries() {
		if (cfgdFeatures.size() == 0)
			return new IFeatureEntry[0];

		return (IFeatureEntry[]) cfgdFeatures.values().toArray(new IFeatureEntry[0]);
	}

	/*
	 * @see IPlatformConfiguration#findConfiguredFeatureEntry(String)
	 */
	public IFeatureEntry findConfiguredFeatureEntry(String id) {
		if (id == null)
			return null;

		return (IFeatureEntry) cfgdFeatures.get(id);
	}

	/*
	 * @see IPlatformConfiguration#getConfigurationLocation()
	 */
	public URL getConfigurationLocation() {
		return configLocation;
	}

	/*
	 * @see IPlatformConfiguration#getChangeStamp()
	 */
	public long getChangeStamp() {
		if (!changeStampIsValid)
			computeChangeStamp();
		return changeStamp;
	}

	/*
	 * @see IPlatformConfiguration#getFeaturesChangeStamp()
	 */
	public long getFeaturesChangeStamp() {
		if (!featuresChangeStampIsValid)
			computeFeaturesChangeStamp();
		return featuresChangeStamp;
	}

	/*
	 * @see IPlatformConfiguration#getPluginsChangeStamp()
	 */
	public long getPluginsChangeStamp() {
		if (!pluginsChangeStampIsValid)
			computePluginsChangeStamp();
		return pluginsChangeStamp;
	}

	/*
	 * @see IPlatformConfiguration#getApplicationIdentifier()
	 */
	public String getApplicationIdentifier() {

		if (cmdInitialize) {
			// we are running post-install initialization. Force
			// running of the reconciler
			return RECONCILER_APP;
		}

		if (featuresChangeStamp != lastFeaturesChangeStamp) {
			// we have detected feature changes ... see if we need to reconcile
			boolean update = !cmdNoUpdate || cmdUpdate;
			if (update)
				return RECONCILER_APP;
		}

		// "normal" startup ... run specified application
		return getApplicationIdentifierInternal();
	}

	private String getApplicationIdentifierInternal() {

		if (cmdApplication != null) // application was specified
			return cmdApplication;
		else {
			// if -feature was not specified use the default feature
			String feature = cmdFeature;
			if (feature == null)
				feature = defaultFeature;

			// lookup application for feature (specified or defaulted)
			if (feature != null) {
				IFeatureEntry fe = findConfiguredFeatureEntry(feature);
				if (fe != null) {
					if (fe.getFeatureApplication() != null)
						return fe.getFeatureApplication();
				}
			}
		}

		// return hardcoded default if we failed
		return DEFAULT_FEATURE_APPLICATION;
	}

	/*
	 * @see IPlatformConfiguration#getPrimaryFeatureIdentifier()
	 */
	public String getPrimaryFeatureIdentifier() {

		if (cmdFeature != null) // -feature was specified on command line
			return cmdFeature;

		// feature was not specified on command line
		if (defaultFeature != null)
			return defaultFeature; // return customized default if set
		else
			return DEFAULT_FEATURE_ID; // return hardcoded default
	}

	/*
	 * @see IPlatformConfiguration#getPluginPath()
	 */
	public URL[] getPluginPath() {
		ArrayList path = new ArrayList();
		Utils.debug("computed plug-in path:"); //$NON-NLS-1$

		ISiteEntry[] sites = getConfiguredSites();
		URL pathURL;
		for (int i = 0; i < sites.length; i++) {
			String[] plugins = sites[i].getPlugins();
			for (int j = 0; j < plugins.length; j++) {
				try {
					pathURL = new URL(((SiteEntry) sites[i]).getResolvedURL(), plugins[j]);
					path.add(pathURL);
					Utils.debug("   " + pathURL.toString()); //$NON-NLS-1$
				} catch (MalformedURLException e) {
					// skip entry ...
					Utils.debug("   bad URL: " + e); //$NON-NLS-1$
				}
			}
		}
		return (URL[]) path.toArray(new URL[0]);
	}

	/*
	 * @see IPlatformConfiguration#getBootstrapPluginIdentifiers()
	 */
	public String[] getBootstrapPluginIdentifiers() {
		return BOOTSTRAP_PLUGINS;
	}

	/*
	 * @see IPlatformConfiguration#setBootstrapPluginLocation(String, URL)
	 */
	public void setBootstrapPluginLocation(String id, URL location) {
		String[] ids = getBootstrapPluginIdentifiers();
		for (int i = 0; i < ids.length; i++) {
			if (ids[i].equals(id)) {
				bootPlugins.put(id, location.toExternalForm());
				break;
			}
		}
	}

	/*
	 * @see IPlatformConfiguration#isUpdateable()
	 */
	public boolean isUpdateable() {
		return true;
	}

	/*
	 * @see IPlatformConfiguration#isTransient()
	 */
	public boolean isTransient() {
		return transientConfig;
	}

	/*
	 * @see IPlatformConfiguration#isTransient(boolean)
	 */
	public void isTransient(boolean value) {
		//		if (this != BootLoader.getCurrentPlatformConfiguration())
		//			transientConfig = value;
	}

	/*
	 * @see IPlatformConfiguration#refresh()
	 */
	public synchronized void refresh() {
		// Reset computed values. Will be lazily refreshed
		// on next access
		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			// reset site entry
			 ((SiteEntry) sites[i]).refresh();
		}
		// reset configuration entry.
		lastFeaturesChangeStamp = featuresChangeStamp;
		lastPluginsChangeStamp = pluginsChangeStamp;
		changeStampIsValid = false;
		featuresChangeStampIsValid = false;
		pluginsChangeStampIsValid = false;
	}

	/*
	 * @see IPlatformConfiguration#save()
	 */
	public void save() throws IOException {
		if (isUpdateable())
			save(configLocation);
	}

	/*
	 * @see IPlatformConfiguration#save(URL)
	 */
	public synchronized void save(URL url) throws IOException {
		if (url == null)
			throw new IOException(Messages.getString("cfig.unableToSave.noURL")); //$NON-NLS-1$

		PrintWriter w = null;
		OutputStream os = null;
		if (!url.getProtocol().equals("file")) { //$NON-NLS-1$
			// not a file protocol - attempt to save to the URL
			URLConnection uc = url.openConnection();
			uc.setDoOutput(true);
			os = uc.getOutputStream();
			w = new PrintWriter(os);
			try {
				write(w);
			} finally {
				w.close();
			}
		} else {
			// file protocol - do safe i/o
			File cfigFile = new File(url.getFile().replace('/', File.separatorChar));
			File cfigDir = cfigFile.getParentFile();
			if (cfigDir != null)
				cfigDir.mkdirs();

			// If config.ini does not exist, generate it
			writeConfigIni(cfigDir);
			
			// first save the file as temp
			File cfigTmp = new File(cfigFile.getAbsolutePath() + CONFIG_FILE_TEMP_SUFFIX);
			os = new FileOutputStream(cfigTmp);
			w = new PrintWriter(os);
			try {
				write(w);
			} finally {
				w.close();
			}

			// make sure we actually succeeded saving the whole configuration.
			InputStream is = new FileInputStream(cfigTmp);
			Properties tmpProps = new Properties();
			try {
				tmpProps.load(is);
				if (!EOF.equals(tmpProps.getProperty(EOF))) {
					throw new IOException(Messages.getString("cfig.unableToSave", cfigTmp.getAbsolutePath())); //$NON-NLS-1$
				}
			} finally {
				is.close();
			}

			// make the saved config the "active" one
			File cfigBak = new File(cfigFile.getAbsolutePath() + CONFIG_FILE_BAK_SUFFIX);
			cfigBak.delete(); // may have old .bak due to prior failure

			if (cfigFile.exists())
				cfigFile.renameTo(cfigBak);

			// at this point we have old config (if existed) as "bak" and the
			// new config as "tmp".
			boolean ok = cfigTmp.renameTo(cfigFile);
			if (ok) {
				// at this point we have the new config "activated", and the old
				// config (if it existed) as "bak"
				cfigBak.delete(); // clean up
			} else {
				// this codepath represents a tiny failure window. The load processing
				// on startup will detect missing config and will attempt to start
				// with "tmp" (latest), then "bak" (the previous). We can also end up
				// here if we failed to rename the current config to "bak". In that
				// case we will restart with the previous state.
				throw new IOException(Messages.getString("cfig.unableToSave", cfigTmp.getAbsolutePath())); //$NON-NLS-1$
			}
		}
	}
	
	private void writeConfigIni(File configDir) {
		try {
			File configIni = new File(configDir, CONFIG_INI);
			if (!configIni.exists()) {
				URL configIniURL = ConfigurationActivator.getBundleContext().getBundle().getEntry(CONFIG_INI);
				File sourceConfigIni = new File(configIniURL.getFile());
				copy(sourceConfigIni, configIni);
			}
		} catch (IOException e) {
			System.out.println(Messages.getString("cfg.unableToCreateConfig.ini"));
		}
	}

	public BootDescriptor getPluginBootDescriptor(String id) {
		// return the plugin descriptor for the specified plugin. This method
		// is used during boot processing to obtain information about "kernel" plugins
		// whose class loaders must be created prior to the plugin registry being
		// available (ie. loaders needed to create the plugin registry).

		if (RUNTIME_PLUGIN_ID.equals(id))
			return runtimeDescriptor;
		else
			return null;
	}

	public static PlatformConfiguration getCurrent() {
		return currentPlatformConfiguration;
	}

	/**
	 * Create and initialize the current platform configuration
	 * @param cmdArgs command line arguments (startup and boot arguments are
	 * already consumed)
	 * @param r10apps application identifies as passed on the BootLoader.run(...)
	 * method. Supported for R1.0 compatibility.
	 */
	public static synchronized String[] startup(String[] cmdArgs, String r10app, URL installURL, String configPath) throws Exception {
		PlatformConfiguration.installURL = installURL;

		cmdApplication = r10app; // R1.0 compatibility

		// process command line arguments
		String[] passthruArgs = processCommandLine(cmdArgs);
		if (cmdDev)
			cmdNoUpdate = true; // force -noupdate when in dev mode (eg. PDE)

		// create current configuration
		if (currentPlatformConfiguration == null)
			currentPlatformConfiguration = new PlatformConfiguration(configPath);

		// check if we will be forcing reconciliation
		passthruArgs = checkForFeatureChanges(passthruArgs, currentPlatformConfiguration);

		// check if we should indicate new changes
		passthruArgs = checkForNewUpdates(currentPlatformConfiguration, passthruArgs);

		return passthruArgs;
	}

	public static synchronized void shutdown() throws IOException {

		// save platform configuration
		PlatformConfiguration config = getCurrent();
		if (config != null) {
			try {
				config.save();
			} catch (IOException e) {
				Utils.debug("Unable to save configuration " + e.toString()); //$NON-NLS-1$
				// will recover on next startup
			}
			config.clearConfigurationLock();
		}
	}

	private synchronized void initializeCurrent(String configPath) throws IOException {
		// FIXME: commented out for now. Remove if not needed.
		//boolean concurrentUse = false;

		if (cmdInitialize) {
			// we are running post-install initialization (-install command
			// line argument). Ignore any configuration URL passed in.
			// Force the configuration to be saved in the install location.
			// Allow an existing configuration to be re-initialized.
			URL url = new URL(getInstallURL(), CONFIG_FILE); // if we fail here, return exception
			// FIXME: commented out for now. Remove if not needed. 
			// I left the call to #getConfigurationLock in just in case
			// calling it has useful side effect. If not, then it can be removed too.
			//concurrentUse = getConfigurationLock(url);
			getConfigurationLock(url);

			resetInitializationConfiguration(url); // [20111]

			configureSite(getRootSite());
			Utils.debug("Initializing configuration " + url.toString()); //$NON-NLS-1$
			configLocation = url;
			verifyPath(configLocation);
			return;
		}

		// Configuration URL was is specified by the OSGi layer. 
		// Default behavior is to look
		// for configuration in the specified meta area. If not found, look
		// for pre-initialized configuration in the installation location.
		// If it is found it is used as the initial configuration. Otherwise
		// a new configuration is created. In either case the resulting
		// configuration is written into the specified configuration area.

		URL configDirURL = new URL("file", "", configPath);
		URL configFileURL = new URL("file", "", configPath + "/" + CONFIG_NAME);
		try {	
			// check concurrent use lock
			// FIXME: might not need this method call.
			getConfigurationLock(configFileURL);

			// try loading the configuration
			try {
				load(configFileURL);
				Utils.debug("Using configuration " + configFileURL.toString()); //$NON-NLS-1$
			} catch (IOException e) {
				// failed to load, see if we can find pre-initialized configuration.
				// Don't attempt this initialization when self-hosting (is unpredictable)
				try {
					URL sharedConfigDirURL = new URL(getInstallURL(), CONFIG_DIR);
					URL sharedConfigFileURL = new URL(getInstallURL(), CONFIG_FILE);

					load(sharedConfigFileURL);
					
					// pre-initialized config loaded OK ... copy any remaining update metadata
					// Only copy if the default config location is not the install location
					if (!sharedConfigDirURL.equals(configDirURL)) {
						copyInitializedState(sharedConfigDirURL, configPath);
						Utils.debug("Configuration initialized from    " + sharedConfigDirURL.toString()); //$NON-NLS-1$
					}
					return;
				} catch (IOException ioe) {
					cmdFirstUse = true;
					// we are creating new configuration
					configureSite(getRootSite());
				}
			}
		} finally {
			configLocation = configFileURL;
			verifyPath(configLocation);
			Utils.debug("Creating configuration " + configFileURL.toString()); //$NON-NLS-1$
		}
	}

	private synchronized void initialize(URL url) throws IOException {
		if (url == null) {
			Utils.debug("Creating empty configuration object"); //$NON-NLS-1$
			return;
		}

		load(url);
		configLocation = url;
		Utils.debug("Using configuration " + configLocation.toString()); //$NON-NLS-1$
	}

	private ISiteEntry getRootSite() {
		// create default site entry for the root
		ISitePolicy defaultPolicy = createSitePolicy(DEFAULT_POLICY_TYPE, DEFAULT_POLICY_LIST);
		URL siteURL = null;
		try {
			siteURL = new URL(PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR + "/" + "base" + "/"); //$NON-NLS-1$ //$NON-NLS-2$ // try using platform-relative URL
		} catch (MalformedURLException e) {
			siteURL = getInstallURL(); // ensure we come up ... use absolute file URL
		}
		ISiteEntry defaultSite = createSiteEntry(siteURL, defaultPolicy);
		return defaultSite;
	}

	private void resetInitializationConfiguration(URL url) throws IOException {
		// [20111]
		if (!supportsDetection(url))
			return; // can't do ...

		URL resolved = resolvePlatformURL(url);
		File initCfg = new File(resolved.getFile().replace('/', File.separatorChar));
		File initDir = initCfg.getParentFile();
		resetInitializationLocation(initDir);
	}

	private void resetInitializationLocation(File dir) {
		// [20111]
		if (dir == null || !dir.exists() || !dir.isDirectory())
			return;
		File[] list = dir.listFiles();
		for (int i = 0; i < list.length; i++) {
			if (list[i].isDirectory())
				resetInitializationLocation(list[i]);
			list[i].delete();
		}
	}

	private boolean getConfigurationLock(URL url) {

//		if (!url.getProtocol().equals("file")) //$NON-NLS-1$
//			return false;
//
//		verifyPath(url);
//		String cfgName = url.getFile().replace('/', File.separatorChar);
//		String lockName = cfgName + CONFIG_FILE_LOCK_SUFFIX;
//		cfgLockFile = new File(lockName);
//
//		//if the lock file already exists, try to delete,
//		//assume failure means another eclipse has it open
//		if (cfgLockFile.exists())
//			cfgLockFile.delete();
//		if (cfgLockFile.exists()) {
//			throw new RuntimeException(Policy.bind("cfig.inUse", cfgName, lockName)); //$NON-NLS-1$
//		}
//
//		// OK so far ... open the lock file so other instances will fail
//		try {
//			cfgLockFileRAF = new RandomAccessFile(cfgLockFile, "rw"); //$NON-NLS-1$
//			cfgLockFileRAF.writeByte(0);
//		} catch (IOException e) {
//			throw new RuntimeException(Policy.bind("cfig.failCreateLock", cfgName)); //$NON-NLS-1$
//		}

		return false;
	}

	private void clearConfigurationLock() {
		try {
			if (cfgLockFileRAF != null) {
				cfgLockFileRAF.close();
				cfgLockFileRAF = null;
			}
		} catch (IOException e) {
			// ignore ...
		}
		if (cfgLockFile != null) {
			cfgLockFile.delete();
			cfgLockFile = null;
		}
	}

	private void computeChangeStamp() {
		computeFeaturesChangeStamp();
		computePluginsChangeStamp();
		changeStamp = featuresChangeStamp ^ pluginsChangeStamp;
		changeStampIsValid = true;
	}

	private void computeFeaturesChangeStamp() {
		if (featuresChangeStampIsValid)
			return;

		long result = 0;
		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			result ^= sites[i].getFeaturesChangeStamp();
		}
		featuresChangeStamp = result;
		featuresChangeStampIsValid = true;
	}

	private void computePluginsChangeStamp() {
		if (pluginsChangeStampIsValid)
			return;

		long result = 0;
		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			result ^= sites[i].getPluginsChangeStamp();
		}
		pluginsChangeStamp = result;
		pluginsChangeStampIsValid = true;
	}

	private void configureExternalLinks() {
		URL linkURL = getInstallURL();
		if (!supportsDetection(linkURL))
			return;

		try {
			linkURL = new URL(linkURL, LINKS + "/"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// skip bad links ...
			Utils.debug("Unable to obtain link URL"); //$NON-NLS-1$
			return;
		}

		File linkDir = new File(linkURL.getFile());
		File[] links = linkDir.listFiles();
		if (links == null || links.length == 0) {
			Utils.debug("No links detected in " + linkURL.toExternalForm()); //$NON-NLS-1$
			return;
		}

		for (int i = 0; i < links.length; i++) {
			if (links[i].isDirectory())
				continue;
			Utils.debug("Link file " + links[i].getAbsolutePath()); //$NON-NLS-1$
			Properties props = new Properties();
			FileInputStream is = null;
			try {
				is = new FileInputStream(links[i]);
				props.load(is);
				configureExternalLinkSites(links[i], props);
			} catch (IOException e) {
				// skip bad links ...
				Utils.debug("   unable to load link file " + e); //$NON-NLS-1$
				continue;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// ignore ...
					}
				}
			}
		}
	}

	private void configureExternalLinkSites(File linkFile, Properties props) {
		String path = props.getProperty(LINK_PATH);
		if (path == null) {
			Utils.debug("   no path definition"); //$NON-NLS-1$
			return;
		}

		String link;
		boolean updateable = true;
		URL siteURL;
		SiteEntry linkSite;
		ISitePolicy linkSitePolicy = createSitePolicy(DEFAULT_POLICY_TYPE, DEFAULT_POLICY_LIST);

		// parse out link information
		if (path.startsWith(LINK_READ + " ")) { //$NON-NLS-1$
			updateable = false;
			link = path.substring(2).trim();
		} else if (path.startsWith(LINK_READ_WRITE + " ")) { //$NON-NLS-1$
			link = path.substring(3).trim();
		} else {
			link = path;
		}

		// 	make sure we have a valid link specification
		try {
			if (!link.endsWith(File.separator))
				link += File.separator;
			File target = new File(link + ECLIPSE);
			link = "file:" + target.getAbsolutePath().replace(File.separatorChar, '/'); //$NON-NLS-1$
			if (!link.endsWith("/")) //$NON-NLS-1$
				link += "/"; // sites must be directories //$NON-NLS-1$
			siteURL = new URL(link);
		} catch (MalformedURLException e) {
			// ignore bad links ...
			Utils.debug("  bad URL " + e); //$NON-NLS-1$
			return;
		}

		// process the link
		linkSite = (SiteEntry) externalLinkSites.get(siteURL);
		if (linkSite == null) {
			// this is a link to a new target so create site for it
			linkSite = (SiteEntry) createSiteEntry(siteURL, linkSitePolicy);
		}
		// update site entry if needed
		linkSite.setUpdateable(updateable);
		linkSite.setLinkFileName(linkFile.getAbsolutePath());

		// configure the new site
		// NOTE: duplicates are not replaced (first one in wins)
		configureSite(linkSite);
		Utils.debug("   " + (updateable ? "R/W -> " : "R/O -> ") + siteURL.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void validateSites() {

		// check to see if all sites are valid. Remove any sites that do not exist.
		SiteEntry[] list = (SiteEntry[]) sites.values().toArray(new SiteEntry[0]);
		for (int i = 0; i < list.length; i++) {
			URL siteURL = list[i].getResolvedURL();
			if (!supportsDetection(siteURL))
				continue;

			File siteRoot = new File(siteURL.getFile().replace('/', File.separatorChar));
			if (!siteRoot.exists()) {
				unconfigureSite(list[i]);
				Utils.debug("Site " + siteURL + " does not exist ... removing from configuration"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private void copyInitializedState(URL source, String target) {
		try {
			if (!source.getProtocol().equals("file")) //$NON-NLS-1$
				return; // need to be able to do "dir"

			copy(new File(source.getFile()), new File(target));

		} catch (IOException e) {
			// this is an optimistic copy. If we fail, the state will be reconciled
			// when the update manager is triggered.
		}
	}

	private void copy(File src, File tgt) throws IOException {
		if (src.isDirectory()) {
			// copy content of directories
			tgt.mkdir();
			File[] list = src.listFiles();
			if (list == null)
				return;
			for (int i = 0; i < list.length; i++) {
				copy(list[i], new File(tgt, list[i].getName()));
			}
		} else {
			// copy individual files
			FileInputStream is = null;
			FileOutputStream os = null;
			try {
				is = new FileInputStream(src);
				os = new FileOutputStream(tgt);
				byte[] buff = new byte[1024];
				int count = is.read(buff);
				while (count != -1) {
					os.write(buff, 0, count);
					count = is.read(buff);
				}
			} catch (IOException e) {
				// continue ... update reconciler will have to reconstruct state
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (IOException e) {
						// ignore ...
					}
				if (os != null)
					try {
						os.close();
					} catch (IOException e) {
						// ignore ...
					}
			}
		}
	}

	private void load(URL url) throws IOException {

		if (url == null)
			throw new IOException(Messages.getString("cfig.unableToLoad.noURL")); //$NON-NLS-1$

		// try to load saved configuration file (watch for failed prior save())
		Properties props = null;
		IOException originalException = null;
		try {
			props = loadProperties(url, null); // try to load config file
		} catch (IOException e1) {
			originalException = e1;
			try {
				props = loadProperties(url, CONFIG_FILE_TEMP_SUFFIX); // check for failures on save
			} catch (IOException e2) {
				try {
					props = loadProperties(url, CONFIG_FILE_BAK_SUFFIX); // check for failures on save
				} catch (IOException e3) {
					throw originalException; // we tried, but no config here ...
				}
			}
		}

		// check version
		String v = props.getProperty(CFG_VERSION);
		if (!VERSION.equals(v)) {
			// the state is invalid, delete any files under the directory
			// bug 33493
			resetUpdateManagerState(url);
			throw new IOException(Messages.getString("cfig.badVersion", v)); //$NON-NLS-1$
		}

		// load simple properties
		defaultFeature = loadAttribute(props, CFG_FEATURE_ENTRY_DEFAULT, null);

		String flag = loadAttribute(props, CFG_TRANSIENT, null);
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				transientConfig = true;
			else
				transientConfig = false;
		}

		String stamp = loadAttribute(props, CFG_FEATURE_STAMP, null);
		if (stamp != null) {
			try {
				lastFeaturesChangeStamp = Long.parseLong(stamp);
			} catch (NumberFormatException e) {
				// ignore bad attribute ...
			}
		}

		stamp = loadAttribute(props, CFG_PLUGIN_STAMP, null);
		if (stamp != null) {
			try {
				lastPluginsChangeStamp = Long.parseLong(stamp);
			} catch (NumberFormatException e) {
				// ignore bad attribute ...
			}
		}

		// load bootstrap entries
		String[] ids = getBootstrapPluginIdentifiers();
		for (int i = 0; i < ids.length; i++) {
			bootPlugins.put(ids[i], loadAttribute(props, CFG_BOOT_PLUGIN + "." + ids[i], null)); //$NON-NLS-1$
		}

		// load feature entries
		IFeatureEntry fe = loadFeatureEntry(props, CFG_FEATURE_ENTRY + ".0", null); //$NON-NLS-1$
		for (int i = 1; fe != null; i++) {
			configureFeatureEntry(fe);
			fe = loadFeatureEntry(props, CFG_FEATURE_ENTRY + "." + i, null); //$NON-NLS-1$
		}

		// load site properties
		SiteEntry root = (SiteEntry) getRootSite();
		String rootUrlString = root.getURL().toExternalForm();
		SiteEntry se = (SiteEntry) loadSite(props, CFG_SITE + ".0", null); //$NON-NLS-1$
		
		for (int i = 1; se != null; i++) {

			// check if we are forcing "first use" processing with an existing
			// platform.cfg. In this case ignore site entry that represents
			// the platform install, and use a root site entry in its place.
			// This ensures we do not get messed up by an exclusion list that
			// is read from the prior state.
			if (cmdFirstUse && rootUrlString.equals(se.getURL().toExternalForm()))
				se = root;

			if (!se.isExternallyLinkedSite())
				configureSite(se);
			else
				// remember external link site state, but do not configure at this point
				externalLinkSites.put(se.getURL(), se);
			se = (SiteEntry) loadSite(props, CFG_SITE + "." + i, null); //$NON-NLS-1$
		}
	}

	private Properties loadProperties(URL url, String suffix) throws IOException {

		// figure out what we will be loading
		if (suffix != null && !suffix.equals("")) //$NON-NLS-1$
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + suffix);

		// try to load saved configuration file
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = url.openStream();
			props.load(is);
			// check to see if we have complete config file
			if (!EOF.equals(props.getProperty(EOF))) {
				throw new IOException(Messages.getString("cfig.unableToLoad.incomplete", url.toString())); //$NON-NLS-1$
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore ...
				}
			}
		}
		return props;
	}

	private ISiteEntry loadSite(Properties props, String name, ISiteEntry dflt) {

		String urlString = loadAttribute(props, name + "." + CFG_URL, null); //$NON-NLS-1$
		if (urlString == null)
			return dflt;

		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			return dflt;
		}

		int policyType;
		String[] policyList;
		String typeString = loadAttribute(props, name + "." + CFG_POLICY, null); //$NON-NLS-1$
		if (typeString == null) {
			policyType = DEFAULT_POLICY_TYPE;
			policyList = DEFAULT_POLICY_LIST;
		} else {
			int i;
			for (i = 0; i < CFG_POLICY_TYPE.length; i++) {
				if (typeString.equals(CFG_POLICY_TYPE[i])) {
					break;
				}
			}
			if (i >= CFG_POLICY_TYPE.length) {
				policyType = DEFAULT_POLICY_TYPE;
				policyList = DEFAULT_POLICY_LIST;
			} else {
				policyType = i;
				policyList = loadListAttribute(props, name + "." + CFG_LIST, new String[0]); //$NON-NLS-1$
			}
		}

		ISitePolicy sp = createSitePolicy(policyType, policyList);
		SiteEntry site = (SiteEntry) createSiteEntry(url, sp);

		String stamp = loadAttribute(props, name + "." + CFG_FEATURE_STAMP, null); //$NON-NLS-1$
		if (stamp != null) {
			try {
				site.setLastFeaturesChangeStamp(Long.parseLong(stamp));
			} catch (NumberFormatException e) {
				// ignore bad attribute ...
			}
		}

		stamp = loadAttribute(props, name + "." + CFG_PLUGIN_STAMP, null); //$NON-NLS-1$
		if (stamp != null) {
			try {
				site.setLastPluginsChangeStamp(Long.parseLong(stamp));
			} catch (NumberFormatException e) {
				// ignore bad attribute ...
			}
		}

		String flag = loadAttribute(props, name + "." + CFG_UPDATEABLE, null); //$NON-NLS-1$
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				site.setUpdateable(true);
			else
				site.setUpdateable(false);
		}

		String linkname = loadAttribute(props, name + "." + CFG_LINK_FILE, null); //$NON-NLS-1$
		if (linkname != null && !linkname.equals("")) { //$NON-NLS-1$
			site.setLinkFileName(linkname.replace('/', File.separatorChar));
		}

		return site;
	}

	private IFeatureEntry loadFeatureEntry(Properties props, String name, IFeatureEntry dflt) {
		String id = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_ID, null); //$NON-NLS-1$
		if (id == null)
			return dflt;
		String version = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_VERSION, null); //$NON-NLS-1$
		String pluginVersion = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_PLUGIN_VERSION, null); //$NON-NLS-1$
		if (pluginVersion == null)
			pluginVersion = version;
		String pluginIdentifier = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER, null); //$NON-NLS-1$
		if (pluginIdentifier == null)
			pluginIdentifier = id;
		String application = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_APPLICATION, null); //$NON-NLS-1$
		ArrayList rootList = new ArrayList();

		// get install locations
		String rootString = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_ROOT + ".0", null); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 1; rootString != null; i++) {
			try {
				URL rootEntry = new URL(rootString);
				rootList.add(rootEntry);
			} catch (MalformedURLException e) {
				// skip bad entries ...
			}
			rootString = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_ROOT + "." + i, null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		URL[] roots = (URL[]) rootList.toArray(new URL[0]);

		// get primary flag
		boolean primary = false;
		String flag = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_PRIMARY, null); //$NON-NLS-1$
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				primary = true;
		}
		return createFeatureEntry(id, version, pluginIdentifier, pluginVersion, primary, application, roots);
	}

	private String[] loadListAttribute(Properties props, String name, String[] dflt) {
		ArrayList list = new ArrayList();
		String value = loadAttribute(props, name + ".0", null); //$NON-NLS-1$
		if (value == null)
			return dflt;

		for (int i = 1; value != null; i++) {
			loadListAttributeSegment(list, value);
			value = loadAttribute(props, name + "." + i, null); //$NON-NLS-1$
		}
		return (String[]) list.toArray(new String[0]);
	}

	private void loadListAttributeSegment(ArrayList list, String value) {

		if (value == null)
			return;

		StringTokenizer tokens = new StringTokenizer(value, ","); //$NON-NLS-1$
		String token;
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.add(token);
		}
		return;
	}

	private String loadAttribute(Properties props, String name, String dflt) {
		String prop = props.getProperty(name);
		if (prop == null)
			return dflt;
		else
			return prop.trim();
	}

	private void loadInitializationAttributes() {

		// look for the product initialization file relative to the install location
		URL url = getInstallURL();

		// load any initialization attributes. These are the default settings for
		// key attributes (eg. default primary feature) supplied by the packaging team.
		// They are always reloaded on startup to pick up any changes due to
		// "native" updates.
		Properties initProps = new Properties();
		InputStream is = null;
		try {
			URL initURL = new URL(url, CONFIG_FILE_INIT);
			is = initURL.openStream();
			initProps.load(is);
			Utils.debug("Defaults from " + initURL.toExternalForm()); //$NON-NLS-1$
		} catch (IOException e) {
			return; // could not load default settings
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore ...
				}
			}
		}

		// use default settings if supplied
		String initId = loadAttribute(initProps, INIT_DEFAULT_FEATURE_ID, null);
		if (initId != null) {
			String application = loadAttribute(initProps, INIT_DEFAULT_FEATURE_APPLICATION, null);
			String initPluginId = loadAttribute(initProps, INIT_DEFAULT_PLUGIN_ID, null);
			if (initPluginId == null)
				initPluginId = initId;
			IFeatureEntry fe = findConfiguredFeatureEntry(initId);

			if (fe == null) {
				// bug 26896 : setup optimistic reconciliation if the primary feature has changed or is new
				cmdFirstUse = true;
				// create entry if not exists
				fe = createFeatureEntry(initId, null, initPluginId, null, true, application, null);
			} else
				// update existing entry with new info
				fe = createFeatureEntry(initId, fe.getFeatureVersion(), fe.getFeaturePluginIdentifier(), fe.getFeaturePluginVersion(), fe.canBePrimary(), application, fe.getFeatureRootURLs());
			configureFeatureEntry(fe);
			defaultFeature = initId;
			if (ConfigurationActivator.DEBUG) {
				Utils.debug("    Default primary feature: " + defaultFeature); //$NON-NLS-1$
				if (application != null)
					Utils.debug("    Default application    : " + application); //$NON-NLS-1$
			}
		}
	}

	private void write(PrintWriter w) {
		// write header
		w.println("# " + (new Date()).toString()); //$NON-NLS-1$
		writeAttribute(w, CFG_VERSION, VERSION);
		if (transientConfig)
			writeAttribute(w, CFG_TRANSIENT, "true"); //$NON-NLS-1$
		w.println(""); //$NON-NLS-1$

		// write global attributes
		writeAttribute(w, CFG_STAMP, Long.toString(getChangeStamp()));
		writeAttribute(w, CFG_FEATURE_STAMP, Long.toString(getFeaturesChangeStamp()));
		writeAttribute(w, CFG_PLUGIN_STAMP, Long.toString(getPluginsChangeStamp()));

		// write out bootstrap entries
		String[] ids = getBootstrapPluginIdentifiers();
		for (int i = 0; i < ids.length; i++) {
			String location = (String) bootPlugins.get(ids[i]);
			if (location != null)
				writeAttribute(w, CFG_BOOT_PLUGIN + "." + ids[i], location); //$NON-NLS-1$
		}

		// write out feature entries
		w.println(""); //$NON-NLS-1$
		writeAttribute(w, CFG_FEATURE_ENTRY_DEFAULT, defaultFeature);
		IFeatureEntry[] feats = getConfiguredFeatureEntries();
		for (int i = 0; i < feats.length; i++) {
			writeFeatureEntry(w, CFG_FEATURE_ENTRY + "." + Integer.toString(i), feats[i]); //$NON-NLS-1$
		}

		// write out site entries
		SiteEntry[] list = (SiteEntry[]) sites.values().toArray(new SiteEntry[0]);
		for (int i = 0; i < list.length; i++) {
			writeSite(w, CFG_SITE + "." + Integer.toString(i), list[i]); //$NON-NLS-1$
		}

		// write end-of-file marker
		writeAttribute(w, EOF, EOF);
	}

	private void writeSite(PrintWriter w, String id, SiteEntry entry) {

		// write site separator
		w.println(""); //$NON-NLS-1$

		// write out site settings
		writeAttribute(w, id + "." + CFG_URL, entry.getURL().toString()); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_STAMP, Long.toString(entry.getChangeStamp())); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_FEATURE_STAMP, Long.toString(entry.getFeaturesChangeStamp())); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_PLUGIN_STAMP, Long.toString(entry.getPluginsChangeStamp())); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_UPDATEABLE, entry.isUpdateable() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (entry.isExternallyLinkedSite()) //$NON-NLS-1$
			writeAttribute(w, id + "." + CFG_LINK_FILE, entry.getLinkFileName().trim().replace(File.separatorChar, '/')); //$NON-NLS-1$

		// write out site policy
		int type = entry.getSitePolicy().getType();
		String typeString = CFG_POLICY_TYPE_UNKNOWN;
		try {
			typeString = CFG_POLICY_TYPE[type];
		} catch (IndexOutOfBoundsException e) {
			// ignore bad attribute ...
		}
		writeAttribute(w, id + "." + CFG_POLICY, typeString); //$NON-NLS-1$
		writeListAttribute(w, id + "." + CFG_LIST, entry.getSitePolicy().getList()); //$NON-NLS-1$
	}

	private void writeFeatureEntry(PrintWriter w, String id, IFeatureEntry entry) {

		// write feature entry separator
		w.println(""); //$NON-NLS-1$

		// write out feature entry settings
		writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_ID, entry.getFeatureIdentifier()); //$NON-NLS-1$
		if (entry.canBePrimary())
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_PRIMARY, "true"); //$NON-NLS-1$ //$NON-NLS-2$
		writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_VERSION, entry.getFeatureVersion()); //$NON-NLS-1$
		if (entry.getFeatureVersion() != null && !entry.getFeatureVersion().equals(entry.getFeaturePluginVersion()))
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_PLUGIN_VERSION, entry.getFeaturePluginVersion()); //$NON-NLS-1$
		if (entry.getFeatureIdentifier() != null && !entry.getFeatureIdentifier().equals(entry.getFeaturePluginIdentifier()))
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER, entry.getFeaturePluginIdentifier()); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_APPLICATION, entry.getFeatureApplication()); //$NON-NLS-1$
		URL[] roots = entry.getFeatureRootURLs();
		for (int i = 0; i < roots.length; i++) {
			// write our as individual attributes (is easier for Main.java to read)
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_ROOT + "." + i, roots[i].toExternalForm()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void writeListAttribute(PrintWriter w, String id, String[] list) {
		if (list == null || list.length == 0)
			return;

		String value = ""; //$NON-NLS-1$
		int listLen = 0;
		int listIndex = 0;
		for (int i = 0; i < list.length; i++) {
			if (listLen != 0)
				value += ","; //$NON-NLS-1$
			else
				value = ""; //$NON-NLS-1$
			value += list[i];

			if (++listLen >= CFG_LIST_LENGTH) {
				writeAttribute(w, id + "." + Integer.toString(listIndex++), value); //$NON-NLS-1$
				listLen = 0;
			}
		}
		if (listLen != 0)
			writeAttribute(w, id + "." + Integer.toString(listIndex), value); //$NON-NLS-1$
	}

	private void writeAttribute(PrintWriter w, String id, String value) {
		if (value == null || value.trim().equals("")) //$NON-NLS-1$
			return;
		w.println(id + "=" + escapedValue(value)); //$NON-NLS-1$
	}

	private String escapedValue(String value) {
		// if required, escape property values as \\uXXXX
		StringBuffer buf = new StringBuffer(value.length() * 2); // assume expansion by less than factor of 2
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (character == '\\' || character == '\t' || character == '\r' || character == '\n' || character == '\f') {
				// handle characters requiring leading \
				buf.append('\\');
				buf.append(character);
			} else if ((character < 0x0020) || (character > 0x007e)) {
				// handle characters outside base range (encoded)
				buf.append('\\');
				buf.append('u');
				buf.append(HEX[(character >> 12) & 0xF]); // first nibble
				buf.append(HEX[(character >> 8) & 0xF]); // second nibble
				buf.append(HEX[(character >> 4) & 0xF]); // third nibble
				buf.append(HEX[character & 0xF]); // fourth nibble
			} else {
				// handle base characters
				buf.append(character);
			}
		}
		return buf.toString();
	}

	private static String[] checkForFeatureChanges(String[] args, PlatformConfiguration cfg) {
		String original = cfg.getApplicationIdentifierInternal();
		String actual = cfg.getApplicationIdentifier();

		if (original.equals(actual))
			// base startup of specified application
			return args;
		else {
			// Will run reconciler.
			// Re-insert -application argument with original app and optionally
			// force "first use" processing
			int newArgCnt = cmdFirstUse ? 3 : 2;
			String[] newArgs = new String[args.length + newArgCnt];
			newArgs[0] = CMD_APPLICATION;
			newArgs[1] = original;
			if (cmdFirstUse)
				newArgs[2] = CMD_FIRSTUSE;
			System.arraycopy(args, 0, newArgs, newArgCnt, args.length);
			Utils.debug("triggering reconciliation ..."); //$NON-NLS-1$
			return newArgs;
		}
	}

	private static String[] checkForNewUpdates(IPlatformConfiguration cfg, String[] args) {
		try {
			URL markerURL = new URL(cfg.getConfigurationLocation(), CHANGES_MARKER);
			File marker = new File(markerURL.getFile());
			if (!marker.exists())
				return args;

			// indicate -newUpdates
			marker.delete();
			String[] newArgs = new String[args.length + 1];
			newArgs[0] = CMD_NEW_UPDATES;
			System.arraycopy(args, 0, newArgs, 1, args.length);
			return newArgs;
		} catch (MalformedURLException e) {
			return args;
		}
	}

	private static String[] processCommandLine(String[] args) throws Exception {
		int[] configArgs = new int[100];
		configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;

			// check for args without parameters (i.e., a flag arg)

			// look for forced "first use" processing (triggered by stale
			// bootstrap information)
			if (args[i].equalsIgnoreCase(CMD_FIRSTUSE)) {
				cmdFirstUse = true;
				found = true;
			}

			// look for the update flag
			if (args[i].equalsIgnoreCase(CMD_UPDATE)) {
				cmdUpdate = true;
				found = true;
			}

			// look for the no-update flag
			if (args[i].equalsIgnoreCase(CMD_NO_UPDATE)) {
				cmdNoUpdate = true;
				found = true;
			}

			// look for the initialization flag
			if (args[i].equalsIgnoreCase(CMD_INITIALIZE)) {
				cmdInitialize = true;
				continue; // do not remove from command line
			}

			// look for the development mode flag ... triggers no-update
			if (args[i].equalsIgnoreCase(CMD_DEV)) {
				cmdDev = true;
				continue; // do not remove from command line
			}

			if (found) {
				configArgs[configArgIndex++] = i;
				continue;
			}

			// check for args with parameters. If we are at the last argument or if the next one
			// has a '-' as the first character, then we can't have an arg with a parm so continue.

			if (i == args.length - 1 || args[i + 1].startsWith("-")) { //$NON-NLS-1$
				continue;
			}

			String arg = args[++i];

			// look for the feature to use for customization.
			if (args[i - 1].equalsIgnoreCase(CMD_FEATURE)) {
				found = true;
				cmdFeature = arg;
			}

			// look for the application to run.  Only use the value from the
			// command line if the application identifier was not explicitly
			// passed on BootLoader.run(...) invocation.
			if (args[i - 1].equalsIgnoreCase(CMD_APPLICATION)) {
				found = true;
				if (cmdApplication == null)
					cmdApplication = arg;
			}

			// done checking for args.  Remember where an arg was found
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}

		// remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0)
			return args;
		String[] passThruArgs = new String[args.length - configArgIndex];
		configArgIndex = 0;
		int j = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex])
				configArgIndex++;
			else
				passThruArgs[j++] = args[i];
		}
		return passThruArgs;
	}

	public static boolean supportsDetection(URL url) {
		String protocol = url.getProtocol();
		if (protocol.equals("file")) //$NON-NLS-1$
			return true;
		else if (protocol.equals(PlatformURLHandler.PROTOCOL)) {
			URL resolved = null;
			try {
				resolved = resolvePlatformURL(url); // 19536
			} catch (IOException e) {
				return false; // we tried but failed to resolve the platform URL
			}
			return resolved.getProtocol().equals("file"); //$NON-NLS-1$
		} else
			return false;
	}

	private static void verifyPath(URL url) {
		String protocol = url.getProtocol();
		String path = null;
		if (protocol.equals("file")) //$NON-NLS-1$
			path = url.getFile();
		else if (protocol.equals(PlatformURLHandler.PROTOCOL)) {
			URL resolved = null;
			try {
				resolved = resolvePlatformURL(url); // 19536
				if (resolved.getProtocol().equals("file")) //$NON-NLS-1$
					path = resolved.getFile();
			} catch (IOException e) {
				// continue ...
			}
		}

		if (path != null) {
			File dir = new File(path).getParentFile();
			if (dir != null)
				dir.mkdirs();
		}
	}

	public static URL resolvePlatformURL(URL url) throws IOException {
		// 19536
		if (url.getProtocol().equals(PlatformURLHandler.PROTOCOL)) {
			URLConnection connection = url.openConnection();
			if (connection instanceof PlatformURLConnection) {
				url = ((PlatformURLConnection) connection).getResolvedURL();
			} else {
				//				connection = new PlatformURLBaseConnection(url);
				//				url = ((PlatformURLConnection)connection).getResolvedURL();
				url = getInstallURL();
			}
		}
		return url;
	}

	private void resetUpdateManagerState(URL url) throws IOException {
		// [20111]
		if (!supportsDetection(url))
			return; // can't do ...

		// find directory where the platform configuration file is	
		URL resolved = resolvePlatformURL(url);
		File initCfg = new File(resolved.getFile().replace('/', File.separatorChar));
		File initDir = initCfg.getParentFile();

		// Find the Update Manager State directory
		if (initDir == null || !initDir.exists() || !initDir.isDirectory())
			return;
		String temp = initCfg.getName() + ".metadata"; //$NON-NLS-1$
		File UMDir = new File(initDir, temp + '/');

		// Attempt to rename it
		if (UMDir == null || !UMDir.exists() || !UMDir.isDirectory())
			return;
		Date now = new Date();
		boolean renamed = UMDir.renameTo(new File(initDir, temp + now.getTime() + '/'));

		if (!renamed)
			resetInitializationLocation(UMDir);
	}

	private static URL getInstallURL() {
		return installURL;
	}
	
	public void invalidateFeaturesChangeStamp() {
		changeStampIsValid = false;
		featuresChangeStampIsValid = false;
	}
	
	public void invalidatePluginsChangeStamp() {
		changeStampIsValid = false;
		pluginsChangeStampIsValid = false;
	}
}
