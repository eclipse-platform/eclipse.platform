package org.eclipse.update.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

import org.apache.xerces.parsers.SAXParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.internal.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * parse the default site.xml
 */

public class SiteLocalParser extends DefaultHandler {

	private SAXParser parser;
	private InputStream siteStream;
	private SiteLocalModel site;
	private String text;
	public static final String SITE = "localsite"; //$NON-NLS-1$
	public static final String CONFIG = "config"; //$NON-NLS-1$
	public static final String PRESERVED_CONFIGURATIONS = "preservedConfigurations"; //$NON-NLS-1$

	private ResourceBundle bundle;

	private IFeatureReference feature;

	// trus if we are now parsing preserved config
	private boolean preserved = false;

	/**
	 * Constructor for DefaultSiteParser
	 */
	public SiteLocalParser(InputStream siteStream, ILocalSite site) throws IOException, SAXException, CoreException {
		super();
		parser = new SAXParser();
		parser.setContentHandler(this);

		this.siteStream = siteStream;
		Assert.isTrue(site instanceof SiteLocalModel);
		this.site = (SiteLocalModel) site;

		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("Start parsing localsite:" + ((SiteLocalModel) site).getLocationURLString()); //$NON-NLS-1$
		}

		bundle = getResourceBundle();

		parser.parse(new InputSource(this.siteStream));
	}

	/**
	 * return the appropriate resource bundle for this sitelocal
	 */
	private ResourceBundle getResourceBundle() throws CoreException {
		ResourceBundle bundle = null;
		URL url = null;
		try {
			url = UpdateManagerUtils.asDirectoryURL(site.getLocationURL());
			ClassLoader l = new URLClassLoader(new URL[] { url }, null);
			bundle = ResourceBundle.getBundle(SiteLocalModel.SITE_LOCAL_FILE, Locale.getDefault(), l);
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
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		try {

			String tag = localName.trim();

			if (tag.equalsIgnoreCase(SITE)) {
				processSite(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(CONFIG)) {
				processConfig(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(PRESERVED_CONFIGURATIONS)) {
				preserved = true;
				return;
			}

		} catch (MalformedURLException e) {
			throw new SAXException(Policy.bind("Parser.UnableToCreateURL",e.getMessage()), e); //$NON-NLS-1$
		} catch (CoreException e) {
			throw new SAXException(Policy.bind("Parser.InternalError",e.toString()), e); //$NON-NLS-1$
		}

	}

	/** 
	 * process the Site info
	 */
	private void processSite(Attributes attributes) throws MalformedURLException {
		//
		String info = attributes.getValue("label"); //$NON-NLS-1$
		info = UpdateManagerUtils.getResourceString(info, bundle);
		site.setLabel(info);

		// history
		String historyString = attributes.getValue("history"); //$NON-NLS-1$
		int history;
		if (historyString == null || historyString.equals("")) { //$NON-NLS-1$
			history = SiteLocalModel.DEFAULT_HISTORY;
		} else {
			history = Integer.parseInt(historyString);
		}
		site.setMaximumHistoryCount(history);

		//stamp
		String stampString = attributes.getValue("stamp"); //$NON-NLS-1$
		long stamp = Long.parseLong(stampString);
		site.setStamp(stamp);
	
		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("End process Site label:" + info); //$NON-NLS-1$
		}

	}

	/** 
	 * process the Config info
	 */
	private void processConfig(Attributes attributes) throws MalformedURLException, CoreException {
		// url
		URL url = UpdateManagerUtils.getURL(site.getLocationURL(), attributes.getValue("url"), null); //$NON-NLS-1$
		String label = attributes.getValue("label"); //$NON-NLS-1$
		label = UpdateManagerUtils.getResourceString(label, bundle);
		
		InstallConfigurationModel config = new BaseSiteLocalFactory().createInstallConfigurationModel();
		config.setLocationURLString(url.toExternalForm());
		config.setLabel(label);
		config.resolve(url,getResourceBundle());
		try {
			config.initialize(); 
			// add the config
			if (preserved) {
				site.addPreservedInstallConfigurationModel(config);
			} else {
				site.addConfigurationModel(config);
			}
		} catch (CoreException e){
			UpdateManagerPlugin.warn("Error processing configuration history:"+url.toExternalForm(),e);
		}
		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("End Processing Config Tag: url:" + url.toExternalForm()); //$NON-NLS-1$
		}
	}

	/*
	 * @see ContentHandler#endElement(String, String, String)
	 */
	public void endElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// DEBUG:		
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_PARSING) {
			UpdateManagerPlugin.debug("End Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String tag = localName.trim();

		if (tag.equalsIgnoreCase(PRESERVED_CONFIGURATIONS)) {
			preserved = false;
			return;
		}

	}

}