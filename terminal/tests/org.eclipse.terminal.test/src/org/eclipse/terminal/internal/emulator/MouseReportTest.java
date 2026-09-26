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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** How a mouse event is spelled for the program, in xterm's two encodings. */
public class MouseReportTest {

	private static final String CSI = "[";

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
	}
}
