/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.core.runtime.IPath;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.webapp.data.RequestScope;
import org.eclipse.help.internal.webapp.data.TocData;
import org.eclipse.help.internal.webapp.data.UrlUtil;

public class ChildLinkInserter {

	private HttpServletRequest req;
	private OutputStream out;
	private static final String NO_CHILDREN = "no_child_topics"; //$NON-NLS-1$
	private static final String HAS_CHILDREN = "has_child_topics"; //$NON-NLS-1$
	private AbstractHelpScope scope;

	public ChildLinkInserter(HttpServletRequest req, OutputStream out) {
		this.req = req;
		this.out = out;
		scope = RequestScope.getScope(req, null, false);
	}

	public void addContents(String encoding) throws IOException {
		String path = req.getParameter(TocData.COMPLETE_PATH_PARAM);
		ITopic[] subtopics = getSubtopics();
		if (subtopics.length == 0) {
			return;
		}
		StringBuilder links = new StringBuilder("\n<ul class=\"childlinks\">\n"); //$NON-NLS-1$
		for (int i=0;i<subtopics.length;++i) {
			if (ScopeUtils.showInTree(subtopics[i], scope)) {
				links.append("\n<li><a href=\""); //$NON-NLS-1$
				String href = subtopics[i].getHref();
				if (href == null) {
					if (path != null && path.length() > 0) {
						href = "/../nav/" + path + '_' + i; //$NON-NLS-1$
					} else {
						href = "nav.html"; //$NON-NLS-1$
					}
				}
				else {
					href = XMLGenerator.xmlEscape(href);
					if (path != null && path.length() > 0) {
						href = TocFragmentServlet.fixupHref(href, path + '_' + i);
					}
				}
				links.append(getBackpath(req.getPathInfo()));
				links.append(href);
				links.append("\">" + subtopics[i].getLabel() + "</a></li>\n");  //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		links.append("\n</ul>\n"); //$NON-NLS-1$
		String linkString = links.toString();
		try {
			if (encoding != null) {
				out.write(linkString.getBytes(encoding));
			} else {
				out.write(linkString.getBytes(StandardCharsets.UTF_8));
			}
		} catch (UnsupportedEncodingException e) {
			out.write(linkString.getBytes());
		}
	}

	private ITopic[] getSubtopics() {
		String locale = UrlUtil.getLocale(req, null);
		String pathInfo = req.getPathInfo();
		String servletPath = req.getServletPath();
		if ("/nav".equals(servletPath)) return new ITopic[0]; //$NON-NLS-1$
		Toc[] tocs =  HelpPlugin.getTocManager().getTocs(locale);
		for (Toc toc : tocs) {
			if (pathInfo.equals(toc.getTopic())) {
				return toc.getTopics();
			}
			ITopic topic = toc.getTopic(pathInfo);
			if (topic != null) {
				return topic.getSubtopics();
			}
		}
		return   new ITopic[0];
	}

	private String getBackpath(String path) {
		int num = IPath.fromOSString(path).segmentCount() - 1;
		StringBuilder buf = new StringBuilder();
		for (int i=0; i < num; ++i) {
			if (i > 0) {
				buf.append('/');
			}
			buf.append(".."); //$NON-NLS-1$
		}
		return buf.toString();
	}

	public void addStyle() throws IOException {
		ITopic[] subtopics = getSubtopics();
		for (ITopic subtopic : subtopics) {
			if (ScopeUtils.showInTree(subtopic, scope)) {
				out.write(HAS_CHILDREN.getBytes(StandardCharsets.UTF_8));
				return;
			}
		}

		out.write(NO_CHILDREN.getBytes(StandardCharsets.UTF_8));
	}

}
