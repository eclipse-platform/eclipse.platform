package org.eclipse.update.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

import org.apache.xerces.parsers.SAXParser;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * parse the default site.xml
 */

public class InstallConfigurationParser extends DefaultHandler {

	private SAXParser parser;
	private InputStream siteStream;
	private URL siteURL;
	private InstallConfigurationModel config;
	private ConfiguredSiteModel configSite;
	public static final String CONFIGURATION = "configuration"; //$NON-NLS-1$
	public static final String CONFIGURATION_SITE = "site"; //$NON-NLS-1$
	public static final String FEATURE = "feature"; //$NON-NLS-1$
	public static final String ACTIVITY = "activity"; //$NON-NLS-1$

	// optimization: cache Site
	private Map sites = new HashMap();

	private ResourceBundle bundle;

	/**
	 * Constructor for DefaultSiteParser
	 */
	public InstallConfigurationParser(
		InputStream siteStream,
		InstallConfigurationModel config)
		throws IOException, SAXException, CoreException {
		super();
		parser = new SAXParser();
		parser.setContentHandler(this);

		this.siteStream = siteStream;
		Assert.isTrue(config instanceof InstallConfigurationModel);
		this.config = (InstallConfigurationModel) config;

		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("Start parsing Configuration:" + (config).getURL().toExternalForm()); //$NON-NLS-1$
		}

		bundle = getResourceBundle();

