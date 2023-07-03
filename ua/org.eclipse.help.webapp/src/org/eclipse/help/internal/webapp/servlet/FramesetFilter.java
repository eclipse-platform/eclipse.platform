/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.webapp.IFilter;

/**
 * This class inserts a script for showing the page inside the appropriate
 * frameset when bookmarked.
 */
public class FramesetFilter implements IFilter {
	private static final String scriptPart1 = "<script type=\"text/javascript\">\nif( self == top ){" //$NON-NLS-1$
			+ "\n  var  anchorParam = location.hash.length > 0 ? '"  //$NON-NLS-1$
			+ UrlUtil.JavaScriptEncode("&") + "anchor=' + location.hash.substr(1) : '';" //$NON-NLS-1$ //$NON-NLS-2$
			+ "\n  window.location.replace( \""; //$NON-NLS-1$
	private static final String scriptPart3 = "\" + anchorParam);\n}\n</script>"; //$NON-NLS-1$

	@Override
	public OutputStream filter(HttpServletRequest req, OutputStream out) {
		String uri = req.getRequestURI();
		String url = req.getPathInfo();
		String query = req.getQueryString();
		if (uri == null) {
			return out;
		}
		boolean isNavPath = UrlUtil.isNavPath(uri);
		if (!uri.endsWith("html") && !uri.endsWith("htm") && !isNavPath) { //$NON-NLS-1$ //$NON-NLS-2$
			return out;
		}

		if ("/nftopic".equals(req.getServletPath()) ||  //$NON-NLS-1$
			"/ntopic".equals(req.getServletPath()) ||  //$NON-NLS-1$
			"/rtopic".equals(req.getServletPath()) ||  //$NON-NLS-1$
			UrlUtil.isBot(req)) {
			return out;
		}

		String noframes = req.getParameter("noframes"); //$NON-NLS-1$
		if ("true".equals(noframes)) { //$NON-NLS-1$
			return out;
		}

		String path = req.getPathInfo();
		if (path == null) {
			return out;
		}
		StringBuilder script = new StringBuilder(scriptPart1);
		for (int i; 0 <= (i = path.indexOf('/')); path = path.substring(i + 1)) {
			script.append("../"); //$NON-NLS-1$
		}
		if (isNavPath) {
			script.append("index.jsp?nav="); //$NON-NLS-1$
		} else {
			script.append("index.jsp?topic="); //$NON-NLS-1$
		}

		// Bug 317055 -  [webapp] URLEncode url requests from local users
		url = URLEncoder.encode(url, StandardCharsets.UTF_8);
		if ( query != null ) {
			query = URLEncoder.encode(query, StandardCharsets.UTF_8);
			url = url + UrlUtil.JavaScriptEncode("&")  + query;  //$NON-NLS-1$
		}
		script.append(url);

		script.append(scriptPart3);
		return new FilterHTMLHeadOutputStream(out, script.toString().getBytes(StandardCharsets.US_ASCII));
	}
}
