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
package org.eclipse.help.internal.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Uses a resource bundle to load images and strings from a property file in a
 * documentation plugin
 */
public class WebappResources {

	// resource bundles indexed by locale
	private static HashMap<Locale, ResourceBundle> resourceBundleTable = new HashMap<>();

	/**
	 * Returns a string from a property file. It uses 'name' as a the key to
	 * retrieve from the webapp.properties file.
	 */
	public static String getString(String name, Locale locale) {

		// get bundle
		ResourceBundle bundle = getBundle(locale);
		if (bundle == null) {
			return name;
		}

		// get value
		try {
			return bundle.getString(name);
		} catch (MissingResourceException mre) {
			return name;
		}
	}

	/**
	 * Returns a string from a property file
	 */
	public static String getString(String name, Locale locale, String replace0) {

		// get bundle
		ResourceBundle bundle = getBundle(locale);
		if (bundle == null) {
			return name;
		}

		// get value
		try {
			String stringFromPropertiesFile = bundle.getString(name);
			stringFromPropertiesFile = MessageFormat.format(stringFromPropertiesFile, replace0);
			return stringFromPropertiesFile;
		} catch (Exception e) {
			return name;
		}

	}

	/**
	 * Returns a string from a property file. It uses 'name' as the key to
	 * retrieve from the webapp.properties file. And it uses args[] to replace the variables in property string.
	 */
	public static String getString(String name, Locale locale, String[] args) {

		// get bundle
		ResourceBundle bundle = getBundle(locale);
		if (bundle == null) {
			return name;
		}

		// get value
		try {
			String stringFromPropertiesFile = bundle.getString(name);
			stringFromPropertiesFile = MessageFormat.format(
					stringFromPropertiesFile, (Object[]) args);
			return stringFromPropertiesFile;
		} catch (Exception e) {
			return name;
		}
	}
	/**
	 * Obtains resource bundle for specified locale. Loads bundle if necessary
	 *
	 * @param locale
	 *            Locale or null to use default locale
	 * @return ResourceBundle or null if not found
	 */
	private static ResourceBundle getBundle(Locale locale) {
		if (locale == null)
			locale = getDefaultLocale();

		// check cache
		ResourceBundle bundle = resourceBundleTable
				.get(locale);

		// load bundle
		if (bundle == null) {
			StringBuilder sb = new StringBuilder();
			String language = locale.getLanguage();
			String contry = locale.getCountry();
			String variant = locale.getVariant();
			ResourceBundle bundle_l = null;
			ResourceBundle bundle_c = null;
			ResourceBundle bundle_v = null;
			if (null != language && language.length() != 0) {
				sb.append("_").append(language); //$NON-NLS-1$
				bundle_l = getResourceBundle(sb.toString());
			}
			if (null != contry && contry.length() != 0) {
				sb.append("_").append(contry); //$NON-NLS-1$
				bundle_c = getResourceBundle(sb.toString());
			}
			if (null != variant && variant.length() != 0) {
				sb.append("_").append(variant); //$NON-NLS-1$
				bundle_v = getResourceBundle(sb.toString());
			}
			if (bundle_v != null) {
				bundle = bundle_v;
			} else if (bundle_c != null) {
				bundle = bundle_c;
			} else if (bundle_l != null) {
				bundle = bundle_l;
			} else {
				bundle = getResourceBundle(""); //$NON-NLS-1$
			}
			// ******end******
			if (bundle != null) {
				resourceBundleTable.put(locale, bundle);
			}
		}
		return bundle;
	}

	private static ResourceBundle getResourceBundle(String key) {
		ResourceBundle bundle;

		Bundle hostBundle = Platform.getBundle(FrameworkUtil.getBundle(HelpWebappPlugin.class).getSymbolicName());
		if (hostBundle == null)
			return null;

		URL url = hostBundle.getResource("org/eclipse/help/internal/webapp/WebappResources" + key + ".properties");  //$NON-NLS-1$//$NON-NLS-2$
		if (url == null)
			return null;

		try (InputStream in= url.openStream()) {
			bundle= new PropertyResourceBundle(in);
		} catch (IOException e) {
			bundle = null;
		}
		return bundle;
	}

	private static Locale getDefaultLocale() {
		String nl = Platform.getNL();
		// sanity test
		if (nl == null)
			return Locale.getDefault();

		// break the string into tokens to get the Locale object
		StringTokenizer locales = new StringTokenizer(nl, "_"); //$NON-NLS-1$
		if (locales.countTokens() == 1)
			return new Locale(locales.nextToken(), ""); //$NON-NLS-1$
		else if (locales.countTokens() == 2)
			return new Locale(locales.nextToken(), locales.nextToken());
		else if (locales.countTokens() == 3)
			return new Locale(locales.nextToken(), locales.nextToken(), locales
					.nextToken());
		else
			return Locale.getDefault();
	}
}
