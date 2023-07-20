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


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A more complete feature parser. It adds the plugins listed to the feature.
 */
public class FullFeatureParser extends DefaultHandler implements IConfigurationConstants{

	private SAXParser parser;
	private final FeatureEntry feature;
	private URL url;
	private boolean isDescription;
	private final StringBuffer description = new StringBuffer();

	/**
	 * Constructs a feature parser.
	 */
	@SuppressWarnings("restriction")
	public FullFeatureParser(FeatureEntry feature) {
		this.feature = feature;
		try {
			this.parser = org.eclipse.core.internal.runtime.XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE(true);
		} catch (ParserConfigurationException e) {
			System.out.println(e);
		} catch (SAXException e) {
			System.out.println(e);
		}
	}
	/**
	 */
	public void parse(){
		InputStream in = null;
		try {
			if (feature.getSite() == null)
				return;
			this.url = new URL(feature.getSite().getResolvedURL(), feature.getURL() + FEATURE_XML);
			in = url.openStream();
			parser.parse(new InputSource(in), this);
		} catch (SAXException e) {
		} catch (IOException e) {
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e1) {
					Utils.log(e1.getLocalizedMessage());
				}
		}
	}

	/**
	 * Handle start of element tags
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 * @since 2.0
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		Utils.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if ("plugin".equals(localName)) { //$NON-NLS-1$
			processPlugin(attributes);
		} else if ("description".equals(localName)){ //$NON-NLS-1$
			isDescription = true;
		} else if ("license".equals(localName)) { //$NON-NLS-1$
			processLicense(attributes);
		}
	}

	/*
	 * Process feature information
	 */
	private void processPlugin(Attributes attributes) {

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		if (id == null || id.trim().isEmpty()
		|| ver == null || ver.trim().isEmpty()) {
			System.out.println(NLS.bind(Messages.FeatureParser_IdOrVersionInvalid, (new String[] { id, ver})));
		} else {
//			String label = attributes.getValue("label"); //$NON-NLS-1$
//			String provider = attributes.getValue("provider-name"); //$NON-NLS-1$
			String nl = attributes.getValue("nl"); //$NON-NLS-1$
			String os = attributes.getValue("os"); //$NON-NLS-1$
			String ws = attributes.getValue("ws"); //$NON-NLS-1$
			String arch = attributes.getValue("arch"); //$NON-NLS-1$
			if (!Utils.isValidEnvironment(os, ws, arch,nl))
				return;

			PluginEntry plugin = new PluginEntry();
			plugin.setPluginIdentifier(id);
			plugin.setPluginVersion(ver);
			feature.addPlugin(plugin);
			
			Utils.
				debug("End process DefaultFeature tag: id:" +id + " ver:" +ver + " url:" + feature.getURL()); 	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	private void processLicense(Attributes attributes ){
		feature.setLicenseURL(attributes.getValue("url")); //$NON-NLS-1$
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (!isDescription)
			return;
		description.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("description".equals(localName)) { //$NON-NLS-1$
			isDescription = false;
			String d = description.toString().trim();
			ResourceBundle bundle = feature.getResourceBundle();
			feature.setDescription(Utils.getResourceString(bundle, d));
		}
	}
}
