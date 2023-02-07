/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.servlet.SearchServlet;

public class MockSearchServlet extends HttpServlet {

	private static final long serialVersionUID = -5115067950875335923L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String phrase = req.getParameter("phrase");
		String locale = UrlUtil.getLocale(req, resp);
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$
		List<SearchHit> results = new ArrayList<>();
		if (isSearchHit(phrase, locale)) {
			SearchHit hit = new SearchHit("http://www.eclipse.org",
					"Hit from Mock Servlet",
					"This is the description of a hit from the mock servlet",
					0, null, null, null, false);
			results.add(hit);
		}
		String response = SearchServlet.serialize(results);
		resp.getWriter().write(response);
	}

	private boolean isSearchHit(String phrase, String locale) {
		if (locale.equals("de")) {
			return "dedfdsadsads".equals(phrase);
		} else {
			return "endfdsadsads".equals(phrase);
		}
	}

}
