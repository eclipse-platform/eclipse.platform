/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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

public class BasicTest {
	@Test
	public void testSearchUnfiltered() {
		SearchTestUtils.searchAllLocales("jehcyqpfjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	// appears in both filtered and unfiltered parts of test1.xhtml
	@Test
	public void testSearchFilteredAndUnfiltered() {
		SearchTestUtils.searchAllLocales ("vkrhjewiwh", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml"});
	}

	// try OR'ing with a word that does't exist.. should find same result
	@Test
	public void testSearchOrWithNonexistent() {
		SearchTestUtils.searchAllLocales("vkrhjewiwh OR this_string_shouldnt_exist_in_any_doc", new String[] {"/org.eclipse.ua.tests/data/help/search/test1.xhtml" }) ;
	}

	// try OR'ing with a string from another doc.. should find both
	@Test
	public void testSearchOrBothExist() {
		SearchTestUtils.searchAllLocales("vkrhjewiwh OR rugnwjfyqj", new String[] {"/org.eclipse.ua.tests/data/help/search/test1.xhtml", "/org.eclipse.ua.tests/data/help/search/test2.xhtml" });
	}

	// these two words only appear next to each other in a paragraph that's filtered out - search should still find it
	@Test
	public void testSearchInFiltered() {
		SearchTestUtils.searchAllLocales("\"vkrhjewiwh riehguanil\"", new String[] {"/org.eclipse.ua.tests/data/help/search/test1.xhtml" } );
	}

	// first one should be found, but second one only exists in a paragraph that's filtered out - search should still find
	@Test
	public void testSearchAndWithFilter() {
		SearchTestUtils.searchAllLocales("vkrhjewiwh AND riehguanil", new String[] {"/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	// only exists in paragraph that's filtered out - search should find
	@Test
	public void testSearchInFilteredOut() {
		SearchTestUtils.searchAllLocales("gsdduvfqnh", new String[] {"/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	// word is in test3.xhtml and also included by test4.xhtml, contributed
	// into test5.xhtml as an extension, and replaces a paragraph in test6.xhtml.
	@Test
	public void testSearchInclusionAndExtension() {
		SearchTestUtils.searchAllLocales("fuejnghqjs", new String[] {"/org.eclipse.ua.tests/data/help/search/test3.xhtml", "/org.eclipse.ua.tests/data/help/search/test4.xhtml", "/org.eclipse.ua.tests/data/help/search/test5.xhtml", "/org.eclipse.ua.tests/data/help/search/test6.xhtml" });
	}

	// only exists in paragraph in test6.xhtml that's replaced by another one
	@Test
	public void testSearchInReplacedOut() {
		SearchTestUtils.searchAllLocales("bheufnjefa", new String[0] /* no hits*/ );
	}

	// Test replacement using ExtensionProvider
	@Test
	public void testSearchInReplaceOutUsingProvider() {
		SearchTestUtils.searchAllLocales("ausjduehf", new String[] {"/org.eclipse.ua.tests/data/help/search/test6.xhtml" }  );
	}

	// Test replacement using ExtensionProvider
	@Test
	public void testSearchInReplacemenTextUsingProvider() {
		SearchTestUtils.searchAllLocales("bheufnjefb", new String[0] /* no hits*/ );
	}

	// sanity test to make sure it finds things in XHTML content in .html file
	@Test
	public void testSearchXhtmlInHtml() {
		SearchTestUtils.searchAllLocales("kejehrgaqm", new String[] {"/org.eclipse.ua.tests/data/help/search/test7.html" });
	}

	// same as above, but in a section that should never be filtered
	@Test
	public void testSearchXhtmlNeverFiltered() {
		SearchTestUtils.searchAllLocales("opqmenhfjs", new String[] {"/org.eclipse.ua.tests/data/help/search/test7.html" });
	}

	// only exists in a paragraph in test7.html that should be filtered out
	// make sure this works for XHTML content inside .html file
	@Test
	public void testSearchFilteredXhtmlInHtml() {
		SearchTestUtils.searchAllLocales("hugftnhdtg", new String[] {"/org.eclipse.ua.tests/data/help/search/test7.html" });
	}

	// this doc is listed in TOC several times, using slightly different paths
	@Test
	public void testSearchMultipleTocReference() {
		SearchTestUtils.searchAllLocales("rqfedajhtg", new String[] {"/org.eclipse.ua.tests/data/help/search/test9.htm" });
	}

	@Test
	public void testSearchMultipleNonadjacentWords() {
		SearchTestUtils.searchAllLocales("gsdduvfqnh riehguanil", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" } );
	}

	@Test
	public void testSearchMultipleNonadjacentExactMatch() {
		SearchTestUtils.searchAllLocales("\"gsdduvfqnh riehguanil\"", new String[0]);
	}

	@Test
	public void testSearchMultipleAdjacentExactMatch() {
		SearchTestUtils.searchAllLocales("\"vkrhjewiwh riehguanil\"", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml"});
	}

	@Test
	public void testSearchContentProducer() {
		SearchTestUtils.searchAllLocales("egrology", new String[] { "/org.eclipse.ua.tests/generated/Generated+Parent/Parent+page+with+searchable+word+egrology+.html"});
	}
}
