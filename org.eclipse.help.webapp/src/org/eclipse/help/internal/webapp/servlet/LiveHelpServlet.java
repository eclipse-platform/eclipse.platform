/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.webapp.data.WebappPreferences;

/**
 * Servlet to handle live help action requests
 */
public class LiveHelpServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	/**
	 */
	@Override
	public void init() throws ServletException {
		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_INFOCENTER) {
			throw new ServletException();
		}
	}

	/**
	 * Called by the server (via the <code>service</code> method) to allow a
	 * servlet to handle a GET request.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_INFOCENTER) {
			return;
		}
		if (!new WebappPreferences().isActiveHelp()) {
			return;
		}
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		String sessionid = req.getSession().getId();
		Cookie[] cookies = req.getCookies();
		boolean jsessOK = false;
		boolean xsessOK = false;
		boolean lsessOK = false;
		// Unique session ID per help server
		int port = req.getLocalPort();
		String xsessname = "XSESSION-" + port; //$NON-NLS-1$
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("JSESSIONID")) {//$NON-NLS-1$
					if (sessionid.length() >= 30 &&
							cookie.getValue().startsWith(sessionid)) {
						jsessOK = true;
					}
				}
				if (cookie.getName().equals(xsessname)) {
					if (cookie.getValue().equals(req.getSession().getAttribute("XSESSION"))) { //$NON-NLS-1$
						xsessOK = true;
					}
				}
			}
		}
		String token = req.getParameter("token"); //$NON-NLS-1$
		if (token != null && token.equals(req.getSession().getAttribute("LSESSION"))) { //$NON-NLS-1$
			lsessOK = true;
		}
		if (!jsessOK) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "JSESSIONID"); //$NON-NLS-1$
			return;
		}
		if (!lsessOK) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "token"); //$NON-NLS-1$
			return;
		}
		if (!xsessOK) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, xsessname);
			return;
		}
		String pluginID = req.getParameter("pluginID"); //$NON-NLS-1$
		if (pluginID == null)
			return;
		String className = req.getParameter("class"); //$NON-NLS-1$
		if (className == null)
			return;
		String arg = req.getParameter("arg"); //$NON-NLS-1$
		BaseHelpSystem.runLiveHelp(pluginID, className, arg);
		/*
		 * @FIXME Should runLiveHelp return an error if the plugin/class is wrong
		 * so a SC_BAD_REQUEST can be returned? Or does this reveal too much?
		 */
		resp.setStatus(HttpServletResponse.SC_ACCEPTED);
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
		doGet(req, resp);
	}
}
