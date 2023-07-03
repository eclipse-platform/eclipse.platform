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
 *     Sebastian Davids <sdavids@gmx.de> - fix for Bug 182466
 *     Dave Carver - modification to isAdvanced(), see Bug 238533
 *******************************************************************************/
package org.eclipse.help.internal.webapp.data;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.util.ProductPreferences;

public class UrlUtil {

	// for Safari build 125.1 finds version 125
	static final Pattern safariPattern = Pattern.compile(
			"Safari/(\\d+)(?:\\.|\\s|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	// Default locale to use for serving requests to help
	private static String defaultLocale;
	// Locales that infocenter can serve in addition to the default locale.
	// null indicates that infocenter can serve every possible client locale.
	private static Collection<String> locales;

	private static final int INFOCENTER_DIRECTION_BY_LOCALE = 1;
	private static final int INFOCENTER_DIRECTION_LTR = 2;
	private static final int INFOCENTER_DIRECTION_RTL = 3;
	private static int infocenterDirection = INFOCENTER_DIRECTION_BY_LOCALE;

	/**
	 * Encodes string for embedding in JavaScript source
	 */
	public static String JavaScriptEncode(String str) {
		if (str == null) return null;
		char[] wordChars = new char[str.length()];
		str.getChars(0, str.length(), wordChars, 0);
		StringBuilder jsEncoded = new StringBuilder();
		for (char unicode : wordChars) {
			// to enhance readability, do not encode A-Z,a-z
			if (('A' <= unicode && unicode <= 'Z')
					|| ('a' <= unicode && unicode <= 'z')) {
				jsEncoded.append(unicode);
				continue;
			}
			// encode the character
			String charInHex = Integer.toString(unicode, 16).toUpperCase();
			switch (charInHex.length()) {
				case 1 :
					jsEncoded.append("\\u000").append(charInHex); //$NON-NLS-1$
					break;
				case 2 :
					jsEncoded.append("\\u00").append(charInHex); //$NON-NLS-1$
					break;
				case 3 :
					jsEncoded.append("\\u0").append(charInHex); //$NON-NLS-1$
					break;
				default :
					jsEncoded.append("\\u").append(charInHex); //$NON-NLS-1$
					break;
			}
		}
		return jsEncoded.toString();
	}

	/**
	 * Encodes string for embedding in html source.
	 */
	public static String htmlEncode(String str) {
		if (str == null) {
			return null;
		}

		StringBuilder result = new StringBuilder();
		for (int i = 0 ; i < str.length(); i++) {
			appendEncodedChar(result, str.charAt(i));
		}
		return result.toString();
	}

	private static void appendEncodedChar(StringBuilder result, char ch) {
		if (needsEncoding(ch)) {
			int chInt = ch;
			result.append("&#" + chInt + ';'); //$NON-NLS-1$
			return;
		}
		result.append(ch);
	}

	private static boolean needsEncoding(char ch) {
		if (ch > 255) {
			return false;
		}
		if (Character.isLetterOrDigit(ch)) {
			return false;
		}
		if ( ch == ' ' || ch == '_' || ch  == '%' || ch == '+') {
			return false;
		}
		return true;
	}

	public static boolean isLocalRequest(HttpServletRequest request) {
		String reqIP = request.getRemoteAddr();
		if ("127.0.0.1".equals(reqIP)) { //$NON-NLS-1$
			return true;
		}

		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			InetAddress[] addr = InetAddress.getAllByName(hostname);
			for (InetAddress element : addr) {
				// test all addresses retrieved from the local machine
				if (element.getHostAddress().equals(reqIP))
					return true;
			}
		} catch (IOException ioe) {
		}
		return false;
	}

	/**
	 * Returns a URL that can be loaded from a browser. This method is used for
	 * all url's except those from the webapp plugin.
	 *
	 * @param url
	 * @return String
	 */
	public static String getHelpURL(String url) {
		return getHelpURL(url, 1);
	}


	/**
	 * Returns a URL that can be used as
	 *
	 * @param url either a complete url including protocol or a help page
	 * path of form /plugin/path
	 * @param depth the number of directory segments beneath /help of the containing pages url
	 * @return a path that can be used as the href value of an anchor
	 */
	public static String getHelpURL(String url, int depth) {
		if (url == null || url.length() == 0)
			url = "about:blank"; //$NON-NLS-1$
		else if (url.startsWith("http:/") || url.startsWith("https:/")); //$NON-NLS-1$ //$NON-NLS-2$
		else if (url.startsWith("file:/") || url.startsWith("jar:file:/")) //$NON-NLS-1$ //$NON-NLS-2$
			url = getHelpUrlPrefix(depth) +'/' + url;
		else
			url =  getHelpUrlPrefix(depth) + url;
		return url;
	}

