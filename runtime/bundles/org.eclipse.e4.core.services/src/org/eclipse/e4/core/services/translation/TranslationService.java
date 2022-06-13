/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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
 *     Dirk Fauth <dirk.fauth@googlemail.com> - Bug 428427
 *     Lars Vogel <Lars.Vogel@vogell.com> - Bug 445444
 ******************************************************************************/
package org.eclipse.e4.core.services.translation;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Provides localization service.
 *
 * @since 1.2
 */
public abstract class TranslationService {

	/**
	 * The name of the context variable with locale information
	 */
	public static final String LOCALE = "org.eclipse.e4.core.locale"; //$NON-NLS-1$
	/**
	 * Prefix for keys to be translated
	 */
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	/**
	 * Prefix that aborts translation
	 */
	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$

	@Inject
	@Named(LOCALE)
	protected Locale locale;

	/**
	 * Translates the key from the contributor. If translation can not be found, the original key
	 * should be returned.
	 * <p>
	 * This method is expected to be overridden by the implementors.
	 * </p>
	 *
	 * @param key
	 *            the key
	 * @param contributorURI
	 *            URI of the contributor
	 * @return localized value, or the original key if the translation can not be done
	 */
	public String translate(String key, String contributorURI) {
		return key;
	}

	/**
	 * Returns the value out of the given ResourceBundle for the given translation key. Note that
	 * this method will only work correctly if the key matches the specification of a translation
	 * key in the application model. That means, it needs to start with a % character.
	 *
	 * @param key
	 *            The value that is set as key in the application model.
	 * @param resourceBundle
	 *            The ResourceBundle that should be used to retrieve the translation.
	 * @return The value value out of the given ResourceBundle for the given translation key.
	 */
	protected String getResourceString(String key, ResourceBundle resourceBundle) {
		String s = key.trim();
		if (!s.startsWith(KEY_PREFIX, 0))
			return s;
		if (s.startsWith(KEY_DOUBLE_PREFIX, 0))
			return s.substring(1);

		int ix = s.indexOf(' ');
		String rbKey = ix == -1 ? s : s.substring(0, ix);
		String dflt = ix == -1 ? s : s.substring(ix + 1);

		if (resourceBundle == null)
			return dflt;

		try {
			return resourceBundle.getString(rbKey.substring(1));
		} catch (MissingResourceException e) {
			// this will avoid requiring a bundle access on the next lookup
			return dflt;
		}
	}
}
