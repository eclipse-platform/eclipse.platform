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

import org.eclipse.help.internal.webapp.parser.ContextParser;
import org.eclipse.help.internal.webapp.servlet.ContextServlet;
import org.eclipse.help.internal.webapp.utils.Utils;

/**
 * Returns a context help entry with the id specified in the
 * <code>id</code> parameter in <code>xml</code> or <code>json</code> form.
 *
 * <p>This servlet is called on infocenters by client workbenches
 * configured for remote help in order to retrieve context help
 * stored on the remote help server.
 *
 * <p>Extends the {@link org.eclipse.help.internal.webapp.servlet.ContextServlet}
 * servlet.
 *
 * @param id			- A <code>String</code> specifying the context id
 * 						for the context help entry
 * @param lang			- (optional) A <code>String</code> specifying the locale
 * @param returnType	- (Optional) A <code>String</code> specifying the
 * 						return type of the servlet. Accepts either
 * 						<code>xml</code> (default) or <code>json</code>
 *
 * @return		A context help entry with the id specified, either as
 * 				<code>xml</code> (default) or <code>json</code>
 *
 * @exception	400 Error - If context <code>id</code> parameter is missing
 * @exception	404 Error - If wrong context <code>id</code> parameter
 *
 * @version	$Version$
 *
 **/
public class ContextService extends ContextServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		// Set standard HTTP/1.1 no-cache headers.
		resp.setHeader("Cache-Control",  //$NON-NLS-1$
				"no-store, no-cache, must-revalidate"); //$NON-NLS-1$

		String response = processRequest(req, resp);
		if ("400".equals(response)) { //$NON-NLS-1$
			resp.sendError(400); // bad request; missing parameter
			return;
		} else if ("404".equals(response)) { //$NON-NLS-1$
			resp.sendError(404); // Wrong context id; not found
			return;
		}

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
		ContextParser searchParser = new ContextParser();
		if (response != null) {
			try (InputStream is = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8))) {
				searchParser.parse(is);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Call after the catch.
		// An empty JSON is created if any Exception is thrown
		// Else returns the complete JSON
		return searchParser.toJSON();
	}

}
