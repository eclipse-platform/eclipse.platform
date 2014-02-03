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
package org.eclipse.update.internal.configurator;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.environment.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configurator.*;
import org.eclipse.update.configurator.IPlatformConfiguration.*;
import org.w3c.dom.*;
import org.xml.sax.*;


public class SiteEntry implements IPlatformConfiguration.ISiteEntry, IConfigurationConstants{	
	private static final String MAC_OS_MARKER = ".DS_Store"; //$NON-NLS-1$
	
	private URL url; // this is the external URL for the site
	private URL resolvedURL; // this is the resolved URL used internally
	private ISitePolicy policy;
	private boolean updateable = true;
	private Map featureEntries;
	private ArrayList pluginEntries;
	private long changeStamp;
	private long featuresChangeStamp;
	private long pluginsChangeStamp;
	private String linkFileName;
	private boolean enabled = true;
	private Configuration config;
	
	private static FeatureParser featureParser = new FeatureParser();
	private static PluginParser pluginParser = new PluginParser();
	private static boolean isMacOS = Utils.getOS().equals(Constants.OS_MACOSX);

	public SiteEntry(URL url) {
		this(url,null);
	}
	
	public SiteEntry(URL url, ISitePolicy policy) {
		if (url == null)
			try {
				url = new URL("platform:/base/"); //$NON-NLS-1$ try using platform-relative URL
			} catch (MalformedURLException e) {
				url = PlatformConfiguration.getInstallURL(); // ensure we come up ... use absolute file URL
			}
			
		if (policy == null)
			policy = new SitePolicy(PlatformConfiguration.getDefaultPolicy(), DEFAULT_POLICY_LIST);

		if (url.getProtocol().equals("file")) { //$NON-NLS-1$
			try {
				// TODO remove this when platform fixes local file url's
				this.url = new File(url.getFile()).toURL(); 
			} catch (MalformedURLException e1) {
				this.url = url;
			}
		} else
			this.url = url;
		
		this.policy = policy;
		this.resolvedURL = this.url;
	}

	public void setConfig(Configuration config) {
		this.config = config;
		if (url.getProtocol().equals("platform")) { //$NON-NLS-1$
			try {
				// resolve the config location relative to the configURL
				if (url.getPath().startsWith("/config")) {	
					URL configURL = config.getURL();
					URL config_loc = new URL(configURL, "..");
					resolvedURL = PlatformConfiguration.resolvePlatformURL(url, config_loc); // 19536
				}
				else 
					resolvedURL = PlatformConfiguration.resolvePlatformURL(url, config.getInstallURL()); // 19536
			} catch (IOException e) {
				// will use the baseline URL ...
			}
		}
	}
	
	public Configuration getConfig() {
		return config;
	}
	
	/*
	 * @see ISiteEntry#getURL()
	 */
	public URL getURL() {
		return url;
	}

	/*
	* @see ISiteEntry#getSitePolicy()
	*/
	public ISitePolicy getSitePolicy() {
		return policy;
	}

	/*
	 * @see ISiteEntry#setSitePolicy(ISitePolicy)
	 */
	public synchronized void setSitePolicy(ISitePolicy policy) {
		if (policy == null)
			throw new IllegalArgumentException();
		this.policy = policy;
	}

	/*
	 * @see ISiteEntry#getFeatures()
	 */
	public String[] getFeatures() {
		return getDetectedFeatures();
	}

	/*
	 * @see ISiteEntry#getPlugins()
	 */
	public String[] getPlugins() {

		ISitePolicy policy = getSitePolicy();

		if (policy.getType() == ISitePolicy.USER_INCLUDE)
			return policy.getList();

		if (policy.getType() == ISitePolicy.USER_EXCLUDE) {
			ArrayList detectedPlugins = new ArrayList(Arrays.asList(getDetectedPlugins()));
			String[] excludedPlugins = policy.getList();
			for (int i = 0; i < excludedPlugins.length; i++) {
				if (detectedPlugins.contains(excludedPlugins[i]))
					detectedPlugins.remove(excludedPlugins[i]);
			}
			return (String[]) detectedPlugins.toArray(new String[0]);
		}
		
		if (policy.getType() == ISitePolicy.MANAGED_ONLY) {
			PluginEntry[] managedPlugins = getManagedPlugins();
			String[] managedPluginsURLs = new String[managedPlugins.length];
			for (int i=0; i<managedPlugins.length; i++)
				managedPluginsURLs[i] = managedPlugins[i].getURL();
			
			return managedPluginsURLs;
		}

		// bad policy type
		return new String[0];
	}

