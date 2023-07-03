/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.browser;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;
import org.eclipse.help.browser.*;
import org.eclipse.help.internal.base.*;
import org.eclipse.osgi.service.environment.*;
import org.eclipse.osgi.util.NLS;

public class CustomBrowser implements IBrowser {
	public static final String CUSTOM_BROWSER_PATH_KEY = "custom_browser_path"; //$NON-NLS-1$

	@Override
	public void close() {
	}

	@Override
	public boolean isCloseSupported() {
		return false;
	}

	@SuppressWarnings("resource")
	@Override
	public void displayURL(String url) throws Exception {
		String path =  Platform.getPreferencesService().getString
			(HelpBasePlugin.PLUGIN_ID, CustomBrowser.CUSTOM_BROWSER_PATH_KEY, "", null); //$NON-NLS-1$
		String[] command = prepareCommand(path, url);

		try {
			Process pr = Runtime.getRuntime().exec(command);
			Thread outConsumer = new StreamConsumer(pr.getInputStream());
			outConsumer.setName("Custom browser adapter output reader"); //$NON-NLS-1$
			outConsumer.start();
			Thread errConsumer = new StreamConsumer(pr.getErrorStream());
			errConsumer.setName("Custom browser adapter error reader"); //$NON-NLS-1$
			errConsumer.start();
		} catch (Exception e) {
			ILog.of(getClass()).error("Launching URL \"" + url + "\" using browser program \"" //$NON-NLS-1$ //$NON-NLS-2$
							+ path + "\" has failed.  Specify another browser in help preferences.", e); //$NON-NLS-1$
			throw new Exception(NLS.bind(HelpBaseResources.CustomBrowser_errorLaunching, url, path));
		}
	}

	@Override
	public boolean isSetLocationSupported() {
		return false;
	}

	@Override
	public boolean isSetSizeSupported() {
		return false;
	}

	@Override
	public void setLocation(int x, int y) {
	}

	@Override
	public void setSize(int width, int height) {
	}

	/**
	 * Creates the final command to launch.
	 *
	 * @param path
	 * @param url
	 * @return String[]
	 */
	private String[] prepareCommand(String path, String url) {
		ArrayList<String> tokenList = new ArrayList<>();
		//Divide along quotation marks
		StringTokenizer qTokenizer = new StringTokenizer(path.trim(),
				"\"", true); //$NON-NLS-1$
		boolean withinQuotation = false;
		String quotedString = ""; //$NON-NLS-1$
		while (qTokenizer.hasMoreTokens()) {
			String curToken = qTokenizer.nextToken();
			if (curToken.equals("\"")) { //$NON-NLS-1$
				if (withinQuotation) {
					if (Constants.OS_WIN32.equalsIgnoreCase(Platform.getOS())) {
						// need to quote URLs on Windows
						tokenList.add("\"" + quotedString + "\""); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						// qotes prevent launching on Unix 35673
						tokenList.add(quotedString);
					}
				} else {
					quotedString = ""; //$NON-NLS-1$
				}
				withinQuotation = !withinQuotation;
				continue;
			} else if (withinQuotation) {
				quotedString = curToken;
				continue;
			} else {
				//divide unquoted strings along white space
				StringTokenizer parser = new StringTokenizer(curToken.trim());
				while (parser.hasMoreTokens()) {
					tokenList.add(parser.nextToken());
				}
			}
		}
		// substitute %1 by url
		boolean substituted = false;
		for (int i = 0; i < tokenList.size(); i++) {
			String token = tokenList.get(i);
			String newToken = doSubstitutions(token, url);
			if (newToken != null) {
				tokenList.set(i, newToken);
				substituted = true;
			}
		}
		// add the url if not substituted already
		if (!substituted)
			tokenList.add(url);

		String[] command = new String[tokenList.size()];
		tokenList.toArray(command);
		return command;
	}

	/**
	 * Replaces any occurrences of <code>"%1"</code> or <code>%1</code> with
	 * the URL.
	 *
	 * @param token
	 *            The token in which the substitutions should be made; must not
	 *            be <code>null</code>.
	 * @return The substituted string, if a substitution is made;
	 *         <code>null</code> if no substitution is made.
	 */
	private String doSubstitutions(String token, String url) {
		boolean substituted = false;
		StringBuilder newToken = new StringBuilder(token);
		String substitutionMarker = "%1"; //$NON-NLS-1$
		int index = newToken.indexOf(substitutionMarker);
		while (index != -1) {
			newToken.replace(index, index + substitutionMarker.length(), url);
			index = newToken.indexOf(substitutionMarker, index + url.length());
			substituted = true;
		}

		if (substituted) {
			return newToken.toString();
		}

		return null;
	}
}
