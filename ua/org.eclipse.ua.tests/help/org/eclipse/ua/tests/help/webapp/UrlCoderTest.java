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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.help.internal.util.URLCoder;
import org.junit.Test;

public class UrlCoderTest {

	private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-={}[]:\";'<>,.?/'";
	private static final String ALPHANUMERIC = "Bxz91r";
	private static final String ALPHA = "acgrdft";
	private static final String EMPTY_STRING = "";
	private static final String ACCENTED = "seg\u00FAn cu\u00E1l oto\u00F1o";
	private static final String CHINESE = "\u4ECA\u5929\u662F\u5929";

	private void encodeDecode(String value) {
		String encoded = URLCoder.encode(value);
		assertEquals(value, URLCoder.decode(encoded));
	}

	private void compactEncodeDecode(String value) {
		String encoded = URLCoder.compactEncode(value);
		assertEquals(value, URLCoder.decode(encoded));
	}

	private boolean compactEncodingIsShorter(String value) {
		String compactEncoded = URLCoder.compactEncode(value);
		String encoded = URLCoder.encode(value);
		return compactEncoded.length() < encoded.length();
	}

	@Test
	public void testEncodeEmpty() {
		encodeDecode(EMPTY_STRING);
	}

	@Test
	public void testEncodeAlphabetic() {
		encodeDecode(ALPHA);
	}

	@Test
	public void testEncodeAlphaNumeric() {
		encodeDecode(ALPHANUMERIC);
	}

	@Test
	public void testEncodeSpecialCharacters() {
		encodeDecode(SPECIAL_CHARACTERS);
	}

	@Test
	public void testEncodeAccented() {
		encodeDecode(ACCENTED);
	}

	@Test
	public void testEncodeChinese() {
		encodeDecode(CHINESE);
	}

	// Compact Encodings
	@Test
	public void testCompactEncodeEmpty() {
		compactEncodeDecode(EMPTY_STRING);
	}

	@Test
	public void testCompactEncodeAlphabetic() {
		compactEncodeDecode(ALPHA);
	}

	@Test
	public void testCompactEncodeAlphaNumeric() {
		compactEncodeDecode(ALPHANUMERIC);
	}

	@Test
	public void testCompactEncodeSpecialCharacters() {
		compactEncodeDecode(SPECIAL_CHARACTERS);
	}

	@Test
	public void testCompactEncodeAccented() {
		compactEncodeDecode(ACCENTED);
	}

	@Test
	public void testCompactEncodeChinese() {
		compactEncodeDecode(CHINESE);
	}

	// Verify compaction

	@Test
	public void testCompactionEmpty() {
		assertFalse(compactEncodingIsShorter(EMPTY_STRING));
	}

	@Test
	public void testCompactionAlphabetic() {
		assertTrue(compactEncodingIsShorter(ALPHA));
	}

	@Test
	public void testCompactionAlphaNumeric() {
		assertTrue(compactEncodingIsShorter(ALPHANUMERIC));
	}

	@Test
	public void testCompactionSpecialCharacters() {
		assertTrue(compactEncodingIsShorter(SPECIAL_CHARACTERS));
	}

	@Test
	public void testCompactionAccented() {
		assertTrue(compactEncodingIsShorter(ACCENTED));
	}

	@Test
	public void testCompactionChinese() {
		assertFalse(compactEncodingIsShorter(CHINESE));
	}

}
