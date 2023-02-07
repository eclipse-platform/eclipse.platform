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

package org.eclipse.ua.tests.help.util;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoadTestServlet extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = -1426745453574711075L;
	private static final String XHTML_1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n<title>"; //$NON-NLS-1$
	private static final String XHTML_2 = "</title>\n <style type = \"text/css\"> td { padding-right : 10px; }</style></head>\n<body>\n"; //$NON-NLS-1$
	private static final String XHTML_3 = "</body>\n</html>"; //$NON-NLS-1$

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("text/html; charset=UTF-8"); //$NON-NLS-1$
		String value = req.getParameter("value");
		String repeatParam = req.getParameter("repeat");
		int repetitions = 200;

		try {
			repetitions = Integer.parseInt(repeatParam);
		} catch (NumberFormatException e) {
		}

		StringBuilder buf = new StringBuilder();
		buf.append("<!--");
		buf.append(value);
		buf.append("-->");
		buf.append(XHTML_1);
		buf.append(value);
		buf.append(XHTML_2);
		for (int i = 0; i < repetitions; i++) {
			buf.append("\n<p>");
			buf.append("Paragraph" + i);
			buf.append("</p>");
		}
		buf.append('$');
		buf.append(XHTML_3);
		String response = buf.toString();
		resp.getWriter().write(response);
	}


}
