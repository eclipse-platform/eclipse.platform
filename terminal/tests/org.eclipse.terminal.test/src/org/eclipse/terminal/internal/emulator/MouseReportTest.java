/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.emulator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.swt.graphics.Point;
import org.junit.jupiter.api.Test;

/** How a mouse event is spelled for the program, in xterm's two encodings. */
public class MouseReportTest {

	private static final String CSI = "\u001b[";

	@Test
	public void testSgrEncoding() {
		// CSI < button ; column ; row M for a press, m for a release, 1-based
		assertEquals(CSI + "<0;10;5M", VT100TerminalControl.mouseReport(true, 0, 5, 10, true));
		assertEquals(CSI + "<0;10;5m", VT100TerminalControl.mouseReport(true, 0, 5, 10, false));
		assertEquals(CSI + "<2;10;5M", VT100TerminalControl.mouseReport(true, 2, 5, 10, true));
		assertEquals(CSI + "<32;10;5M", VT100TerminalControl.mouseReport(true, 32, 5, 10, true)); // drag
		assertEquals(CSI + "<64;10;5M", VT100TerminalControl.mouseReport(true, 64, 5, 10, true)); // wheel up
		assertEquals(CSI + "<16;10;5M", VT100TerminalControl.mouseReport(true, 0 + 16, 5, 10, true)); // control
		assertEquals(CSI + "<72;10;5M", VT100TerminalControl.mouseReport(true, 64 + 8, 5, 10, true)); // alt-wheel
		// room for any column, which the older encoding has not
		assertEquals(CSI + "<0;300;5M", VT100TerminalControl.mouseReport(true, 0, 5, 300, true));
	}

	@Test
	public void testX10Encoding() {
		// CSI M then button, column and row each offset by 32
		assertEquals(CSI + "M" + (char) 32 + (char) 42 + (char) 37,
				VT100TerminalControl.mouseReport(false, 0, 5, 10, true));
		// a release says only that some button came up
		assertEquals(CSI + "M" + (char) 35 + (char) 42 + (char) 37,
				VT100TerminalControl.mouseReport(false, 0, 5, 10, false));
		// with the keys held kept, as in the press: alt and left up is 3 + 8
		assertEquals(CSI + "M" + (char) (32 + 11) + (char) 42 + (char) 37,
				VT100TerminalControl.mouseReport(false, 0 + 8, 5, 10, false));
		assertEquals(CSI + "M" + (char) (32 + 19) + (char) 42 + (char) 37,
				VT100TerminalControl.mouseReport(false, 2 + 16, 5, 10, false));
		// no room past column or row 223: nothing is reported, as in xterm
		assertEquals("", VT100TerminalControl.mouseReport(false, 0, 5, 224, true));
		assertEquals("", VT100TerminalControl.mouseReport(false, 0, 224, 10, true));
	}

	@Test
	public void testReportsAreBytes() {
		// column 100 is the one byte 132, not the two UTF-8 would make of it
		assertArrayEquals(new byte[] { 27, '[', 'M', 32, (byte) 132, 37 },
				VT100TerminalControl.mouseReportBytes(VT100TerminalControl.mouseReport(false, 0, 5, 100, true)));
		assertArrayEquals("\u001b[<0;100;5M".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
				VT100TerminalControl.mouseReportBytes(VT100TerminalControl.mouseReport(true, 0, 5, 100, true)));
	}

	@Test
	public void testCellsOnTheScreen() {
		// 100 lines of text, the last 24 of them the screen: line 76 is its first line
		assertEquals(new Point(1, 1), VT100TerminalControl.onScreen(new Point(0, 76), 100, 24, 80));
		assertEquals(new Point(10, 24), VT100TerminalControl.onScreen(new Point(9, 99), 100, 24, 80));
		// the history above it is not the program's
		assertNull(VT100TerminalControl.onScreen(new Point(0, 75), 100, 24, 80));
		// the strip under the last line and the margin right of the last column are the last ones
		assertEquals(new Point(80, 24), VT100TerminalControl.onScreen(new Point(85, 100), 100, 24, 80));
		// no history: the text is the screen
		assertEquals(new Point(3, 2), VT100TerminalControl.onScreen(new Point(2, 1), 24, 24, 80));
	}
}
