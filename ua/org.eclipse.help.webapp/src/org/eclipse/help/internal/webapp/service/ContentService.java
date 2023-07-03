/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.webapp.utils.Utils;

/**
 * Returns an HTML page for the selected topic passed as request path info.
 *
 * <p>Passes the request to {@link org.eclipse.help.internal.webapp.servlet.ContentServlet}
 * servlet.
 *
 * @param lang	- (optional) specifies the locale
 *
 * @return		An html page for the selected topic
 *
 * @version	$Version$
 *
 **/
public class ContentService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

		String baseURL = req.getRequestURL().toString();
		String contentURL = baseURL.replaceFirst(Utils.SERVICE_CONTEXT, ""); //$NON-NLS-1$
		String query = req.getQueryString();
		if (query != null)
			contentURL += '?' + query;

		URL url = new URL(contentURL);
		URLConnection con = ProxyUtil.getConnection(url);
		con.setAllowUserInteraction(false);
		con.setDoInput(true);
		con.connect();

		String contentType;
		ServletContext context = getServletContext();
		String pathInfo = req.getPathInfo();
		String mimeType = context.getMimeType(pathInfo);
		if (mimeType != null && !mimeType.equals("application/xhtml+xml")) { //$NON-NLS-1$
			contentType = mimeType;
		} else {
			contentType = con.getContentType();
		}
		resp.setContentType(contentType);

		@SuppressWarnings("resource")
		InputStream is = con.getInputStream();
		OutputStream out = resp.getOutputStream();
		if (!contentType.equals("application/xhtml+xml")  //$NON-NLS-1$
				&& !contentType.equals("text/html")  //$NON-NLS-1$
				&& !con.getContentType().equals("text/html")) { //$NON-NLS-1$
			Utils.transferContent(is, out);
			out.flush();
		} else {
			String response = Utils.convertStreamToString(ProxyUtil.getStream(url));
			response = Utils.updateResponse(response);
			try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
				writer.write(response);
			}
		}
	}

}
