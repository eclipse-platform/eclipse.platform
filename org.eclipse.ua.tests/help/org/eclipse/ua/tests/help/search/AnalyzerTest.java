/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eclipse.help.internal.search.AnalyzerDescriptor;
import org.junit.Test;

public class AnalyzerTest {

	private final String[] supportedLanguages = { "en", "pt", "ja", "zh", "cs", "de", "el", "fr", "nl", "ru", "ar" };

	@Test
	public void testEnglishAnalyzer() {
		checkAnalyzer("en", "en");
	}

	@Test
	public void testEnglishUsAnalyzer() {
		checkAnalyzer("en_us", "en");
	}

	@Test
	public void testGermanAnalyzer_de() {
		checkAnalyzer("de", "de");
	}

	@Test
	public void testGermanAnalyzer_de_DE() {
		checkAnalyzer("de_DE", "de");
	}

	@Test
	public void testJapaneseAnalyzer() {
		checkAnalyzer("ja", "ja");
	}

	@Test
	public void testFrenchAnalyzer() {
		checkAnalyzer("fr", "fr");
	}

	@Test
	public void testChineseAnalyzer() {
		checkAnalyzer("zh", "zh");
	}

	// Korean and japanese share an analyzer
	@Test
	public void testKoreanAnalyzer() {
		checkAnalyzer("ko", "ja");
	}

	@Test
	public void testRussianAnalyzer() {
		checkAnalyzer("ru", "ru");
	}

	@Test
	public void testGreekAnalyzer() {
		checkAnalyzer("el", "el");
	}

	// Uses default
	@Test
	public void testSpanishAnalyzer() {
		checkAnalyzer("es", "ar");
	}

	@Test
	public void testPortugueseAnalyzer() {
		checkAnalyzer("pt", "pt");
	}

	@Test
	public void testDutchAnalyzer() {
		checkAnalyzer("nl", "nl");
	}

	@Test
	public void testCzechAnalyzer() {
		checkAnalyzer("cs", "cs");
	}

	// Uses default
	@Test
	public void testArabicAnalyzer() {
		checkAnalyzer("ar", "ar");
	}

	// Use default
	@Test
	public void testHebrewAnalyzer() {
		checkAnalyzer("il", "ar");
	}

	private void checkAnalyzer(String language, String analyzerKind) {
		AnalyzerDescriptor an = new AnalyzerDescriptor(language);

		for (String nextLocale : supportedLanguages) {
			AnalyzerDescriptor expected = new AnalyzerDescriptor(nextLocale);
			String analyzerClassName = expected.getAnalyzerClassName();
			if (nextLocale.equals(analyzerKind)) {
				assertEquals("Comparing " + nextLocale + " to " + language, analyzerClassName, an.getAnalyzerClassName());
			} else {
				assertFalse("Both " + nextLocale + " and " + language + " have value of " + analyzerClassName, analyzerClassName.equals(an.getAnalyzerClassName()));

			}
		}

	}

}
