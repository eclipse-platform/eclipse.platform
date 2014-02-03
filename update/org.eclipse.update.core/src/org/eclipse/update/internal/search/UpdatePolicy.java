/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 191368, Policy URL doesn't support UTF-8 characters
 *******************************************************************************/
package org.eclipse.update.internal.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.URLEncoder;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.core.connection.ConnectionFactory;
import org.eclipse.update.internal.core.connection.IResponse;
import org.eclipse.update.search.IUpdateSiteAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * This class opens connection to the update map resource
 * and parses the file to load update URL mappings.
 * These mappings are used to redirect new updates search
 * when the redirection pattern matches.
 */

public class UpdatePolicy {
	private static final String TAG_POLICY = "update-policy"; //$NON-NLS-1$
	private static final String TAG_URL_MAP = "url-map"; //$NON-NLS-1$
	private static final String ATT_URL = "url"; //$NON-NLS-1$
	private static final String ATT_PATTERN = "pattern"; //$NON-NLS-1$
	private static final String ATT_TYPE = "url-type"; //$NON-NLS-1$
	private static final String ATT_TYPE_VALUE_UPDATE = "update"; //$NON-NLS-1$
	//private static final String ATT_TYPE_VALUE_BOTH = "both"; //$NON-NLS-1$
	private static final String ATT_TYPE_VALUE_DISCOVERY = "discovery"; //$NON-NLS-1$

	private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

	private static class MapSite implements IUpdateSiteAdapter {
		private URL url;
		public MapSite(URL url) {
			this.url = url;
		}
		public String getLabel() {
			if (url == null) {
				return ""; //$NON-NLS-1$
			}
			return url.toString();
		}
		public URL getURL() {
			return url;
		}
	}

	private static class UpdateMapEntry {
		private IUpdateSiteAdapter site;
		private String pattern;

		public UpdateMapEntry(String pattern, URL url) {
			this.pattern = pattern;
			this.site = new MapSite(url);
		}
		public IUpdateSiteAdapter getSite() {
			return site;
		}
		public boolean matches(String id) {
			return id.startsWith(pattern);
		}
		public String getPattern() {
			return pattern;
		}
	}

	private ArrayList entries;
	private ArrayList discoveryEntries;
	private IUpdateSiteAdapter defaultSite;
	private IUpdateSiteAdapter defaultDiscoverySite;
	private boolean loaded = false;
	private boolean fallbackAllowed = true;

	public UpdatePolicy() {
		entries = new ArrayList();
		discoveryEntries = new ArrayList();
	}

