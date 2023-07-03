/*******************************************************************************
 *  Copyright (c) 2009, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.webextension;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.webapp.data.TocData;

public class TitleSearchData extends TocData {

public TitleSearchData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);
}

private List<SearchResult> results;
private String searchTerm;

public static class SearchResult {
	public String title;
	public String href;
}

public SearchResult[] getSearchResults() {
		results = new ArrayList<>();
		searchTerm = request.getParameter("searchWord");
		IToc[] tocs = getTocs();
		for (IToc toc : tocs) {
			ITopic[] topics = toc.getTopics();
			for (ITopic topic : topics) {
				searchTopic(topic);
			}
		}
		return results.toArray(new SearchResult[results.size()]);
}

private void searchTopic(ITopic topic) {
	if (topic.getLabel().toLowerCase().contains(searchTerm.toLowerCase())
		&& topic.getHref() != null) {
		SearchResult result = new SearchResult();
		result.title = topic.getLabel();
		result.href = "../../../topic" + topic.getHref();
		results.add(result);
	}
	ITopic[] topics = topic.getSubtopics();
	for (ITopic topic2 : topics) {
		searchTopic(topic2);
	}
}

}
