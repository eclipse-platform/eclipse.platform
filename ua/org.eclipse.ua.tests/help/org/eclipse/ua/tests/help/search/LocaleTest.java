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


import org.junit.Test;


public class LocaleTest {
	@Test
	public void testSearchWordInDefaultOnly() {
		SearchTestUtils.searchOneLocale("duejrnfjudshebn", new String[] { "/org.eclipse.ua.tests/data/help/search/testnl1.xhtml"}, "en");
		SearchTestUtils.searchOneLocale("duejrnfjudshebn", new String[0], "de");
	}

	@Test
	public void testSearchWordInNonDefaultOnly() {
		SearchTestUtils.searchOneLocale("dkdskadksak", new String[] { "/org.eclipse.ua.tests/data/help/search/testnl1.xhtml"}, "de");
		SearchTestUtils.searchOneLocale("dkdskadksak", new String[0], "en");
	}

	@Test
	public void testSearchExactMatchInDefaultOnly() {
		SearchTestUtils.searchOneLocale("\"fesaggresgf duejrnfjudshebn\"", new String[] { "/org.eclipse.ua.tests/data/help/search/testnl1.xhtml"}, "en");
		SearchTestUtils.searchOneLocale("\"fesaggresgf duejrnfjudshebn\"", new String[0], "de");
	}

	@Test
	public void testSearchExactMatchInNonDefaultOnly() {
		SearchTestUtils.searchOneLocale("\"dkdskadksak redfrewfdsa\"", new String[] { "/org.eclipse.ua.tests/data/help/search/testnl1.xhtml"}, "de");
		SearchTestUtils.searchOneLocale("\"dkdskadksak redfrewfdsa\"", new String[0], "en");
	}

	@Test
	public void testSearchWithWildcardInDefaultOnly() {
		SearchTestUtils.searchOneLocale("duejrnf?udshebn", new String[] { "/org.eclipse.ua.tests/data/help/search/testnl1.xhtml"}, "en");
		SearchTestUtils.searchOneLocale("duejrnf?udshebn", new String[0], "de");
	}

	@Test
	public void testSearchWithWildcardInNonDefaultOnly() {
		SearchTestUtils.searchOneLocale("dkd?kadksak", new String[] { "/org.eclipse.ua.tests/data/help/search/testnl1.xhtml"}, "de");
		SearchTestUtils.searchOneLocale("dkd?kadksak", new String[0], "en");
	}

	@Test
	public void testSearchPageNotInTocForLocale() {
		SearchTestUtils.searchOneLocale("undefgfdsgfds", new String[0], "de");
		SearchTestUtils.searchOneLocale("undefgfdsgfds", new String[0], "en");
	}

	@Test
	public void testSearchPageOnlyInDefaultToc() {
		SearchTestUtils.searchOneLocale("idskrekfuej", new String[0], "de");
		SearchTestUtils.searchOneLocale("idskrekfuej", new String[] { "/org.eclipse.ua.tests/data/help/search/test_en.html"}, "en");
	}

	@Test
	public void testSearchPageOnlyInLocalToc() {
		SearchTestUtils.searchOneLocale("deuufjfu", new String[0], "en");
		SearchTestUtils.searchOneLocale("deuufjfu", new String[] { "/org.eclipse.ua.tests/data/help/search/test_de.html"}, "de");
	}

	@Test
	public void testSearchEnglishStemming_ed() {
		SearchTestUtils.searchOneLocale("udjerufdjd", new String[0], "de");
		SearchTestUtils.searchOneLocale("udjerufdjd", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml"}, "en");
	}

	@Test
	public void testSearchEnglishStemming_ing() {
		SearchTestUtils.searchOneLocale("kjfdskajdfska", new String[0], "de");
		SearchTestUtils.searchOneLocale("kjfdskajdfska", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml"}, "en");
	}

	// Prefixes are not matched by stemming
	@Test
	public void testSearchEnglishStemming_re() {
		SearchTestUtils.searchOneLocale("dhdsahkdshakjd", new String[0], "de");
		SearchTestUtils.searchOneLocale("dhdsahkdshakjd", new String[0], "en");
	}

	/**
	 * Tests that the stemmer properly removes the German "e" suffix.
	 */
	@Test
	public void testSearchGermanStemming_e() {
		SearchTestUtils.searchOneLocale("dfskajkfsafaisch", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml"}, "de");
	}

	/**
	 * Tests that the stemmer properly removes the German "en" suffix.
	 */
	@Test
	public void testSearchGermanStemming_en() {
		SearchTestUtils.searchOneLocale("kirch", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml"}, "de");
		SearchTestUtils.searchOneLocale("kirch", new String[0], "en");
	}

	@Test
	public void testSearchQualifiedNames_en() {
		SearchTestUtils.searchOneLocale("usjenfdj.udjeudj.uejdhehj", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml" },
				"en" );
		SearchTestUtils.searchOneLocale("uejdhehj", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml" },
				"en" );
	}

	/*
	 * FAILS - see bug Bug 219928
	public void testSearchQualifiedNames_de() {
		SearchTestUtils.searchOneLocale("usjenfdj.udjeudj.uejdhehj", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml" },
				"de" );
		SearchTestUtils.searchOneLocale("uejdhehj", new String[] { "/org.eclipse.ua.tests/data/help/search/test10.xhtml" },
				"de" );
	}

	*/


}
