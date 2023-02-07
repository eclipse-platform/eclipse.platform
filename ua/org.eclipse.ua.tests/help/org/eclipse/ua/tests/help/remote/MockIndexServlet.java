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

import org.eclipse.help.internal.index.Index;
import org.eclipse.help.internal.index.IndexContribution;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.servlet.IndexServlet;
import org.eclipse.ua.tests.help.other.UserIndex;
import org.eclipse.ua.tests.help.other.UserIndexEntry;
import org.eclipse.ua.tests.help.other.UserTopic;

public class MockIndexServlet extends IndexServlet {

	private static final long serialVersionUID = -930969620357059313L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String locale = UrlUtil.getLocale(req, resp);
		String response;
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$

		IndexContribution[] contributions = getIndex(req, locale);
		try {
			response = serialize(contributions, locale);
		} catch (TransformerException e) {
			throw new ServletException(e);
		}
		resp.getWriter().write(response);
	}

	private IndexContribution[] getIndex(HttpServletRequest req, String locale) {
		int port = req.getLocalPort();
		UserIndex index = new UserIndex(true);
		UserIndexEntry entry1 = new UserIndexEntry("entry1_" + locale, false);
		UserTopic topic1 = new UserTopic("topic1_", "href.html", false);
		index.addEntry(entry1);
		entry1.addTopic(topic1);
		UserIndexEntry entry2 = new UserIndexEntry("entry2_" + locale, false);
		UserTopic topic2 = new UserTopic("topic2_"  + port, "href" + port + ".html", false);
		index.addEntry(entry2);
		entry2.addTopic(topic2);
		IndexContribution contribution = new IndexContribution();
		contribution.setId("mock.index");
		contribution.setIndex(new Index(index));
		contribution.setLocale(locale);
		return new IndexContribution[] { contribution };
	}

}


