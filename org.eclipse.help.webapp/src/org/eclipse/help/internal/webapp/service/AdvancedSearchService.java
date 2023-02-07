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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchProgressMonitor;
import org.eclipse.help.internal.webapp.data.SearchData;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.parser.SearchParser;
import org.eclipse.help.internal.webapp.utils.SearchXMLGenerator;
import org.eclipse.help.internal.webapp.utils.Utils;

/**
 * Returns the search hits in <code>xml</code> or <code>json</code>
 * form for the query provided in the <code>searchWord</code> parameter.
 *
 * <p>This servlet is called on infocenters by client workbenches
 * configured for remote help in order to retrieve search hits
 * from the remote help server.
 *
 * <p>Internally reads {@link org.eclipse.help.internal.webapp.data.SearchData}.
 *
 * @param searchWord	- specifies the search keyword
 * @param quickSearch	- (optional) specifies if it is a quick search. Scopes
 * 						  is just the selected toc or topic
 * @param quickSearchType	- (optional) specifies <code>QuickSearchTopic</code> for topic
 * 							  quick search
 * @param scope			- (optional) specifies search scope values
 * @param noCategory	- (optional) if true, do not categorize by help topic
 * @param workingSet	- (optional) specifies the working set for scoped search
 * @param maxHits		- (optional) specifies the number of hits to return, default value is 500
 * @param fieldSearch	- (optional) specifies if field only search should be performed;
 * 						  if set to false, default field "contents" and all other fields will be searched
 * @param lang			- (optional) specifies the locale
 * @param returnType	- (Optional) specifies the return type of the servlet.
 * 						  Accepts either <code>xml</code> (default) or
 * 						  <code>json</code>
 *
 * @return		The search hits, either as <code>xml</code> (default) or
 * 				<code>json</code>
 *
 * @version	$Version$
 *
 **/
public class AdvancedSearchService extends HttpServlet {

	private static final long serialVersionUID = 1L;
	public static final String XID = "xid"; //$NON-NLS-1$

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		// Set standard HTTP/1.1 no-cache headers.
		resp.setHeader("Cache-Control",  //$NON-NLS-1$
				"no-store, no-cache, must-revalidate"); //$NON-NLS-1$

		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$

		ServletContext context = req.getSession().getServletContext();
		SearchData searchData = new SearchData(context, req, resp);

		String noCat = req.getParameter(Utils.NO_CATEGORY);
		boolean boolIsCategory = (noCat == null
				|| !noCat.equalsIgnoreCase("true")); //$NON-NLS-1$

		String locale = UrlUtil.getLocale(req, resp);
		SearchProgressMonitor pm = SearchProgressMonitor
				.getProgressMonitor(locale);
		while (!pm.isDone()) {
			try {
				Thread.sleep(500); // Sleep for 0.5 sec
			} catch(InterruptedException ex) {}
		}

		// Load search results
		searchData.readSearchResults();
		SearchHit[] hits = searchData.getResults();

		String response = SearchXMLGenerator.serialize(hits, boolIsCategory);

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
		SearchParser searchParser = new SearchParser();

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
