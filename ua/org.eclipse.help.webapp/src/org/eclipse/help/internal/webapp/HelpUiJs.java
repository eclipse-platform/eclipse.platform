/*******************************************************************************
 * Copyright (c) 2021 Holger Voormann and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.help.internal.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;

public class HelpUiJs extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String JS_TEMPLATE_PROPERTY = "org.eclipse.help.webapp.experimental.ui.js"; //$NON-NLS-1$
    private static final String JS_TEMPLATE_DEFAULT = "org.eclipse.help.webapp/m/index.js"; //$NON-NLS-1$
    private static final String JS_TEMPLATE = loadJsTemplate();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
        response.setContentType("text/html; charset=UTF-8"); //$NON-NLS-1$
        try (PrintWriter writer = response.getWriter()) {
			writer.write(HelpUi.resolve(JS_TEMPLATE, request));
		}
    }

    private static String loadJsTemplate() {
        String customJsTemplate = System.getProperty(JS_TEMPLATE_PROPERTY);
        String jsTemplateLocation = customJsTemplate == null ? JS_TEMPLATE_DEFAULT : customJsTemplate;
        try {
            return loadTemplate(jsTemplateLocation);
        } catch (Exception e) {
            String msg = "Failed to load template file for 'index.js': " + jsTemplateLocation; //$NON-NLS-1$
            Platform.getLog(HelpUiJs.class).error(msg, e);
            try {
                return loadTemplate(JS_TEMPLATE_DEFAULT);
            } catch (Exception e2) {
                String msg2 = "Failed to load default template file for 'index.js': " + JS_TEMPLATE_DEFAULT; //$NON-NLS-1$
                Platform.getLog(HelpUiJs.class).error(msg2, e2);
                return ""; //$NON-NLS-1$
            }
        }
    }

    private static String loadTemplate(String bundleLocation) throws IOException {
        String[] bundleAndPath = bundleLocation.split("/", 2); //$NON-NLS-1$
        URL resourceAsUrl = Platform.getBundle(bundleAndPath[0]).getResource(bundleAndPath[1]);

        // read it as InputStream and convert it to a String
        // (by using a Scanner with a delimiter that cannot be found: \A - start of input)
        try (Scanner scanAll = new Scanner(resourceAsUrl.openStream()).useDelimiter("\\A")) { //$NON-NLS-1$
        	return scanAll.hasNext() ? scanAll.next() : ""; //$NON-NLS-1$
        }
    }

}
