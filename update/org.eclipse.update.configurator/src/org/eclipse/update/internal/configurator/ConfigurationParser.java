/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * parse the default site.xml
 */

public class ConfigurationParser extends DefaultHandler implements IConfigurationConstants {
	
	private static final String URL_PROPERTY = "org.eclipse.update.resolution_url"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();
	private SAXParser parser;
	
	private URL currentSiteURL;
	private Configuration config;
	private URL configURL;
	private URL installLocation;
	
	/**
	 * Constructor for ConfigurationParser
	 */
	public ConfigurationParser() throws InvocationTargetException {

		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			Utils.log(Utils.newStatus("ConfigurationParser", e)); //$NON-NLS-1$
			throw new InvocationTargetException(e);
		} catch (SAXException e) {
			Utils.log(Utils.newStatus("ConfigurationParser", e)); //$NON-NLS-1$
			throw new InvocationTargetException(e);
		}
	}
	
	public Configuration parse(URL url, URL installLocation) throws Exception {

		// DEBUG:		
		Utils.debug("Start parsing Configuration:" + url); //$NON-NLS-1$	
		long lastModified = 0;
		this.configURL = url;
		this.installLocation = installLocation;
		try {
			if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
				File inputFile = URIUtil.toFile(URIUtil.toURI(url));
				if (!inputFile.exists() || !inputFile.canRead())
					return null;
				lastModified = inputFile.lastModified();
				try (InputStream input = new FileInputStream(inputFile)) {
					parser.parse(new InputSource(input), this);
				}
			} else {
				try (InputStream input = url.openStream()) {
					parser.parse(new InputSource(input), this);
				}
			}
			return config;
		} catch (Exception e) {
			Utils.log(Utils.newStatus("ConfigurationParser.parse() error:", e)); //$NON-NLS-1$
			throw e;
		} finally {
			if (config != null) {
				config.setLastModified(lastModified);
			}
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		// DEBUG:		
		Utils.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {

			String tag = localName.trim();

			if (tag.equalsIgnoreCase(CFG)) {
				processConfig(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(CFG_SITE)) {
				processSite(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(CFG_FEATURE_ENTRY)) {
				processFeature(attributes);
				return;
			}

		} catch (MalformedURLException e) {
			throw new SAXException(NLS.bind(Messages.InstalledSiteParser_UnableToCreateURL, (new String[] { e.getMessage() })), e);
		} catch (CoreException e) {
			throw new SAXException(NLS.bind(Messages.InstalledSiteParser_ErrorParsingFile, (new String[] { e.toString() })), e);
		}
	}

	/** 
	 * process the Site info
	 */
	private void processSite(Attributes attributes)
		throws MalformedURLException, CoreException {

		if (config == null)
			return;
		
		// reset current site
		currentSiteURL = null;
		
		String urlString = attributes.getValue(CFG_URL); 
		if (urlString == null)
			return;

		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			// try relative to install url
			url = new URL(PlatformConfiguration.getInstallURL(), urlString);
			return;
		}
		
		// when reading externalized URLs, need to convert them to absolute form
		String property = System.getProperty(URL_PROPERTY, EMPTY_STRING);
		URL root = property == null || property.length() == 0 ? Utils.getInstallURL() : new URL(property);
		url = Utils.makeAbsolute(root, url);
		
		if (!isValidSite(url))
			return;
		
		// use this new site
		currentSiteURL = url;

		int policyType;
		String[] policyList = null;
		String typeString = attributes.getValue(CFG_POLICY); 
		if (typeString == null) {
			policyType = PlatformConfiguration.getDefaultPolicy();
			policyList = DEFAULT_POLICY_LIST;
		} else {
			int i;
			for (i = 0; i < CFG_POLICY_TYPE.length; i++) {
				if (typeString.equals(CFG_POLICY_TYPE[i])) {
					break;
				}
			}
			if (i >= CFG_POLICY_TYPE.length) {
				policyType = PlatformConfiguration.getDefaultPolicy();
				policyList = DEFAULT_POLICY_LIST;
			} else {
				policyType = i;
				String pluginList = attributes.getValue(CFG_LIST);
				if (pluginList != null) {
					StringTokenizer st = new StringTokenizer(pluginList,","); //$NON-NLS-1$
					policyList = new String[st.countTokens()];
					for (i=0; i<policyList.length; i++)
						policyList[i] = st.nextToken();
				}
			}
		}

		SitePolicy sp = new SitePolicy(policyType, policyList);
		SiteEntry site = new SiteEntry(url, sp);

		String flag = attributes.getValue(CFG_UPDATEABLE); 
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				site.setUpdateable(true);
			else
				site.setUpdateable(false);
		}
		
		flag = attributes.getValue(CFG_ENABLED); 
		if (flag != null && flag.equals("false")) //$NON-NLS-1$
			site.setEnabled(false);
		else
			site.setEnabled(true);

		String linkname = attributes.getValue(CFG_LINK_FILE); 
		if (linkname != null && !linkname.isEmpty()) {
			site.setLinkFileName(linkname.replace('/', File.separatorChar));
		}

		// DEBUG:		
		Utils.debug("End process config site url:" + urlString + " policy:" + typeString + " updatable:"+flag ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		currentSiteURL = site.getURL();
		config.addSiteEntry(currentSiteURL.toExternalForm(), site);
	}
	
	/** 
	 * process the DefaultFeature info
	 */
	private void processFeature(Attributes attributes)
		throws MalformedURLException, CoreException {

		if (currentSiteURL == null)
			return; // the site was not correct
			
		String id = attributes.getValue(CFG_FEATURE_ENTRY_ID); 
		if (id == null)
			return;
		String version = attributes.getValue(CFG_FEATURE_ENTRY_VERSION); 
		String pluginVersion = attributes.getValue(CFG_FEATURE_ENTRY_PLUGIN_VERSION); 
		if (pluginVersion == null || pluginVersion.trim().length() == 0)
			pluginVersion = version;
		String pluginIdentifier = attributes.getValue(CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER); 
		if (pluginIdentifier != null && pluginIdentifier.trim().length() == 0)
			pluginIdentifier = null;
		String application = attributes.getValue(CFG_FEATURE_ENTRY_APPLICATION); 
		
		// get install locations
		String locations = attributes.getValue(CFG_FEATURE_ENTRY_ROOT);
		StringTokenizer st = locations != null ? new StringTokenizer(locations,",") : new StringTokenizer(""); //$NON-NLS-1$ //$NON-NLS-2$
		ArrayList<URL> rootList = new ArrayList<>(st.countTokens());
		while (st.hasMoreTokens()){
			try{
				URL rootEntry = new URL(st.nextToken());
				rootList.add(rootEntry);
			} catch (MalformedURLException e) {
				// skip bad entries ...
			}
		}
		URL[] roots = rootList.toArray(new URL[rootList.size()]);

		// get primary flag
		boolean primary = false;
		String flag = attributes.getValue(CFG_FEATURE_ENTRY_PRIMARY); 
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				primary = true;
		}
		
		FeatureEntry featureEntry =  new FeatureEntry(id, version, pluginIdentifier, pluginVersion, primary, application, roots);

		// set the url
		String url = attributes.getValue(CFG_URL); 
		if (url != null && url.trim().length() > 0)
			featureEntry.setURL(url);
		
		SiteEntry site = config.getSiteEntry(currentSiteURL.toExternalForm());
		site.addFeatureEntry(featureEntry);
		
		// configured ?
//		String configuredString = attributes.getValue("configured"); //$NON-NLS-1$
//		boolean configured = configuredString.trim().equalsIgnoreCase("true") ? true : false; //$NON-NLS-1$
	}


	/** 
	 * process the Config info
	 */
	private void processConfig(Attributes attributes) {
		String date = attributes.getValue(CFG_DATE);
		if (date == null || date.trim().length() == 0)
			config = new Configuration(); // constructed with current date
		else {
			long time = 0;
			try {
				time = Long.parseLong(date);
				config = new Configuration(new Date(time));
			} catch (NumberFormatException e1) {
				time = new Date().getTime();
				Utils.log(NLS.bind(Messages.InstalledSiteParser_date, (new String[] { date })));
				config = new Configuration(); // constructed with current date
			}
		}
		
		config.setURL(configURL);
		config.setInstallLocation(installLocation);
		
		try {
			String sharedURLString = attributes.getValue(CFG_SHARED_URL);
			if (sharedURLString != null) {
				URL sharedURL = Utils.makeAbsolute(installLocation, new URL(sharedURLString));
				if (!sharedURL.equals(configURL)) {
					/*
					 * Bug 490591: Avoid StackOverflowError: The linked
					 * installations URLs are relative and intended to be
					 * dereferenced against the shared installation location.
					 * But it's possible our installLocation is not the real
					 * shared installation location.
					 */
					ConfigurationParser parser = new ConfigurationParser();
					Configuration sharedConfig = parser.parse(sharedURL, installLocation);
					if (sharedConfig == null)
						throw new Exception("Failed to parse shared configuration: " + sharedURL);
					config.setLinkedConfig(sharedConfig);
				}
			}
		} catch (Exception e) {
			// could not load from shared install
			Utils.log(Utils.newStatus(Messages.ConfigurationParser_cannotLoadSharedInstall, e)); 
		}

		String flag = attributes.getValue(CFG_TRANSIENT);
		if (flag != null) {
			config.setTransient(flag.equals("true")); //$NON-NLS-1$
		}
		
		// DEBUG:		
		Utils.debug("End Processing Config Tag: date:" + attributes.getValue(CFG_DATE)); //$NON-NLS-1$
	}
	
	private boolean isValidSite(URL url) {
		URL resolvedURL=  url;
		if (url.getProtocol().equals("platform")) { //$NON-NLS-1$
			try {				
				// resolve the config location relative to the configURL
				if (url.getPath().startsWith("/config")) {	
					URL config_loc = new URL(configURL, "..");
					resolvedURL = PlatformConfiguration.resolvePlatformURL(url, config_loc); // 19536
				}
				else 
					resolvedURL = PlatformConfiguration.resolvePlatformURL(url, config.getInstallURL()); // 19536
			} catch (IOException e) {
				// will use the baseline URL ...
			}
		}
		
		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL()))
			return false;

		File siteRoot = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		if (!siteRoot.exists()) {
			Utils.debug("Site " + resolvedURL + " does not exist "); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		return true;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		super.endElement(uri, localName, qName);
		
		// DEBUG:		
		Utils.debug("End Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {

			String tag = localName.trim();

			if (tag.equalsIgnoreCase(CFG)) {
				// This is a bit of a hack.
				// When no features were added to the site, but the site is initialized from platform.xml 
				// we need to set the feature set to empty, so we don't try to detect them.
				for (SiteEntry site : config.getSites())
					site.initialized();
				return;
			}
		} catch (Exception e) {
			// silent ignore
		}
	}
}
