/*******************************************************************************
 * Copyright (c) 2014, 2015  Dirk Fauth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/
package org.eclipse.e4.core.internal.tests.nls;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.eclipse.e4.core.internal.services.ResourceBundleHelper;
import org.junit.Test;

public class LocaleTransformationTest {

	@Test
	public void testValidLanguageCountryVariant() {
		String localeString = "de_DE_EURO";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("de", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testValidLanguageCountry() {
		String localeString = "de_DE";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("de", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testValidLanguage() {
		String localeString = "de";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("de", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testValidCountry() {
		String localeString = "_DE";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testValidLanguageVariant() {
		String localeString = "de__EURO";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("de", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testValidVariant() {
		String localeString = "__EURO";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testValidCountryVariant() {
		String localeString = "_DE_EURO";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testInvalidLanguage() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		String localeString = "1234";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());

		Locale.setDefault(defaultLocale);
	}

	@Test
	public void testInvalidOneLetterLanguage() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		String localeString = "a";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());

		Locale.setDefault(defaultLocale);
	}

	@Test
	public void testThreeLetterValidLanguage() {
		String localeString = "kok";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("kok", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testInvalidOneLetterCountry() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		String localeString = "_X";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());

		Locale.setDefault(defaultLocale);
	}

	@Test
	public void testInvalidThreeLetterCountry() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		String localeString = "_XXX";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());

		Locale.setDefault(defaultLocale);
	}

	@Test
	public void testValidNumericAreaCode() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		String localeString = "_029";
		Locale locale = ResourceBundleHelper.toLocale(localeString);
		assertEquals("", locale.getLanguage());
		assertEquals("029", locale.getCountry());
		assertEquals("", locale.getVariant());

		Locale.setDefault(defaultLocale);
	}
}
