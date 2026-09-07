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
package org.eclipse.core.internal.runtime.bundlegroups;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses a feature.xml file. In header mode parsing stops after the feature
 * element, which is all the platform.xml fallback needs.
 */
class FeatureXmlParser extends DefaultHandler {

	private static final SAXException STOP_PARSING = new SAXException(""); //$NON-NLS-1$

	private final boolean headerOnly;
	private final FeatureManifest manifest = new FeatureManifest();
	private final StringBuilder description = new StringBuilder();
	private boolean isDescription;

	static FeatureManifest parse(URL featureXML, boolean headerOnly) {
		FeatureXmlParser handler = new FeatureXmlParser(headerOnly);
		SAXParser parser;
		try {
			parser = createParser();
		} catch (ParserConfigurationException | SAXException e) {
			ILog.of(FeatureXmlParser.class).error("Failed to create a parser for " + featureXML, e); //$NON-NLS-1$
			return null;
		}
		try (InputStream in = featureXML.openStream()) {
			parser.parse(new InputSource(in), handler);
		} catch (SAXException e) {
			if (e != STOP_PARSING) {
				return handler.manifest.id == null ? null : handler.manifest;
			}
		} catch (IOException e) {
			return null;
		}
		return handler.manifest.id == null ? null : handler.manifest;
	}

	@SuppressWarnings("restriction")
	private static SAXParser createParser() throws ParserConfigurationException, SAXException {
		return org.eclipse.core.internal.runtime.XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE(true);
	}

	private FeatureXmlParser(boolean headerOnly) {
		this.headerOnly = headerOnly;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("feature".equals(localName)) { //$NON-NLS-1$
			processFeature(attributes);
			if (headerOnly) {
				throw STOP_PARSING;
			}
		} else if ("plugin".equals(localName)) { //$NON-NLS-1$
			processPlugin(attributes);
		} else if ("description".equals(localName)) { //$NON-NLS-1$
			isDescription = true;
		} else if ("license".equals(localName)) { //$NON-NLS-1$
			manifest.licenseURL = attributes.getValue("url"); //$NON-NLS-1$
		}
	}

	private void processFeature(Attributes attributes) {
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String version = attributes.getValue("version"); //$NON-NLS-1$
		if (id == null || id.trim().isEmpty() || version == null || version.trim().isEmpty()) {
			ILog.of(FeatureXmlParser.class).warn(NLS.bind(Messages.FeatureParser_IdOrVersionInvalid, id, version));
			return;
		}
		if (!isValidEnvironment(attributes)) {
			return;
		}
		manifest.id = id;
		manifest.version = version;
		manifest.pluginIdentifier = attributes.getValue("plugin"); //$NON-NLS-1$
		manifest.application = attributes.getValue("application"); //$NON-NLS-1$
		manifest.primary = "true".equals(attributes.getValue("primary")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void processPlugin(Attributes attributes) {
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String version = attributes.getValue("version"); //$NON-NLS-1$
		if (id == null || id.trim().isEmpty() || version == null || version.trim().isEmpty()) {
			ILog.of(FeatureXmlParser.class).warn(NLS.bind(Messages.FeatureParser_IdOrVersionInvalid, id, version));
			return;
		}
		if (!isValidEnvironment(attributes)) {
			return;
		}
		manifest.pluginIds.add(id);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (isDescription) {
			description.append(ch, start, length);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("description".equals(localName)) { //$NON-NLS-1$
			isDescription = false;
			manifest.description = description.toString().trim();
		}
	}

	private static boolean isValidEnvironment(Attributes attributes) {
		String os = attributes.getValue("os"); //$NON-NLS-1$
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		String nl = attributes.getValue("nl"); //$NON-NLS-1$
		if (os != null && !isMatching(os, Platform.getOS())) {
			return false;
		}
		if (ws != null && !isMatching(ws, Platform.getWS())) {
			return false;
		}
		if (arch != null && !isMatching(arch, Platform.getOSArch())) {
			return false;
		}
		if (nl != null && !isMatchingLocale(nl, Platform.getNL())) {
			return false;
		}
		return true;
	}

	private static boolean isMatching(String candidateValues, String siteValues) {
		if (siteValues == null) {
			return false;
		}
		if ("*".equalsIgnoreCase(candidateValues)) { //$NON-NLS-1$
			return true;
		}
		siteValues = siteValues.toUpperCase();
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (siteValues.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isMatchingLocale(String candidateValues, String locale) {
		if (locale == null) {
			return false;
		}
		if ("*".equalsIgnoreCase(candidateValues)) { //$NON-NLS-1$
			return true;
		}
		locale = locale.toUpperCase();
		candidateValues = candidateValues.toUpperCase();
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String candidate = stok.nextToken();
			if (locale.indexOf(candidate) == 0) {
				return true;
			}
			if (candidate.indexOf(locale) == 0) {
				return true;
			}
		}
		return false;
	}
}
