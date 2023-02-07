/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.model.url;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.eclipse.ui.internal.intro.impl.util.Log;

/**
 * A parser that knows how to parser OOBE action URLs. If URL is a valid intro
 * url, it will create an instance of the IntroURL class.
 */
public class IntroURLParser {

	// private String url_string = null;
	private boolean hasProtocol = false;
	private boolean isIntroUrl = false;

	private URL url_inst;

	/**
	 * Constructor that gets the URL to parse.
	 */
	public IntroURLParser(String url) {
		// create a URL instance, and parser it for parameters.
		parseUrl(url);
	}

	private void parseUrl(String url) {
		if (url == null)
			return;
		url_inst = null;
		try {
			url_inst = new URL(url);
		} catch (MalformedURLException e) {
			// not a valid URL. set state.
			return;
		}

		if (url_inst.getProtocol() != null) {
			// URL has some valid protocol. Check to see if it is an intro url.
			hasProtocol = true;
			isIntroUrl = isIntroUrl(url_inst);
			return;
		}

		// not an Intro URL. do nothing.
		return;
	}


	/**
	 * @return Returns the hasProtocol.
	 */
	public boolean hasProtocol() {
		return hasProtocol;
	}

	/**
	 * @return Returns the isIntroUrl.
	 */
	public boolean hasIntroUrl() {
		return isIntroUrl;
	}


	/**
	 * @return Returns the currebt url Protocol.
	 */
	public String getProtocol() {
		return url_inst.getProtocol();
	}


	/**
	 * @return Returns the currebt url Protocol.
	 */
	public String getHost() {
		return url_inst.getHost();
	}


	/**
	 * Checks to see if tha passed URL is an Intro URL. An intro URL is an http
	 * URL that has the intro plugin id as a host. eg:
	 * "http://org.eclipse.ui.intro/test".
	 *
	 * @param url
	 * @return true if url is an intro URL.
	 */
	private boolean isIntroUrl(URL url) {
		if (!url.getProtocol().equalsIgnoreCase(IntroURL.INTRO_PROTOCOL))
			// quick exit. If it is not http, url is not an Intro url.
			return false;

		if (url.getHost().equalsIgnoreCase(IntroURL.INTRO_HOST_ID))
			return true;

		return false;
	}



	/**
	 * @return Returns the introURL. Will be null if the parsed URL is not an
	 *         Intro URL.
	 */
	public IntroURL getIntroURL() {
		IntroURL introURL = null;
		if (isIntroUrl) {
			// valid intro URL. Extract the action and parameters.
			String action = getPathAsAction(url_inst);
			Properties parameters = getQueryParameters(url_inst);

			// class instance vars are already populated by now.
			introURL = new IntroURL(action, parameters);
		}
		return introURL;
	}



	/**
	 * Retruns the path attribute of the passed URL, stripped out of the leading
	 * "/". Returns null if the url does not have a path.
	 *
	 * @param url
	 * @return
	 */
	private String getPathAsAction(URL url) {
		// get possible action.
		String action = url.getPath();
		// remove leading "/" from path.
		if (action != null)
			action = action.substring(1);
		return action;
	}

	/**
	 * Retruns the Query part of the URL as an instance of a Properties class.
	 *
	 * @param url
	 * @return
	 */
	public Properties getQueryParameters(URL url) {
		// parser all query parameters.
		Properties properties = new Properties();
		String query = url.getQuery();
		if (query == null)
			// we do not have any parameters in this URL, return an empty
			// Properties instance.
			return properties;

		// now extract the key/value pairs from the query.
		String[] params = query.split("&"); //$NON-NLS-1$
		for (int i = 0; i < params.length; i++) {
			// for every parameter, ie: key=value pair, create a property
			// entry. we know we have the key as the first string in the array,
			// and the value as the second array.
			String[] keyValuePair = params[i].split("="); //$NON-NLS-1$
			if (keyValuePair.length != 2) {
				Log.warning("Ignoring the following Intro URL parameter: " //$NON-NLS-1$
						+ params[i]);
				continue;
			}

			String key = urlDecode(keyValuePair[0]);
			if (key == null) {
				Log.warning("Failed to URL decode key: " + keyValuePair[0]); //$NON-NLS-1$
				continue;
			}

			String value = urlDecode(keyValuePair[1]);
			if (value == null) {
				Log.warning("Failed to URL decode value: " + keyValuePair[1]); //$NON-NLS-1$
				continue;
			}

			properties.setProperty(key, value);
		}
		return properties;
	}


	/*
	 * Note: This was copied and adapted from org.eclipse.help.internal.util.URLCoder
	 */
	private static String urlDecode(String encodedURL) {
		int len = encodedURL.length();
		ByteArrayOutputStream os = new ByteArrayOutputStream(len);

		for (int i = 0; i < len;) {
			switch (encodedURL.charAt(i)) {
			case '%':
				if (len >= i + 3) {
					os.write(Integer.parseInt(encodedURL.substring(i + 1, i + 3), 16));
				}
				i += 3;
				break;
			case '+': // exception from standard
				os.write(' ');
				i++;
				break;
			default:
				os.write(encodedURL.charAt(i++));
				break;
			}
		}
		return new String(os.toByteArray(), StandardCharsets.UTF_8);
	}

}