	public void load(URL mapFile, IProgressMonitor monitor)
		throws CoreException {
		InputStream policyStream = null;
		try {
			IResponse response = ConnectionFactory.get(mapFile);
			UpdateManagerUtils.checkConnectionResult(response, mapFile);
			policyStream = response.getInputStream(monitor);
			// the stream can be null if the user cancels the connection
			if (policyStream == null)
				return;
			
			documentBuilderFactory.setNamespaceAware(true);
			DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
			Document doc = parser.parse(new InputSource(policyStream));

			processUpdatePolicy(doc);
			loaded = true;
		} catch (IOException e) {
			throw Utilities.newCoreException(
				NLS.bind(Messages.SiteURLFactory_UnableToAccessSiteStream, (new String[] { mapFile == null ? "" : mapFile.toExternalForm() })), //$NON-NLS-1$
				ISite.SITE_ACCESS_EXCEPTION,
				e);
		} catch (SAXException e) {
			throw Utilities.newCoreException(
				Messages.UpdatePolicy_parsePolicy, 
				0,
				e);

		} catch(ParserConfigurationException e) {
			throw Utilities.newCoreException(
				Messages.UpdatePolicy_parsePolicy, 
				0,
				e);
		} finally {
			if (policyStream != null) {
				try {
					policyStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public boolean isLoaded() {
		return loaded;
	}
	
	/*
	 * Given the feature ID, returns the mapped update URL if
	 * found in the mappings. This URL will be used INSTEAD of
	 * the update URL encoded in the feature itself during the
	 * new update search.
	 * <p>In case of multiple matches (e.g. org.eclipse and org.eclipse.platform)
	 * the URL for the longer pattern will be picked (i.e. org.eclipse.platform).
	 */
	public IUpdateSiteAdapter getMappedSite(String id) {
		UpdateMapEntry lastEntry = null;
		for (int i = 0; i < entries.size(); i++) {
			UpdateMapEntry entry = (UpdateMapEntry) entries.get(i);
			if (entry.matches(id)) {
				if (lastEntry == null)
					lastEntry = entry;
				else {
					// Choose the match with longer pattern.
					// For example, if two matches are found:
					// 'org.eclipse' and 'org.eclipse.platform',
					// pick 'org.eclipse.platform'.
					String pattern = entry.getPattern();
					String lastPattern = lastEntry.getPattern();
					if (pattern.length() > lastPattern.length())
						lastEntry = entry;
				}
			}
		}
		if (lastEntry != null)
			return lastEntry.getSite();
		else
			return defaultSite;
	}
	
	/*
	 * Given the feature ID, returns the mapped discovery URL if
	 * found in the mappings. This URL will be used INSTEAD of
	 * the discovery URL encoded in the feature itself during the
	 * new search.
	 * <p>In case of multiple matches (e.g. org.eclipse and org.eclipse.platform)
	 * the URL for the longer pattern will be picked (i.e. org.eclipse.platform).
	 */
	public IUpdateSiteAdapter getMappedDiscoverySite(String id) {
		UpdateMapEntry lastEntry = null;
		for (int i = 0; i < discoveryEntries.size(); i++) {
			UpdateMapEntry entry = (UpdateMapEntry) discoveryEntries.get(i);
			if (entry.matches(id)) {
				if (lastEntry == null)
					lastEntry = entry;
				else {
					// Choose the match with longer pattern.
					// For example, if two matches are found:
					// 'org.eclipse' and 'org.eclipse.platform',
					// pick 'org.eclipse.platform'.
					String pattern = entry.getPattern();
					String lastPattern = lastEntry.getPattern();
					if (pattern.length() > lastPattern.length())
						lastEntry = entry;
				}
			}
		}
		if (lastEntry != null)
			return lastEntry.getSite();
		else
			return defaultDiscoverySite;
	}

	public boolean isFallbackAllowed() {
		return fallbackAllowed;
	}
	
	private void reset() {
		if (!entries.isEmpty())
			entries.clear();
		if (!discoveryEntries.isEmpty())
			discoveryEntries.clear();
	}

	private void processUpdatePolicy(Document document) throws CoreException {
		Node root = document.getDocumentElement();
		reset();
		
		if (root.getNodeName().equals(TAG_POLICY)==false)
			throwCoreException("'"+TAG_POLICY+Messages.UpdatePolicy_policyExpected, null);  //$NON-NLS-1$
				
		NodeList nodes = root.getChildNodes();
		
		for (int i=0; i<nodes.getLength(); i++) {
			Node child = nodes.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			String tag = child.getNodeName();
			if (tag.equals(TAG_URL_MAP))
				processMapNode(child);
		}
	}
	private void processMapNode(Node node) throws CoreException {
		String pattern = getAttribute(node, ATT_PATTERN);
		String urlName = getAttribute(node, ATT_URL);
		String type = getAttribute(node, ATT_TYPE);
		
		assertNotNull(ATT_PATTERN, pattern);
		assertNotNull(ATT_URL, urlName);
		
		// empty url means feature is not updateable
		if (urlName.trim().length() == 0) {
			addUpdateEntry(pattern, null, type);
			return;
		}

		try {
			URL url = new URL(urlName);
			URL resolvedURL = URLEncoder.encode(url);
			addUpdateEntry(pattern, resolvedURL, type);
		} catch (MalformedURLException e) {
			throwCoreException(Messages.UpdatePolicy_invalidURL+urlName, null); 
		} 
	}
	
	private void assertNotNull(String name, String value) throws CoreException {
		if (value==null)
			throwCoreException(name+Messages.UpdatePolicy_nameNoNull, null); 
	}
	
	private String getAttribute(Node node, String name) {
		NamedNodeMap attMap = node.getAttributes();
		Node att = attMap.getNamedItem(name);
		if (att==null) return null;
		return att.getNodeValue();
	}

	private void addUpdateEntry(String pattern, URL url, String type) {
		if (pattern.equalsIgnoreCase("*")) {//$NON-NLS-1$
			if (type == null)
				defaultSite = new MapSite(url);
			else if (type.equals(ATT_TYPE_VALUE_UPDATE))
				defaultSite = new MapSite(url);
			else if (type.equals(ATT_TYPE_VALUE_DISCOVERY))
				defaultDiscoverySite = new MapSite(url);
			else {
				defaultSite = new MapSite(url);
				defaultDiscoverySite = new MapSite(url);
			}
		} else {
			if (type == null )
				entries.add(new UpdateMapEntry(pattern, url));
			else if (type.equals(ATT_TYPE_VALUE_UPDATE))
				entries.add(new UpdateMapEntry(pattern, url));
			else if (type.equals(ATT_TYPE_VALUE_DISCOVERY))
				discoveryEntries.add(new UpdateMapEntry(pattern, url));
			else {
				entries.add(new UpdateMapEntry(pattern, url));
				discoveryEntries.add(new UpdateMapEntry(pattern, url));
			}
		}
	}
	
	private void throwCoreException(String message, Throwable e) throws CoreException {
		String fullMessage = Messages.UpdatePolicy_UpdatePolicy+message; 
		throw Utilities.newCoreException(fullMessage, 0, e);
	}
}
