/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CharWidthTest {

	@Test
	public void testNarrow() {
		assertEquals(1, CharWidth.of('A'));
		assertEquals(1, CharWidth.of(0x00E9)); // e with acute
		assertEquals(1, CharWidth.of(0x00B1)); // ambiguous: plus-minus
		assertEquals(1, CharWidth.of(0x2500)); // ambiguous: box drawing
		assertEquals(1, CharWidth.of(0xFFFD)); // ambiguous: replacement character
	}

	@Test
	public void testWide() {
		assertEquals(2, CharWidth.of(0xAC00)); // hangul syllable
		assertEquals(2, CharWidth.of(0x6F22)); // han ideograph
		assertEquals(2, CharWidth.of(0xFF21)); // fullwidth A
		assertEquals(2, CharWidth.of(0x1100)); // hangul jamo, leading consonant
		assertEquals(2, CharWidth.of(0x3131)); // hangul compatibility jamo
		assertEquals(2, CharWidth.of(0x3000)); // ideographic space
		assertEquals(2, CharWidth.of(0x1F600)); // emoji
	}

	@Test
	public void testZeroWidth() {
		assertEquals(0, CharWidth.of(0x0301)); // combining acute
		assertEquals(0, CharWidth.of(0x200B)); // zero width space
		assertEquals(0, CharWidth.of(0x1161)); // hangul jamo vowel, combines with the consonant before it
		assertEquals(0, CharWidth.of('\n'));
		assertEquals(0, CharWidth.of(0));
		assertEquals(0, CharWidth.of(0x7F));
		assertEquals(0, CharWidth.of(0x85)); // C1 control
	}

	@Test
	public void testOfString() {
		assertEquals(0, CharWidth.ofString(""));
		assertEquals(3, CharWidth.ofString("abc"));
		assertEquals(7, CharWidth.ofString("한글abc")); // two hangul syllables, three letters
		assertEquals(4, CharWidth.ofString("a😀b")); // surrogate pair counts once, as two cells
		assertEquals(1, CharWidth.ofString("é")); // combining mark adds nothing
	}

	@Test
	public void testIsFiller() {
		assertTrue(CharWidth.isFiller("가\000", 1));
		assertTrue(CharWidth.isFiller("😀\000", 2));
		assertFalse(CharWidth.isFiller("a\000", 1)); // an empty cell after a narrow character
		assertFalse(CharWidth.isFiller("\000a", 0));
		assertFalse(CharWidth.isFiller("ab", 1));
		assertFalse(CharWidth.isFiller("가\000\000", 2)); // only the first null is the filler
	}
}
