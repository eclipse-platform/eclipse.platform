/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.util.zip.*;

import org.eclipse.core.internal.boot.*;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.configurator.IPlatformConfiguration.*;
import org.w3c.dom.*;
import org.xml.sax.*;


public class SiteEntry implements IPlatformConfiguration.ISiteEntry, IConfigurationConstants{	
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

	public SiteEntry(URL url) {
		this(url,null);
	}
	
	public SiteEntry(URL url, ISitePolicy policy) {
		if (url == null)
			try {
				url = new URL(PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR + "/" + "base" + "/"); //$NON-NLS-1$ //$NON-NLS-2$ // try using platform-relative URL
			} catch (MalformedURLException e) {
				url = PlatformConfiguration.getInstallURL(); // ensure we come up ... use absolute file URL
			}
			
		if (policy == null)
			policy = new SitePolicy(DEFAULT_POLICY_TYPE, DEFAULT_POLICY_LIST);

		if (url.getProtocol().equals("file")) {
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
		if (url.getProtocol().equals(PlatformURLHandler.PROTOCOL)) {
			try {
				resolvedURL = PlatformConfiguration.resolvePlatformURL(url); // 19536
			} catch (IOException e) {
				// will use the baseline URL ...
			}
		}
	}

	public void setConfig(Configuration config) {
		this.config = config;
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

		// bad policy type
		return new String[0];
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

		if (!PlatformConfiguration.supportsDetection(resolvedURL))
			return;

		// locate feature entries on site
		File siteRoot = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		File featuresDir = new File(siteRoot, FEATURES);
		if (featuresDir.exists()) {
			// handle the installed features under the features directory
			File[] dirs = featuresDir.listFiles(new FileFilter() {
				public boolean accept(File f) {
					boolean valid = f.isDirectory() && (new File(f,FEATURE_XML).exists());
					if (!valid)
						System.out.println("Unable to find feature.xml in directory:" + f.getAbsolutePath());
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
					Utils.log(Messages.getString("InstalledSiteParser.UnableToCreateURLForFile", featuresDir.getAbsolutePath()));
					//$NON-NLS-1$
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

		if (pluginEntries != null)
			validatePluginEntries();
		else
			pluginEntries = new ArrayList();

		if (!PlatformConfiguration.supportsDetection(resolvedURL))
			return;

		// locate plugin entries on site
		File pluginsDir = new File(resolvedURL.getFile(), PLUGINS);
		
		if (pluginsDir.exists() && pluginsDir.isDirectory()) {
			File[] files = pluginsDir.listFiles();
			for (int i = 0; i < files.length; i++) {
				if(files[i].isDirectory()){
					detectUnpackedPlugin(files[i]);
				}else if(files[i].getName().endsWith(".jar")){
					detectPackedPlugin(files[i]);
				}else{
					// not bundle file
				}
			}
		} 
		
		Utils.debug(resolvedURL.toString() + " located  " + pluginEntries.size() + " plugin(s)"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param file
	 *            a plugin jar
	 */
	private void detectPackedPlugin(File file) {
		// plugin to run directly from jar
		long jarTimestamp = file.lastModified();
		if (jarTimestamp <= pluginsChangeStamp) {
			return;
		}
		String entryName = META_MANIFEST_MF;
		ZipFile z = null;
		InputStream bundleManifestIn = null;
		InputStream pluginManifestIn = null;
		String pluginURL = PLUGINS + "/" + file.getName();
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
			String pluginFileString2 = pluginURL + "!" + entryName;
			Utils.log(Messages.getString("InstalledSiteParser.ErrorAccessing",
					pluginFileString2)); //$NON-NLS-1$
		} catch (SAXException e3) {
			String pluginFileString1 = pluginURL + "!" + entryName;
			Utils.log(Messages.getString(
					"InstalledSiteParser.ErrorParsingFile", pluginFileString1)); //$NON-NLS-1$
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
	 * @param file
	 *            a plugin directory
	 */
	private void detectUnpackedPlugin(File file) {
		// unpacked plugin
		long dirTimestamp = file.lastModified();
		File pluginFile = new File(file, META_MANIFEST_MF);
		try {
			// First, check if has valid bundle manifest
			BundleManifest bundleManifest = new BundleManifest(pluginFile);
			if (bundleManifest.exists()) {
				if (dirTimestamp <= pluginsChangeStamp
						&& pluginFile.lastModified() <= pluginsChangeStamp)
					return;
				PluginEntry entry = bundleManifest.getPluginEntry();
				addPluginEntry(entry);
			} else {
				// no bundle manifest, check for plugin.xml or fragment.xml
				pluginFile = new File(file, PLUGIN_XML);
				if (!pluginFile.exists()) { //$NON-NLS-1$
					pluginFile = new File(file, FRAGMENT_XML); //$NON-NLS-1$
				}
				if (pluginFile.exists() && !pluginFile.isDirectory()) {
					// TODO in the future, assume that the timestamps are not
					// reliable,
					// or that the user manually modified an existing plugin,
					// so
					// the apparently modifed plugin may actually be configured
					// already.
					// We will need to double check for this. END to do.
					if (dirTimestamp <= pluginsChangeStamp
							&& pluginFile.lastModified() <= pluginsChangeStamp)
						return;
					PluginEntry entry = pluginParser.parse(pluginFile);
					addPluginEntry(entry);
				}
			}
		} catch (IOException e) {
			String pluginFileString = pluginFile.getAbsolutePath();
			if (ConfigurationActivator.DEBUG)
				Utils.log(Utils.newStatus(Messages.getString(
						"InstalledSiteParser.ErrorParsingFile", pluginFileString), e));//$NON-NLS-1$
			else
				Utils.log(Messages.getString("InstalledSiteParser.ErrorAccessing",
						pluginFileString)); //$NON-NLS-1$
		} catch (SAXException e) {
			String pluginFileString = pluginFile.getAbsolutePath();
			Utils.log(Messages.getString(
					"InstalledSiteParser.ErrorParsingFile", pluginFileString)); //$NON-NLS-1$
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
		if (PlatformConfiguration.supportsDetection(resolvedURL)) {
			File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
			File featuresDir = new File(root, FEATURES);
			dirStamp = featuresDir.lastModified();
		}
		featuresChangeStamp = Math.max(dirStamp, computeStamp(features));
		if (ConfigurationActivator.DEBUG) {
			long end = (new Date()).getTime();
			Utils.debug(resolvedURL.toString() + " feature stamp: " + featuresChangeStamp + " in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		return featuresChangeStamp;
	}

	private synchronized long computePluginsChangeStamp() {
		if (pluginsChangeStamp > 0)
			return pluginsChangeStamp;
		
		long start = 0;
		if (ConfigurationActivator.DEBUG)
			start = (new Date()).getTime();
		String[] plugins = getPlugins();
		// compute stamp for the features directory
		long dirStamp = 0;
		if (PlatformConfiguration.supportsDetection(resolvedURL)) {
			File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
			File pluginsDir = new File(root, PLUGINS);
			dirStamp = pluginsDir.lastModified();
		}
		pluginsChangeStamp = Math.max(dirStamp, computeStamp(plugins));
		if (ConfigurationActivator.DEBUG) {
			long end = (new Date()).getTime();
			Utils.debug(resolvedURL.toString() + " plugin stamp: " + pluginsChangeStamp + " in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		return pluginsChangeStamp;
	}

	private long computeStamp(String[] targets) {

		long result = 0;
		if (!PlatformConfiguration.supportsDetection(resolvedURL)) {
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
			if (existingVersion.compareVersion(newVersion) == VersionedIdentifier.LESS_THAN) {
				featureEntries.put(feature.getFeatureIdentifier(), feature);
				pluginsChangeStamp = 0;
			} else if (existingVersion.equals(newVersion)) {
				// log error if same feature version/id but a different url
				if (feature instanceof FeatureEntry && existing instanceof FeatureEntry &&
						!((FeatureEntry)feature).getURL().equals(((FeatureEntry)existing).getURL()))
				Utils.log("Duplicate feature found on site "+getURL() + " : " + existing.getFeatureIdentifier());
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
		else
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
		
		if (getURL().toString() != null)
			siteElement.setAttribute(CFG_URL, getURL().toString());

		siteElement.setAttribute(CFG_ENABLED, isEnabled() ? "true" : "false");
		siteElement.setAttribute(CFG_UPDATEABLE, isUpdateable() ? "true" : "false");
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
	void initialized() { 
		if (featureEntries == null)
			featureEntries = new HashMap();
	}
}