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
package org.eclipse.update.internal.model;


import java.io.*;
import javax.xml.parsers.*;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Parse default feature.xml
 */

public class DefaultPluginParser extends DefaultHandler {
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();
	private SAXParser parser;
	private String id = null;
	private String version = null;
	private PluginEntry pluginEntry;

	private static final String PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String FRAGMENT = "fragment"; //$NON-NLS-1$

	private class ParseCompleteException extends SAXException {

        private static final long serialVersionUID = 1L;

        public ParseCompleteException(String arg0) {
			super(arg0);
		}
	}

	/**
	 * Constructor for DefaultFeatureParser
	 */
	public DefaultPluginParser() {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			UpdateCore.log(e);
		} catch (SAXException e) {
			UpdateCore.log(e);
		}
	}

	/**
	 * @since 2.0
	 */
	public synchronized PluginEntry parse(InputStream in) throws SAXException, IOException {
		try {
			pluginEntry = new PluginEntry();
			parser.parse(new InputSource(in), this);
		} catch (ParseCompleteException e) {
			// expected, we stopped the parsing when we have the information we need
			/// no need to pursue the parsing
		}

		if (id == null || id.trim().length() == 0)
			id = "_no_id_"; //$NON-NLS-1$
		pluginEntry.setVersionedIdentifier(new VersionedIdentifier(id, version));
		return pluginEntry;
	}

	/**
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		String tag = localName.trim();

		if (tag.equalsIgnoreCase(PLUGIN)) {
			pluginEntry.isFragment(false);			
			processPlugin(attributes);
			return;
		}

		if (tag.equalsIgnoreCase(FRAGMENT)) {
			pluginEntry.isFragment(true);			
			processPlugin(attributes);
			return;
		}
	}

	/** 
	 * process plugin entry info
	 */
	private void processPlugin(Attributes attributes) throws ParseCompleteException {
		id = attributes.getValue("id"); //$NON-NLS-1$
		version = attributes.getValue("version"); //$NON-NLS-1$
		throw new ParseCompleteException(""); //$NON-NLS-1$
	}
}
