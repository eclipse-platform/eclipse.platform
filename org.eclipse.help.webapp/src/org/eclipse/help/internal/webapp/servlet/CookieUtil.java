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

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.webapp.HelpWebappPlugin;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;

/**
 * Utilities for working with cookies
 *
 * @since 3.0
 */
public class CookieUtil {
	private static final int COOKIE_LIFE = 5 * 365 * 24 * 60 * 60;
	private static final int MAX_COOKIE_PAYLOAD = 4096
			- "wset_contents01=".length() - "81920<".length() - 1; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * @return null or String
	 */
	public static String getCookieValue(String name, HttpServletRequest request) {
		String ret = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					ret = cookie.getValue();
					break;
				}
			}
		}
		if (HelpWebappPlugin.DEBUG_WORKINGSETS) {
			System.out.println("CookieUtil.getCookieValue(" //$NON-NLS-1$
					+ name + ", " //$NON-NLS-1$
					+ request.getRequestURI() + ") returning " //$NON-NLS-1$
					+ ret);
		}
		return ret;
	}

	public static void setCookieValue(String name, String value,
			HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(COOKIE_LIFE);

		if ( needsCookiePath(request)) {
			cookie.setPath(getCookiePath(request)); // Only set path if necessary
		}
		response.addCookie(cookie);
		if (HelpWebappPlugin.DEBUG_WORKINGSETS) {
			System.out
					.println("CookieUtil.setCookieValue(" + name + ", " + value + ",...)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public static void setCookieValueWithoutPath(String name, String value,
			HttpServletRequest request, HttpServletResponse response ) {
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(COOKIE_LIFE);

		response.addCookie(cookie);
		if (HelpWebappPlugin.DEBUG_WORKINGSETS) {
			System.out
					.println("CookieUtil.setCookieValueWithoutPath(" + name + ", " + value + ",...)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private static boolean needsCookiePath(HttpServletRequest request) {
		// All cookies should be at the same path level as the context ( /help )
		// The cookie path needs to be set if there are more path segments
		// between the context path and file name
		String requestURI = request.getRequestURI();
		String contextPath = request.getContextPath();
		return requestURI.indexOf('/', contextPath.length() + 1) > 0;
	}

	private static String getCookiePath(HttpServletRequest request) {
		return request.getContextPath() + '/';
	}

	public static void deleteCookie(String name, HttpServletRequest request, HttpServletResponse response) {
		deleteCookieUsingPath(name, request, response, getCookiePath(request));
	}

	protected static void deleteCookieUsingPath(String name, HttpServletRequest request,
			HttpServletResponse response, String cookiePath) {
		Cookie cookie = new Cookie(name, ""); //$NON-NLS-1$
		String requestURI = request.getRequestURI();
		if (!(requestURI.startsWith(cookiePath) && requestURI.indexOf('/', cookiePath.length() + 1) == -1)) {
			cookie.setPath(cookiePath);
		}
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	public static void deleteObsoleteCookies(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		HashSet<String> cookiesToKeep= new HashSet<>();
		HashSet<String> cookiesToDelete = new HashSet<>();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String name = cookie.getName();
				if (isObsoleteCookie(name) || cookiesToKeep.contains(name)) {
					cookiesToDelete.add(name);
				}
				cookiesToKeep.add(name);
			}

			for (String name : cookiesToDelete) {
				deleteCookieUsingPath(name, request, response, request.getContextPath() + "/advanced/"); //$NON-NLS-1$
				deleteCookieUsingPath(name, request, response, "/"); //$NON-NLS-1$
			}
		}
	}

	/*
	 * Is this one of the cookies that was used before Eclipse 3.6 of the
	 * form wset_nn
	 */
	private static boolean isObsoleteCookie(String name) {
		final String WSET_PREFIX = "wset"; //$NON-NLS-1$
		if (!name.startsWith(WSET_PREFIX)) {
			return false;
		}
		for (int i = WSET_PREFIX.length(); i < name.length(); i++) {
			char suffixChar = name.charAt(i);
			if (suffixChar < '0' || suffixChar > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Saves string in multiple browser cookies. Cookies can store limited
	 * length string. This method will attempt to split string among multiple
	 * cookies. The following cookies will be set name1=length &lt;substing1
	 * name2=substring2 ... namen=substringn
	 *
	 * @param data
	 *            a string containing legal characters for cookie value
	 * @throws IOException
	 *             when data is too long.
	 */
	public static void saveString(String name, String data, int maxCookies,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		int len = data.length();
		int n = len / MAX_COOKIE_PAYLOAD;
		if (n > maxCookies) {
			throw new IOException(WebappResources.getString(
					"CookieUtil.tooManyCookiesNeeded", UrlUtil.getLocaleObj( //$NON-NLS-1$
							request, response)));
		}
		for (int i = 1; i <= n; i++) {
			if (i == 1) {
				setCookieValue(name + "1", //$NON-NLS-1$
						len + "<" + data.substring(0, MAX_COOKIE_PAYLOAD), //$NON-NLS-1$
						request, response);
			} else {
				setCookieValue(name + i, data.substring(MAX_COOKIE_PAYLOAD
						* (i - 1), MAX_COOKIE_PAYLOAD * i), request, response);
			}
		}
		if (len % MAX_COOKIE_PAYLOAD > 0) {
			if (n == 0) {
				setCookieValue(name + "1", //$NON-NLS-1$
						len + "<" + data.substring(0, len), //$NON-NLS-1$
						request, response);
			} else {
				setCookieValue(name + (n + 1), data.substring(
						MAX_COOKIE_PAYLOAD * n, len), request, response);
			}
		}

		// if using less cookies than maximum, delete not needed cookies from
		// last time
		for (int i = n + 1; i <= maxCookies; i++) {
			if (i == n + 1 && len % MAX_COOKIE_PAYLOAD > 0) {
				continue;
			}
			if (getCookieValue(name + i, request) != null) {
				deleteCookie(name + i, request, response);
			} else {
				break;
			}
		}
	}
	/**
	 * @return null or String
	 */
	public static String restoreString(String name, HttpServletRequest request) {
		String value1 = CookieUtil.getCookieValue(name + "1", request); //$NON-NLS-1$
		if (value1 == null) {
			// no cookie
			return null;
		}
		String[] lengthAndSubstring1 = value1.split("<"); //$NON-NLS-1$
		if (lengthAndSubstring1.length < 2) {
			return null;
		}
		int len = 0;
		try {
			len = Integer.parseInt(lengthAndSubstring1[0]);
		} catch (NumberFormatException nfe) {
			return null;
		}
		if (len <= 0) {
			return null;
		}
		StringBuilder data = new StringBuilder(len);
		data.append(lengthAndSubstring1[1]);
		int n = len / MAX_COOKIE_PAYLOAD;
		for (int i = 2; i <= n; i++) {
			String substring = CookieUtil.getCookieValue(name + i, request);
			if (substring == null) {
				return null;
			}
			data.append(substring);
		}
		if (len % MAX_COOKIE_PAYLOAD > 0 && n > 0) {
			String substring = CookieUtil.getCookieValue(name + (n + 1),
					request);
			if (substring == null) {
				return null;
			}
			data.append(substring);
		}

		if (data.length() != len) {
			return null;
		}

		return data.toString();
	}
}
