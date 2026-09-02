/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.view.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.eclipse.terminal.view.ui.internal.InputChunks;
import org.junit.jupiter.api.Test;

public class InputChunksTest {

	private static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	public void testWritesDoNotCutAnEscapeSequence() {
		// the end marker of a bracketed paste across the 1000th byte goes whole into the next write
		byte[] cut = bytes("x".repeat(996) + "\033[201~more");
		assertEquals(996, InputChunks.writeLength(cut, 0, 1000));
		assertEquals(10, InputChunks.writeLength(cut, 996, 1000));
		// one that ends before the cut stays in this write
		assertEquals(1000, InputChunks.writeLength(bytes("x".repeat(990) + "\033[201~" + "y".repeat(20)), 0, 1000));
		// an arrow key in application mode, ESC O A
		assertEquals(998, InputChunks.writeLength(bytes("x".repeat(998) + "\033OAz"), 0, 1000));
	}

	@Test
	public void testWritesDoNotCutACharacter() {
		// the 1000th byte is inside a three byte character: the write stops before it
		assertEquals(999, InputChunks.writeLength(bytes("x".repeat(999) + "한" + "y"), 0, 1000));
		assertEquals(1000, InputChunks.writeLength(bytes("x".repeat(997) + "한" + "y"), 0, 1000));
	}

	@Test
	public void testShortWritesAndLongSequences() {
		assertEquals(5, InputChunks.writeLength(bytes("hello"), 0, 1000));
		// nothing to cut at: the write is as long as allowed rather than empty
		assertEquals(4, InputChunks.writeLength(bytes("\033[" + "1".repeat(10)), 0, 4));
	}
}
