/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchHitCollector;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.QueryTooComplexException;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.utils.SearchXMLGenerator;

/*
 * Returns the search hits for the query provided in the phrase parameter.
 *
 * This is called on infocenters by client workbenches configured for remote
 * help in order to retrieve search hits from the remote help server.
 */
public class SearchServlet extends HttpServlet {

	private final class HitCollector implements ISearchHitCollector {
		public Collection<SearchHit> results = new ArrayList<>();

		@Override
		public void addHits(List<SearchHit> hits, String wordsSearched) {
			if (results != null) {
				results.addAll(hits);
			}
		}

		@Override
		public void addQTCException(QueryTooComplexException exception)
				throws QueryTooComplexException {
			searchException = exception;
		}
	}

	private static final long serialVersionUID = 1L;
	private static final String PARAMETER_PHRASE = "phrase"; //$NON-NLS-1$
	private QueryTooComplexException searchException;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String response = processRequest(req, resp);
		if ("".equals(response)) //$NON-NLS-1$
			resp.sendError(400); // bad request; missing parameter
		else
			resp.getWriter().write(response);
	}

	protected String processRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		BaseHelpSystem.checkMode();
		HitCollector collector = new HitCollector();
		String locale = UrlUtil.getLocale(req, resp);
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$
		String phrase = req.getParameter(PARAMETER_PHRASE);
		if (phrase != null) {
			ISearchQuery query = new SearchQuery(phrase, false, Collections.<String> emptyList(), locale);
			collector.results.clear();
			BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
			if (searchException == null) {
				return serialize(collector.results);
			}
		}
		return ""; //$NON-NLS-1$
	}

	public static String serialize(Collection<SearchHit> results) {
		return SearchXMLGenerator.serialize(results);
	}
}
