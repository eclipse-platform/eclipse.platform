/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.*;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.search.ISearchEngine;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.help.search.ISearchEngineResultCollector;
import org.eclipse.help.search.ISearchScope;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * This implementation of <code>ISearchEngine</code> interface performs search
 * by running a query on the remote InfoCenter and presenting the results
 * locally. Instances of this engine type are required to supply the URL to the
 * InfoCenter.
 *
 * <p>
 * This class is made public in order to be instantiated and parametrized
 * directly in the extentsions. Clients are required to supply the required URL
 * as a parameter <code>url</code>.
 *
 * <p>
 * This class is not expected to be subclassed or otherwise accessed
 * programmatically.
 *
 * @since 3.1
 */

public final class InfoCenter implements ISearchEngine {
	private Hashtable<String, IHelpResource> tocs;

	public static class Scope implements ISearchScope {
		String url;

		boolean searchSelected;

		String[] tocs;

		public Scope(String url, boolean searchSelected, String[] tocs) {
			this.url = url;
			this.searchSelected = searchSelected;
			this.tocs = tocs;
		}
	}

	private class InfoCenterResult implements ISearchEngineResult {
		private IHelpResource category;

		private Element node;

		private String baseURL;

		public InfoCenterResult(String baseURL, Element node) {
			this.baseURL = baseURL;
			this.node = node;
			createCategory(node);
		}

		private void createCategory(Element node) {
			final String href = node.getAttribute("toc"); //$NON-NLS-1$
			final String label = node.getAttribute("toclabel"); //$NON-NLS-1$
			if (href != null && label != null) {
				category = tocs.get(href);
				if (category == null) {
					category = new IHelpResource() {
						@Override
						public String getLabel() {
							return label;
						}

						@Override
						public String getHref() {
							return href;
						}
					};
					tocs.put(href, category);
				}
			}
		}

		@Override
		public String getLabel() {
			return node.getAttribute("label"); //$NON-NLS-1$
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public IHelpResource getCategory() {
			return category;
		}

		@Override
		public String getHref() {
			return node.getAttribute("href"); //$NON-NLS-1$
		}

		@Override
		public float getScore() {
			String value = node.getAttribute("score"); //$NON-NLS-1$
			if (value != null)
				return Float.parseFloat(value);
			return (float) 0.0;
		}

		@Override
		public boolean getForceExternalWindow() {
			return false;
		}

		@Override
		public String toAbsoluteHref(String href, boolean frames) {
			String url = baseURL;
			if (!url.endsWith("/")) //$NON-NLS-1$
				url = url + "/"; //$NON-NLS-1$
			if (frames) {
				return url + "topic" + href; //$NON-NLS-1$
			}
			if (url.indexOf('?')> 0)  {
				return url + "topic" + href + "&noframes=true"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return url + "topic" + href + "?noframes=true"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * The default constructor.
	 */
	public InfoCenter() {
		tocs = new Hashtable<>();
	}

	@Override
	public void run(String query, ISearchScope scope,
			ISearchEngineResultCollector collector, IProgressMonitor monitor)
			throws CoreException {
		URL url = createURL(query, (Scope) scope);
		if (url == null)
			return;
		InputStream is = null;
		tocs.clear();
		try {
			URLConnection connection = ProxyUtil.getConnection(url);
			SubMonitor subMonitor = SubMonitor.convert(monitor, HelpBaseResources.InfoCenter_connecting, 5);
			is = connection.getInputStream();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				subMonitor.worked(1);
				load(((Scope) scope).url, reader, collector, subMonitor.split(4));
			}
		} catch (FileNotFoundException e) {
			reportError(HelpBaseResources.InfoCenter_fileNotFound, e, collector);
		} catch (IOException e) {
			reportError(HelpBaseResources.InfoCenter_io, e, collector);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void reportError(String message, IOException e,
			ISearchEngineResultCollector collector) {
		Status status = new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID,
				IStatus.OK, message, e);
		collector.error(status);
	}

	private void load(String baseURL, Reader r,
			ISearchEngineResultCollector collector, IProgressMonitor monitor) {
		Document document = null;
		try {
			if (monitor.isCanceled())
				return;
			SubMonitor subMonitor = SubMonitor.convert(monitor, 5);
			subMonitor.subTask(HelpBaseResources.InfoCenter_searching);
			document = LocalEntityResolver.parse(new InputSource(r));
			if (monitor.isCanceled())
				return;

			// Strip out any comments first
			Node root = document.getFirstChild();
			while (root.getNodeType() == Node.COMMENT_NODE) {
				document.removeChild(root);
				root = document.getFirstChild();
				if (monitor.isCanceled())
					return;
			}
			subMonitor.worked(1);
			load(baseURL, document, (Element) root, collector, subMonitor.split(4));
		} catch (ParserConfigurationException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		} catch (SAXException e) {
			// ignore
		}
	}

	private void load(String baseURL, Document doc, Element root,
			ISearchEngineResultCollector collector, IProgressMonitor monitor) {
		NodeList topics = root.getElementsByTagName("hit"); //$NON-NLS-1$
		ISearchEngineResult[] results = new ISearchEngineResult[topics
				.getLength()];
		monitor.subTask(HelpBaseResources.InfoCenter_processing);
		monitor.beginTask("", results.length); //$NON-NLS-1$
		for (int i = 0; i < topics.getLength(); i++) {
			Element el = (Element) topics.item(i);
			if (monitor.isCanceled())
				break;
			results[i] = new InfoCenterResult(baseURL, el);
			monitor.worked(1);
		}
		collector.accept(results);
	}

	private URL createURL(String query, Scope scope) {
		if (scope.url == null) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		buf.append(scope.url);
		if (!scope.url.endsWith("/")) //$NON-NLS-1$
			buf.append("/search?phrase="); //$NON-NLS-1$
		else
			buf.append("search?phrase="); //$NON-NLS-1$
		buf.append(URLEncoder.encode(query, StandardCharsets.UTF_8));
		buf.append("&locale="); //$NON-NLS-1$
		buf.append(Platform.getNL());
		if (scope.searchSelected && scope.tocs != null) {
			buf.append("&scopedSearch=true"); //$NON-NLS-1$
			for (int i = 0; i < scope.tocs.length; i++) {
				String toc = URLEncoder.encode(scope.tocs[i], StandardCharsets.UTF_8);
				buf.append("&scope="); //$NON-NLS-1$
				buf.append(toc);
			}
		}
		try {
			return new URL(buf.toString());
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
