/*******************************************************************************
 * Copyright (c) 2020, 2025 Kichwa Coders Canada Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.terminal.model;

import static org.eclipse.terminal.model.TerminalColor.BLACK;
import static org.eclipse.terminal.model.TerminalColor.BLUE;
import static org.eclipse.terminal.model.TerminalColor.CYAN;
import static org.eclipse.terminal.model.TerminalColor.GREEN;
import static org.eclipse.terminal.model.TerminalColor.MAGENTA;
import static org.eclipse.terminal.model.TerminalColor.RED;
import static org.eclipse.terminal.model.TerminalColor.WHITE;
import static org.eclipse.terminal.model.TerminalColor.YELLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * This is a UI test because {@link TerminalColor#convertColor(boolean, boolean)}
 * requires a Display to operate the ColorRegistry.
 */
public class TerminalColorUITest {

	private static Display display = null;

	@BeforeAll
	public static void createDisplay() {
		Display current = Display.getCurrent();
		if (current == null) {
			display = new Display();
		}
	}

	@AfterAll
	public static void disposeDisplay() {
		if (display != null) {
			display.dispose();
		}
	}

	@Test
	public void testInversionsStandard() {

		assertEquals(BLACK.convertColor(false, false), WHITE.convertColor(true, false));
		assertNotEquals(BLACK.convertColor(false, false), WHITE.convertColor(false, false));

		assertEquals(RED.convertColor(false, false), RED.convertColor(true, false));
		assertEquals(GREEN.convertColor(false, false), GREEN.convertColor(true, false));
		assertEquals(YELLOW.convertColor(false, false), YELLOW.convertColor(true, false));
		assertEquals(BLUE.convertColor(false, false), BLUE.convertColor(true, false));
		assertEquals(MAGENTA.convertColor(false, false), MAGENTA.convertColor(true, false));
		assertEquals(CYAN.convertColor(false, false), CYAN.convertColor(true, false));

		assertEquals(WHITE.convertColor(false, false), BLACK.convertColor(true, false));
		assertNotEquals(WHITE.convertColor(false, false), BLACK.convertColor(false, false));

	}

	@Test
	public void testInversionsBright() {
		assertEquals(BLACK.convertColor(false, true), WHITE.convertColor(true, true));
		assertNotEquals(BLACK.convertColor(false, true), WHITE.convertColor(false, true));

		assertEquals(RED.convertColor(false, true), RED.convertColor(true, true));
		assertEquals(GREEN.convertColor(false, true), GREEN.convertColor(true, true));
		assertEquals(YELLOW.convertColor(false, true), YELLOW.convertColor(true, true));
		assertEquals(BLUE.convertColor(false, true), BLUE.convertColor(true, true));
		assertEquals(MAGENTA.convertColor(false, true), MAGENTA.convertColor(true, true));
		assertEquals(CYAN.convertColor(false, true), CYAN.convertColor(true, true));

		assertEquals(WHITE.convertColor(false, true), BLACK.convertColor(true, true));
		assertNotEquals(WHITE.convertColor(false, true), BLACK.convertColor(false, true));
	}

	@Test
	public void testIndexesResolveToStandardColors() {
		// check explicit colors
		assertEquals(TerminalColor.BLACK.convertColor(false, false),
				TerminalColor.getIndexedTerminalColor(0).convertColor(false, false));
		assertEquals(TerminalColor.RED.convertColor(false, false),
				TerminalColor.getIndexedTerminalColor(1).convertColor(false, false));

		// Now check all colors
		for (int i = 0; i < 8; i++) {
			assertEquals(TerminalColor.values()[i].convertColor(false, false),
					TerminalColor.getIndexedTerminalColor(i).convertColor(false, false));
		}
	}

	@Test
	public void testIndexesResolveToBrightColors() {
		// check explicit colors
		assertEquals(TerminalColor.BLACK.convertColor(false, true),
				TerminalColor.getIndexedTerminalColor(8).convertColor(false, false));
		assertEquals(TerminalColor.RED.convertColor(false, true),
				TerminalColor.getIndexedTerminalColor(9).convertColor(false, false));

		// Now check all colors
		for (int i = 0; i < 8; i++) {
			assertEquals(TerminalColor.values()[i].convertColor(false, true),
					TerminalColor.getIndexedTerminalColor(i + 8).convertColor(false, false));
		}
	}

	@Test
	public void testIndexesInRange() {
		for (int i = 0; i < 16; i++) {
			assertNotNull(TerminalColor.getIndexedTerminalColor(i));
		}
		for (int i = 16; i < 256; i++) {
			assertNotNull(TerminalColor.getIndexedRGBColor(i));
		}
	}

	@Test
	public void testIndexesOutOfRange_m1TerminalColor() {
		assertThrows(IllegalArgumentException.class, () -> TerminalColor.getIndexedTerminalColor(-1));
	}

	@Test
	public void testIndexesOutOfRange_m1RGBColor() {
		assertThrows(IllegalArgumentException.class, () -> TerminalColor.getIndexedRGBColor(-1));
	}

	@Test
	public void testIndexesOutOfRange_16() {
		assertThrows(IllegalArgumentException.class, () -> TerminalColor.getIndexedTerminalColor(16));
	}

	@Test
	public void testIndexesOutOfRange_15() {
		assertThrows(IllegalArgumentException.class, () -> TerminalColor.getIndexedRGBColor(15));
	}

	@Test
	public void testIndexesOutOfRange_256TerminalColor() {
		assertThrows(IllegalArgumentException.class, () -> TerminalColor.getIndexedTerminalColor(256));
	}

	@Test
	public void testIndexesOutOfRange_256RGBColor() {
		assertThrows(IllegalArgumentException.class, () -> TerminalColor.getIndexedRGBColor(256));
	}

}
