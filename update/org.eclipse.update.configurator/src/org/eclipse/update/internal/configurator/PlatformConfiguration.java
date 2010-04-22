/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 176250, Configurator needs to handle more platform urls 
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is responsible for providing the features and plugins (bundles) to 
 * the runtime. Configuration data is stored in the configuration/org.eclipse.update/platform.xml file.
 * When eclipse starts, it tries to load the config info from platform.xml.
 * If the file does not exist, then it also tries to read it from a temp or backup file.
 * If this does not succeed, a platform.xml is created by inspecting the eclipse 
 * installation directory (its features and plugin folders).
 * If platform.xml already exists, a check is made to see when it was last modified
 * and whether there are any file system changes that are newer (users may manually unzip 
 * features and plugins). In this case, the newly added features and plugins are picked up.
 * A check for existence of features and plugins is also performed, to detect deletions.
 */
public class PlatformConfiguration implements IPlatformConfiguration, IConfigurationConstants {

	private static PlatformConfiguration currentPlatformConfiguration = null;
	//private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static final String XML_ENCODING = "UTF-8"; //$NON-NLS-1$

	private Configuration config;
	private URL configLocation;
	private HashMap externalLinkSites; // used to restore prior link site state
	private long changeStamp;
	private long featuresChangeStamp;
	private boolean featuresChangeStampIsValid;
	private long pluginsChangeStamp;
	private boolean pluginsChangeStampIsValid;
	//PAL nio optional
	//private FileLock lock;
	private Locker lock = null;
	private static int defaultPolicy = DEFAULT_POLICY_TYPE;
	private static boolean checkNio = false;
	private static boolean useNio;

	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$
	private static final String CONFIG_HISTORY = "history"; //$NON-NLS-1$
	private static final String PLATFORM_XML = "platform.xml"; //$NON-NLS-1$
	private static final String CONFIG_NAME = ConfigurationActivator.NAME_SPACE + "/" + PLATFORM_XML; //$NON-NLS-1$
	private static final String CONFIG_INI = "config.ini"; //NON-NLS-1$ //$NON-NLS-1$
	private static final String CONFIG_FILE_LOCK_SUFFIX = ".lock"; //$NON-NLS-1$
	private static final String CONFIG_FILE_TEMP_SUFFIX = ".tmp"; //$NON-NLS-1$
	private static final String LINKS = "links"; //$NON-NLS-1$
	private static final String[] BOOTSTRAP_PLUGINS = {}; 

	private static final String DEFAULT_FEATURE_APPLICATION = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

	private static final String LINK_PATH = "path"; //$NON-NLS-1$
	private static final String LINK_READ = "r"; //$NON-NLS-1$
	private static final String LINK_READ_WRITE = "rw"; //$NON-NLS-1$
	private static URL installURL;
	
	private PlatformConfiguration(Location platformConfigLocation) throws CoreException, IOException {

		this.externalLinkSites = new HashMap();
		this.config = null;
		
		// initialize configuration
		initializeCurrent(platformConfigLocation);
		if(config != null)
			setDefaultPolicy();

		// Detect external links. These are "soft link" to additional sites. The link
		// files are usually provided by external installation programs. They are located
		// relative to this configuration URL.
		// Note: don't do it for self hosting or if update reconciler is disabled
		if (ConfigurationActivator.isReconciling()) {
			if (!isTransient())
				configureExternalLinks();

			// Validate sites in the configuration. Causes any sites that do not exist to
			// be removed from the configuration
			validateSites();
		}

		// compute differences between configuration and actual content of the sites
		// (base sites and link sites)
		// Note: when the config is transient (generated by PDE, etc.) we don't reconcile
		if (isTransient())
			return;
		
		// for 'osgi.clean' or osgi.checkConfiguration', force a refresh
		boolean osgiClean = "true".equals(ConfigurationActivator.getBundleContext().getProperty("osgi.clean")); //$NON-NLS-1$ //$NON-NLS-2$
		boolean osgiCheckConfiguration = "true".equals(ConfigurationActivator.getBundleContext().getProperty("osgi.checkConfiguration")); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (osgiClean || osgiCheckConfiguration) {
			// We have to call refresh() for features to be rescanned correctly
			refresh();
			reconcile();
		}
		else {
			changeStamp = computeChangeStamp();
			if (changeStamp > config.getDate().getTime())
				reconcile();
		}
	}