	private PluginEntry[] getManagedPlugins() {
		// Note:
		// We detect all the plugins on the site, but it would be faster
		// to just lookup the plugins that correspond to the entries found in each feature.
		// TODO fix the above
		if (pluginEntries == null)
			detectPlugins();
		if (featureEntries == null)
			detectFeatures();
		
		// cache all the plugin entries for faster lookup later
		Map cachedPlugins = new HashMap(pluginEntries.size());
		for (int i=0; i<pluginEntries.size(); i++) {
			PluginEntry p = (PluginEntry)pluginEntries.get(i);
			cachedPlugins.put(p.getVersionedIdentifier(), p);
		}
		
		ArrayList managedPlugins = new ArrayList();
		for (Iterator iterator=featureEntries.values().iterator(); iterator.hasNext();) {
			Object feature = iterator.next();
			if (!(feature instanceof FeatureEntry))
				continue;
			
			PluginEntry[] plugins = ((FeatureEntry)feature).getPluginEntries();
			for (int i=0; i<plugins.length; i++)
				if (cachedPlugins.containsKey(plugins[i].getVersionedIdentifier()))
					managedPlugins.add(cachedPlugins.get(plugins[i].getVersionedIdentifier()));
					
		}
		return (PluginEntry[])managedPlugins.toArray(new PluginEntry[managedPlugins.size()]);
	}
	
	public PluginEntry[] getPluginEntries() {
		String[] pluginURLs = getPlugins();
		// hash the array, for faster lookups
		HashMap map = new HashMap(pluginURLs.length);
		for (int i=0; i<pluginURLs.length; i++)
			map.put(pluginURLs[i], pluginURLs[i]);
		
		if (pluginEntries == null)
				detectPlugins();
		
		ArrayList plugins = new ArrayList(pluginURLs.length);
		for (int i=0; i<pluginEntries.size(); i++) {
			PluginEntry p = (PluginEntry)pluginEntries.get(i);
			if (map.containsKey(p.getURL()))
				plugins.add(p);
		}
		return (PluginEntry[])plugins.toArray(new PluginEntry[plugins.size()]);
	}
	
	/*
	 * @see ISiteEntry#getChangeStamp()
	 */
	public long getChangeStamp() {
		if (changeStamp == 0)
			computeChangeStamp();
		return changeStamp;
	}

	/*
	 * @see ISiteEntry#getFeaturesChangeStamp()
	 */
	public long getFeaturesChangeStamp() {
		if (featuresChangeStamp == 0)
			computeFeaturesChangeStamp();
		return featuresChangeStamp;
	}

	/*
	 * @see ISiteEntry#getPluginsChangeStamp()
	 */
	public long getPluginsChangeStamp() {
		if (pluginsChangeStamp == 0)
			computePluginsChangeStamp();
		return pluginsChangeStamp;
	}

	/*
	 * @see ISiteEntry#isUpdateable()
	 */
	public boolean isUpdateable() {
		return updateable;
	}
	
	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	/*
	 * @see ISiteEntry#isNativelyLinked()
	 */
	public boolean isNativelyLinked() {
		return isExternallyLinkedSite();
	}

	public URL getResolvedURL() {
		return resolvedURL;
	}
	
