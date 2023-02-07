/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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

package org.eclipse.help.internal.webapp.servlet;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/**
 * A servlet that provides an informational page about the plugins that make up
 * the web application.
 */
public class AboutServlet extends HttpServlet {

	protected static final int NUMBER_OF_COLUMNS = 4;

	protected Locale locale;

	protected static class PluginDetails {
		public String[] columns = new String[NUMBER_OF_COLUMNS];

		public PluginDetails(String[] columns) {
			this.columns = columns;
			for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
				if (columns[i] == null) {
					columns[i] = ""; //$NON-NLS-1$
				}
			}
		}
	}

	protected static class PluginComparator implements Comparator<PluginDetails> {

		public PluginComparator(int column) {
			this.column = column;
		}

		private int column;

		@Override
		public int compare(PluginDetails pd1, PluginDetails pd2) {
			return Collator.getInstance().compare(pd1.columns[column], pd2.columns[column]);
		}

	}

	public AboutServlet() {
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -1426745453574711075L;
	private static final String XHTML_1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n<title>"; //$NON-NLS-1$
	private static final String XHTML_2 = "</title>\n <style type = \"text/css\"> td { padding-right : 10px; }</style></head>\n<body>\n"; //$NON-NLS-1$
	private static final String XHTML_3 = "</body>\n</html>"; //$NON-NLS-1$

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("text/html; charset=UTF-8"); //$NON-NLS-1$
		locale = UrlUtil.getLocaleObj(req, resp);
		StringBuilder buf = new StringBuilder();
		buf.append(XHTML_1);
		String showParam = req.getParameter("show"); //$NON-NLS-1$
		if ("agent".equalsIgnoreCase(showParam)) { //$NON-NLS-1$
			getAgent(req, resp);
			return;
		}
		if ("preferences".equalsIgnoreCase(showParam)) { //$NON-NLS-1$
			getPreferences(resp);
			return;
		}
		String sortParam = req.getParameter("sortColumn"); //$NON-NLS-1$
		int sortColumn = 3;
		if (sortParam != null) {
			try {
				sortColumn = Integer.parseInt(sortParam);
			} catch (NumberFormatException e) {
			}
		}

		String title = WebappResources.getString("aboutPlugins", locale); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(title));
		buf.append(XHTML_2);
		String app = System.getProperty("eclipse.application", null); //$NON-NLS-1$
		String build = System.getProperty("eclipse.buildId", null); //$NON-NLS-1$
		if (app != null || build != null) {
			buf.append("<p>"); //$NON-NLS-1$
			if (app != null)
				buf.append(WebappResources.getString("application", locale, app) + "<br/>");//$NON-NLS-1$ //$NON-NLS-2$
			if (build != null)
				buf.append(WebappResources.getString("buildId", locale, build) + "<br/>");//$NON-NLS-1$ //$NON-NLS-2$
			buf.append("</p>"); //$NON-NLS-1$
		}

		buf.append("<table>"); //$NON-NLS-1$
		List<PluginDetails> plugins = new ArrayList<>();
		Bundle[] bundles = FrameworkUtil.getBundle(AboutServlet.class).getBundleContext().getBundles();
		for (Bundle bundle : bundles) {
			plugins.add(pluginDetails(bundle));
		}

		Comparator<PluginDetails> pluginComparator = new PluginComparator(sortColumn);
		plugins.sort(pluginComparator);
		String[] headerColumns = new String[] {WebappResources.getString("provider", locale), //$NON-NLS-1$
				WebappResources.getString("pluginName", locale), //$NON-NLS-1$
				WebappResources.getString("version", locale), //$NON-NLS-1$
				WebappResources.getString("pluginId", locale) //$NON-NLS-1$
		};
		PluginDetails header = new PluginDetails(headerColumns);
		buf.append(headerRowFor(header));
		for (PluginDetails details : plugins) {
			buf.append(tableRowFor(details));
		}
		buf.append("</table>"); //$NON-NLS-1$
		buf.append(XHTML_3);
		String response = buf.toString();
		resp.getWriter().write(response);
	}

	private void getPreferences(HttpServletResponse resp) throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append(XHTML_1);
		String title = WebappResources.getString("preferences", locale); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(title));
		buf.append(XHTML_2);
		buf.append("<h1>"); //$NON-NLS-1$
		buf.append(title);
		buf.append("</h1>"); //$NON-NLS-1$
		PreferenceWriter writer = new PreferenceWriter(buf, locale);
		writer.writePreferences();
		buf.append(XHTML_3);
		String response = buf.toString();
		resp.getWriter().write(response);
	}

	private void getAgent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append(XHTML_1);
		String title = WebappResources.getString("userAgent", locale); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(title));
		buf.append(XHTML_2);
		buf.append("<h1>"); //$NON-NLS-1$
		buf.append(title);
		buf.append("</h1>"); //$NON-NLS-1$
		String agent = req.getHeader("User-Agent"); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(agent));
		buf.append(XHTML_3);
		String response = buf.toString();
		resp.getWriter().write(response);
	}

	private String headerRowFor(PluginDetails details) {
		String row = "<tr>\n"; //$NON-NLS-1$
		for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
			row += ("<td><a href = \"about.html?sortColumn="); //$NON-NLS-1$
			row += i;
			row += "\">"; //$NON-NLS-1$
			row += UrlUtil.htmlEncode(details.columns[i]);
			row += "</a></td>\n"; //$NON-NLS-1$
		}
		row += "</tr>"; //$NON-NLS-1$
		return row;
	}

	private String tableRowFor(PluginDetails details) {
		String row = "<tr>\n"; //$NON-NLS-1$
		for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
			row += ("<td>"); //$NON-NLS-1$
			row += UrlUtil.htmlEncode(details.columns[i]);
			row += "</td>\n"; //$NON-NLS-1$
		}
		row += "</tr>"; //$NON-NLS-1$
		return row;
	}

	protected PluginDetails pluginDetails(Bundle bundle) {
		String[] values = new String[] {getResourceString(bundle, Constants.BUNDLE_VENDOR), getResourceString(bundle, Constants.BUNDLE_NAME), getResourceString(bundle, Constants.BUNDLE_VERSION), bundle.getSymbolicName()};
		PluginDetails details = new PluginDetails(values);

		return details;
	}

	private static String getResourceString(Bundle bundle, String headerName) {
		String value = bundle.getHeaders().get(headerName);
		return value == null ? null : Platform.getResourceString(bundle, value);
	}

}
