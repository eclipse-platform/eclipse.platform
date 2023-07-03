/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov - Bug 460787
 *     Sopot Cela - Bug 466829
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;

/**
 * Lucene Analyzer. LowerCaseFilter-&gt;StandardTokenizer
 */
public final class DefaultAnalyzer extends Analyzer {

	private Locale locale;

	/**
	 * Creates a new analyzer using the given locale.
	 */
	public DefaultAnalyzer(String localeString) {
		super();
		// Create a locale object for a given locale string
		Locale userLocale = getLocale(localeString);

		// Check if the locale is supported by BreakIterator
		// check here to do it only once.
		Locale[] availableLocales = BreakIterator.getAvailableLocales();
		for (Locale availableLocale : availableLocales) {
			if (userLocale.equals(availableLocale)) {
				locale = userLocale;
				break;
			}
		}
		if (locale == null && userLocale.getDisplayVariant().length() > 0) {
			// Check if the locale without variant is supported by BreakIterator
			Locale countryLocale = new Locale(userLocale.getLanguage(), userLocale.getCountry());
			for (Locale availableLocale : availableLocales) {
				if (countryLocale.equals(availableLocale)) {
					locale = countryLocale;
					break;
				}
			}
		}
		if (locale == null && userLocale.getCountry().length() > 0) {
			// Check if at least the language is supported by BreakIterator
			Locale language = new Locale(userLocale.getLanguage(), ""); //$NON-NLS-1$
			for (Locale availableLocale : availableLocales) {
				if (language.equals(availableLocale)) {
					locale = language;
					break;
				}
			}
		}

		if (locale == null) {
			// Locale is not supported, will use en_US
			ILog.of(getClass()).error(
							"Text Analyzer could not be created for locale {0}.  An analyzer that extends org.eclipse.help.luceneAnalyzer extension point needs to be plugged in for locale " //$NON-NLS-1$
									+ localeString
									+ ", or Java Virtual Machine needs to be upgraded to version with proper support for locale {0}.", //$NON-NLS-1$
							null);
			locale = new Locale("en", "US"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Creates a Locale object out of a string representation
	 */
	private Locale getLocale(String clientLocale) {
		if (clientLocale == null)
			clientLocale = Platform.getNL();
		if (clientLocale == null)
			clientLocale = Locale.getDefault().toString();

		// break the string into tokens to get the Locale object
		StringTokenizer locales = new StringTokenizer(clientLocale, "_"); //$NON-NLS-1$
		if (locales.countTokens() == 1)
			return new Locale(locales.nextToken(), ""); //$NON-NLS-1$
		else if (locales.countTokens() == 2)
			return new Locale(locales.nextToken(), locales.nextToken());
		else if (locales.countTokens() == 3)
			return new Locale(locales.nextToken(), locales.nextToken(), locales.nextToken());
		else
			return Locale.getDefault();
	}

	/*
	 * Can't use try-with-resources because the Lucene internally reuses
	 * components. See {@link org.apache.lucene.analysis.Analyzer.ReuseStrategy}
	 */
	@SuppressWarnings("resource")
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer source = new StandardTokenizer();
		LowerCaseFilter filter = new LowerCaseFilter(source);
		TokenStreamComponents components = new TokenStreamComponents(source, filter);
		return components;
	}
}
