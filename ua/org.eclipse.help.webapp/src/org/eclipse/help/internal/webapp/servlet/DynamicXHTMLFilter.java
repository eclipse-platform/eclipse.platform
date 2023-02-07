/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.xhtml.DynamicXHTMLProcessor;
import org.eclipse.help.webapp.IFilter;

/*
 * A filter that performs any required XHTML processing, if the content is
 * XHTML. Using a servlet path of /rtopic (raw topic) will turn off this
 * filter.
 */
public class DynamicXHTMLFilter implements IFilter {

	private static final String ERROR_PAGE_PREFIX = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\">\n<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n</head>\n<body>"; //$NON-NLS-1$
	private static final String ERROR_PAGE_SUFFIX = "</body>\n</html>"; //$NON-NLS-1$

	@Override
	public OutputStream filter(final HttpServletRequest req, final OutputStream out) {
		final String uri = req.getRequestURI();
		if (uri == null || !uri.endsWith("html") && !uri.endsWith("htm")) { //$NON-NLS-1$ //$NON-NLS-2$
			return out;
		}

		/*
		 * Remote help does all XHTML processing on the client side, so we
		 * want raw XHTML topics from the remote help server.
		 */
		if ("/rtopic".equals(req.getServletPath())) { //$NON-NLS-1$
			return out;
		}

		/*
		 * Buffers the contents of the document until stream is closed, where
		 * the document is processed then pushed through the filter.
		 */
		ByteArrayOutputStream out2 = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				super.close();
				byte[] buf = toByteArray();
				ByteArrayInputStream in = new ByteArrayInputStream(buf);

				String locale = UrlUtil.getLocale(req, null);
				String contextPath = req.getContextPath();
				String servletPath = req.getServletPath();
				String href = uri;
				if (href.startsWith(contextPath)) {
					href = href.substring(contextPath.length());
				}
				if (href.startsWith(servletPath)) {
					href = href.substring(servletPath.length());
				}

				try {
					boolean filter = ProductPreferences.useEnablementFilters();
					try (InputStream in2 = DynamicXHTMLProcessor.process(href, in, locale, filter)) {
						transferContent(in2, out);
					}
					out.close();
				}
				catch (Throwable t) {
					try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
						writer.println(ERROR_PAGE_PREFIX);
						writer.println("<p>"); //$NON-NLS-1$
						writer.println(WebappResources.getString("ProcessingError", req.getLocale())); //$NON-NLS-1$
						writer.println("</p>"); //$NON-NLS-1$
						writer.println("<pre>"); //$NON-NLS-1$

						StringWriter w1 = new StringWriter();
						PrintWriter w2 = new PrintWriter(w1);
						t.printStackTrace(w2);

						writer.println(UrlUtil.htmlEncode(w1.getBuffer().toString()));
						writer.println("</pre>"); //$NON-NLS-1$
						writer.println(ERROR_PAGE_SUFFIX);
					}
				}
			}
		};
		return out2;
	}

	/*
	 * Forward the contents of the input stream to the output stream.
	 */
	private void transferContent(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[4096];
		int len = 0;
		while (true) {
			len = in.read(buffer);
			if (len == -1)
				break;
			out.write(buffer, 0, len);
		}
	}
}
