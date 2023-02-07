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
package org.eclipse.help.internal.webapp.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.eclipse.help.IIndexContribution;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.dynamic.DocumentWriter;
import org.eclipse.help.internal.index.Index;
import org.eclipse.help.internal.webapp.data.UrlUtil;

/*
 * Sends all available keyword index data in XML form. The data is sent as one
 * large index contribution that includes all merged contributions from the
 * system.
 *
 * This is called on infocenters by client workbenches configured for remote
 * help in order to gather all the index keywords and assemble them into a
 * complete index.
 */
public class IndexServlet extends HttpServlet {

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
		BaseHelpSystem.checkMode();
		String locale = UrlUtil.getLocale(req, resp);
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

		if (responseByLocale == null) {
			responseByLocale = new WeakHashMap<>();
		}
		String response = responseByLocale.get(locale);
		if (response == null) {
			IIndexContribution[] contributions = HelpPlugin.getIndexManager().getIndexContributions(locale);
			try {
				response = serialize(contributions, locale);
			}
			catch (TransformerException e) {
				throw new ServletException(e);
			}
			responseByLocale.put(locale, response);
		}

		return (response != null) ? response : ""; //$NON-NLS-1$
	}

	public String serialize(IIndexContribution[] contributions, String locale) throws TransformerException {
		StringBuilder buf = new StringBuilder();
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
		buf.append("<indexContributions>\n"); //$NON-NLS-1$
		if (writer == null) {
			writer = new DocumentWriter();
		}
		for (IIndexContribution contrib : contributions) {
			buf.append("<indexContribution\n"); //$NON-NLS-1$
			buf.append("      id=\"" + contrib.getId() + '"'); //$NON-NLS-1$
			buf.append("      locale=\"" + contrib.getLocale() + "\">\n"); //$NON-NLS-1$ //$NON-NLS-2$
			buf.append(writer.writeString((Index)contrib.getIndex(), false));
			buf.append("</indexContribution>\n"); //$NON-NLS-1$
		}
		buf.append("</indexContributions>\n"); //$NON-NLS-1$
		return buf.toString();
	}
}
