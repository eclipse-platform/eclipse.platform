/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.base.BaseHelpSystem;
/**
 * Servlet to interface client with remote Eclipse
 */
public class ContentServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private EclipseConnector connector;

	/**
	 */
	@Override
	public void init() throws ServletException {
		try {
			connector = new EclipseConnector(getServletContext());
		} catch (Throwable e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Called by the server (via the <code>service</code> method) to allow a
	 * servlet to handle a GET request.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		BaseHelpSystem.checkMode();
		if (connector != null) {
			connector.transfer(req, resp);
		}
	}
	/**
	 *
	 * Called by the server (via the <code>service</code> method) to allow a
	 * servlet to handle a POST request.
	 *
	 * Handle the search requests,
	 *
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (connector != null)
			connector.transfer(req, resp);
	}
}
