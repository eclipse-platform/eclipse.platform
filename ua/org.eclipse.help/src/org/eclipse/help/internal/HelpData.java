/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.help.internal.util.ProductPreferences;
import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Interface to the help data file contents defined in plugin_customization.ini under
 * key org.eclipse.help/HELP_DATA.
 */
public class HelpData {

	private static final String ELEMENT_TOC_ORDER = "tocOrder"; //$NON-NLS-1$
	private static final String ELEMENT_HIDDEN = "hidden"; //$NON-NLS-1$
	private static final String ELEMENT_TOC = "toc"; //$NON-NLS-1$
	private static final String ELEMENT_CATEGORY = "category"; //$NON-NLS-1$
	private static final String ELEMENT_INDEX = "index"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ELEMENT_OTHER_TOCS = "otherTocs"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SORT = "sort"; //$NON-NLS-1$
	private static final String PLUGINS_ROOT_SLASH = "PLUGINS_ROOT/"; //$NON-NLS-1$

	private static HelpData productHelpData;

	private URL url;
	private List<String> tocOrder;
	private Set<String> hiddenTocs;
	private Set<String> hiddenIndexes;
	private String sortMode;

	/*
	 * Get the active product's help data, or null if the product doesn't have
	 * help data or there is no active product.
	 */
	public static synchronized HelpData getProductHelpData() {
		if (productHelpData == null) {
			String pluginId = null;
			IProduct product = Platform.getProduct();
			if (product != null) {
				pluginId = product.getDefiningBundle().getSymbolicName();
			}
			String helpDataFile = Platform.getPreferencesService().getString(HelpPlugin.PLUGIN_ID, HelpPlugin.HELP_DATA_KEY, "", null); //$NON-NLS-1$
			if (helpDataFile.length() > 0) {
				if (helpDataFile.startsWith(PLUGINS_ROOT_SLASH)) {
					int nextSlash = helpDataFile.indexOf('/', PLUGINS_ROOT_SLASH.length());
					if (nextSlash > 0) {
						pluginId = helpDataFile.substring(PLUGINS_ROOT_SLASH.length(), nextSlash);
						helpDataFile = helpDataFile.substring(nextSlash + 1);
					}
				}
			}
			if (helpDataFile.length() > 0 && pluginId != null) {
				Bundle bundle = Platform.getBundle(pluginId);
				if (bundle != null) {
					URL helpDataUrl = bundle.getEntry(helpDataFile);
					productHelpData = new HelpData(helpDataUrl);
				}
			}
			if (productHelpData == null) {
				productHelpData = new HelpData(null);
			}
		}
		return productHelpData;
	}

	/*
	 * For testing
	 */
	public static void clearProductHelpData() {
		productHelpData = null;
	}

	/*
	 * Constructs help data from the XML at the given URL.
	 */
	public HelpData(URL url) {
		this.url = url;
	}

	/*
	 * Returns a list of strings which are the IDs of the tocs/categories listed
	 * in tocOrder.
	 */
	public synchronized List<String> getTocOrder() {
		if (tocOrder == null) {
			loadHelpData();
		}
		return tocOrder;
	}

	/*
	 * Returns a set of strings which are the IDs of the tocs/categories listed
	 * in the hidden section.
	 */
	public synchronized Set<String> getHiddenTocs() {
		if (hiddenTocs == null) {
			loadHelpData();
		}
		return hiddenTocs;
	}

	/*
	 * Returns a set of strings which are the IDs of the indexes listed
	 * in the hidden section.
	 */
	public synchronized Set<String> getHiddenIndexes() {
		if (hiddenIndexes == null) {
			loadHelpData();
		}
		return hiddenIndexes;
	}

	/*
	 * Returns true if tocs not specified in the toc order are sorted
	 */
	public synchronized boolean isSortOthers() {
		if (sortMode == null) {
			loadHelpData();
		}
		return "true".equals(sortMode); //$NON-NLS-1$
	}

