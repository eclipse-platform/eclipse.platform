/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.webapp.data.RequestScope;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.parser.NavParser;
import org.eclipse.help.internal.webapp.servlet.NavServlet;
import org.eclipse.help.internal.webapp.servlet.XMLGenerator;
import org.eclipse.help.internal.webapp.utils.Utils;

/**
 * Generates either xml, json or html page having navigation informations where topic
 * is not present in the table of contents for the selected toc passed as request
 * path info. Displays links to the direct child topics.
 *
 * <p>This servlet is called on infocenters by client workbenches
 * configured for remote help in order to generate the navigation pages.
 *
 * <p>Passes the request to {@link org.eclipse.help.internal.webapp.servlet.NavServlet}
 * servlet.
 *
 * @param returnType	- (Optional) specifies the return type of the servlet.
 * 						  Accepts either <code>xml</code> (default) or <code>html</code>
 * 						  or <code>json</code>
 *
 * @return	A navigation information having the links to the direct child topics for
 * 			the selected toc, either as <code>xml</code> (default) or
 * 			<code>html</code> or <code>json</code>
 *
 * @version	$Version$
 *
 **/
public class NavService extends NavServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

		String returnType = req.getParameter(Utils.RETURN_TYPE);
		boolean boolIsHTML = (returnType != null
				&& returnType.equalsIgnoreCase(Utils.HTML));
		// If HTML output is required, call AboutServlet class
		if (boolIsHTML) {
			processHTMLOutputRequest(req, resp);
			return;
		}

		// Set standard HTTP/1.1 no-cache headers.
		resp.setHeader("Cache-Control",  //$NON-NLS-1$
				"no-store, no-cache, must-revalidate"); //$NON-NLS-1$
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$

		// create XML response
		String response = processRequest(req, resp);

		boolean boolIsJSON = (returnType != null
				&& returnType.equalsIgnoreCase(Utils.JSON));

		// If JSON output is required
		if (boolIsJSON) {
			resp.setContentType("text/plain"); //$NON-NLS-1$
			response = getJSONResponse(response);
		}

		resp.getWriter().write(response);
	}

	private void processHTMLOutputRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("text/html; charset=UTF-8"); //$NON-NLS-1$

		String baseURL = req.getRequestURL().toString();
		String navURL = baseURL.replaceFirst(Utils.SERVICE_CONTEXT, ""); //$NON-NLS-1$
		String query = req.getQueryString();
		if (query != null)
			navURL += '?' + query;
		URL url = new URL(navURL);
		String response = Utils.convertStreamToString(ProxyUtil.getStream(url));
		response = Utils.updateResponse(response);

		OutputStream out = resp.getOutputStream();
		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
			writer.write(response);
		}
	}

	protected String getJSONResponse(String response)
			throws IOException {
		NavParser navParser = new NavParser();
		if (response != null) {
			try (InputStream is = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8))) {
				navParser.parse(is);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Call after the catch.
		// An empty JSON is created if any Exception is thrown
		// Else returns the complete JSON
		return navParser.toJSON();
	}

	private String processRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Locale locale = getLocale(req, resp);

		String path = req.getPathInfo().substring(1);
		int index = path.indexOf("service/nav/"); //$NON-NLS-1$
		if (index > -1)
			path = path.substring(index+12);
		ITopic topic = getTopic(path, locale);

		AbstractHelpScope scope = RequestScope.getScope(req, resp, false);
		return "" + writeContent(topic, path, locale, scope); //$NON-NLS-1$
	}

	private String writeContent(ITopic topic, String path, Locale locale,
			AbstractHelpScope scope) {
		StringBuilder buff = new StringBuilder();
		buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$

		buff.append("<nav\n      title=\""); //$NON-NLS-1$
		buff.append(XMLGenerator.xmlEscape(topic.getLabel()));
		buff.append("\">"); //$NON-NLS-1$

		ITopic[] subtopics = topic.getSubtopics();
		for (int i=0;i<subtopics.length;++i) {
			if (ScopeUtils.showInTree(subtopics[i], scope)) {
				buff.append("\n        <topic\n          href=\""); //$NON-NLS-1$
				String href = subtopics[i].getHref();
				if (href == null) {
					href = path + '_' + i;
				}
				else {
					href = XMLGenerator.xmlEscape(UrlUtil.getHelpURL(href));
				}
				buff.append(href);
				buff.append("\"\n          title=\""); //$NON-NLS-1$
				buff.append(XMLGenerator.xmlEscape(subtopics[i].getLabel()));
				buff.append("\">\n        </topic>"); //$NON-NLS-1$
			}
		}
		buff.append("\n</nav>"); //$NON-NLS-1$

		return buff.toString();
	}

}