	PlatformConfiguration(URL url) throws Exception {
		this.externalLinkSites = new HashMap();
		URL installLocation = Utils.getInstallURL();
		// Retrieve install location with respect to given url if possible
		try {
			if (url.getProtocol().equals("file")) {
				if (url.getPath().endsWith("configuration/org.eclipse.update/platform.xml"))
					installLocation = new Path(url.getPath()).removeLastSegments(3).toFile().toURL();
			}
		} catch (Exception e) {
		//
		}
		initialize(url, installLocation);		
	}
		
	public PlatformConfiguration(URL url, URL installLocation) throws Exception {
		this.externalLinkSites = new HashMap();
		initialize(url, installLocation);
	}

	private void setDefaultPolicy() {
		// Assumption: If the configuration that we initialize with 
		// has a MANAGED_ONLY policy, then all sites should have default policy 
		// of MANAGED_ONLY.  
		ISiteEntry[] sentries = getConfiguredSites();
		if(sentries != null && sentries.length >0){
			int policyType = sentries[0].getSitePolicy().getType();
			if(policyType == ISitePolicy.MANAGED_ONLY){
				defaultPolicy = policyType;
			}
		}
	}

	public static int getDefaultPolicy(){
		return defaultPolicy;
	}

	/*
	 * @see IPlatformConfiguration#createSiteEntry(URL, ISitePolicy)
	 */
	public ISiteEntry createSiteEntry(URL url, ISitePolicy policy) {
		return new SiteEntry(url, policy);
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
		if (config.getSiteEntry(key) != null && !replace)
			return;
	
		if (entry instanceof SiteEntry)
			config.addSiteEntry(key, (SiteEntry)entry);
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
		if (entry instanceof SiteEntry)
			config.removeSiteEntry(key);
	}

	/*
	 * @see IPlatformConfiguration#getConfiguredSites()
	 */
	public ISiteEntry[] getConfiguredSites() {
		if (config == null)
			return new ISiteEntry[0];
		
		SiteEntry[] sites = config.getSites();
		ArrayList enabledSites = new ArrayList(sites.length);
		for (int i=0; i<sites.length; i++) {
			if (sites[i].isEnabled())
				enabledSites.add(sites[i]);
		}
		return (ISiteEntry[])enabledSites.toArray(new ISiteEntry[enabledSites.size()]);
	}

	/*
	 * @see IPlatformConfiguration#findConfiguredSite(URL)
	 */
	public ISiteEntry findConfiguredSite(URL url) {
		return findConfiguredSite(url, true);
	}
	