	/*
	 * Allow unit tests to override for providing test data.
	 */
	public InputStream getHelpDataFile(String filePath) throws IOException {
		IProduct product = Platform.getProduct();
		Bundle definingBundle = product != null ? product.getDefiningBundle() : null;
		URL entry = definingBundle != null ? definingBundle.getEntry(filePath) : null;
		if(entry == null) {
			throw new IOException("No entry to '"+filePath+"' could be found or caller does not have the appropriate permissions.");//$NON-NLS-1$ //$NON-NLS-2$
		}
		return entry.openStream();
	}

	/*
	 * Loads and parses the file and populates the data structures.
	 */
	private void loadHelpData() {
		tocOrder = new ArrayList<>();
		hiddenTocs = new HashSet<>();
		hiddenIndexes = new HashSet<>();
		sortMode = "true"; //$NON-NLS-1$
		if (url != null) {
			try (InputStream in = url.openStream()) {
				@SuppressWarnings("restriction")
				SAXParser parser = org.eclipse.core.internal.runtime.XmlProcessorFactory
						.createSAXParserWithErrorOnDOCTYPE(false);
				parser.parse(in, new Handler());
			}
			catch (Throwable t) {
				String msg = "Error loading help data file \"" + url + "\""; //$NON-NLS-1$ //$NON-NLS-2$
				ILog.of(getClass()).error(msg, t);
			}
		} else {
			// Derive information from preferences
			IPreferencesService preferencesService = Platform.getPreferencesService();
			String baseTocs = preferencesService.getString(HelpPlugin.PLUGIN_ID, HelpPlugin.BASE_TOCS_KEY, "", null); //$NON-NLS-1$
			String ignoredTocs = preferencesService.getString(HelpPlugin.PLUGIN_ID, HelpPlugin.IGNORED_TOCS_KEY, "", null); //$NON-NLS-1$
			String ignoredIndexes = preferencesService.getString(HelpPlugin.PLUGIN_ID, HelpPlugin.IGNORED_INDEXES_KEY, "", null); //$NON-NLS-1$
			tocOrder = ProductPreferences.tokenize(baseTocs);
			hiddenTocs.addAll(ProductPreferences.tokenize(ignoredTocs));
			hiddenIndexes.addAll(ProductPreferences.tokenize(ignoredIndexes));
		}
	}

	/*
	 * SAX Handler for parsing the help data file.
	 */
	private class Handler extends DefaultHandler {

		private boolean inTocOrder;
		private boolean inHidden;

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (ELEMENT_TOC_ORDER.equals(name)) {
				inTocOrder = true;
			}
			else if (ELEMENT_HIDDEN.equals(name)) {
				inHidden = true;
			}
			else if (ELEMENT_TOC.equals(name) || ELEMENT_CATEGORY.equals(name)) {
				String id = attributes.getValue(ATTRIBUTE_ID);
				if (id != null) {
					if (inTocOrder) {
						tocOrder.add(id);
					}
					else if (inHidden) {
						hiddenTocs.add(id);
					}
				}
			}
			else if (ELEMENT_INDEX.equals(name) && inHidden) {
				String id = attributes.getValue(ATTRIBUTE_ID);
				if (id != null) {
					hiddenIndexes.add(id);
				}
			}
			else if (ELEMENT_OTHER_TOCS.equals(name)) {
				String sortAttribute = attributes.getValue(ATTRIBUTE_SORT);
				if (sortAttribute != null) {
					sortMode = sortAttribute.toLowerCase();
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (ELEMENT_TOC_ORDER.equals(name)) {
				inTocOrder = false;
			}
			else if (ELEMENT_HIDDEN.equals(name)) {
				inHidden = false;
			}
		}

		/*
		 * Note: throws clause does not declare IOException due to a bug in
		 * sun jdk: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6327149
		 *
		 * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
		 */
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
			return new InputSource(new StringReader("")); //$NON-NLS-1$
		}
	}
}
