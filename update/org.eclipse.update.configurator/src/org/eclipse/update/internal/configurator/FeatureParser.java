/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import javax.xml.parsers.*;

import org.eclipse.osgi.util.NLS;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Default feature parser.
 * Parses the feature manifest file as defined by the platform.
 * 
 * @since 3.0
 */
public class FeatureParser extends DefaultHandler {

	private SAXParser parser;
	private FeatureEntry feature;
	private URL url;

	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();

	/**
	 * Constructs a feature parser.
	 */
	public FeatureParser() {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			System.out.println(e);
		} catch (SAXException e) {
			System.out.println(e);
		}
	}
	/**
	 * Parses the specified url and constructs a feature
	 */
	public FeatureEntry parse(URL featureURL){
		feature=null;
		InputStream in = null;
		try {
			this.url = featureURL;
			in = featureURL.openStream();
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
		return feature;
	}

	/**
	 * Handle start of element tags
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 * @since 2.0
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		Utils.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if ("feature".equals(localName)) { //$NON-NLS-1$
			processFeature(attributes);
			// stop parsing now
			throw new SAXException(""); //$NON-NLS-1$
		} 
	}

	/*
	 * Process feature information
	 */
	private void processFeature(Attributes attributes) {

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		if (id == null || id.trim().equals("") //$NON-NLS-1$
		|| ver == null || ver.trim().equals("")) { //$NON-NLS-1$
			System.out.println(NLS.bind(Messages.FeatureParser_IdOrVersionInvalid, (new String[] { id, ver})));
		} else {
//			String label = attributes.getValue("label"); //$NON-NLS-1$
//			String provider = attributes.getValue("provider-name"); //$NON-NLS-1$
//			String imageURL = attributes.getValue("image"); //$NON-NLS-1$
			String os = attributes.getValue("os"); //$NON-NLS-1$
			String ws = attributes.getValue("ws"); //$NON-NLS-1$
			String nl = attributes.getValue("nl"); //$NON-NLS-1$
			String arch = attributes.getValue("arch"); //$NON-NLS-1$
			if (!Utils.isValidEnvironment(os, ws, arch, nl)) 
				return;
//			String exclusive = attributes.getValue("exclusive"); //$NON-NLS-1$
//			String affinity = attributes.getValue("colocation-affinity"); //$NON-NLS-1$

			String primary = attributes.getValue("primary"); //$NON-NLS-1$
			boolean isPrimary = "true".equals(primary); //$NON-NLS-1$
			String application = attributes.getValue("application"); //$NON-NLS-1$
			String plugin = attributes.getValue("plugin"); //$NON-NLS-1$

			//TODO rootURLs
			feature = new FeatureEntry(id, ver, plugin, "", isPrimary, application, null ); //$NON-NLS-1$
			if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
				File f = new File(url.getFile().replace('/', File.separatorChar));
				feature.setURL("features" + "/" + f.getParentFile().getName() + "/");// + f.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} else {
				// externalized URLs might be in relative form, ensure they are absolute				
				feature.setURL(Utils.makeAbsolute(Utils.getInstallURL(), url).toExternalForm());
			}

			Utils.
				debug("End process DefaultFeature tag: id:" +id + " ver:" +ver + " url:" + feature.getURL()); 	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}
