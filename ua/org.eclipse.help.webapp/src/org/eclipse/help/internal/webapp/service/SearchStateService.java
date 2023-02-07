/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.search.SearchProgressMonitor;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.utils.Utils;

/**
 * Returns <code>xml</code> or <code>String</code> representing search progress monitor
 *
 * @param lang			- (optional) specifies the locale
 * @param returnType	- (Optional) specifies the return type of the servlet.
 * 						  Accepts either <code>xml</code> (default) or
 * 						  <code>json</code>
 *
 * @return		Search progress monitor state, either as <code>xml</code>
 * or <code>String</code> (default)
 *
 * @version	$Version$
 *
 **/
public class SearchStateService extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String STATE = "state"; //$NON-NLS-1$
	private static final String PERCENT = "percent"; //$NON-NLS-1$

	@Override
	public void init() throws ServletException {
	}

	/**
	 * Called by the server (via the <code>service</code> method) to allow a
	 * Servlet to handle a GET request.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		process(req,resp);
	}

	/**
	 *
	 * Called by the server (via the <code>service</code> method) to allow a
	 * Servlet to handle a POST request.
	 *
	 * Handle the search requests,
	 *
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		process(req, resp);
	}


	/**
	 * Processes all requests to the servlet.
	 *
	 */
	private void process(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$
		resp.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

		int indexCompletion = 0;
		String locale = UrlUtil.getLocale(req, resp);
		SearchProgressMonitor pm = SearchProgressMonitor
				.getProgressMonitor(locale);
		if (pm.isDone()) {
			indexCompletion = 100;
		} else {
			indexCompletion = pm.getPercentage();
			if (indexCompletion >= 100) {
				// 38573 We do not have results, so index cannot be 100
				indexCompletion = 100 - 1;
			}
		}

		String returnType = req.getParameter(Utils.RETURN_TYPE);
		boolean isXML = Utils.XML.equalsIgnoreCase(returnType);
		if (isXML) {
			resp.setContentType("application/xml"); //$NON-NLS-1$
			resp.getWriter().write(toXML(indexCompletion));
		} else {
			resp.setContentType("text/plain"); //$NON-NLS-1$
			resp.getWriter().write(toString(indexCompletion));
		}
		resp.getWriter().flush();
	}

	public static String toXML(int percent) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"; //$NON-NLS-1$
		xml += '<'+STATE+">\n"; //$NON-NLS-1$
		xml += "	<"+PERCENT+'>'+percent+"</"+PERCENT+">\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		xml += "</"+STATE+">"; //$NON-NLS-1$ //$NON-NLS-2$
		return xml;
	}

	public static String toString(int percent) {
		return "Percent:" + percent; //$NON-NLS-1$
	}
}
