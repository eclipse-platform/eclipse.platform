/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.remote;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.webapp.data.UrlUtil;

public class MockContentServlet extends HttpServlet {

	private static final long serialVersionUID = 2360013070409217702L;
	private static int callcount = 0;

	/**
	 * Return a create page based on the path and locale unless the path
	 * starts with "/invalid" in which case return an I/O error
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String locale = UrlUtil.getLocale(req, resp);
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$
		String path = req.getPathInfo();
		int slash = path.indexOf('/', 1);
		String plugin = path.substring(1, slash);
		String file = path.substring(slash);
		if (file.startsWith("/invalid")) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			int port = req.getLocalPort();
			String response = RemoteTestUtils.createMockContent(plugin, file, locale, port);
			resp.getWriter().write(response);
		}
		callcount++;
	}

	public static int getCallcount() {
		return callcount;
	}

}