	/**
	 * Detect new features (timestamp > current site timestamp)
	 * and validates existing features (they might have been removed)
	 */
	private void detectFeatures() {

		if (featureEntries != null)
			validateFeatureEntries();
		else
			featureEntries = new HashMap();

		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL()))
			return;

		// locate feature entries on site
		File siteRoot = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		File featuresDir = new File(siteRoot, FEATURES);
		if (featuresDir.exists()) {
			// handle the installed features under the features directory
			File[] dirs = featuresDir.listFiles(new FileFilter() {
				public boolean accept(File f) {
					// mac os folders contain a file .DS_Store in each folder, and we need to skip it (bug 76869) 
					if (isMacOS && f.getName().equals(MAC_OS_MARKER))
						return false;
					boolean valid = f.isDirectory() && (new File(f,FEATURE_XML).exists());
					if (!valid)
						Utils.log(NLS.bind(Messages.SiteEntry_cannotFindFeatureInDir, (new String[] { f.getAbsolutePath() })));
					return valid;
				}
			});
		
			for (int index = 0; index < dirs.length; index++) {
				try {
					File featureXML = new File(dirs[index], FEATURE_XML);
					if (featureXML.lastModified() <= featuresChangeStamp &&
						dirs[index].lastModified() <= featuresChangeStamp)
						continue;
					URL featureURL = featureXML.toURL();
					FeatureEntry featureEntry = featureParser.parse(featureURL);
					if (featureEntry != null)
						addFeatureEntry(featureEntry);
				} catch (MalformedURLException e) {
					Utils.log(NLS.bind(Messages.InstalledSiteParser_UnableToCreateURLForFile, (new String[] { featuresDir.getAbsolutePath() })));
				}
			}
		}
		
		Utils.debug(resolvedURL.toString() + " located  " + featureEntries.size() + " feature(s)"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Detect new plugins (timestamp > current site timestamp)
	 * and validates existing plugins (they might have been removed)
	 */
	private void detectPlugins() {
		boolean compareTimeStamps = false;
		if (pluginEntries != null) {
			validatePluginEntries();
			compareTimeStamps = true; // only pick up newer plugins
		} else
			pluginEntries = new ArrayList();

		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL()))
			return;

		// locate plugin entries on site
		File pluginsDir = new File(resolvedURL.getFile(), PLUGINS);
		
		if (pluginsDir.exists() && pluginsDir.isDirectory()) {
			File[] files = pluginsDir.listFiles();
			for (int i = 0; i < files.length; i++) {
				if(files[i].isDirectory()){
					detectUnpackedPlugin(files[i], compareTimeStamps);
				}else if(files[i].getName().endsWith(".jar")){ //$NON-NLS-1$
					detectPackedPlugin(files[i], compareTimeStamps);
				}else{
					// not bundle file
				}
			}
		} 
		
		Utils.debug(resolvedURL.toString() + " located  " + pluginEntries.size() + " plugin(s)"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param file a plugin jar
	 * @param compareTimeStamps set to true when looking for plugins changed since last time they were detected
	 */
	private void detectPackedPlugin(File file, boolean compareTimeStamps) {
		// plugin to run directly from jar
		if (compareTimeStamps && file.lastModified() <= pluginsChangeStamp) {
			return;
		}
		String entryName = META_MANIFEST_MF;
		ZipFile z = null;
		InputStream bundleManifestIn = null;
		InputStream pluginManifestIn = null;
		String pluginURL = PLUGINS + "/" + file.getName(); //$NON-NLS-1$
		try {
			// First, check if has valid bundle manifest
			z = new ZipFile(file);
			if (z.getEntry(entryName) != null) {
				bundleManifestIn = z.getInputStream(new ZipEntry(entryName));
				BundleManifest manifest = new BundleManifest(bundleManifestIn,
						pluginURL);
				if (manifest.exists()) {
					addPluginEntry(manifest.getPluginEntry());
					return;
				}
			}
			// no bundle manifest, check for plugin.xml or fragment.xml
			entryName = PLUGIN_XML;
			if (z.getEntry(entryName) == null) {
				entryName = FRAGMENT_XML;
			}
			if (z.getEntry(entryName) != null) {
				pluginManifestIn = z.getInputStream(new ZipEntry(entryName));
				PluginEntry entry1 = pluginParser.parse(pluginManifestIn,
						pluginURL);
				addPluginEntry(entry1);
			}
		} catch (IOException e5) {
			String pluginFileString2 = pluginURL + "!" + entryName; //$NON-NLS-1$
			Utils.log(NLS.bind(Messages.InstalledSiteParser_ErrorAccessing, (new String[] { pluginFileString2 })));
		} catch (SAXException e3) {
			String pluginFileString1 = pluginURL + "!" + entryName; //$NON-NLS-1$
			Utils.log(NLS.bind(Messages.InstalledSiteParser_ErrorParsingFile, (new String[] { pluginFileString1 })));
		} finally {
			if (bundleManifestIn != null) {
				try {
					bundleManifestIn.close();
				} catch (IOException e4) {
				}
			}
			if (pluginManifestIn != null) {
				try {
					pluginManifestIn.close();
				} catch (IOException e2) {
				}
			}
			if (z != null) {
				try {
					z.close();
				} catch (IOException e1) {
				}
			}
		}
	}
	/**
	 * @param file a plugin directory
	 * @param compareTimeStamps set to true when looking for plugins changed since last time they were detected
	 */
	private void detectUnpackedPlugin(File file, boolean compareTimeStamps) {
		// unpacked plugin
		long dirTimestamp = file.lastModified();
		File pluginFile = new File(file, META_MANIFEST_MF);
		try {
			// First, check if has valid bundle manifest
			BundleManifest bundleManifest = new BundleManifest(pluginFile);
			if (bundleManifest.exists()) {
				if (compareTimeStamps
						&& dirTimestamp <= pluginsChangeStamp
						&& pluginFile.lastModified() <= pluginsChangeStamp)
					return;
				PluginEntry entry = bundleManifest.getPluginEntry();
				addPluginEntry(entry);
			} else {
				// no bundle manifest, check for plugin.xml or fragment.xml
				pluginFile = new File(file, PLUGIN_XML);
				if (!pluginFile.exists()) { 
					pluginFile = new File(file, FRAGMENT_XML); 
				}
				if (pluginFile.exists() && !pluginFile.isDirectory()) {
					// TODO in the future, assume that the timestamps are not
					// reliable,
					// or that the user manually modified an existing plugin,
					// so
					// the apparently modifed plugin may actually be configured
					// already.
					// We will need to double check for this. END to do.
					if (compareTimeStamps 
							&& dirTimestamp <= pluginsChangeStamp
							&& pluginFile.lastModified() <= pluginsChangeStamp)
						return;
					PluginEntry entry = pluginParser.parse(pluginFile);
					addPluginEntry(entry);
				}
			}
		} catch (IOException e) {
			String pluginFileString = pluginFile.getAbsolutePath();
			if (ConfigurationActivator.DEBUG)
				Utils.log(Utils.newStatus(NLS.bind(Messages.InstalledSiteParser_ErrorParsingFile, (new String[] { pluginFileString })), e));
			else
				Utils.log(NLS.bind(Messages.InstalledSiteParser_ErrorAccessing, (new String[] { pluginFileString })));
		} catch (SAXException e) {
			String pluginFileString = pluginFile.getAbsolutePath();
			Utils.log(NLS.bind(Messages.InstalledSiteParser_ErrorParsingFile, (new String[] { pluginFileString })));
        }
	}

	/**
	 * @return list of feature url's (relative to site)
	 */
	private synchronized String[] getDetectedFeatures() {
		if (featureEntries == null)
			detectFeatures();
		String[] features = new String[featureEntries.size()];
		Iterator iterator = featureEntries.values().iterator();
		for (int i=0; i<features.length; i++)
			features[i] = ((FeatureEntry)iterator.next()).getURL();
		return features;
	}

	/**
	 * @return list of plugin url's (relative to site)
	 */
	private synchronized String[] getDetectedPlugins() {
		if (pluginEntries == null)
			detectPlugins();
		
		String[] plugins = new String[pluginEntries.size()];
		for (int i=0; i<plugins.length; i++)
			plugins[i] = ((PluginEntry)pluginEntries.get(i)).getURL();
		return plugins;
	}

	private void computeChangeStamp() {
		changeStamp = Math.max(computeFeaturesChangeStamp(), computePluginsChangeStamp());
//		changeStampIsValid = true;
	}

	private synchronized long computeFeaturesChangeStamp() {
		if (featuresChangeStamp > 0)
			return featuresChangeStamp;
		
		long start = 0;
		if (ConfigurationActivator.DEBUG)
			start = (new Date()).getTime();
		String[] features = getFeatures();
	
		// compute stamp for the features directory
		long dirStamp = 0;
		if (PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL())) {
			File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
			File featuresDir = new File(root, FEATURES);
			dirStamp = featuresDir.lastModified();
		}
		featuresChangeStamp = Math.max(dirStamp, computeStamp(features));
		if (ConfigurationActivator.DEBUG) {
			long end = (new Date()).getTime();
			Utils.debug(resolvedURL.toString() + " feature stamp: " + featuresChangeStamp + " in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$   
		}
		return featuresChangeStamp;
	}

	private synchronized long computePluginsChangeStamp() {
		if (pluginsChangeStamp > 0)
			return pluginsChangeStamp;
		
		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL())) {
			Utils.log(NLS.bind(Messages.SiteEntry_computePluginStamp, (new String[] { resolvedURL.toExternalForm() })));
			return 0;
		}

		// compute stamp for the plugins directory
		File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		File pluginsDir = new File(root, PLUGINS);
		if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
			Utils.debug(NLS.bind(Messages.SiteEntry_pluginsDir, (new String[] { pluginsDir.getAbsolutePath() })));
			return 0;
		}

		pluginsChangeStamp = pluginsDir.lastModified();
		return pluginsChangeStamp;
	}

	private long computeStamp(String[] targets) {

		long result = 0;
		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL())) {
			// NOTE:  this path should not be executed until we support running
			//        from an arbitrary URL (in particular from http server). For
			//        now just compute stamp across the list of names. Eventually
			//        when general URLs are supported we need to do better (factor
			//        in at least the existence of the target). However, given this
			//        code executes early on the startup sequence we need to be
			//        extremely mindful of performance issues.
			// In fact, we should get the last modified from the connection
			for (int i = 0; i < targets.length; i++)
				result ^= targets[i].hashCode();
			Utils.debug("*WARNING* computing stamp using URL hashcodes only"); //$NON-NLS-1$
		} else {
			// compute stamp across local targets
			File rootFile = new File(resolvedURL.getFile().replace('/', File.separatorChar));
			if (rootFile.exists()) {
				File f = null;
				for (int i = 0; i < targets.length; i++) {
					f = new File(rootFile, targets[i]);
					if (f.exists())
						result = Math.max(result, f.lastModified());
				}
			}
		}

		return result;
	}
	
	public void setLinkFileName(String linkFileName) {
		this.linkFileName = linkFileName;
	}
	
	public String getLinkFileName() {
		return linkFileName;
	}

	public boolean isExternallyLinkedSite() {
		return (linkFileName != null && !linkFileName.trim().equals("")); //$NON-NLS-1$
	}

	public synchronized void refresh() {
		// reset computed values. Will be updated on next access.
		featuresChangeStamp = 0;
		pluginsChangeStamp = 0;
		changeStamp = 0;
		featureEntries = null;
		pluginEntries = null;
	}
	
	public void refreshPlugins() {
		// reset computed values. Will be updated on next access.
		pluginsChangeStamp = 0;
		changeStamp = 0;
		pluginEntries = null;
	}
	
	public void addFeatureEntry(IFeatureEntry feature) {
		if (featureEntries == null)
			featureEntries = new HashMap();
		// Make sure we keep the larger version of same feature
		IFeatureEntry existing = (FeatureEntry)featureEntries.get(feature.getFeatureIdentifier());
		if (existing != null) {
			VersionedIdentifier existingVersion = new VersionedIdentifier(existing.getFeatureIdentifier(), existing.getFeatureVersion());
			VersionedIdentifier newVersion = new VersionedIdentifier(feature.getFeatureIdentifier(), feature.getFeatureVersion());
			if (existingVersion.getVersion().compareTo(newVersion.getVersion()) < 0) {
				featureEntries.put(feature.getFeatureIdentifier(), feature);
				pluginsChangeStamp = 0;
			} else if (existingVersion.equals(newVersion)) {
				// log error if same feature version/id but a different url
				if (feature instanceof FeatureEntry && existing instanceof FeatureEntry &&
						!((FeatureEntry)feature).getURL().equals(((FeatureEntry)existing).getURL()))
				Utils.log(NLS.bind(Messages.SiteEntry_duplicateFeature, (new String[] { getURL().toExternalForm(), existing.getFeatureIdentifier() })));
			}
		} else {
			featureEntries.put(feature.getFeatureIdentifier(), feature);
			pluginsChangeStamp = 0;
		}
		if (feature instanceof FeatureEntry)
			((FeatureEntry)feature).setSite(this);
	}
	
	public FeatureEntry[] getFeatureEntries() {
		if (featureEntries == null)
			detectFeatures();
		
		if (featureEntries == null)
			return new FeatureEntry[0];
		return (FeatureEntry[])featureEntries.values().toArray(new FeatureEntry[featureEntries.size()]);
	}
	
	public void addPluginEntry(PluginEntry plugin) {
		if (pluginEntries == null)
			pluginEntries = new ArrayList();
		// Note: we could use the latest version of the same plugin, like we do for features, but we let the runtime figure it out
		pluginEntries.add(plugin);
	}
	
	public PluginEntry[] getAllPluginEntries() {
		if (pluginEntries == null)
			detectPlugins();
		return (PluginEntry[])pluginEntries.toArray(new PluginEntry[pluginEntries.size()]);
	}
	
	public void loadFromDisk(long lastChange) throws CoreException{
		featuresChangeStamp = lastChange;
		pluginsChangeStamp = lastChange;
		detectFeatures();
		detectPlugins();
	}
	
	/**
	 * Saves state as xml content in a given parent element
	 * @param doc
	 */
	public Element toXML(Document doc) {

		Element siteElement = doc.createElement(CFG_SITE);
		
		if (getURL() != null) {
			URL toPersist = (config == null || config.isTransient()) ? getURL() : Utils.makeRelative(Utils.getInstallURL(), getURL());
			siteElement.setAttribute(CFG_URL, toPersist.toString());
		}
		
		siteElement.setAttribute(CFG_ENABLED, isEnabled() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		siteElement.setAttribute(CFG_UPDATEABLE, isUpdateable() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		if (isExternallyLinkedSite()) 
			siteElement.setAttribute(CFG_LINK_FILE, getLinkFileName().trim().replace(File.separatorChar, '/')); 

		int type = getSitePolicy().getType();
		String typeString = CFG_POLICY_TYPE_UNKNOWN;
		try {
			typeString = CFG_POLICY_TYPE[type];
		} catch (IndexOutOfBoundsException e) {
			// ignore bad attribute ...
		}
		siteElement.setAttribute(CFG_POLICY, typeString); 
		String[] list = getSitePolicy().getList();
		if (list.length > 0) {
			StringBuffer sb = new StringBuffer(256);
			for (int i=0; i<list.length-1; i++) {
				sb.append(list[i]);
				sb.append(',');
			}
			sb.append(list[list.length-1]);
			siteElement.setAttribute(CFG_LIST, sb.toString());
		}
//		// note: we don't save features inside the site element.
		
		// collect feature entries
//		configElement.setAttribute(CFG_FEATURE_ENTRY_DEFAULT, defaultFeature);
		FeatureEntry[] feats = getFeatureEntries();
		for (int i = 0; i < feats.length; i++) {
			Element featureElement = feats[i].toXML(doc);
			siteElement.appendChild(featureElement);
		}
		
		return siteElement;
	}
	
	private void validateFeatureEntries() {
		File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		Iterator iterator = featureEntries.values().iterator();
		Collection deletedFeatures = new ArrayList();
		while(iterator.hasNext()) {
			FeatureEntry feature = (FeatureEntry)iterator.next();
			// Note: in the future, we can check for absolute url as well.
			//       For now, feature url is features/org.eclipse.foo/feature.xml
			File featureXML = new File(root, feature.getURL());
			if (!featureXML.exists())
				deletedFeatures.add(feature.getFeatureIdentifier());
		}
		for(Iterator it=deletedFeatures.iterator(); it.hasNext();){
			featureEntries.remove(it.next());
		}
	}
	
	private void validatePluginEntries() {
		File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		Collection deletedPlugins = new ArrayList();
		for (int i=0; i<pluginEntries.size(); i++) {
			PluginEntry plugin = (PluginEntry)pluginEntries.get(i);
			// Note: in the future, we can check for absolute url as well.
			//       For now, feature url is plugins/org.eclipse.foo/plugin.xml
			File pluginLocation = new File(root, plugin.getURL());
			if (!pluginLocation.exists())
				deletedPlugins.add(plugin);
		}
		for(Iterator it=deletedPlugins.iterator(); it.hasNext();){
			pluginEntries.remove(it.next());
		}
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enable) {
		this.enabled = enable;
	}
	
	public FeatureEntry getFeatureEntry(String id) {
		FeatureEntry[] features = getFeatureEntries();
		for (int i=0; i<features.length; i++)
			if (features[i].getFeatureIdentifier().equals(id)) 
				return features[i];
		return null;
	}
	
	
	public boolean unconfigureFeatureEntry(IFeatureEntry feature) {
		FeatureEntry existingFeature = getFeatureEntry(feature.getFeatureIdentifier());
		if (existingFeature != null)
			featureEntries.remove(existingFeature.getFeatureIdentifier());
		return existingFeature != null;
	}
	
	/*
	 * This is a bit of a hack.
	 * When no features were added to the site, but the site is initialized from platform.xml 
	 * we need to set the feature set to empty, so we don't try to detect them.
	 */
	public void initialized() { 
		if (featureEntries == null)
			featureEntries = new HashMap();
	}
}
