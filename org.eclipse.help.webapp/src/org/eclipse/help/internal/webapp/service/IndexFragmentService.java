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

import org.eclipse.help.internal.webapp.parser.IndexFragmentParser;
import org.eclipse.help.internal.webapp.servlet.IndexFragmentServlet;
import org.eclipse.help.internal.webapp.utils.Utils;

/**
 * Returns <code>xml</code> or <code>json</code> representing selected parts
 * of the index.
 *
 * <p>This servlet is called on infocenters by client workbenches
 * configured for remote help in order to gather selected parts of the index.
 *
 * <p>Extends the {@link org.eclipse.help.internal.webapp.servlet.IndexFragmentServlet}
 * servlet.
 *
 * @param start			- (optional) represents the part of the index to
 * 						  start reading from
 * @param size			- (optional) indicates the number of entries to read,
 * 						  no size parameter or a negative size parameter
 * 						  indicates that all entries which match the start
 * 						  letters should be displayed
 * @param mode			- (optional) specifies either <code>next</code> or
 * 						  <code>previous</code>
 * @param entry			- (optional) represents the starting point relative
 * 						  to the start
 * @param showAll		- (optional) specifies  either <code>on</code> or
 * 						  <code>off</code> to set filter enablement of
 * 						  activity support
 * @param showconfirm	- (optional) specifies <code>true</code> or
 * 						  <code>false</code> to show/hide all confirm dialog
 * @param lang			- (optional) specifies the locale
 * @param returnType	- (Optional) specifies the return type of the servlet.
 * 						  Accepts either <code>xml</code> (default) or
 * 						  <code>json</code>
 *
 * @return		Selected parts of the index, either as <code>xml</code>
 * 				(default) or <code>json</code>
 *
 * @version	$Version$
 *
 **/
public class IndexFragmentService extends IndexFragmentServlet {

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
			response = getJSONResponse(response);
		}

		resp.getWriter().write(response);
	}

	protected String getJSONResponse(String response)
			throws IOException {
		IndexFragmentParser indexParser = new IndexFragmentParser();
		if (response != null) {
			try (InputStream is = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8))) {
				indexParser.parse(is);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Call after the catch.
		// An empty JSON is created if any Exception is thrown
		// Else returns the complete JSON
		return indexParser.toJSON();
	}

}
