/*******************************************************************************
 * Copyright (c) 2012, 2015 Dirk Fauth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@gmail.com> - initial API and implementation
 *     Ragnar Nevries <r.eclipse@nevri.es> - Bug 458798
 ******************************************************************************/
package org.eclipse.e4.core.internal.services;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Wrapper class for accessing translations out of a {@link ResourceBundle}.
 *
 * @author Dirk Fauth
 *
 */
public class ResourceBundleTranslationProvider {

	/**
	 * The {@link ResourceBundle} to use for translations.
	 */
	private ResourceBundle resourceBundle;

	/**
	 *
	 * @param resourceBundle
	 *            The {@link ResourceBundle} to use for translations. Can be <code>null</code>,
	 *            which will lead to simply return the key modified by prefixing and suffixing it
	 *            with "!" when calling translate(String).
	 */
	public ResourceBundleTranslationProvider(ResourceBundle resourceBundle) {
		this.setResourceBundle(resourceBundle);
	}

	/**
	 * Tries to retrieve the translation value for the given key out of the {@link ResourceBundle}
	 * set to this {@link ResourceBundleTranslationProvider}. If there is no {@link ResourceBundle}
	 * set or there is no translation found for the given key, the key itself prefixed and suffixed
	 * with "!" will be returned to indicate that there is no translation found.
	 * <p>
	 * This implementation also supports the usage of dot separation for property keys. As in Java
	 * variables can not be separated with a dot, the underscore needs to be used for separation of
	 * the variable. This will be replaced automatically to a dot, if there is no translation found
	 * with an underscore as separator.
	 * </p>
	 * This implementation also supports camel cased member variables to match the appropriate
	 * property keys. As a result, all combinations of replacing camel cased strings by
	 * their "underscorified" version and replacing underscores by dots are considered. The order
	 * is shown by the following example:
	 * <p>
	 * Let the property key to translate be <i>camelCase_Subtree</i>. Then the
	 * {@link ResourceBundle} is browsed for the following keys (in that order):
	 * </p>
	 * <ol>
	 * <li>camelCase_Subtree</li>
	 * <li>camel_case__subtree</li>
	 * <li>camel.case..subtree</li>
	 * <li>camelCase.Subtree</li>
	 * <li>camel_case._subtree</li>
	 * </ol>
	 *
	 *
	 * @param key
	 *            The key of the requested translation property.
	 * @return The translation for the given key or the key itself prefixed and suffixed with "!" to
	 *         indicate that there is no translation available for the given key.
	 */
	public String translate(String key) {
		String result = translate_rec(key);
		if (result != null) {
			return result;
		} else {
			return "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private String translate_rec(String key) {
		String result = null;
		try {
			if (this.resourceBundle != null) {
				result = this.resourceBundle.getString(key);
			}
		} catch (MissingResourceException e) {
			// key was not found as is, check if it is camel cased or contains
			// underscores

			// try to de-camel-casify and recurse
			if (key.matches(".*[A-Z].*")) { //$NON-NLS-1$
				result = translate_rec(underscorify(key));
			}

			// if no succes, try to de-underscorify and recurse
			if (result == null && key.contains("_")) { //$NON-NLS-1$
				result = translate_rec(key.replace('_', '.'));
			}
		}
		return result;
	}

	/**
	 * @return The {@link ResourceBundle} that is used for translations.
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * @param resourceBundle
	 *            The {@link ResourceBundle} to use for translations.
	 */
	public void setResourceBundle(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
	}

	/**
	 * This method is used to create a underscorified key out of a camel cased String.
	 * <p>
	 * Note: This method is introduced to support Java Naming Conventions for member variables,
	 * which says that member variables should be camel cased. See <a href=
	 * "http://www.oracle.com/technetwork/java/javase/documentation/codeconventions-135099.html#367"
	 * > Naming Conventions</a>.
	 *
	 * @param key
	 *            The possible camel cased key that needs to be underscorified.
	 * @return The underscorified key.
	 */
	public static String underscorify(String key) {
		if (key == null || key.isEmpty()) {
			return key;
		}

		StringBuilder result = new StringBuilder();
		for (int i = 0; i < key.length(); i++) {
			if (Character.isUpperCase(key.charAt(i))) {
				result.append("_").append(Character.toLowerCase(key.charAt(i))); //$NON-NLS-1$
			} else {
				result.append(key.charAt(i));
			}
		}
		return result.toString();
	}
}