		parser.parse(new InputSource(this.siteStream));
	}

	/**
	 * return the appropriate resource bundle for this sitelocal
	 */
	private ResourceBundle getResourceBundle() throws IOException, CoreException {
		ResourceBundle bundle = null;
		URL url = null;
		try {
			url = UpdateManagerUtils.asDirectoryURL(config.getURL());
			ClassLoader l = new URLClassLoader(new URL[] { url }, null);
			bundle =
				ResourceBundle.getBundle(
					SiteLocalModel.SITE_LOCAL_FILE,
					Locale.getDefault(),
					l);
		} catch (MissingResourceException e) {
			UpdateManagerPlugin.warn(e.getLocalizedMessage() + ":" + url.toExternalForm()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			UpdateManagerPlugin.warn(e.getLocalizedMessage()); //$NON-NLS-1$
		}
		return bundle;
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
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		try {

			String tag = localName.trim();

			if (tag.equalsIgnoreCase(CONFIGURATION)) {
				processConfig(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(CONFIGURATION_SITE)) {
				processSite(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(FEATURE)) {
				processFeature(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(ACTIVITY)) {
				processActivity(attributes);
				return;
			}

		} catch (MalformedURLException e) {
			throw new SAXException(Policy.bind("Parser.UnableToCreateURL", e.getMessage()), e); //$NON-NLS-1$
		} catch (CoreException e) {
			throw new SAXException(Policy.bind("Parser.InternalError", e.toString()), e); //$NON-NLS-1$
		}

	}

	/** 
	 * process the Site info
	 */
	private void processSite(Attributes attributes)
		throws MalformedURLException, CoreException {

		//site url
		String urlString = attributes.getValue("url"); //$NON-NLS-1$
		siteURL = new URL(urlString);
		ISite site = SiteManager.getSite(siteURL);
		sites.put(urlString,site);

		// policy
		String policyString = attributes.getValue("policy"); //$NON-NLS-1$
		int policy = Integer.parseInt(policyString);

		// configuration site
		BaseSiteLocalFactory factory = new BaseSiteLocalFactory();
		configSite = factory.createConfigurationSiteModel((SiteModel) site, policy);

		//platform url
		String platformURLString = attributes.getValue("platformURL"); //$NON-NLS-1$
		configSite.setPlatformURLString(platformURLString);

		// check if the site exists and is updatable
		// update configSite
		URL	urlToCheck = new URL(configSite.getPlatformURLString());
	 	IPlatformConfiguration runtimeConfig = BootLoader.getCurrentPlatformConfiguration();			
	 	IPlatformConfiguration.ISiteEntry entry = runtimeConfig.findConfiguredSite(urlToCheck);	 
	 	if (entry!=null){	
		 	configSite.isUpdatable(entry.isUpdateable());
	 	} else {
	 		UpdateManagerPlugin.warn("Unable to retrieve site:" +platformURLString+" from platform.");
	 	}
	 	String updatable = configSite.isUpdatable()?"true":"false";

		// add to install configuration
		config.addConfigurationSiteModel(configSite);

		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("End process config site url:" + urlString + " policy:" + policyString + " updatable:"+updatable ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

	}

	/** 
	 * process the DefaultFeature info
	 */
	private void processFeature(Attributes attributes)
		throws MalformedURLException, CoreException {

		// url
		String path = attributes.getValue("url"); //$NON-NLS-1$
		URL url = UpdateManagerUtils.getURL(siteURL, path, null);

		// configured ?
		String configuredString = attributes.getValue("configured"); //$NON-NLS-1$
		boolean configured = configuredString.trim().equalsIgnoreCase("true") ? true : false; //$NON-NLS-1$

		if (url != null) {
			FeatureReference ref = new FeatureReference();
			ref.setSite((ISite) configSite.getSiteModel());
			ref.setURL(url);
			if (ref != null)
				if (configured) {
					(configSite.getConfigurationPolicyModel()).addConfiguredFeatureReference(
						(FeatureReferenceModel) ref);
				} else
					(configSite.getConfigurationPolicyModel()).addUnconfiguredFeatureReference(
						(FeatureReferenceModel) ref);

			//updateURL
			String updateURLString = attributes.getValue("updateURL"); //$NON-NLS-1$
			URLEntry entry = new URLEntry();
			entry.setURLString(updateURLString);
			entry.resolve(siteURL, null);

			// DEBUG:		
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
				UpdateManagerPlugin.debug("End Processing DefaultFeature Tag: url:" + url.toExternalForm()); //$NON-NLS-1$
			}

		} else {
			UpdateManagerPlugin.log( Policy.bind("InstallConfigurationParser.FeatureReferenceNoURL"), new Exception());
		}

	}

	/** 
	 * process the Activity info
	 */
	private void processActivity(Attributes attributes) {

		// action
		String actionString = attributes.getValue("action"); //$NON-NLS-1$
		int action = Integer.parseInt(actionString);

		// create
		ConfigurationActivityModel activity =
			new BaseSiteLocalFactory().createConfigurationActivityModel();
		activity.setAction(action);

		// label
		String label = attributes.getValue("label"); //$NON-NLS-1$
		if (label != null)
			activity.setLabel(label);

		// date
		String dateString = attributes.getValue("date"); //$NON-NLS-1$
		Date date = new Date(Long.parseLong(dateString));
		activity.setDate(date);

		// status
		String statusString = attributes.getValue("status"); //$NON-NLS-1$
		int status = Integer.parseInt(statusString);
		activity.setStatus(status);

		config.addActivityModel(activity);

		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("End Processing Activity: action:" + actionString + " label: " + label + " date:" + dateString + " status" + statusString); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

	}

	/** 
	 * process the Config info
	 */
	private void processConfig(Attributes attributes) {

		// date
		long date = Long.parseLong(attributes.getValue("date")); //$NON-NLS-1$
		config.setCreationDate(new Date(date));
		
		//timeline
		String timelineString = attributes.getValue("timeline"); //$NON-NLS-1$
		long timeline = 0;
		if (timelineString!=null) {
			timeline = Long.parseLong(timelineString);
		} else {
			timeline = config.getCreationDate().getTime();			
		}
		config.setTimeline(timeline);

		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("End Processing Config Tag: date:" + date+" timeline:"+ timeline); //$NON-NLS-1$
		}

	}
}