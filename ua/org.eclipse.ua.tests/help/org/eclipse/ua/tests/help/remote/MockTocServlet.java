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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.toc.TocContribution;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.servlet.TocServlet;
import org.eclipse.ua.tests.help.other.UserToc;
import org.eclipse.ua.tests.help.other.UserTopic;

public class MockTocServlet extends TocServlet {

	private static final long serialVersionUID = 2934062693291854845L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String locale = UrlUtil.getLocale(req, resp);
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$
		UserToc toc1 = new UserToc("Mock Toc " + locale, null, true);
		UserTopic topic1 = new UserTopic("Topic_" + locale, "http://www.eclipse.org", true);
		toc1.addTopic(topic1);
		TocContribution contribution1 = createToc(toc1, "org.eclipse.help.base", locale);
		UserToc toc2 = new UserToc("Mock Toc 2 " + locale, null, true);
		UserTopic topic2 = new UserTopic("Topic_" + locale, "http://www.eclipse.org", true);
		toc2.addTopic(topic2);
		TocContribution contribution2 = createToc(toc2, "mock.toc", locale);
		String response;
		try {
			response = serialize(new TocContribution[] { contribution1, contribution2 }, locale);
			resp.getWriter().write(response);
		} catch (TransformerException e) {
			resp.sendError(400);
		}
	}

	protected TocContribution createToc(UserToc toc, String id, String locale) {
		TocContribution contribution;
		contribution = new TocContribution();
		contribution.setCategoryId(null);
		contribution.setContributorId(id);
		contribution.setExtraDocuments(new String[0]);
		contribution.setLocale(locale);
		contribution.setPrimary(true);
		contribution.setSubToc(false);
		contribution.setId(id);
		contribution.setToc(new Toc(toc));
		return contribution;
	}

}
