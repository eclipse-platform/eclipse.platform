/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.webapp.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.webapp.HelpWebappPlugin;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.internal.webapp.utils.Utils;

/*
 * Class is responsible for implementing security protection.  All servlets
 * who use the org.eclipse.help.webapp.validatedServlet extension point will
 * will be processed for security failures by this class.
 *
 * Any URL that starts with <path>/vs<etc> will be redirected here for further
 * processing.  If the validatedServlet extension point has an alias that
 * matches the URL passed here, it will finish the processing and return
 * results here for validation.  If there are no malicious threats detected,
 * this class will return the output to the client.
 *
 */
public class ValidatorServlet extends HttpServlet {

	private static final long serialVersionUID = -3783758607845176051L;
	private Hashtable<String, HttpServlet> servletTable = new Hashtable<>();

	protected void process(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String baseURL = req.getRequestURL().toString();
		baseURL = baseURL.substring(0, baseURL.indexOf(req.getServletPath()));

		Locale locale = UrlUtil.getLocaleObj(req,resp);

		String service = req.getRequestURL().toString().substring(
				(baseURL).length()+("/vs".length())); //$NON-NLS-1$

		try {
			HttpServletResponseAdv response = new HttpServletResponseAdv(resp);

			HttpServlet servlet = getServlet(service);
			ServletConfig config = getServletConfig();
			servlet.init(config);
			servlet.service(req, response);

			if (isSecure(req, response))
				response.commitOutput();

		} catch(Exception ex) {

			String errorMsg = WebappResources.getString("cantCreateServlet", //$NON-NLS-1$
					locale, service);
			ILog.of(getClass()).error(errorMsg, ex);

			@SuppressWarnings("resource")
			PrintWriter writer = resp.getWriter();
			writer.println(errorMsg);
			ex.printStackTrace(writer);

			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private HttpServlet getServlet(String name)
			throws CoreException {

		HttpServlet servlet = servletTable.get(name);

		if (servlet == null) {

			IConfigurationElement[] configs =
				Platform.getExtensionRegistry().getConfigurationElementsFor(HelpWebappPlugin.PLUGIN_ID+".validatedServlet"); //$NON-NLS-1$

			for (IConfigurationElement config2 : configs) {

				String alias = config2.getAttribute("alias"); //$NON-NLS-1$
				if (alias != null) {

					if (isMatch(alias, name)) {
						servlet = (HttpServlet)config2.createExecutableExtension("class"); //$NON-NLS-1$
						servletTable.put(name, servlet);
						break;
					}
				}
			}
		}

		return servlet;
	}

	private boolean isMatch(String alias, String name) {

		int index = name.indexOf(alias);
		if (index == 0) {
			int offset = alias.length();
			if (name.length() == offset)
				return true;
			char ch = name.charAt(offset);
			if (ch == '/' || ch == '?')
				return true;
		}
		return false;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		process(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		process(req, resp);
	}

	public boolean isSecure(HttpServletRequest req,HttpServletResponseAdv resp)
			throws SecurityException {
		Enumeration<String> names = req.getParameterNames();
		List<String> values = new ArrayList<>();
		List<String> scripts = new ArrayList<>();

		while (names.hasMoreElements()) {

			String name = names.nextElement();
			String val = req.getParameter(name);
			values.add(val);
			if (replaceAll(val, '+', "").indexOf("<script")>-1) //$NON-NLS-1$ //$NON-NLS-2$
				scripts.add(val);
		}

		if (resp.getWriter() != null) {
			String data = resp.getString();
			for (int s=0; s < scripts.size(); s++)
				if (data.indexOf(scripts.get(s)) > -1)
					throw new SecurityException("Potential cross-site scripting detected."); //$NON-NLS-1$
		}

		return true;
	}

	public String replaceAll(String str, char remove, String add) {

		StringBuilder buffer = new StringBuilder();
		for (int s=0; s < str.length(); s++) {

			char ch = str.charAt(s);
			if (ch == remove)
				buffer.append(add);
			else
				buffer.append(ch);
		}
		return buffer.toString();
	}

	private class HttpServletResponseAdv extends HttpServletResponseWrapper {

		private HttpServletResponse response;
		private ServletPrintWriter writer;
		private ServletOutputStream stream;

		public HttpServletResponseAdv(HttpServletResponse response) {
			super(response);
			this.response = response;
		}

		@Override
		public PrintWriter getWriter() {

			if (writer == null && stream == null)
				writer = new ServletPrintWriter();
			return writer;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {

			if (stream == null && writer == null)
				stream = response.getOutputStream();
			return stream;
		}

		@SuppressWarnings("resource")
		public void commitOutput() throws IOException {

			OutputStream os = response.getOutputStream();
			InputStream is = getInputStream();
			if (is != null) {
				Utils.transferContent(is, os);
			}
			os.flush();
		}

		public InputStream getInputStream() {
			if (writer != null) {

				try {
					return new ByteArrayInputStream(writer.toString().getBytes(getCharacterEncoding()));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		public String getString() {

			if (writer != null)
				return writer.toString();

			return null;
		}
	}
}