	/**
	 * 
	 * @param url site url
	 * @param checkPlatformURL if true, check for url format that is platform:/...
	 * @return
	 */
	public SiteEntry findConfiguredSite(URL url, boolean checkPlatformURL) {
		if (url == null)
			return null;
		String key = url.toExternalForm();

		SiteEntry result = config.getSiteEntry(key);	
		if (result == null) { // retry with decoded URL string
			try {
				//PAL foundation
				//key = URLDecoder.decode(key, "UTF-8"); //$NON-NLS-1$
				key = UpdateURLDecoder.decode(key, "UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				// ignore
			}
			result = config.getSiteEntry(key);
		}
			
		if (result == null && checkPlatformURL) {
			try {
				result = findConfiguredSite(config.asPlatformURL(url), false);
			} catch (Exception e) {
				//ignore
			}
		}
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

		// we should check each site and find where the feature is
		// located and then configure it
		if (config == null)
			config = new Configuration();

		SiteEntry[] sites = config.getSites();
		for (int i=0; i<sites.length; i++) {
			// find out what site contains the feature and configure it
			try {
				URL url = new URL(sites[i].getURL(), FEATURES + "/" + entry.getFeatureIdentifier()+ "_" + entry.getFeatureVersion() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				try {
					url = resolvePlatformURL(url, getBasePathLocation(url, config.getInstallURL(), config.getURL()));
				} catch (IOException e) {
				}
				if (new File(url.getFile()).exists())
					sites[i].addFeatureEntry(entry);
				else  {
					url = new URL(sites[i].getURL(), FEATURES + "/" + entry.getFeatureIdentifier() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
					if (new File(url.getFile()).exists())
						sites[i].addFeatureEntry(entry);
				}
			} catch (MalformedURLException e) {
			}
		}
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

		config.unconfigureFeatureEntry(entry);
	}

	/*
	 * @see IPlatformConfiguration#getConfiguredFeatureEntries()
	 */
	public IFeatureEntry[] getConfiguredFeatureEntries() {
		ArrayList configFeatures = new ArrayList();
		SiteEntry[] sites = config.getSites();
		for (int i=0; i<sites.length; i++) {
			FeatureEntry[] features = sites[i].getFeatureEntries();
			for (int j=0; j<features.length; j++)
				configFeatures.add(features[j]);
		}
		return (IFeatureEntry[])configFeatures.toArray(new FeatureEntry[configFeatures.size()]);
	}

	/*
	 * @see IPlatformConfiguration#findConfiguredFeatureEntry(String)
	 */
	public IFeatureEntry findConfiguredFeatureEntry(String id) {
		if (id == null)
			return null;

		SiteEntry[] sites = config.getSites();
		for (int i=0; i<sites.length; i++) {
			FeatureEntry f = sites[i].getFeatureEntry(id);
			if (f != null)
				return f;
		}
		return null;
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
		if (config.getLinkedConfig() == null)
			return config.getDate().getTime();
		return Math.max(config.getDate().getTime(), config.getLinkedConfig().getDate().getTime());
	}

	/*
	 * @see IPlatformConfiguration#getFeaturesChangeStamp()
	 * @deprecated Don't use this method
	 */
	public long getFeaturesChangeStamp() {
		return 0;
	}

	/*
	 * @see IPlatformConfiguration#getPluginsChangeStamp()
	 * @deprecated Don't use this method
	 */
	public long getPluginsChangeStamp() {
		return 0;
	}


	public String getApplicationIdentifier() {
		// Return the app if defined in system properties
		String application = ConfigurationActivator.getBundleContext().getProperty(ECLIPSE_APPLICATION);
		if (application != null)
			return application;

		// Otherwise, try to get it from the primary feature (aka product)
		String feature = getPrimaryFeatureIdentifier();

		// lookup application for feature (specified or defaulted)
		if (feature != null) {
			IFeatureEntry fe = findConfiguredFeatureEntry(feature);
			if (fe != null) {
				if (fe.getFeatureApplication() != null)
					return fe.getFeatureApplication();
			}
		}

		// return hardcoded default if we failed
		return DEFAULT_FEATURE_APPLICATION;
	}

	/*
	 * @see IPlatformConfiguration#getPrimaryFeatureIdentifier()
	 */
	public String getPrimaryFeatureIdentifier() {
		// Return the product if defined in system properties
		String primaryFeatureId = ConfigurationActivator.getBundleContext().getProperty(ECLIPSE_PRODUCT);
		if (primaryFeatureId != null) {
			// check if feature exists
			IFeatureEntry feature = findConfiguredFeatureEntry(primaryFeatureId);
			if (feature != null && feature.canBePrimary())
				return primaryFeatureId;
		}
		return null;
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
	
	public Set getPluginPaths() {
		
		HashSet paths = new HashSet();
		ISiteEntry[] sites = getConfiguredSites();

		for (int i = 0; i < sites.length; i++) {
			String[] plugins = sites[i].getPlugins();
			for (int j = 0; j < plugins.length; j++) {
					paths.add(plugins[j]);
			}
		}
		
		return paths;
	}
	

	/*
	 * A variation of the getPluginPath, but it returns the actual plugin entries
	 */
	public PluginEntry[] getPlugins() {
		ArrayList allPlugins = new ArrayList();
		Utils.debug("computed plug-ins:"); //$NON-NLS-1$

		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			if (!(sites[i] instanceof SiteEntry)) {
				Utils.debug("Site " + sites[i].getURL() + " is not a SiteEntry"); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}
			PluginEntry[] plugins = ((SiteEntry)sites[i]).getPluginEntries();
			for (int j = 0; j < plugins.length; j++) {
				allPlugins.add(plugins[j]);
				Utils.debug("   " + plugins[j].getURL()); //$NON-NLS-1$
			}
		}
		return (PluginEntry[]) allPlugins.toArray(new PluginEntry[0]);
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
		return (config != null) ? config.isTransient() : false;
	}

	/*
	 * @see IPlatformConfiguration#isTransient(boolean)
	 */
	public void isTransient(boolean value) {
		if (this != getCurrent() && config != null)
			config.setTransient(value);
	}

	/*
	 * @see IPlatformConfiguration#refresh()
	 */
	public synchronized void refresh() {
		// Reset computed values. Will be lazily refreshed
		// on next access
		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			if (sites[i].isUpdateable()) {
				// reset site entry
				((SiteEntry) sites[i]).refresh();
			}
		}
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
			throw new IOException(Messages.cfig_unableToSave_noURL); 

		OutputStream os = null;
		if (!url.getProtocol().equals("file")) { //$NON-NLS-1$
			// not a file protocol - attempt to save to the URL
			URLConnection uc = url.openConnection();
			uc.setDoOutput(true);
			os = uc.getOutputStream();
			try {
				saveAsXML(os);
				config.setDirty(false);
			} catch (CoreException e) {
				Utils.log(e.getMessage());
				Utils.log(e.getStatus());
				throw new IOException(NLS.bind(Messages.cfig_unableToSave, (new String[] { url.toExternalForm() })));
			} finally {
				os.close();
			}
		} else {
			// file protocol - do safe i/o
			File cfigFile = new File(url.getFile().replace('/', File.separatorChar));
			if (!cfigFile.getName().equals(PLATFORM_XML)) {
				if (cfigFile.exists() && cfigFile.isFile()) {
					Utils.log(Messages.PlatformConfiguration_expectingPlatformXMLorDirectory + cfigFile.getName()); 
					cfigFile = cfigFile.getParentFile();
				}
				cfigFile = new File(cfigFile, CONFIG_NAME);
			}
			File workingDir = cfigFile.getParentFile();
			if (workingDir != null && !workingDir.exists())
				workingDir.mkdirs();

			// Do safe i/o: 
			//    - backup current config, by moving it to the history folder
			//    - write new config to platform.xml.tmp file
			//    - rename the temp file to platform.xml
			File cfigFileOriginal = new File(cfigFile.getAbsolutePath());
			File cfigTmp = new File(cfigFile.getAbsolutePath() + CONFIG_FILE_TEMP_SUFFIX);
			
			// Backup old file
			if (cfigFile.exists()){
				File backupDir = new File(workingDir, CONFIG_HISTORY);
				if (!backupDir.exists())
					backupDir.mkdir();
				long timestamp = cfigFile.lastModified();
				File preservedFile = new File(backupDir, String.valueOf(timestamp)+".xml"); //$NON-NLS-1$
				// If the target file exists, increment the timestamp. Try at most 100 times.
				long increment = 1;
				while (preservedFile.exists() && increment < 100){
					preservedFile = new File(backupDir, String.valueOf(timestamp+increment++)+".xml"); //$NON-NLS-1$
				}
				if (!preservedFile.exists()) {
					// try renaming current config to backup copy
					if (!cfigFile.renameTo(preservedFile))
						Utils.log(Messages.PlatformConfiguration_cannotBackupConfig); 
				}
			}

			// first save the file as temp
			os = new FileOutputStream(cfigTmp);
			
			try {
				saveAsXML(os);
				// Try flushing any internal buffers, and synchronize with the disk
				try {
					os.flush();
					((FileOutputStream)os).getFD().sync();
				} catch (SyncFailedException e2) {
					Utils.log(e2.getMessage());
				} catch (IOException e2) {
					Utils.log(e2.getMessage());
				}
				try {
					os.close();
					os = null;
				} catch (IOException e1) {
					Utils.log(Messages.PlatformConfiguration_cannotCloseStream + cfigTmp); 
					Utils.log(e1.getMessage());
				}
				// set file time stamp to match that of the config element
				cfigTmp.setLastModified(config.getDate().getTime());
				// set this on config, in case the value was rounded off
				config.setLastModified(cfigTmp.lastModified());
				// make the change stamp to be the same as the config file
				changeStamp = config.getDate().getTime();
				config.setDirty(false);
			} catch (CoreException e) {
				throw new IOException(NLS.bind(Messages.cfig_unableToSave, (new String[] { cfigTmp.getAbsolutePath() })));
			} finally {
				if (os != null)
					try {
						os.close();
					} catch (IOException e1) {
						Utils.log(Messages.PlatformConfiguration_cannotCloseTempFile + cfigTmp); 
					}
			}

			// at this point we have old config (if existed) as "bak" and the
			// new config as "tmp".
			boolean ok = cfigTmp.renameTo(cfigFileOriginal);
			if (!ok) {
				// this codepath represents a tiny failure window. The load processing
				// on startup will detect missing config and will attempt to start
				// with "tmp" (latest), then "bak" (the previous). We can also end up
				// here if we failed to rename the current config to "bak". In that
				// case we will restart with the previous state.
				Utils.log(Messages.PlatformConfiguration_cannotRenameTempFile); 
				
				throw new IOException(NLS.bind(Messages.cfig_unableToSave, (new String[] { cfigTmp.getAbsolutePath() })));
			}
		}
	}


	public static PlatformConfiguration getCurrent() {
		return currentPlatformConfiguration;
	}

	/**
	 * Starts a platform installed at specified installURL using configuration located at platformConfigLocation.
	 */
	public static synchronized void startup(URL installURL, Location platformConfigLocation) throws Exception {
		PlatformConfiguration.installURL = installURL;
	
		// create current configuration
		if (currentPlatformConfiguration == null) {
			currentPlatformConfiguration = new PlatformConfiguration(platformConfigLocation);
			if (currentPlatformConfiguration.config == null)
				throw new Exception(Messages.PlatformConfiguration_cannotLoadConfig + platformConfigLocation.getURL()); 
			if (currentPlatformConfiguration.config.isDirty())
				// If this is a transient config (generated by PDE),do nothing
				// otherwise, save the configuration with proper date
				if (!currentPlatformConfiguration.isTransient())
					currentPlatformConfiguration.save();
		}
	}

	public static synchronized void shutdown() throws IOException {

		// save platform configuration
		PlatformConfiguration config = getCurrent();
		if (config != null) {
			// only save if there are changes in the config
			if (config.config.isDirty() && !config.isTransient()) {
				try {
					config.save();
				} catch (IOException e) {
					Utils.debug("Unable to save configuration " + e.toString()); //$NON-NLS-1$
					// will recover on next startup
				}
			}
		}
	}


	private synchronized void initializeCurrent(Location platformConfigLocation) throws IOException {

		// Configuration URL was is specified by the OSGi layer. 
		// Default behavior is to look
		// for configuration in the specified meta area. If not found, look
		// for pre-initialized configuration in the installation location.
		// If it is found it is used as the initial configuration. Otherwise
		// a new configuration is created. In either case the resulting
		// configuration is written into the specified configuration area.

		URL configFileURL = new URL(platformConfigLocation.getURL(), CONFIG_NAME);
		try {	
			// check concurrent use lock
			getConfigurationLock(platformConfigLocation.getURL());

			// try loading the configuration
			try {
				config = loadConfig(configFileURL, installURL);
				Utils.debug("Using configuration " + configFileURL.toString()); //$NON-NLS-1$
			} catch (Exception e) {
				// failed to load, see if we can find pre-initialized configuration.
				try {
					Location parentLocation = platformConfigLocation.getParentLocation();
					if (parentLocation == null)
						throw new IOException(); // no platform.xml found, need to create default site
					
					URL sharedConfigFileURL = new URL(parentLocation.getURL(), CONFIG_NAME);
					config = loadConfig(sharedConfigFileURL, installURL);
					
					// pre-initialized config loaded OK ... copy any remaining update metadata
					// Only copy if the default config location is not the install location
					if (!sharedConfigFileURL.equals(configFileURL)) {
						// need to link config info instead of using a copy
						linkInitializedState(config, parentLocation, platformConfigLocation);
						Utils.debug("Configuration initialized from    " + sharedConfigFileURL.toString()); //$NON-NLS-1$
					}
					return;
				} catch (Exception ioe) {
					Utils.debug("Creating default configuration from " + configFileURL.toExternalForm()); //$NON-NLS-1$
					createDefaultConfiguration(configFileURL, installURL);
				}
			} finally {
				// if config == null an unhandled exception has been thrown and we allow it to propagate
				if (config != null) {
					configLocation = configFileURL;
					 if (config.getURL() == null)
						config.setURL(configFileURL);
					 verifyPath(configLocation, config.getInstallURL());
					Utils.debug("Creating configuration " + configFileURL.toString()); //$NON-NLS-1$
				}
			}
		} finally {			
			// releaes concurrent use lock
			clearConfigurationLock();
		}
	}

	
	private synchronized void initialize(URL url, URL installLocation) throws Exception {
		if (url != null) {
			config = loadConfig(url, installLocation);	
			Utils.debug("Using configuration " + url.toString()); //$NON-NLS-1$
		}
		if (config == null) {
			config = new Configuration();		
			Utils.debug("Creating empty configuration object"); //$NON-NLS-1$
		}
		config.setURL(url);
		config.setInstallLocation(installLocation);
		configLocation = url;
	}

	private void createDefaultConfiguration(URL url, URL installLocation)throws IOException{
		// we are creating new configuration
		config = new Configuration();
		config.setURL(url);
		config.setInstallLocation(installLocation);
		SiteEntry defaultSite = (SiteEntry)getRootSite();
		configureSite(defaultSite);
		try {
			// parse the site directory to discover features
			defaultSite.loadFromDisk(0);
		} catch (CoreException e1) {
			Utils.log(Messages.PlatformConfiguration_cannotLoadDefaultSite + defaultSite.getResolvedURL()); 
			return;
		}
	}
	private ISiteEntry getRootSite() {
		// create default site entry for the root
		ISitePolicy defaultPolicy = createSitePolicy(getDefaultPolicy(), DEFAULT_POLICY_LIST);
		URL siteURL = null;
		try {
			siteURL = new URL("platform:/base/"); //$NON-NLS-1$  // try using platform-relative URL
		} catch (MalformedURLException e) {
			siteURL = getInstallURL(); // ensure we come up ... use absolute file URL
		}
		ISiteEntry defaultSite = createSiteEntry(siteURL, defaultPolicy);
		return defaultSite;
	}
	
	/**
	 * Gets the configuration lock
	 * @param url configuration directory
	 */
	private void getConfigurationLock(URL url) {
		if (!url.getProtocol().equals("file")) //$NON-NLS-1$
			return;

		File lockFile = new File(url.getFile(), ConfigurationActivator.NAME_SPACE+ File.separator+CONFIG_FILE_LOCK_SUFFIX);
		verifyPath(url, config == null ? null : config.getInstallURL());
		// PAL nio optional
		lock = createLocker(lockFile); 
		try {
			lock.lock();
		} catch (IOException ioe) {
			lock = null;
		}	
	}
	
	private void clearConfigurationLock() {
		// PAL nio optional
		if (lock != null) {
			lock.release();
		}
	}
	
	/**
	 * Create a locker using java new I/O or regular I/O
	 * depending whether we run in J2SE or cdcFoundation
	 * PAL nio optional
	 */
	private static Locker createLocker(File lock) {
		if (!checkNio) {
			useNio = true;
			try {
				 Class.forName("java.nio.channels.FileLock"); //$NON-NLS-1$
			} catch (ClassNotFoundException e) {
				useNio = false;
			}
		}
		if (useNio)
			return new Locker_JavaNio(lock);
		
		return new Locker_JavaIo(lock);
	}
	
	private long computeChangeStamp() {
		featuresChangeStamp = computeFeaturesChangeStamp();
		pluginsChangeStamp = computePluginsChangeStamp();
		changeStamp = Math.max(featuresChangeStamp, pluginsChangeStamp);
		// round off to seconds
		changeStamp = (changeStamp/1000)*1000;
		return changeStamp;
	}

	private long computeFeaturesChangeStamp() {
		if (featuresChangeStampIsValid)
			return featuresChangeStamp;

		long result = 0;
		ISiteEntry[] sites = config.getSites();
		for (int i = 0; i < sites.length; i++) {
			result = Math.max(result, sites[i].getFeaturesChangeStamp());
		}
		featuresChangeStamp = result;
		featuresChangeStampIsValid = true;
		return featuresChangeStamp;
	}

	private long computePluginsChangeStamp() {
		if (pluginsChangeStampIsValid)
			return pluginsChangeStamp;

		long result = 0;
		ISiteEntry[] sites = config.getSites();
		for (int i = 0; i < sites.length; i++) {
			result = Math.max(result, sites[i].getPluginsChangeStamp());
		}
		pluginsChangeStamp = result;
		pluginsChangeStampIsValid = true;
		return pluginsChangeStamp;
	}

	private void configureExternalLinks() {
		URL linkURL = getInstallURL();
		if (!supportsDetection(linkURL, config.getInstallURL()))
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
				configureExternalLinkSite(links[i], props);
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

	private void configureExternalLinkSite(File linkFile, Properties props) {
		String path = props.getProperty(LINK_PATH);
		if (path == null) {
			Utils.debug("   no path definition"); //$NON-NLS-1$
			return;
		}

		String link;
		boolean updateable = true;
		URL siteURL;

		// parse out link information
		if (path.startsWith(LINK_READ + " ")) { //$NON-NLS-1$
			updateable = false;
			link = path.substring(2).trim();
		} else if (path.startsWith(LINK_READ_WRITE + " ")) { //$NON-NLS-1$
			link = path.substring(3).trim();
		} else {
			link = path.trim();
		}

		// 	make sure we have a valid link specification
		try {
			File siteFile = new File(link);
			siteFile = new File(siteFile, ECLIPSE);
			siteURL = siteFile.toURL();
			if (findConfiguredSite(siteURL, true) != null)
				// linked site is already known
				return;
		} catch (MalformedURLException e) {
			// ignore bad links ...
			Utils.debug("  bad URL " + e); //$NON-NLS-1$
			return;
		}
		
		// process the link
		SiteEntry linkSite = (SiteEntry) externalLinkSites.get(siteURL);
		if (linkSite == null) {
			// this is a link to a new target so create site for it
			ISitePolicy linkSitePolicy = createSitePolicy(getDefaultPolicy(), DEFAULT_POLICY_LIST);
			linkSite = (SiteEntry) createSiteEntry(siteURL, linkSitePolicy);
		}
		// update site entry if needed
		linkSite.setUpdateable(updateable);
		linkSite.setLinkFileName(linkFile.getAbsolutePath());

		// configure the new site
		// NOTE: duplicates are not replaced (first one in wins)
		configureSite(linkSite);
		// there are changes in the config
		config.setDirty(true);
		Utils.debug("   " + (updateable ? "R/W -> " : "R/O -> ") + siteURL.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void validateSites() {

		// check to see if all sites are valid. Remove any sites that do not exist.
		SiteEntry[] list = config.getSites();
		for (int i = 0; i < list.length; i++) {
			URL siteURL = list[i].getResolvedURL();
			if (!supportsDetection(siteURL, config.getInstallURL()))
				continue;

			File siteRoot = new File(siteURL.getFile().replace('/', File.separatorChar));
			if (!siteRoot.exists()) {
				unconfigureSite(list[i]);
				Utils.debug("Site " + siteURL + " does not exist ... removing from configuration"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			// If multiple paths are defined in the same link file
			// or if the path changes, the old site will still be kept.
			// A better algorithm could be implemented by keeping track 
			// of the previous content of the link file.
			// TODO do the above
			String linkName = list[i].getLinkFileName();
			if (linkName != null) {
				File linkFile = new File(linkName);
				if (!linkFile.exists())  {
					unconfigureSite(list[i]);
					config.setDirty(true);
					Utils.debug("Site " + siteURL + " is no longer linked ... removing from configuration"); //$NON-NLS-1$ //$NON-NLS-2$	
				}
			}
		}
	}
	
	private void linkInitializedState(Configuration sharedConfig, Location sharedConfigLocation, Location newConfigLocation) {
		try {
			URL newConfigIniURL = new URL(newConfigLocation.getURL(), CONFIG_INI);
			if (!newConfigIniURL.getProtocol().equals("file")) //$NON-NLS-1$
				return; // need to be able to do write

			// modify config.ini and platform.xml to only link original files
			File configIni = new File(newConfigIniURL.getFile());
			Properties props = new Properties();
			String externalForm = Utils.makeRelative(config.getInstallURL(), sharedConfigLocation.getURL()).toExternalForm();
			props.put("osgi.sharedConfiguration.area", externalForm); //$NON-NLS-1$
			props.store(new FileOutputStream(configIni), "Linked configuration"); //$NON-NLS-1$
			
			config = new Configuration(new Date());
			config.setURL(new URL(newConfigLocation.getURL(), CONFIG_NAME));
			config.setLinkedConfig(sharedConfig);
			config.setDirty(true);
		} catch (IOException e) {
			// this is an optimistic copy. If we fail, the state will be reconciled
			// when the update manager is triggered.
			System.out.println(e);
		}
	}
	
	private Configuration loadConfig(URL url, URL installLocation) throws Exception {
		if (url == null)
			throw new IOException(Messages.cfig_unableToLoad_noURL); 

		// try to load saved configuration file (watch for failed prior save())
		ConfigurationParser parser = null;
		try {
			parser = new ConfigurationParser();
		} catch (InvocationTargetException e) {
			throw (Exception)e.getTargetException();
		}
		
		config = null;
		Exception originalException = null;
		try {
			config = parser.parse(url, installLocation);
			if (config == null)
				throw new Exception(Messages.PlatformConfiguration_cannotFindConfigFile); 
		} catch (Exception e1) {
			// check for save failures, so open temp and backup configurations
			originalException = e1;
			try {
				URL tempURL = new URL(url.toExternalForm()+CONFIG_FILE_TEMP_SUFFIX);
				config = parser.parse(tempURL, installLocation);  
				if (config == null)
					throw new Exception();
				config.setDirty(true); // force saving to platform.xml
			} catch (Exception e2) {
				try {
					// check the backup
					if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
						File cfigFile = new File(url.getFile().replace('/', File.separatorChar));
						File workingDir = cfigFile.getParentFile();
						if (workingDir != null && workingDir.exists()) {
							File[] backups = workingDir.listFiles(new FileFilter(){
								public boolean accept(File pathname) {
									return pathname.isFile() && pathname.getName().endsWith(".xml"); //$NON-NLS-1$
								}});
							if (backups != null && backups.length > 0) {
								URL backupUrl = backups[backups.length-1].toURL();
								config = parser.parse(backupUrl, installLocation);
							}
						}
					}
					if (config == null)
						throw originalException; // we tried, but no config here ...
					config.setDirty(true); // force saving to platform.xml
				} catch (IOException e3) {
					throw originalException; // we tried, but no config here ...
				}
			}
		}

		return config;
	}

	public static boolean supportsDetection(URL url, URL installLocation) {
		String protocol = url.getProtocol();
		if (protocol.equals("file")) //$NON-NLS-1$
			return true;
		else if (protocol.equals("platform")) { //$NON-NLS-1$
			URL resolved = null;
			try {
				resolved = resolvePlatformURL(url, installLocation); // 19536
			} catch (IOException e) {
				return false; // we tried but failed to resolve the platform URL
			}
			return resolved.getProtocol().equals("file"); //$NON-NLS-1$
		} else
			return false;
	}

	private static void verifyPath(URL url, URL installLocation) {
		String protocol = url.getProtocol();
		String path = null;
		if (protocol.equals("file")) //$NON-NLS-1$
			path = url.getFile();
		else if (protocol.equals("platform")) { //$NON-NLS-1$
			URL resolved = null;
			try {
				resolved = resolvePlatformURL(url, installLocation); // 19536
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

	public static URL resolvePlatformURL(URL url, URL base_path_Location)
			throws IOException {
		if (url.getProtocol().equals("platform")) { //$NON-NLS-1$
			if (base_path_Location == null) {
				url = FileLocator.toFileURL(url);
				File f = new File(url.getFile());
				url = f.toURL();
			} else {
				final String BASE = "platform:/base/";
				final String CONFIG = "platform:/config/";
				String toResolve = url.toExternalForm();
				if (toResolve.startsWith(BASE)) 
					url = new URL(base_path_Location, toResolve.substring(BASE
							.length()));
				else if (toResolve.startsWith(CONFIG)) {
					url = new URL(base_path_Location, toResolve.substring(CONFIG
							.length()));
				}
				else
					url = base_path_Location;
			}
		}
		return url;
	}
	
	private URL getBasePathLocation(URL  url, URL installLocation, URL configLocation) {
		final String BASE = "platform:/base/";
		final String CONFIG = "platform:/config/";
		String toResolve = url.toExternalForm();
		if(toResolve.startsWith(BASE)) {
			return installLocation;
		}else if(toResolve.startsWith(CONFIG)){
			URL config_loc;
			try {
				config_loc = new URL(configLocation, "..");
			} catch (MalformedURLException e) {
				return configLocation;
			}
			return config_loc;
		}
		return url;
	}
	
	public static URL getInstallURL() {
		return installURL;
	}
	
	private void saveAsXML(OutputStream stream) throws CoreException,IOException {			
		BufferedWriter xmlWriter = new BufferedWriter(new OutputStreamWriter(stream,XML_ENCODING));
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setExpandEntityReferences(false);
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			if (config == null)
				throw Utils.newCoreException(Messages.PlatformConfiguration_cannotSaveNonExistingConfig,null); 
			
			config.setDate(new Date());
			Element configElement = config.toXML(doc);
			doc.appendChild(configElement);
			
			// This is not DBCS friendly... PAL
			//XMLPrintHandler.printComment(xmlWriter,"Created on " + config.getDate().toString());
			XMLPrintHandler.printNode(xmlWriter,doc,XML_ENCODING);
			
		} catch (Exception e) {
			throw Utils.newCoreException("", e); //$NON-NLS-1$
		} finally {
			xmlWriter.flush();
			// will close the stream in the caller	
			//xmlWriter.close();
		}
	} 
	
	private void reconcile() throws CoreException {
		long lastChange = config.getDate().getTime();
		SiteEntry[] sites = config.getSites();
		for (int s = 0; s < sites.length; s++) {
			if (sites[s].isUpdateable()) {
				long siteTimestamp = sites[s].getChangeStamp();
				if (siteTimestamp > lastChange)
					sites[s].loadFromDisk(lastChange);
			}
		}
		config.setDirty(true);
	}
	
	public Configuration getConfiguration() {
		return config;
	}
}
