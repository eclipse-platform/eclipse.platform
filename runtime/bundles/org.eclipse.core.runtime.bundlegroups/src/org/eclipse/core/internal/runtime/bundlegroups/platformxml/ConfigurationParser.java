/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.runtime.bundlegroups.platformxml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.URIUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parser for platform.xml files.
 */
public class ConfigurationParser implements ConfigurationConstants {
	static final String PLATFORM_BASE = "platform:/base/"; //$NON-NLS-1$
	private final URL osgiInstallArea;

	/*
	 * Parse the given file handle which points to a platform.xml file and return a
	 * configuration object. Returns null if the file doesn't exist or cannot be read.
	 */
	public static Configuration parse(File file, URL osgiInstallArea) {
		return new ConfigurationParser(osgiInstallArea).internalParse(file);
	}

	private ConfigurationParser(URL osgiInstallArea) {
		this.osgiInstallArea = osgiInstallArea;
	}

	/*
	 * Create a feature object based on the given DOM node. Return the new feature.
	 */
	private static Feature createFeature(Node node) {
		Feature result = new Feature();
		String id = getAttribute(node, ATTRIBUTE_ID);
		if (id != null) {
			result.setId(id);
		}
		String url = getAttribute(node, ATTRIBUTE_URL);
		if (url != null) {
			result.setUrl(url);
		}
		String version = getAttribute(node, ATTRIBUTE_VERSION);
		if (version != null) {
			result.setVersion(version);
		}
		String pluginIdentifier = getAttribute(node, ATTRIBUTE_PLUGIN_IDENTIFIER);
		if (pluginIdentifier != null) {
			result.setPluginIdentifier(pluginIdentifier);
		}
		String pluginVersion = getAttribute(node, ATTRIBUTE_PLUGIN_VERSION);
		// plug-in version is the same as the feature version if it is missing
		if (pluginVersion == null) {
			pluginVersion = version;
		}
		if (pluginVersion != null) {
			result.setPluginVersion(pluginVersion);
		}
		return result;
	}

	/*
	 * Create the features from the given DOM node.
	 */
	private static void createFeatures(Node node, Site site) {
		NodeList children = node.getChildNodes();
		int size = children.getLength();
		for (int i = 0; i < size; i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (!ELEMENT_FEATURE.equalsIgnoreCase(child.getNodeName())) {
				continue;
			}
			site.addFeature(createFeature(child));
		}
	}

	/*
	 * Create a site based on the given DOM node.
	 */
	private Site createSite(Node node) {
		Site result = new Site();
		String url = getAttribute(node, ATTRIBUTE_URL);
		if (url != null) {
			try {
				// do this to ensure the location is an encoded URI
				URI uri = URIUtil.fromString(url);
				URI osgiURI = osgiInstallArea != null ? URIUtil.toURI(osgiInstallArea) : null;
				result.setUrl(getLocation(uri, osgiURI).toString());
			} catch (URISyntaxException e) {
				result.setUrl(url);
			}
		}
		createFeatures(node, result);
		return result;
	}

	/*
	 * Convert the given url string to an absolute url. If the string is
	 * platform:/base/ then return a string which represents the osgi install area.
	 */
	private static URI getLocation(URI location, URI osgiArea) {
		if (osgiArea == null) {
			return location;
		}
		if (PLATFORM_BASE.equals(location.toString())) {
			return osgiArea;
		}
		return URIUtil.makeAbsolute(location, osgiArea);
	}

	/*
	 * Return the attribute with the given name, or null if it does not exist.
	 */
	private static String getAttribute(Node node, String name) {
		NamedNodeMap attributes = node.getAttributes();
		Node temp = attributes.getNamedItem(name);
		return temp == null ? null : temp.getNodeValue();
	}

	/*
	 * Load the given file into a DOM document.
	 */
	@SuppressWarnings("restriction")
	private static Document load(InputStream input) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory factory = org.eclipse.core.internal.runtime.XmlProcessorFactory
				.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
		DocumentBuilder builder = factory.newDocumentBuilder();
		try (InputStream buffered = new BufferedInputStream(input)) {
			return builder.parse(buffered);
		}
	}

	private Configuration internalParse(File file) {
		if (!file.exists()) {
			return null;
		}
		try (InputStream input = new FileInputStream(file)) {
			Configuration result = process(load(input));
			if (result != null) {
				result.setOsgiInstallArea(osgiInstallArea);
			}
			return result;
		} catch (IOException | ParserConfigurationException | SAXException e) {
			ILog.of(ConfigurationParser.class).error("Failed to read " + file, e); //$NON-NLS-1$
			return null;
		}
	}

	/*
	 * Process the given DOM document and create the appropriate site objects.
	 */
	private Configuration process(Document document) {
		Node node = getConfigElement(document);
		if (node == null) {
			return null;
		}
		Configuration configuration = createConfiguration(node);
		NodeList children = node.getChildNodes();
		int size = children.getLength();
		for (int i = 0; i < size; i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (!ELEMENT_SITE.equalsIgnoreCase(child.getNodeName())) {
				continue;
			}
			Site site = createSite(child);
			if (site != null) {
				configuration.add(site);
			}
		}
		return configuration;
	}

	private static Configuration createConfiguration(Node node) {
		Configuration result = new Configuration();
		String value = getAttribute(node, ATTRIBUTE_SHARED_UR);
		if (value != null) {
			result.setSharedUR(value);
		}
		return result;
	}

	private static Node getConfigElement(Document doc) {
		NodeList children = doc.getChildNodes();
		int size = children.getLength();
		for (int i = 0; i < size; i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (ELEMENT_CONFIG.equalsIgnoreCase(child.getNodeName())) {
				return child;
			}
		}
		return null;
	}
}
