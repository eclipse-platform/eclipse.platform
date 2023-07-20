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

package org.eclipse.ua.tests.cheatsheet.composite;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.ui.internal.cheatsheets.composite.parser.MarkupParser;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class TestMarkupParser {

	private Document readString(String input) {
		StringReader reader = new StringReader(input);
		InputSource source = new InputSource(reader);

		try {
			return LocalEntityResolver.parse(source);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private String parse(String input) {
		return MarkupParser.parseAndTrimTextMarkup(readString(input).getFirstChild());
	}

	@Test
	public void testParseEmptyString() {
		assertEquals("", parse("<root></root>"));
	}

	@Test
	public void testParseTags() {
		assertEquals("a<b>c</b>d<b>e</b>", parse("<root>a<b>c</b>d<b>e</b></root>"));
	}

	@Test
	public void testParseNestedTags() {
		assertEquals("<p>a<b>c</b>d</p>", parse("<root><p>a<b>c</b>d</p></root>"));
	}

	@Test
	public void testEscape() {
		assertEquals("a&lt;b&gt;c", parse("<root>a&lt;b&gt;c</root>"));
	}

	@Test
	public void testEscapeAmpersand() {
		assertEquals("a&amp;c", parse("<root>a&amp;c</root>"));
	}

	@Test
	public void testNoEscapeQuotes() {
		assertEquals("a'b'\"c\"", parse("<root>a'b'\"c\"</root>"));
	}

	@Test
	public void testAttributes() {
		assertEquals("a<b attr1 = \"true\" attr2 = \"false\">c</b>d<b>e</b>",
				parse("<root>a<b attr1 = \"true\" attr2=\"false\">c</b>d<b>e</b></root>"));
	}

	@Test
	public void testCreateParagraphEmptyString() {
		assertEquals("<p></p>", MarkupParser.createParagraph("", null));
	}

	@Test
	public void testCreateParagraphNoTags() {
		assertEquals("<p>abc</p>", MarkupParser.createParagraph("abc", null));
	}

	@Test
	public void testCreateParagraphTag_p() {
		assertEquals("<p>abc</p>", MarkupParser.createParagraph("<p>abc</p>", null));
	}

	@Test
	public void testCreateParagraphTag_br() {
		assertEquals("<p><br>abc</p>", MarkupParser.createParagraph("<br>abc", null));
	}

	@Test
	public void testCreateParagraphTag_li() {
		assertEquals("<li>abc</li>", MarkupParser.createParagraph("<li>abc</li>", null));
	}

	@Test
	public void testCreateParagraphWithImage_li() {
		assertEquals("<p><img href=\"def\"/> abc</p>", MarkupParser.createParagraph("abc", "def"));
	}

}
