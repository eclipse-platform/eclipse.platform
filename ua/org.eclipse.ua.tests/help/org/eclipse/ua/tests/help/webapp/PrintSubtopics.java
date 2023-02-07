/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertEquals;

import org.eclipse.help.internal.webapp.data.PrintData;
import org.junit.Test;

/**
 * Test for methods in PrintData
 */

public class PrintSubtopics {
	@Test
	public void testHeadingInsertion() {
		String result = PrintData.injectHeading("<body> <p>Title</p>", "1");
		assertEquals("<body> <p><a id=\"section1\">1. </a>Title</p>", result);
	}

	@Test
	public void testHeaderInsertionSkipsWhitespace() {
		checkHeadingInsertion("<body> <p>  \n\r</p><h1>", "Title</h1>");
	}

	@Test
	public void testAccentedCharacter() {
		checkHeadingInsertion("<body> <p>", "\u00E1guila</p>");
	}

	@Test
	public void testinvertedQuestionmark() {
		checkHeadingInsertion("<body> <p>", "\u00BFQu\u00E9 es Eclipse?</p>");
	}

	@Test
	public void testSlash() {
		checkHeadingInsertion("<body> <p>", "/usr/bin</p>");
	}

	@Test
	public void testChineseCharacter() {
		checkHeadingInsertion("<body> <p>", "\u623F\u5B50</p>");
	}

	@Test
	public void testChineseExtbCharacter() {
		checkHeadingInsertion("<body> <p>", "\uD840\uDC06</p>");
	}

	/*
	 * Check that insertions occur between preInsert and postInsert
	 */
	public void checkHeadingInsertion(String preInsert, String postInsert) {
		String result = PrintData.injectHeading(preInsert + postInsert, "1");
		assertEquals(preInsert + "<a id=\"section1\">1. </a>" + postInsert, result);
	}

}
