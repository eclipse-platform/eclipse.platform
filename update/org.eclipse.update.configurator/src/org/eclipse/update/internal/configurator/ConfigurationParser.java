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
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

import org.eclipse.core.internal.boot.*;
import org.eclipse.core.runtime.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * parse the default site.xml
 */

public class ConfigurationParser extends DefaultHandler implements IConfigurationConstants {
	
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();
	private SAXParser parser;
	
	private URL currentSiteURL;
	private Configuration config;
	private URL configURL;
	private InputStream input;

	/**
	 * Constructor for ConfigurationParser
	 */
	public ConfigurationParser() throws InvocationTargetException {

		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			Utils.log(Utils.newStatus("ConfigurationParser", e));
			throw new InvocationTargetException(e);
		} catch (SAXException e) {
			Utils.log(Utils.newStatus("ConfigurationParser", e));
			throw new InvocationTargetException(e);
		}
	}
	
	public Configuration parse(URL url) throws Exception {

		// DEBUG:		
		//Utils.debug("Start parsing Configuration:" + (config).getURL().toExternalForm()); //$NON-NLS-1$	
		try {
			configURL = url;
			if ("file".equals(url.getProtocol())) {
				File inputFile = new File(url.getFile());
				if (!inputFile.exists() || !inputFile.canRead())
					return null;
				input = new FileInputStream(inputFile);
			} else 
				input = url.openStream();
			parser.parse(new InputSource(input), this);
			return config;
		} catch (Exception e) {
			Utils.log(Utils.newStatus("ConfigurationParser.parse() error:", e));
			throw e;
		} finally {
			try {
				if (input != null) { 
					input.close();
					input = null;
				}
			} catch (IOException e1) {
				Utils.log(e1.getLocalizedMessage());
			}
		}
	}

	/**
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 */
	public void startElement(
		String uri,
		String localName,
		String qName,
		Attributes attributes)
		throws SAXException {

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
			throw new SAXException(Messages.getString("Parser.UnableToCreateURL", e.getMessage()), e); //$NON-NLS-1$
		} catch (CoreException e) {
			throw new SAXException(Messages.getString("Parser.InternalError", e.toString()), e); //$NON-NLS-1$
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
		
		String urlString = attributes.getValue(CFG_URL); //$NON-NLS-1$
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
		
		if (!isValidSite(url))
			return;
		
		// use this new site
		currentSiteURL = url;

		int policyType;
		String[] policyList = null;
		String typeString = attributes.getValue(CFG_POLICY); //$NON-NLS-1$
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
				String pluginList = attributes.getValue(CFG_LIST);
				if (pluginList != null) {
					StringTokenizer st = new StringTokenizer(pluginList,",");
					policyList = new String[st.countTokens()];
					for (i=0; i<policyList.length; i++)
						policyList[i] = st.nextToken();
				}
			}
		}

		SitePolicy sp = new SitePolicy(policyType, policyList);
		SiteEntry site = (SiteEntry) new SiteEntry(url, sp);

		String flag = attributes.getValue(CFG_UPDATEABLE); //$NON-NLS-1$
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				site.setUpdateable(true);
			else
				site.setUpdateable(false);
		}
		
		flag = attributes.getValue(CFG_ENABLED); //$NON-NLS-1$
		if (flag != null && flag.equals("false"))
			site.setEnabled(false);
		else
			site.setEnabled(true);

		String linkname = attributes.getValue(CFG_LINK_FILE); //$NON-NLS-1$
		if (linkname != null && !linkname.equals("")) { //$NON-NLS-1$
			site.setLinkFileName(linkname.replace('/', File.separatorChar));
		}

		// DEBUG:		
		Utils.debug("End process config site url:" + urlString + " policy:" + typeString + " updatable:"+flag ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		config.addSiteEntry(url.toExternalForm(), site);
	}
	
	/** 
	 * process the DefaultFeature info
	 */
	private void processFeature(Attributes attributes)
		throws MalformedURLException, CoreException {

		if (currentSiteURL == null)
			return; // the site was not correct
			
		String id = attributes.getValue(CFG_FEATURE_ENTRY_ID); //$NON-NLS-1$
		if (id == null)
			return;
		String version = attributes.getValue(CFG_FEATURE_ENTRY_VERSION); //$NON-NLS-1$
		String pluginVersion = attributes.getValue(CFG_FEATURE_ENTRY_PLUGIN_VERSION); //$NON-NLS-1$
		if (pluginVersion == null || pluginVersion.trim().length() == 0)
			pluginVersion = version;
		String pluginIdentifier = attributes.getValue(CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER); //$NON-NLS-1$
		if (pluginIdentifier == null || pluginIdentifier.trim().length() == 0)
			pluginIdentifier = id;
		String application = attributes.getValue(CFG_FEATURE_ENTRY_APPLICATION); //$NON-NLS-1$
		
		// get install locations
		String locations = attributes.getValue(CFG_FEATURE_ENTRY_ROOT);
		StringTokenizer st = locations != null ? new StringTokenizer(locations,",") : new StringTokenizer("");
		ArrayList rootList = new ArrayList(st.countTokens());
		while (st.hasMoreTokens()){
			try{
				URL rootEntry = new URL(st.nextToken());
				rootList.add(rootEntry);
			} catch (MalformedURLException e) {
				// skip bad entries ...
			}
		}
		URL[] roots = (URL[]) rootList.toArray(new URL[rootList.size()]);

		// get primary flag
		boolean primary = false;
		String flag = attributes.getValue(CFG_FEATURE_ENTRY_PRIMARY); //$NON-NLS-1$
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				primary = true;
		}
		
		FeatureEntry featureEntry =  new FeatureEntry(id, version, pluginIdentifier, pluginVersion, primary, application, roots);

		// set the url
		String url = attributes.getValue(CFG_URL); //$NON-NLS-1$
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
		else
			config = new Configuration(new Date(date));
		
		config.setURL(configURL);
		
		try {
			String sharedURL = attributes.getValue(CFG_SHARED_URL);
			if (sharedURL != null) {
				ConfigurationParser parser = new ConfigurationParser();
				Configuration sharedConfig = parser.parse(new URL(sharedURL));
				if (sharedConfig == null)
					throw new Exception();
				config.setLinkedConfig(sharedConfig);
			}
		} catch (Exception e) {
			// could not load from shared install
			Utils.log(Utils.newStatus("Could not load from shared install", e));
		}

		String flag = attributes.getValue(CFG_TRANSIENT);
		if (flag != null) {
			config.setTransient(flag.equals("true"));
		}
		
		// DEBUG:		
		Utils.debug("End Processing Config Tag: date:" + attributes.getValue(CFG_DATE)); //$NON-NLS-1$
	}
	
	private boolean isValidSite(URL url) {
		URL resolvedURL=  url;
		if (url.getProtocol().equals(PlatformURLHandler.PROTOCOL)) {
			try {
				resolvedURL = PlatformConfiguration.resolvePlatformURL(url); // 19536
			} catch (IOException e) {
				// will use the baseline URL ...
			}
		}
		
		if (!PlatformConfiguration.supportsDetection(resolvedURL))
			return false;

		File siteRoot = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		if (!siteRoot.exists()) {
			Utils.debug("Site " + resolvedURL + " does not exist "); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		} else
			return true;
	}
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
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
				SiteEntry[] sites = config.getSites();
				for (int i=0; i<sites.length; i++)
					sites[i].initialized();
				return;
			}
		} catch (Exception e) {
			// silent ignore
		}
	}
}
