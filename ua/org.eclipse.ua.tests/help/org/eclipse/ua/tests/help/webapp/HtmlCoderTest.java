/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.junit.Test;

public class HtmlCoderTest {
	@Test
	public void testEncodeEmpty() {
		String encoded = UrlUtil.htmlEncode(null);
		assertNull(encoded);
	}

	/**
	 * Verify that alpha characters are not encoded
	 */
	@Test
	public void testEncodeAlpha() {
		final String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String encoded = UrlUtil.htmlEncode(letters);
		assertEquals(letters, encoded);
	}

	/**
	 * Verify that alpha characters are not encoded
	 */
	@Test
	public void testEncodeNumeric() {
		final String numbers = "1234567890";
		String encoded = UrlUtil.htmlEncode(numbers);
		assertEquals(numbers, encoded);
	}

	/**
	 * Verify that space is not encoded
	 */
	@Test
	public void testEncodeSpace() {
		final String spaces = "  ";
		String encoded = UrlUtil.htmlEncode(spaces);
		assertEquals(spaces, encoded);
	}

	/**
	 * Verify that quote is encoded
	 */
	@Test
	public void testEncodeQuote() {
		final String source = "\'";
		String encoded = UrlUtil.htmlEncode(source);
		assertNotSame(source, encoded);
	}

	/**
	 * Verify that less than is encoded
	 */
	@Test
	public void testEncodeLt() {
		final String source = "<";
		String encoded = UrlUtil.htmlEncode(source);
		assertNotSame(source, encoded);
	}

	/**
	 * Verify that greater than is encoded
	 */
	@Test
	public void testEncodeGt() {
		final String source = ">";
		String encoded = UrlUtil.htmlEncode(source);
		assertNotSame(source, encoded);
	}

	/**
	 * Verify that ampersand is encoded
	 */
	@Test
	public void testEncodeAmp() {
		final String source = "&";
		String encoded = UrlUtil.htmlEncode(source);
		assertNotSame(source, encoded);
	}

	/**
	 * Verify that ampersand is encoded
	 */
	@Test
	public void testEncodeBackslash() {
		final String source = "\\";
		String encoded = UrlUtil.htmlEncode(source);
		assertNotSame(source, encoded);
	}

	/**
	 * Verify that newline is encoded
	 */
	@Test
	public void testEncodeNewline() {
		final String source = "\n";
		String encoded = UrlUtil.htmlEncode(source);
		assertNotSame(source, encoded);
	}

	/**
	 * Verify that CR is encoded
	 */
	@Test
	public void testEncodeCarriageReturn() {
		final String source = "\r";
		String encoded = UrlUtil.htmlEncode(source);
		assertNotSame(source, encoded);
	}

	/**
	 * Verify that accented character is not encoded
	 */
	@Test
	public void testNoEncodeAccented() {
		final String source = "\u00c1";
		String encoded = UrlUtil.htmlEncode(source);
		assertEquals(source, encoded);
	}

	/**
	 * Verify that Chinese character is not encoded
	 */
	@Test
	public void testNoEncodeChinese() {
		final String source = "\u4e01";
		String encoded = UrlUtil.htmlEncode(source);
		assertEquals(source, encoded);
	}

}
