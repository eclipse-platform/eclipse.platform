/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.webapp.parser.TocFragmentParser;
import org.eclipse.help.internal.webapp.servlet.TocFragmentServlet;
import org.eclipse.help.internal.webapp.utils.Utils;

/**
 * Returns <code>xml</code> or <code>json</code> representing selected parts
 * of one or more TOCs  depending on the parameters.
 *
 * <p>This servlet is called on infocenters by client workbenches
 * configured for remote help in order to gather selected parts of the TOCs.
 *
 * <p>Extends the {@link org.eclipse.help.internal.webapp.servlet.TocFragmentServlet}
 * servlet.
 *
 * @param topic			- (optional)
 * @param toc			- (optional) specifies the toc id value in xml node
 * @param expandPath	- (optional)
 * @param anchor		- (optional)
 * @param path			- (optional) specifies initial root path
 * @param href			- (optional) specifies
 * @param errorSuppress - (optional) (default) false
 * @param lang			- (optional) specifies the locale
 * @param returnType	- (Optional) specifies the return type of the servlet.
 * 						  Accepts either <code>xml</code> (default) or
 * 						  <code>json</code>
 *
 * @return		The selected parts of one or more TOCs, either as <code>xml</code>
 * (default) or <code>json</code>
 *
 * @version	$Version$
 *
 **/
public class TocFragmentService extends TocFragmentServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		// Set standard HTTP/1.1 no-cache headers.
		resp.setHeader("Cache-Control",  //$NON-NLS-1$
				"no-store, no-cache, must-revalidate"); //$NON-NLS-1$
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$

		String response = processRequest(req, resp);

		String returnType = req.getParameter(Utils.RETURN_TYPE);
		boolean boolIsJSON = (returnType != null
				&& returnType.equalsIgnoreCase(Utils.JSON));

		// If JSON output is required
		if (boolIsJSON) {
			resp.setContentType("text/plain"); //$NON-NLS-1$
			String toc = req.getParameter("toc"); //$NON-NLS-1$
			String path = req.getParameter("path"); //$NON-NLS-1$
			response = getJSONResponse(toc, path, response);
		}

		resp.getWriter().write(response);
	}

	protected String getJSONResponse(String toc, String path, String xmlResource)
			throws IOException {
		TocFragmentParser tocParser = new TocFragmentParser();
		if (xmlResource != null) {
			try (InputStream is = new ByteArrayInputStream(xmlResource.getBytes(StandardCharsets.UTF_8))) {

				int level = 0;
				if (toc != null && toc.length() > 0) {
					level++;

					if (path != null && path.length() > 0) {
						String[] pathIdxs = path.split("_"); //$NON-NLS-1$
						level += pathIdxs.length;
					}
				}

				tocParser.parse(is, level);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Call after the catch.
		// An empty JSON is created if any Exception is thrown
		// Else returns the complete JSON
		return tocParser.toJSON();
	}
}
