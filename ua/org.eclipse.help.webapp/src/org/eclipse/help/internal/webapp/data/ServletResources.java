/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.webapp.WebappResources;

/**
 * Uses a resource bundle to load images and strings from a property file in a
 * documentation plugin
 */
public class ServletResources {

	/**
	 * Resources constructor.
	 */
	protected ServletResources() {
		super();
	}

	public static String getConfirmShowAllExplanation(HttpServletRequest request) {
		String  message = HelpBasePlugin.getActivitySupport().getShowAllMessage();
		if (message==null)
			message = getString("confirmShowAllExplanation", request); //$NON-NLS-1$
		return message;
	}

	/**
	 * Returns a string from a property file. It uses 'name' as a the key to
	 * retrieve from the webapp.properties file.
	 *
	 * @param request
	 *            HttpServletRequest or null; default locale will be used if
	 *            null passed
	 */
	public static String getString(String name, HttpServletRequest request) {
		String property = WebappResources.getString(name, UrlUtil.getLocaleObj(
				request, null));
		if (property == null || property.length() <= 0) {
			return property;
		}
		int amp = property.indexOf('&');
		if (amp <0 || amp >= property.length() - 1) {
			return property;
		}
		return property.substring(0, amp)
				+ property.substring(amp + 1);
	}

	/**
	 * Returns a string from a property file. It uses 'name' as a the key to
	 * retrieve from the webapp.properties file.
	 *
	 * @param request
	 *            HttpServletRequest or null; default locale will be used if
	 *            null passed
	 */
	public static String getString(String name, String replace0,
			HttpServletRequest request) {
		String property = WebappResources.getString(name, UrlUtil.getLocaleObj(
				request, null), replace0);
		return property;
	}

	/**
	 * Returns a string from a property file. It uses 'name' as a the key to
	 * retrieve from the webapp.properties file. 'args[]' is used to replace
	 * the variables in property string.
	 *
	 * @param request
	 *            HttpServletRequest or null; default locale will be used if
	 *            null passed
	 */
	public static String getString(String name, String[] args,
			HttpServletRequest request) {
		String property = WebappResources.getString(name, UrlUtil.getLocaleObj(
				request, null), args);
		return property;
	}

	/**
	 * Returns a string from a property file, with underlined access key. Access
	 * key can be specified in the label by &amp; character following character
	 * in the label that is to serve as access key It uses 'name' as a the key
	 * to retrieve from the webapp.properties file.
	 *
	 * @param request
	 *            HttpServletRequest or null; default locale will be used if
	 *            null passed
	 */
	public static String getLabel(String name, HttpServletRequest request) {
		String property = WebappResources.getString(name, UrlUtil.getLocaleObj(
				request, null));
		if (property == null || property.length() <= 0) {
			return property;
		}
		int amp = property.indexOf('&');
		if (amp <0 || amp >= property.length() - 1) {
			return property;
		}
		boolean isIE = UrlUtil.isIE(request);
		String acceleratorPrefix = isIE ? "<u STYLE=\"ACCELERATOR:true\">" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		String acceleratorSuffix = isIE ? "</u>" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		return property.substring(0, amp)
				+ acceleratorPrefix
				+ property.charAt(amp+1) + acceleratorSuffix
				+ property.substring(amp + 2);
	}

	/**
	 * Returns access key for a named label from property file. It uses 'name'
	 * as a the key to retrieve from the webapp.properties file.
	 *
	 * @param request
	 *            HttpServletRequest or null; default locale will be used if
	 *            null passed
	 */
	public static String getAccessKey(String name, HttpServletRequest request) {
		String property = WebappResources.getString(name, UrlUtil.getLocaleObj(
				request, null));
		if (property == null || property.length() <= 0) {
			return null;
		}
		int amp = property.indexOf('&');
		if (amp <0 || amp >= property.length() - 1) {
			return null;
		}
		return ("" + property.charAt(amp +1)).toLowerCase(Locale.ENGLISH); //$NON-NLS-1$
	}

}
