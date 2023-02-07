/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.MissingContentManager;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.RequestScope;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.webapp.IFilter;

/*
 * Generates navigation pages where topics are not present in the table
 * of contents. Displays links to the direct child topics.
 */
public class NavServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String XHTML_1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n<title>"; //$NON-NLS-1$
	private static final String XHTML_2 = "</title>\n</head>\n<body>\n"; //$NON-NLS-1$
	private static final String XHTML_2_RTL = "</title>\n</head>\n<body dir = \"rtl\">\n"; //$NON-NLS-1$
	private static final String XHTML_3 = "</body>\n</html>"; //$NON-NLS-1$

	private static final IFilter filters[] = new IFilter[]{
		new FramesetFilter(), new InjectionFilter(false), new BreadcrumbsFilter(),
		new ShowInTocFilter(), new ExtraFilters() };

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Locale locale = getLocale(req, resp);

		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("text/html; charset=UTF-8"); //$NON-NLS-1$

		String path;
		ITopic topic;
		try {
			path = req.getPathInfo().substring(1);
			topic = getTopic(path, locale);
		} catch (Exception e) {
			showPageNotFoundPage(req, resp);
			return;
		}

		@SuppressWarnings("resource")
		OutputStream out = resp.getOutputStream();
		for (IFilter filter : filters) {
			out = filter.filter(req, out);
		}

		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
			AbstractHelpScope scope = RequestScope.getScope(req, resp, false);
			writeContent(topic, path, locale, writer, UrlUtil.isRTL(req, resp), scope);
		}
	}

	private void showPageNotFoundPage(HttpServletRequest req,
			HttpServletResponse resp) {
		String errorPage = MissingContentManager.getInstance().getPageNotFoundPage(req.getServletPath(), false);
		if (errorPage != null && errorPage.length() > 0) {
			String href = req.getRequestURL().toString();
			int navIndex = href.indexOf("/nav"); //$NON-NLS-1$
			if (navIndex >= 0 ) {
				href = href.substring(0, navIndex);
				href += "/nftopic"; //$NON-NLS-1$
				if (errorPage.charAt(0) != '/') {
					href += '/';
				}
				href += errorPage;
				try {
					resp.sendRedirect(href);
					return;
				} catch (IOException e) {
					// Fall through
				}
			}
		}
		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	protected Locale getLocale (HttpServletRequest req, HttpServletResponse resp) {
		Locale locale;
		String nl = UrlUtil.getLocale(req, resp);
		// break the string into tokens to get the Locale object
		StringTokenizer locales = new StringTokenizer(nl, "_"); //$NON-NLS-1$
		if (locales.countTokens() == 1)
			locale = new Locale(locales.nextToken(), ""); //$NON-NLS-1$
		else if (locales.countTokens() == 2)
			locale = new Locale(locales.nextToken(), locales.nextToken());
		else if (locales.countTokens() == 3)
			locale = new Locale(locales.nextToken(), locales.nextToken(), locales
					.nextToken());
		else
			locale = Locale.getDefault();
		return locale;
	}

	protected ITopic getTopic(String topicPath, Locale locale) {
		StringTokenizer tok = new StringTokenizer(topicPath, "_"); //$NON-NLS-1$
		int index = Integer.parseInt(tok.nextToken());
		ITopic topic = HelpPlugin.getTocManager().getTocs(locale.toString())[index].getTopic(null);
		while (tok.hasMoreTokens()) {
			index = Integer.parseInt(tok.nextToken());
			topic = topic.getSubtopics()[index];
		}
		return topic;
	}

	private void writeContent(ITopic topic, String path, Locale locale, PrintWriter writer, boolean isRTL, AbstractHelpScope scope) {
		writer.write(XHTML_1);
		writer.write(topic.getLabel());
		if (isRTL) {
			writer.write(XHTML_2_RTL);
		} else {
			writer.write(XHTML_2);
		}
		writer.write("<h1 class=\"NavTitle\">" + topic.getLabel() + "</h1>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.write("<h3 class=\"NavListTitle\">" + WebappResources.getString("TocHeading", locale) + "</h3>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.write("<ul class=\"NavList\">\n"); //$NON-NLS-1$
		ITopic[] subtopics = topic.getSubtopics();
		for (int i=0;i<subtopics.length;++i) {
			if (ScopeUtils.showInTree(subtopics[i], scope)) {
				writer.write("<li><a href=\""); //$NON-NLS-1$
				String href = subtopics[i].getHref();
				if (href == null) {
					href = path + '_' + i;
				} else {
					href = XMLGenerator.xmlEscape(UrlUtil.getHelpURL(href));
					href = TocFragmentServlet.fixupHref(href, path + '_' + i);
				}
				writer.write(href);
				writer.write("\">" + subtopics[i].getLabel() + "</a></li>\n");  //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		writer.write("</ul>\n"); //$NON-NLS-1$
		writer.write(XHTML_3);
	}
}
