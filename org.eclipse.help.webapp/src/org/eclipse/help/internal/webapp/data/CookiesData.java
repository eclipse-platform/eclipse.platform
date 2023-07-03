/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.data;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.webapp.servlet.CookieUtil;

/**
 * Helper class for reading cookie values
 */
public class CookiesData extends RequestData {

	public CookiesData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);

	}

	public boolean isSynchToc() {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
				for (Cookie cookie : cookies) {
					if ("synchToc".equals(cookie.getName())) { //$NON-NLS-1$
						return String.valueOf(true).equals(cookie.getValue());
					}
				}
		}
		boolean isSynchToc = Platform.getPreferencesService().getBoolean
			(HelpBasePlugin.PLUGIN_ID, "advanced.syncDefault", false, null); //$NON-NLS-1$
		CookieUtil.setCookieValue("synchToc", Boolean.toString(isSynchToc), request, response); //$NON-NLS-1$
		return isSynchToc;
	}

}
