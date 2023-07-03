/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
import org.eclipse.help.internal.base.HelpApplication;
import org.eclipse.help.internal.base.HelpDisplay;
import org.eclipse.help.internal.webapp.data.UrlUtil;

/**
 * Servlet to control Eclipse helpApplication from standalone application.
 * Accepts the following parameters: command=displayHelp | shutdown
 * | install | update | enable | disable | uninstall | search | listFeatures
 * | addSite | removeSite | apply.
 * href - may be provided if comand==displayHelp.
 * featureId, version, from, to, verifyOnly may be provided for update commands
 */
public class ControlServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static final String CMD_DISPLAYHELP = "displayHelp"; //$NON-NLS-1$

	public static final String CMD_DISPLAYHELPWINDOW = "displayHelpWindow"; //$NON-NLS-1$

	public static final String CMD_SHUTDOWN = "shutdown"; //$NON-NLS-1$

	public static final String CMD_INSTALL = "install"; //$NON-NLS-1$

	public static final String CMD_UPDATE = "update"; //$NON-NLS-1$

	public static final String CMD_ENABLE = "enable"; //$NON-NLS-1$

	public static final String CMD_DISABLE = "disable"; //$NON-NLS-1$

	public static final String CMD_UNINSTALL = "uninstall"; //$NON-NLS-1$

	public static final String CMD_SEARCH = "search"; //$NON-NLS-1$

	public static final String CMD_LIST = "listFeatures"; //$NON-NLS-1$

	public static final String CMD_ADDSITE = "addSite"; //$NON-NLS-1$

	public static final String CMD_APPLY = "apply"; //$NON-NLS-1$

	public static final String CMD_REMOVESITE = "removeSite"; //$NON-NLS-1$

	public static final String PACKAGE_PREFIX = "org.eclipse.update.standalone."; //$NON-NLS-1$

	public static final String CLASS_INSTALL = PACKAGE_PREFIX
			+ "InstallCommand"; //$NON-NLS-1$

	public static final String CLASS_UPDATE = PACKAGE_PREFIX + "UpdateCommand"; //$NON-NLS-1$

	public static final String CLASS_ENABLE = PACKAGE_PREFIX + "EnableCommand"; //$NON-NLS-1$

	public static final String CLASS_DISABLE = PACKAGE_PREFIX
			+ "DisableCommand"; //$NON-NLS-1$

	public static final String CLASS_UNINSTALL = PACKAGE_PREFIX
			+ "UninstallCommand"; //$NON-NLS-1$

	public static final String CLASS_SEARCH = PACKAGE_PREFIX + "SearchCommand"; //$NON-NLS-1$

	public static final String CLASS_LIST = PACKAGE_PREFIX
			+ "ListFeaturesCommand"; //$NON-NLS-1$

	public static final String CLASS_ADDSITE = PACKAGE_PREFIX
			+ "AddSiteCommand"; //$NON-NLS-1$

	public static final String CLASS_REMOVESITE = PACKAGE_PREFIX
			+ "RemoveSiteCommand"; //$NON-NLS-1$

	public static final String PARAM_FEATUREID = "featureId"; //$NON-NLS-1$

	public static final String PARAM_VERSION = "version"; //$NON-NLS-1$

	public static final String PARAM_FROM = "from"; //$NON-NLS-1$

	public static final String PARAM_TO = "to"; //$NON-NLS-1$

	public static final String PARAM_VERIFYONLY = "verifyOnly"; //$NON-NLS-1$

	private HelpDisplay helpDisplay = null;

	private boolean shuttingDown = false;

	/**
	 * Called by the servlet container to indicate to a servlet that the servlet
	 * is being placed into service.
	 */
	@Override
	public void init() throws ServletException {
		super.init();
		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_STANDALONE) {
			helpDisplay = BaseHelpSystem.getHelpDisplay();
		}
	}

	/**
	 * Called by the server (via the <code>service</code> method) to allow a
	 * servlet to handle a GET request.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		processRequest(req, resp);
	}

	/**
	 * Called by the server (via the <code>service</code> method) to allow a
	 * servlet to handle a POST request.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		processRequest(req, resp);
	}

	private void processRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

		// for HTTP 1.1
		resp.setHeader("Cache-Control", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
		// for HTTP 1.0
		resp.setHeader("Pragma", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
		resp.setDateHeader("Expires", 0); //$NON-NLS-1$
		//prevents caching at the proxy server

		if (!UrlUtil.isLocalRequest(req)) {
			// do not allow remote clients to execute this servlet
			return;
		}
		if (!"/help".equals(req.getContextPath()) //$NON-NLS-1$
				|| !"/control".equals(req.getServletPath())) { //$NON-NLS-1$
			// do not allow arbitrary URLs to execute this servlet
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, ""); //$NON-NLS-1$
			return;
		}

		if (shuttingDown) {
			return;
		}

		String command = req.getParameter("command"); //$NON-NLS-1$
		if (command == null) {
			// this should never happen and is invisible to the user
			resp.getWriter().print("No command."); //$NON-NLS-1$
			return;
		}

		if (CMD_SHUTDOWN.equalsIgnoreCase(command)) {
			shutdown();
		} else if (CMD_DISPLAYHELP.equalsIgnoreCase(command)) {
			if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_STANDALONE) {
				displayHelp(req);
			}
		} else if (CMD_DISPLAYHELPWINDOW.equalsIgnoreCase(command)) {
			if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_STANDALONE) {
				displayHelp(req);
				HelpApplication.setShutdownOnClose(true);
			}
		} else if (CMD_INSTALL.equalsIgnoreCase(command)
				|| CMD_ENABLE.equalsIgnoreCase(command)
				|| CMD_UPDATE.equalsIgnoreCase(command)
				|| CMD_DISABLE.equalsIgnoreCase(command)
				|| CMD_UNINSTALL.equalsIgnoreCase(command)
				|| CMD_SEARCH.equalsIgnoreCase(command)
				|| CMD_LIST.equalsIgnoreCase(command)
				|| CMD_ADDSITE.equalsIgnoreCase(command)
				|| CMD_REMOVESITE.equalsIgnoreCase(command)
				|| CMD_APPLY.equalsIgnoreCase(command)) {
			// skip old update system commands
		} else {
			// this should never happen and is invisible to the user
			resp.getWriter().print("Unrecognized command."); //$NON-NLS-1$
		}
	}

	/**
	 * Shuts-down Eclipse helpApplication.
	 */
	private void shutdown() {
		shuttingDown = true;
		HelpApplication.stopHelp();
	}

	/**
	 * Displays help.
	 *
	 * @param req
	 *            HttpServletRequest that might contain href parameter, which is
	 *            the resource to display
	 */
	private void displayHelp(HttpServletRequest req) {
		String href = req.getParameter("href"); //$NON-NLS-1$
		if (href != null) {
			helpDisplay.displayHelpResource(href, false);
		} else {
			helpDisplay.displayHelp(false);
		}
	}
}
