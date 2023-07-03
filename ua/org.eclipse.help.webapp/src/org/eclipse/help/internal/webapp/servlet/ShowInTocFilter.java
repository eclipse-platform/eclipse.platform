/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.webapp.IFilter;

/**
 * This class inserts a script for showing the page inside the table of contents
 */
public class ShowInTocFilter implements IFilter {
	private static final String scriptPart1 = "\n<script type=\"text/javascript\" src=\""; //$NON-NLS-1$
	private static final String scriptPart3 = "advanced/synchWithToc.js\"></script>"; //$NON-NLS-1$

	@Override
	public OutputStream filter(HttpServletRequest req, OutputStream out) {
		String uri = req.getRequestURI();
		if (uri == null) return out;
		if (!uri.endsWith("html") && !uri.endsWith("htm") && !UrlUtil.isNavPath(uri)) { //$NON-NLS-1$ //$NON-NLS-2$
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
		String relativePath = FilterUtils.getRelativePathPrefix(req);
		script.append(relativePath);

		script.append(scriptPart3);
		return new FilterHTMLHeadOutputStream(out, script.toString().getBytes(StandardCharsets.US_ASCII));
	}


}