	private static String getHelpUrlPrefix(int depth) {
		String prefix;
		prefix = ""; //$NON-NLS-1$
		for (int d = 0; d < depth; d++) {
			prefix += "../"; //$NON-NLS-1$
		}
		prefix += "topic"; //$NON-NLS-1$
		return prefix;
	}

	/**
	 * Tests to see if the path specified in a topic parameter can be opened in
	 * the content frame. See Bug 233466 for an explanation of why in general we do
	 * do not want to open external sites in the content frame.
	 *
	 * If the preference org.eclipse.help.base/restrictTopicParameter is false any
	 * path is permitted. Any href which was just opened from the help display is
	 * also permitted, this is required for cheat sheets and intro. Otherwise the
	 * path is permitted only if it does not contain a protocol.
	 * @param path the path passed as a ?topic parameter. May not be null.
	 * @return true if the conditions above are met.
	 */
	public static boolean isValidTopicParamOrWasOpenedFromHelpDisplay(String path) {
		// Topics opened via the help display ( including cheat sheets and intro )
		// are are always valid
		if (wasOpenedFromHelpDisplay(path)) {
			return true;
		}

		if (new WebappPreferences().isRestrictTopicParameter()) {
			if (path.contains(":/")) {  //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}

	public static boolean wasOpenedFromHelpDisplay(String path) {
		if (path.equals(BaseHelpSystem.getHelpDisplay().getHrefOpenedFromHelpDisplay())) {
			return true;
		}
		return path.equals(getHelpURL(BaseHelpSystem.getHelpDisplay().getHrefOpenedFromHelpDisplay()));
	}

	/**
	 * Returns a path to the given topic in the form of child indexes. For
	 * example, if the path points to the 3rd subtopic under the 2nd topic of
	 * the 4th toc, it will return { 3, 1, 2 }.
	 *
	 * @param path the path portion of the url without the initial "/help", e.g. "/topic/my.plugin/foo.html"
	 * @param locale
	 * @return path to the topic using zero-based indexes
	 * If the path is empty or has invalid syntax null is returned
	 */
	public static int[] getTopicPath(String path, String locale) {
		if (path.startsWith("/nav/")) { //$NON-NLS-1$
			path = path.substring(5);
			return splitPath(path);
		}
		else {
			// grab the part after /help/*topic/
			String href = path.substring(path.indexOf('/', 1));
			return HelpPlugin.getTocManager().getTopicPath(href, locale);
		}
	}

	/*
	 * Similar to getTopicPath except that the path has no prefix
	 */
	public static int[] splitPath(String path) {
		try {
			StringTokenizer tok = new StringTokenizer(path, "_"); //$NON-NLS-1$
			int[] array = new int[tok.countTokens()];
			if (array.length == 0) {
				return null;
			}
			for (int i=0;i<array.length;++i) {
				array[i] = Integer.parseInt(tok.nextToken());
			}
			return array;
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static boolean isBot(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		if (agent==null)
			return false;
		agent=agent.toLowerCase(Locale.ENGLISH);
		// sample substring Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)
		return agent.contains("bot") || agent.contains("crawl")//$NON-NLS-1$ //$NON-NLS-2$
				|| request.getParameter("bot") != null;//$NON-NLS-1$
	}

	public static boolean isGecko(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return isGecko(agent);
	}

	public static boolean isGecko(String agent) {
		if (agent==null)
			return false;
		agent=agent.toLowerCase(Locale.ENGLISH);
		// sample substring Gecko/20020508
		if (agent.contains("like gecko")) { //$NON-NLS-1$
			return false;
		}
		return agent.contains("gecko"); //$NON-NLS-1$
	}

	public static boolean isIE(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return isIE(agent);
	}

	public static boolean isIE(String agent) {
		if (agent==null)
			return false;
		agent=agent.toLowerCase(Locale.ENGLISH);

		// When accessing with Bobby identified Bobby return 5.5 to allow
		// testing advanced UI as bobby cannot identifiy as IE >=5.5
		if (agent.startsWith("bobby/")) { //$NON-NLS-1$
			return true;
		}
		//

		return (agent.contains("msie")); //$NON-NLS-1$
	}

	public static String getIEVersion(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return getIEVersion(agent);
	}

	public static String getIEVersion(String agent) {
		if (agent==null)
			return "0"; //$NON-NLS-1$

		agent=agent.toLowerCase(Locale.ENGLISH);
		// When accessing with Bobby identified Bobby return 5.5 to allow
		// testing advanced UI as bobby cannot identifiy as IE >=5.5
		if (agent.startsWith("bobby/")) { //$NON-NLS-1$
			return "5.5"; //$NON-NLS-1$
		}
		//

		int start = agent.indexOf("msie ") + "msie ".length(); //$NON-NLS-1$ //$NON-NLS-2$
		if (start < "msie ".length() || start >= agent.length()) //$NON-NLS-1$
			return "0"; //$NON-NLS-1$
		int end = agent.indexOf(";", start); //$NON-NLS-1$
		if (end <= start)
			return "0"; //$NON-NLS-1$
		return agent.substring(start, end);
	}

	public static boolean isKonqueror(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return isKonqueror(agent);
	}

	public static boolean isKonqueror(String agent) {
		if (agent==null)
			return false;
		agent=agent.toLowerCase(Locale.ENGLISH);
		return agent.contains("konqueror"); //$NON-NLS-1$
	}

	/**
	 * Test to see if this is a "mozilla" browser, i.e.
	 * just about anything other than Internet Explorer
	 * @param request a request from the browser
	 * @return true if the browser is Netcape, Firefox, Safari or Konqueror
	 */
	public static boolean isMozilla(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return isMozilla(agent);
	}

	public static boolean isMozilla(String agent) {
		if (agent==null)
			return false;
		agent=agent.toLowerCase(Locale.ENGLISH);
		return agent.contains("mozilla/5"); //$NON-NLS-1$
	}

	public static String getMozillaVersion(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return getMozillaVersion(agent);
	}

	public static String getMozillaVersion(String agent) {
		if (agent==null)
			return "0"; //$NON-NLS-1$
		agent=agent.toLowerCase(Locale.ENGLISH);
		if (!agent.contains("mozilla/5")) //$NON-NLS-1$
			return "0"; //$NON-NLS-1$
		int start = agent.indexOf("rv:") + "rv:".length(); //$NON-NLS-1$ //$NON-NLS-2$
		if (start < "rv:".length() || start >= agent.length()) //$NON-NLS-1$
			return "0"; //$NON-NLS-1$
		int end = agent.indexOf(")", start); //$NON-NLS-1$
		if (end <= start)
			return "0"; //$NON-NLS-1$
		return agent.substring(start, end);
	}

	public static boolean isOpera(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return isOpera(agent);
	}

	public static boolean isOpera(String agent) {
		if (agent==null)
			return false;
		agent=agent.toLowerCase(Locale.ENGLISH);
		return agent.contains("opera"); //$NON-NLS-1$
	}

	public static String getOperaVersion(String agent) {
		if (agent==null)
			return "0"; //$NON-NLS-1$
		agent=agent.toLowerCase(Locale.ENGLISH);
		final String OperaPrefix = "opera/"; //$NON-NLS-1$
		int start = agent.indexOf(OperaPrefix) + OperaPrefix.length();
		if (start < OperaPrefix.length() || start >= agent.length())
			return "0"; //$NON-NLS-1$
		int end = agent.indexOf(" (", start); //$NON-NLS-1$
		if (end <= start)
			return "0"; //$NON-NLS-1$
		return agent.substring(start, end);
	}

	public static boolean isSafari(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return isSafari(agent);
	}

	public static boolean isSafari(String agent) {
		if (agent==null)
			return false;
		agent=agent.toLowerCase(Locale.ENGLISH);
		return agent.contains("safari/"); //$NON-NLS-1$
	}

	public static String getSafariVersion(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent"); //$NON-NLS-1$
		return getSafariVersion(agent);
	}

	public static String getSafariVersion(String agent) {
		String version = "0"; //$NON-NLS-1$
		if (agent==null)
			return version;
		agent=agent.toLowerCase(Locale.ENGLISH);
		Matcher m = safariPattern.matcher(agent);
		boolean matched = m.find();
		if (matched) {
			version = m.group(1);
			while (version.length() < 3) {
				version = "0" + version; //$NON-NLS-1$
			}
		}
		return version;
	}
	/**
	 *
	 * @param request
	 * @param response
	 *            HttpServletResponse or null (locale will not be persisted in
	 *            session cookie)
	 * @return
	 */
	public static Locale getLocaleObj(HttpServletRequest request,
			HttpServletResponse response) {
		String localeStr = getLocale(request, response);
		return getLocale(localeStr);
	}

	/**
	 * Returns the locale object from the provided string.
	 * @param localeStr the encoded locale string
	 * @return the Locale object
	 *
	 * @since 3.1
	 */
	public static Locale getLocale(String localeStr) {
		if (localeStr.length() >= 5) {
			return new Locale(localeStr.substring(0, 2), localeStr.substring(3,
					5));
		} else if (localeStr.length() >= 2) {
			return new Locale(localeStr.substring(0, 2), ""); //$NON-NLS-1$
		} else {
			return Locale.getDefault();
		}
	}
	/**
	 *
	 * @param request
	 * @param response
	 *            HttpServletResponse or null (locale will not be persisted in
	 *            session cookie)
	 * @return
	 */
	public static String getLocale(HttpServletRequest request,
			HttpServletResponse response) {
		if (defaultLocale == null) {
			initializeNL();
		}
		if ((BaseHelpSystem.getMode() != BaseHelpSystem.MODE_INFOCENTER)
				|| request == null) {
			return defaultLocale;
		}

		// use locale passed in a request in current user session
		String forcedLocale = getForcedLocale(request, response);
		if (forcedLocale != null) {
			if (locales == null) {
				// infocenter set up to serve any locale
				return forcedLocale;
			}
			// match forced locale with one of infocenter locales
			if (locales.contains(forcedLocale)) {
				return forcedLocale;
			}
			// match language of forced locale with one of infocenter locales
			if (forcedLocale.length() > 2) {
				String ll = forcedLocale.substring(0, 2);
				if (locales.contains(ll)) {
					return ll;
				}
			}
		}

		// use one of the browser locales
		if (locales == null) {
			// infocenter set up to serve any locale
			return request.getLocale().toString();
		}
		// match client browser locales with one of infocenter locales
		for (Enumeration<Locale> e = request.getLocales(); e.hasMoreElements();) {
			String locale = e.nextElement().toString();
			if (locale.length() >= 5) {
				String ll_CC = locale.substring(0, 5);
				if (locales.contains(ll_CC)) {
					// client locale available
					return ll_CC;
				}
			}
			if (locale.length() >= 2) {
				String ll = locale.substring(0, 2);
				if (locales.contains(ll)) {
					// client language available
					return ll;
				}
			}
		}
		// no match
		return defaultLocale;
	}

	/*
	 * Replace any characters other than alphanumeric or hyphen in a locale string with "_"
	 * See Bug 223361
	 */
	public static String cleanLocale(String parameter) {
		if (parameter == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < parameter.length(); i++) {
			char nextChar = parameter.charAt(i);
			if (Character.isLetterOrDigit(nextChar) || nextChar == '-') {
				result.append(nextChar);
			} else {
				result.append('_');
			}
		}
		return result.toString();
	}

	/**
	 * Obtains locale passed as lang parameter with a request during user
	 * session
	 *
	 * @param request
	 * @param response
	 *            response or null; if null, locale will not be persisted (in
	 *            session cookie)
	 * @return ll_CC or ll or null
	 */
	private static String getForcedLocale(HttpServletRequest request,
			HttpServletResponse response) {
		// get locale passed in this request
		String forcedLocale = cleanLocale(request.getParameter("lang")); //$NON-NLS-1$
		if (forcedLocale != null) {
			// save locale (in session cookie) for later use in a user session
			if (response != null) {
				Cookie cookieTest = new Cookie("lang", forcedLocale); //$NON-NLS-1$
				response.addCookie(cookieTest);
			}
		} else {
			// check if locale was passed earlier in this session
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
					for (Cookie cookie : cookies) {
						if ("lang".equals(cookie.getName())) { //$NON-NLS-1$
							forcedLocale = cookie.getValue();
							break;
						}
					}
			}
		}

		// format forced locale
		if (forcedLocale != null) {
			if (forcedLocale.length() >= 5) {
				forcedLocale = forcedLocale.substring(0, 2) + "_" //$NON-NLS-1$
						+ forcedLocale.substring(3, 5);
			} else if (forcedLocale.length() >= 2) {
				forcedLocale = forcedLocale.substring(0, 2);
			}
		}
		return forcedLocale;
	}

	/**
	 * If locales for infocenter specified in prefernces or as command line
	 * parameters, this methods stores these locales in locales local variable
	 * for later access.
	 */
	private static synchronized void initializeNL() {
		if (defaultLocale != null) {
			// already initialized
			return;
		}
		initializeLocales();
		if ((BaseHelpSystem.getMode() == BaseHelpSystem.MODE_INFOCENTER)) {
			initializeIcDirection();
		}

	}
	/**
	 *
	 */
	private static void initializeLocales() {
		// initialize default locale
		defaultLocale = Platform.getNL();
		if (defaultLocale == null) {
			defaultLocale = Locale.getDefault().toString();
		}
		if (BaseHelpSystem.getMode() != BaseHelpSystem.MODE_INFOCENTER) {
			return;
		}

		// locale strings as passed in command line or in preferences
		final List<String> infocenterLocales= new ArrayList<>();

		// first check if locales passed as command line arguments
		String[] args = Platform.getCommandLineArgs();
		boolean localeOption = false;
		for (String arg : args) {
			if ("-locales".equalsIgnoreCase(arg)) { //$NON-NLS-1$
				localeOption = true;
				continue;
			} else if (arg.startsWith("-")) { //$NON-NLS-1$
				localeOption = false;
				continue;
			}
			if (localeOption) {
				infocenterLocales.add(arg);
			}
		}
		// if no locales from command line, get them from preferences
		if (infocenterLocales.isEmpty()) {
			String preferredLocales = Platform.getPreferencesService().getString
				(HelpBasePlugin.PLUGIN_ID, ("locales"), "", null); //$NON-NLS-1$ //$NON-NLS-2$
			StringTokenizer tokenizer = new StringTokenizer(preferredLocales,
					" ,\t"); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				infocenterLocales.add(tokenizer.nextToken());
			}
		}

		// format locales and collect in a set for lookup
		if (!infocenterLocales.isEmpty()) {
			locales = new HashSet<>(10, 0.4f);
			for (String locale : infocenterLocales) {
				if (locale.length() >= 5) {
					locales.add(locale.substring(0, 2).toLowerCase(Locale.ENGLISH) + "_" //$NON-NLS-1$
							+ locale.substring(3, 5).toUpperCase(Locale.ENGLISH));

				} else if (locale.length() >= 2) {
					locales.add(locale.substring(0, 2).toLowerCase(Locale.ENGLISH));
				}
			}
		}
	}

	private static void initializeIcDirection() {
		// from property
		String orientation = System.getProperty("eclipse.orientation"); //$NON-NLS-1$
		if ("rtl".equals(orientation)) { //$NON-NLS-1$
			infocenterDirection = INFOCENTER_DIRECTION_RTL;
			return;
		} else if ("ltr".equals(orientation)) { //$NON-NLS-1$
			infocenterDirection = INFOCENTER_DIRECTION_LTR;
			return;
		}
		// from command line
		String[] args = Platform.getCommandLineArgs();
		for (int i = 0; i < args.length; i++) {
			if ("-dir".equalsIgnoreCase(args[i])) { //$NON-NLS-1$
				if ((i + 1) < args.length
						&& "rtl".equalsIgnoreCase(args[i + 1])) { //$NON-NLS-1$
					infocenterDirection = INFOCENTER_DIRECTION_RTL;
					return;
				}
				infocenterDirection = INFOCENTER_DIRECTION_LTR;
				return;
			}
		}
		// by client locale
	}

	public static boolean isRTL(HttpServletRequest request,
			HttpServletResponse response) {
		if (BaseHelpSystem.getMode() != BaseHelpSystem.MODE_INFOCENTER) {
			return ProductPreferences.isRTL();
		}
		if (infocenterDirection == INFOCENTER_DIRECTION_RTL) {
			return true;
		} else if (infocenterDirection == INFOCENTER_DIRECTION_LTR) {
			return false;
		}
		String locale = getLocale(request, response);
		if (locale.startsWith("ar") || locale.startsWith("fa") //$NON-NLS-1$ //$NON-NLS-2$
				|| locale.startsWith("he") || locale.startsWith("iw") //$NON-NLS-1$ //$NON-NLS-2$
				|| locale.startsWith("ur")) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	// Return true if the URI is of the form /<context>/nav/*
	public static boolean isNavPath(String uri) {
		int slash1 = uri.indexOf('/');
		int slash2 = uri.indexOf('/', 1);
		return (slash1 == 0 && slash2 >= 0 && uri.substring(slash2).startsWith("/nav")); //$NON-NLS-1$
	}

	// Create a relative path based on the current URL
	public static String getRelativePath(HttpServletRequest req, String filePath) {
		StringBuilder result = new StringBuilder(""); //$NON-NLS-1$
		String reqPath = req.getPathInfo();
		if (reqPath != null) {
			for (int i; 0 <= (i = reqPath.indexOf('/', 1));) {
				if (result.length() == 0 && filePath.startsWith(reqPath.substring(0, i + 1))) {
					filePath = filePath.substring(i);
				} else {
					result.append("../"); //$NON-NLS-1$
				}
				reqPath = reqPath.substring(i);
			}
		}
		return result.toString() + filePath.substring(1);
	}
}
