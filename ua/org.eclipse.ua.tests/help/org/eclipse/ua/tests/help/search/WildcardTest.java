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

public class WildcardTest {
	@Test
	public void testSearchWithStar() {
		SearchTestUtils.searchAllLocales("jehc*qpfjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	@Test
	public void testSearchWithTwoStars() {
		SearchTestUtils.searchAllLocales("jehc*qp*js", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	@Test
	public void testSearchWithStarReplacingThreeChars() {
		SearchTestUtils.searchAllLocales("jehc*fjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	// Test that a star does not match spaces
	@Test
	public void testSearchWithStarReplacingSpace() {
		SearchTestUtils.searchAllLocales("jehcyqpfjs*vkrhjewiwh", new String[0]);
	}

	@Test
	public void testSearchWithQuestionMark() {
		SearchTestUtils.searchAllLocales("jehc?qpfjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	@Test
	public void testSearchWithTwoQuestionMarks() {
		SearchTestUtils.searchAllLocales("j?hc?qpfjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	@Test
	public void testSearchQuestionMarkCannotReplaceTwoChars() {
		SearchTestUtils.searchAllLocales("jehc?pfjs", new String[0] );
	}

	@Test
	public void testSearchSuccessiveQuestionMarks() {
		SearchTestUtils.searchAllLocales("jehc??pfjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" } );
	}

	@Test
	public void testSearchLeadingStar() {
		SearchTestUtils.searchAllLocales("*hcyqpfjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

	@Test
	public void testSearchLeadingQuestionMark() {
		SearchTestUtils.searchAllLocales("?ehcyqpfjs", new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" });
	}

}
