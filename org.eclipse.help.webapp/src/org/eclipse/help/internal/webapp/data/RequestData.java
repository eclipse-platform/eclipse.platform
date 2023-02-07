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
package org.eclipse.help.internal.webapp.data;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.base.BaseHelpSystem;

/**
 * Helper class for contents.jsp initialization
 */
public class RequestData {
	public static final int MODE_WORKBENCH = BaseHelpSystem.MODE_WORKBENCH;
	public static final int MODE_INFOCENTER = BaseHelpSystem.MODE_INFOCENTER;
	public static final int MODE_STANDALONE = BaseHelpSystem.MODE_STANDALONE;

	protected ServletContext context;
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected String locale;
	protected WebappPreferences preferences;
	/**
	 * Constructs the data for a request.
	 *
	 * @param context
	 * @param request
	 */
	public RequestData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		this.context = context;
		this.request = request;
		this.response = response;
		preferences = new WebappPreferences();

		locale = UrlUtil.getLocale(request, response);
	}

	/**
	 * Returns the preferences object
	 */
	public WebappPreferences getPrefs() {
		return preferences;
	}

	public boolean isBot() {
		return UrlUtil.isBot(request);
	}

	public boolean isGecko() {
		return UrlUtil.isGecko(request);
	}

	public boolean isIE() {
		return UrlUtil.isIE(request);
	}

	public String getIEVersion() {
		return UrlUtil.getIEVersion(request);
	}

	public boolean isKonqueror() {
		return UrlUtil.isKonqueror(request);
	}

	public boolean isMozilla() {
		return UrlUtil.isMozilla(request);
	}

	public boolean isMacMozilla() {
		return UrlUtil.isMozilla(request) &&
		(request.getHeader("User-Agent").indexOf("Macintosh") > 0 ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getMozillaVersion() {
		return UrlUtil.getMozillaVersion(request);
	}

	public boolean isSafari() {
		return UrlUtil.isSafari(request);
	}

	public String getSafariVersion() {
		return UrlUtil.getSafariVersion(request);
	}

	public boolean isOpera() {
		return UrlUtil.isOpera(request);
	}

	public String getLocale() {
		return locale;
	}

	public int getMode() {
		return BaseHelpSystem.getMode();
	}
}
