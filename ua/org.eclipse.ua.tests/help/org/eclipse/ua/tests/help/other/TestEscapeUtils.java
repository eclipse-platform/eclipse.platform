/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertEquals;

import org.eclipse.help.ui.internal.util.EscapeUtils;
import org.eclipse.ui.internal.cheatsheets.views.ViewUtilities;
import org.junit.Test;

public class TestEscapeUtils {
	@Test
	public void testEscapeEmpty() {
		assertEquals("", EscapeUtils.escapeSpecialChars(""));
		assertEquals("", EscapeUtils.escapeSpecialCharsLeavinggBold(""));
	}

	@Test
	public void testEscapeSimple() {
		assertEquals("abc", EscapeUtils.escapeSpecialChars("abc"));
		assertEquals("abc", EscapeUtils.escapeSpecialCharsLeavinggBold("abc"));
	}

	@Test
	public void testEscapeTabs() {
		assertEquals("a  bc", EscapeUtils.escapeSpecialChars("a\t\tbc"));
		assertEquals("a  bc", EscapeUtils.escapeSpecialCharsLeavinggBold("a\t\tbc"));
	}

	@Test
	public void testEscapeAmpersand() {
		assertEquals("&amp;1&amp;", EscapeUtils.escapeSpecialChars("&1&"));
		assertEquals("&amp;1&amp;", EscapeUtils.escapeSpecialCharsLeavinggBold("&1&"));
		assertEquals("&amp;1&amp;", EscapeUtils.escapeAmpersand("&1&"));
	}

	@Test
	public void testEscapeQuotes() {
		assertEquals("&quot;&quot;&apos;&apos;", EscapeUtils.escapeSpecialChars("\"\"\'\'"));
		assertEquals("&quot;&quot;&apos;&apos;", EscapeUtils.escapeSpecialCharsLeavinggBold("\"\"\'\'"));
	}

	@Test
	public void testEscapePTag() {
		assertEquals("&lt;p&gt;", EscapeUtils.escapeSpecialChars("<p>"));
		assertEquals("&lt;p&gt;", EscapeUtils.escapeSpecialCharsLeavinggBold("<p>"));
	}

	@Test
	public void testEscapeLowerBTag() {
		assertEquals("&lt;b&gt;", EscapeUtils.escapeSpecialChars("<b>"));
		assertEquals("<b>", EscapeUtils.escapeSpecialCharsLeavinggBold("<b>"));
	}

	@Test
	public void testEscapeUpperBTag() {
		assertEquals("&lt;B&gt;", EscapeUtils.escapeSpecialChars("<B>"));
		assertEquals("<B>", EscapeUtils.escapeSpecialCharsLeavinggBold("<B>"));
	}

	@Test
	public void testEscapeClosingBTag() {
		assertEquals("&lt;/b&gt;", EscapeUtils.escapeSpecialChars("</b>"));
		assertEquals("</b>", EscapeUtils.escapeSpecialCharsLeavinggBold("</b>"));
		assertEquals("&lt;/B&gt;", EscapeUtils.escapeSpecialChars("</B>"));
		assertEquals("</B>", EscapeUtils.escapeSpecialCharsLeavinggBold("</B>"));
	}

	@Test
	public void testEscapeLabelEmpty() {
		assertEquals("", ViewUtilities.escapeForLabel(""));
	}

	@Test
	public void testEscapeLabelNonEmpty() {
		assertEquals("abc", ViewUtilities.escapeForLabel("abc"));
	}

	@Test
	public void testEscapeLabelWithAmpersand() {
		assertEquals("ab&&c", ViewUtilities.escapeForLabel("ab&c"));
	}

	@Test
	public void testEscapeLabelMultipleAmpersand() {
		assertEquals("a&&b&&cd&&e", ViewUtilities.escapeForLabel("a&b&cd&e"));
	}

}
