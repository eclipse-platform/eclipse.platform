/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.dynamic.DocumentWriter;
import org.eclipse.help.internal.extension.ContentExtension;
import org.eclipse.help.internal.webapp.data.UrlUtil;

/*
 * Sends all topic extensions available on this host in XML form.
 *
 * This is called on infocenters by client workbenches configured for remote
 * help in order to gather all the pieces of a document.
 */
public class ExtensionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Map<String, String> responseByLocale;
	private DocumentWriter writer;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// set the character-set to UTF-8 before calling resp.getWriter()
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$
		resp.getWriter().write(processRequest(req, resp));
	}

	protected String processRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String locale = UrlUtil.getLocale(req, resp);
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

		if (responseByLocale == null) {
			responseByLocale = new WeakHashMap<>();
		}
		String response = responseByLocale.get(locale);
		if (response == null) {
			ContentExtension[] extensions = HelpPlugin.getContentExtensionManager().getExtensions(locale);
			try {
				response = serialize(extensions);
			}
			catch (TransformerException e) {
				throw new ServletException(e);
			}
			responseByLocale.put(locale, response);
		}
		return response;
	}

	private String serialize(ContentExtension[] extensions) throws TransformerException {
		StringBuilder buf = new StringBuilder();
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
		buf.append("<contentExtensions>\n"); //$NON-NLS-1$
		for (ContentExtension extension : extensions) {
			if (writer == null) {
				writer = new DocumentWriter();
			}
			buf.append(writer.writeString(extension, false));
		}
		buf.append("</contentExtensions>\n"); //$NON-NLS-1$
		return buf.toString();
	}
}
