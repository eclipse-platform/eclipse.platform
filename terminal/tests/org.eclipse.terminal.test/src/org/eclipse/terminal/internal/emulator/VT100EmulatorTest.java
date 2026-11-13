/*******************************************************************************
 * Copyright (c) 2021 Kichwa Coders Canada Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.emulator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.eclipse.swt.SWT;
import org.eclipse.terminal.connector.Logger;
import org.eclipse.terminal.model.ITerminalTextData;
import org.eclipse.terminal.model.TerminalStyle;
import org.eclipse.terminal.model.TerminalTextDataFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VT100EmulatorTest {

	private static final int WINDOW_COLUMNS = 80;
	private static final int WINDOW_LINES = 24;
	private static final String CLEAR_CURSOR_TO_EOL = "\033[K";
	private static final String CURSOR_POSITION_TOP_LEFT = "\033[H";
	private static final String CLEAR_ENTIRE_SCREEN_AND_SCROLLBACK = "\033[3J";
	private static final String CLEAR_ENTIRE_SCREEN = "\033[2J";
	private static final String SCROLL_REVERSE = "\033M";

	private static String TITLE_ST_BEL(String title) {
		return "\033]0;" + title + "\007";
	}

	private static String TITLE_ST_ESC_BACKSLASH(String title) {
		return "\033]0;" + title + "\033\\";
	}

	private static String TITLE(String title) {
		return TITLE_ST_BEL(title);
	}

	private static String SCROLL_REGION(int startRow, int endRow) {
		return "\033[" + startRow + ";" + endRow + "r";
	}

	private static final String ALTERNATE_SCREEN_ON = "\u001b[?1049h";
	private static final String ALTERNATE_SCREEN_OFF = "\u001b[?1049l";

	/**
	 * Set the cursor position to line/column. Note that this is the logical
	 * line and column, so 1, 1 is the top left.
	 */

	private static String CURSOR_POSITION(int line, int column) {
		return "\033[" + line + ";" + column + "H";
	}

	@BeforeAll
	public static void beforeAll() {
		Logger.setUnderTest(true);
	}

	@AfterAll
	public static void afterAll() {
		Logger.setUnderTest(false);
	}

	private ITerminalTextData data;

	private MockTerminalControlForText control = new MockTerminalControlForText();

	private VT100Emulator emulator;

	@BeforeEach
	public void before() {
		data = TerminalTextDataFactory.makeTerminalTextData();
		emulator = new VT100Emulator(data, control, null);
		emulator.resetState();
		emulator.setDimensions(WINDOW_LINES, WINDOW_COLUMNS);
	}

	private Reader input(String... input) {
		StringReader reader = new StringReader(String.join("", input));
		emulator.setInputStreamReader(reader);
		return reader;
	}

	private void run(String... input) {
		Reader reader = input(input);
		emulator.processText();
		try {
			assertEquals(-1, reader.read());
		} catch (IOException e) {
			throw new RuntimeException("Wrap exception so that run can be called in functions", e);
		}
	}

	/**
	 * Convert the data's char arrays into a string that can be compared with
	 * an expected array of lines. Each line in the data has its \0 characters
	 * changed to spaces and then stripTrailing is run.
	 *
	 * @param expectedArray lines that are joined with \n before testing against actual
	 */
	private void assertTextEquals(String... expectedArray) {
		int height = data.getHeight();
		StringJoiner sj = new StringJoiner("\n");
		for (int i = 0; i < height; i++) {
			char[] chars = data.getChars(i);
			String line = chars == null ? "" : new String(chars);
			String lineCleanedup = line.replace('\0', ' ');
			String stripTrailing = lineCleanedup.stripTrailing();
			sj.add(stripTrailing);
		}
		String expected = String.join("\n", expectedArray).stripTrailing();
		String actual = sj.toString().stripTrailing();
		assertEquals(expected, actual);
	}

	private void assertTextEquals(List<String> expected) {
		assertTextEquals(expected.toArray(String[]::new));
	}

	private void assertCursorLocation(int line, int column) {
		assertAll(() -> assertEquals(line, data.getCursorLine(), "cursor line"),
				() -> assertEquals(column, data.getCursorColumn(), "cursor column"));
	}

	/**
	 * This tests the test harness ({@link #assertTextEquals(String...)} as much as the code.
	 */
	@Test
	public void testBasicOperaiion() {
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals(""));
		run("Hello");
		assertAll(() -> assertCursorLocation(0, 5), () -> assertTextEquals("Hello"));
		emulator.clearTerminal();
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals(""));

		// test multiline
		emulator.clearTerminal();
		run("Hello 1\r\nHello 2");
		// test both ways of asserting multiple lines
		assertAll(() -> assertCursorLocation(1, 7), //
				() -> assertTextEquals("Hello 1\nHello 2"), //
				() -> assertTextEquals("Hello 1", "Hello 2"));

		// test with no carriage return
		emulator.clearTerminal();
		run("Hello 1\nHello 2");
		assertTextEquals("Hello 1", "       Hello 2");

		// test \b backspace
		emulator.clearTerminal();
		run("Hello 1");
		assertAll(() -> assertCursorLocation(0, 7), () -> assertTextEquals("Hello 1"));
		run("\b\b");
		assertAll(() -> assertCursorLocation(0, 5), () -> assertTextEquals("Hello 1"));
		run(CLEAR_CURSOR_TO_EOL);
		assertAll(() -> assertCursorLocation(0, 5), () -> assertTextEquals("Hello"));
	}

	@Test
	public void testMultiline() {
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < data.getHeight(); i++) {
			String line = "Hello " + i;
			expected.add(line);
			run(line);
			if (i != data.getHeight() - 1) {
				run("\r\n");
			}
		}
		assertTextEquals(expected);

		// add the final newline and check that the first line has been scrolled away
		run("\r\n");
		expected.remove(0);
		assertTextEquals(expected);
	}

	@Test
	public void testScrollBack() {
		data.setMaxHeight(1000);
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			String line = "Hello " + i;
			run(line + "\r\n");
			expected.add(line);
		}
		expected.remove(0);
		assertTextEquals(expected);
	}

	@Test
	public void testCursorPosition() {
		run(CURSOR_POSITION_TOP_LEFT);
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals(""));
		run("Hello");
		assertAll(() -> assertCursorLocation(0, 5), () -> assertTextEquals("Hello"));
		run(CURSOR_POSITION_TOP_LEFT);
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals("Hello"));
		run(CURSOR_POSITION(2, 2));
		assertAll(() -> assertCursorLocation(1, 1), () -> assertTextEquals("Hello"));
		emulator.clearTerminal();

		data.setMaxHeight(1000);
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < WINDOW_LINES; i++) {
			String line = "Hello " + i;
			run(line + "\r\n");
			expected.add(line);
		}
		assertAll(() -> assertCursorLocation(WINDOW_LINES, 0), () -> assertTextEquals(expected));
		run(CURSOR_POSITION_TOP_LEFT);
		// because we added WINDOW_LINES number of lines, and ended it with a \r\n the first
		// line we added is now in the scrollback, so the cursor is at line 1
		assertAll(() -> assertCursorLocation(1, 0), () -> assertTextEquals(expected));
		run("Bye           \r\n");
		expected.set(1, "Bye");
		assertAll(() -> assertCursorLocation(2, 0), () -> assertTextEquals(expected));
		run(CURSOR_POSITION_TOP_LEFT);
		assertAll(() -> assertCursorLocation(1, 0), () -> assertTextEquals(expected));
		run(CURSOR_POSITION(2, 2));
		assertAll(() -> assertCursorLocation(2, 1), () -> assertTextEquals(expected));
	}

	@Test
	public void testTitle() {
		run( //
				TITLE("TITLE1"), //
				"HELLO", //
				TITLE("TITLE2"));
		assertAll(() -> assertTextEquals("HELLO"),
				() -> assertEquals(List.of("TITLE1", "TITLE2"), control.getAllTitles()));
	}

	@Test
	public void testE3ClearScreenAndScrollback() {
		data.setMaxHeight(1000);
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			String line = "Hello " + i;
			run(line + "\r\n");
			expected.add(line);
		}
		expected.remove(0);
		assertAll(() -> assertCursorLocation(999, 0), () -> assertTextEquals(expected));
		run(CLEAR_ENTIRE_SCREEN_AND_SCROLLBACK);
		assertAll(() -> assertTextEquals(""));
	}

	/**
	 * Runs what "clear" command does on modern Linux installs, including E3 extension
	 */
	@Test
	public void testClear() {
		data.setMaxHeight(1000);
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			String line = "Hello " + i;
			run(line + "\r\n");
			expected.add(line);
		}
		expected.remove(0);
		assertAll(() -> assertCursorLocation(999, 0), () -> assertTextEquals(expected));
		run(CURSOR_POSITION_TOP_LEFT + CLEAR_ENTIRE_SCREEN + CLEAR_ENTIRE_SCREEN_AND_SCROLLBACK);
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals(""));
	}

	@Test
	public void testClearScreenKeepsItInHistory() {
		data.setMaxHeight(1000);
		for (int i = 0; i < 30; i++) {
			run("Hello " + i + "\r\n");
		}
		// what ConPTY sends at the start of every connection on Windows
		run(CLEAR_ENTIRE_SCREEN + "\u001b[m" + CURSOR_POSITION_TOP_LEFT + "prompt$ ");
		int top = data.getHeight() - WINDOW_LINES; // first line of the screen
		assertAll(() -> assertEquals("Hello 29", new String(data.getChars(top - 1)).trim()),
				() -> assertEquals("Hello 0", new String(data.getChars(0)).trim()),
				() -> assertEquals("prompt$", new String(data.getChars(top)).replace('\000', ' ').trim()),
				() -> assertCursorLocation(top, 8)); // the top of the screen, counted in the buffer
	}

	/**
	 * Runs what "up arrow" would send back to terminal in less/man/etc.
	 */
	@Test
	public void testScrollReverseNoScrollback() {
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < WINDOW_LINES; i++) {
			String line = "Hello " + i;
			run(line);
			expected.add(line);
			if (i != data.getHeight() - 1) {
				run("\r\n");
			}
		}
		assertAll(() -> assertCursorLocation(WINDOW_LINES - 1, expected.get(expected.size() - 1).length()),
				() -> assertTextEquals(expected));
		run(CURSOR_POSITION_TOP_LEFT);
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals(expected));
		run(SCROLL_REVERSE);
		expected.add(0, "");
		expected.remove(expected.size() - 1);
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals(expected));
	}

	/**
	 * Runs what "up arrow" would send back to terminal in less/man/etc.
	 */
	@Test
	public void testScrollReverse() {
		data.setMaxHeight(1000);
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < WINDOW_LINES; i++) {
			String line = "Hello " + i;
			run(line);
			expected.add(line);
			run("\r\n");
		}
		assertAll(() -> assertCursorLocation(WINDOW_LINES, 0), () -> assertTextEquals(expected));
		run(CURSOR_POSITION_TOP_LEFT);
		assertAll(() -> assertCursorLocation(1, 0), () -> assertTextEquals(expected));
		run(SCROLL_REVERSE);
		expected.add(1, "");
		assertAll(() -> assertCursorLocation(1, 0), () -> assertTextEquals(expected));
		run("New text on top line following scroll reverse");
		expected.set(1, "New text on top line following scroll reverse");
		assertAll(() -> assertCursorLocation(1, expected.get(1).length()), () -> assertTextEquals(expected));
	}

	/**
	 * Runs what "up arrow" would send back to terminal in less/man/etc.
	 * but with a scrolling region set
	 */
	@Test
	public void testScrollReverseScrollingRegion() {
		data.setMaxHeight(1000);
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < WINDOW_LINES; i++) {
			String line = "Hello " + i;
			run(line);
			expected.add(line);
			run("\r\n");
		}
		assertAll(() -> assertCursorLocation(WINDOW_LINES, 0), () -> assertTextEquals(expected));
		run(CURSOR_POSITION_TOP_LEFT + "\n");
		assertAll(() -> assertCursorLocation(2, 0), () -> assertTextEquals(expected));
		run(SCROLL_REGION(2, WINDOW_LINES));
		run(SCROLL_REVERSE);
		expected.add(2, "");
		assertAll(() -> assertCursorLocation(2, 0), () -> assertTextEquals(expected));
		run("New text on top line following scroll reverse");
		expected.set(2, "New text on top line following scroll reverse");
		assertAll(() -> assertCursorLocation(2, expected.get(2).length()), () -> assertTextEquals(expected));
	}

	@Test
	public void testStringTerminator() {
		run( //
				TITLE_ST_BEL("TITLE1"), //
				"HELLO1", //
				TITLE_ST_ESC_BACKSLASH("TITLE2"), //
				"HELLO2", //
				TITLE_ST_BEL("TITLE3"), //
				"HELLO3", //
				TITLE_ST_ESC_BACKSLASH("TITLE4") //
		);
		assertAll(() -> assertTextEquals("HELLO1HELLO2HELLO3"),
				() -> assertEquals(List.of("TITLE1", "TITLE2", "TITLE3", "TITLE4"), control.getAllTitles()));
	}

	@Test
	public void testMalformedStringTerminator() {
		run( //
				TITLE_ST_ESC_BACKSLASH("TITLE1"), //
				"\033]0;" + "TITLE1" + "\033X", //
				TITLE_ST_ESC_BACKSLASH("TITLE2"), //
				"HELLO" //
		);
		assertAll(() -> assertTextEquals("HELLO"),
				() -> assertEquals(List.of("TITLE1", "TITLE2"), control.getAllTitles()));
	}

	@Test
	public void testAlternateScreen() {
		data.setMaxHeight(1000);
		run("Hello 1\r\nHello 2");
		assertAll(() -> assertCursorLocation(1, 7), () -> assertTextEquals("Hello 1", "Hello 2"));
		// a full screen program takes the screen: it starts blank and no history is kept
		run(ALTERNATE_SCREEN_ON);
		assertAll(() -> assertCursorLocation(0, 0), () -> assertTextEquals(""));
		assertEquals(WINDOW_LINES, data.getHeight());
		run("Full screen");
		assertTextEquals("Full screen");
		run(ALTERNATE_SCREEN_ON); // programs do ask twice
		assertTextEquals("Full screen");
		// and gives it back with what was there before, cursor included
		run(ALTERNATE_SCREEN_OFF);
		assertAll(() -> assertCursorLocation(1, 7), () -> assertTextEquals("Hello 1", "Hello 2"));
		run(ALTERNATE_SCREEN_OFF); // harmless twice as well
		assertAll(() -> assertCursorLocation(1, 7), () -> assertTextEquals("Hello 1", "Hello 2"));
	}

	@Test
	public void testResetStateLeavesAlternateScreen() {
		data.setMaxHeight(1000);
		run("Hello 1\r\nHello 2");
		run(ALTERNATE_SCREEN_ON);
		run("Full screen");
		// the connection ends while the program has the screen; the next one starts
		// on the normal screen, with its history back
		emulator.resetState();
		assertAll(() -> assertTextEquals("Hello 1", "Hello 2"), () -> assertEquals(1000, data.getMaxHeight()));
		// a late request to leave does not bring back the old program's screen
		run(ALTERNATE_SCREEN_OFF);
		assertTextEquals("Hello 1", "Hello 2");
		for (int i = 0; i < WINDOW_LINES; i++) {
			run("\r\nmore");
		}
		assertEquals(WINDOW_LINES + 2, data.getHeight()); // history accumulates again
	}

	@Test
	public void testRepeatedAlternateScreenOffKeepsMargins() {
		run("A\r\nB\r\nC");
		run(SCROLL_REGION(1, 2));
		run(ALTERNATE_SCREEN_OFF); // already on the normal screen: must change nothing
		run(CURSOR_POSITION(2, 1), "\n");
		// the newline on the bottom margin scrolls rows 1 and 2 only
		assertTextEquals("B", "", "C");
	}

	@Test
	public void testBufferLineLimitWhileAlternateScreen() {
		data.setMaxHeight(1000);
		emulator.setBufferLineLimit(300);
		assertEquals(300, data.getMaxHeight());
		run(ALTERNATE_SCREEN_ON);
		// a preference change must not give the alternate screen a history
		emulator.setBufferLineLimit(500);
		assertEquals(WINDOW_LINES, data.getMaxHeight());
		for (int i = 0; i < 2 * WINDOW_LINES; i++) {
			run("line\r\n");
		}
		assertEquals(WINDOW_LINES, data.getHeight());
		// and the new limit is the normal screen's once it is back
		run(ALTERNATE_SCREEN_OFF);
		assertEquals(500, data.getMaxHeight());
	}

	@Test
	public void testFullResetLeavesAlternateScreen() {
		data.setMaxHeight(1000);
		run("Hello 1\r\nHello 2");
		run(ALTERNATE_SCREEN_ON);
		run("Full screen");
		run("\u001bc"); // ESC c, a full reset
		assertEquals(1000, data.getMaxHeight());
		// a late request to leave does not bring back the screen from before the reset
		run("after");
		run(ALTERNATE_SCREEN_OFF);
		// the reset kept the shell's lines in the history, and the screen shows what came after
		assertEquals("after", new String(data.getChars(data.getHeight() - WINDOW_LINES), 0, 5));
		for (int line = 0; line < data.getHeight(); line++) {
			char[] chars = data.getChars(line);
			assertTrue(chars == null || !new String(chars).contains("Full screen"), "line " + line);
		}
	}

	@Test
	public void testFullResetErasesInTheDefaultBackground() {
		run("\u001b[42m", "\u001bc", "x");
		// the screen a program left green comes back in the default background
		int line = data.getHeight() - WINDOW_LINES;
		TerminalStyle erased = data.getStyle(line, 1);
		assertTrue(erased == null
				|| erased.getBackgroundTerminalColor() == data.getStyle(line, 0).getBackgroundTerminalColor());
	}

	@Test
	public void testResetStateResetsTheStyleAfterTheAlternateScreen() {
		run("\u001b[31m");
		run(ALTERNATE_SCREEN_ON);
		emulator.resetState();
		// leaving the alternate screen puts back the red; the reset after it still resets
		run("x\u001b[0my");
		int line = data.getHeight() - WINDOW_LINES;
		assertEquals(data.getStyle(line, 1), data.getStyle(line, 0));
	}

	@Test
	public void testAlternateScreenModesAndTheCursor() {
		data.setMaxHeight(1000);
		run("Hello 1\r\nHello 2");
		// 47 and 1047 move no cursor, neither in nor out: it stays where it is
		run(CURSOR_POSITION(5, 3), "\u001b[?1047h");
		assertCursorLocation(4, 2);
		run("x", "\u001b[?1047l");
		assertAll(() -> assertCursorLocation(4, 3), () -> assertTextEquals("Hello 1", "Hello 2"));
		run(CURSOR_POSITION(2, 8));
		// 1049 brings it back to where it was on the way in
		run(ALTERNATE_SCREEN_ON, CURSOR_POSITION(5, 3), "x", ALTERNATE_SCREEN_OFF);
		assertCursorLocation(1, 7);
		// ESC 7 / ESC 8 keep a slot per screen: the program's does not overwrite the shell's
		run(CURSOR_POSITION(1, 2), "\u001b7", ALTERNATE_SCREEN_ON, CURSOR_POSITION(6, 6), "\u001b7",
				CURSOR_POSITION(1, 1), "\u001b8");
		assertCursorLocation(5, 5);
		run(ALTERNATE_SCREEN_OFF, CURSOR_POSITION(3, 3), "\u001b8");
		assertCursorLocation(0, 1);
		// and 1048 saves and restores as they do
		run(CURSOR_POSITION(2, 4), "\u001b[?1048h", CURSOR_POSITION(6, 1), "\u001b[?1048l");
		assertCursorLocation(1, 3);
	}

	@Test
	public void testClearOnAlternateScreenClearsWhatIsBehindIt() {
		data.setMaxHeight(1000);
		run("secret 1\r\nsecret 2");
		run(ALTERNATE_SCREEN_ON, "Full screen");
		emulator.clearTerminal(); // Clear Terminal while the program shows
		run(ALTERNATE_SCREEN_OFF);
		// what the user cleared does not come back with the shell
		for (int line = 0; line < data.getHeight(); line++) {
			char[] chars = data.getChars(line);
			assertTrue(chars == null || !new String(chars).contains("secret"), "line " + line);
		}
		assertEquals(1000, data.getMaxHeight());
	}

	@Test
	public void testBufferLineLimitWhileOnTheAlternateScreen() {
		emulator.setBufferLineLimit(1000);
		run(ALTERNATE_SCREEN_ON);
		// the normal screen's limit, not the alternate screen's height
		assertEquals(1000, emulator.getBufferLineLimit());
		emulator.setBufferLineLimit(emulator.getBufferLineLimit()); // read and written back
		run(ALTERNATE_SCREEN_OFF);
		assertEquals(1000, data.getMaxHeight());
	}

	@Test
	public void testRedrawingOnTheAlternateScreenKeepsTheHistory() {
		data.setMaxHeight(1000);
		run("Hello 1\r\nHello 2");
		int height = data.getHeight();
		// a full screen program redraws with CSI 2 J; its frames do not go into the history
		run(ALTERNATE_SCREEN_ON);
		for (int i = 0; i < 5; i++) {
			run("\u001b[2J\u001b[H", "frame " + i);
		}
		run(ALTERNATE_SCREEN_OFF);
		assertEquals(height, data.getHeight());
		for (int line = 0; line < data.getHeight(); line++) {
			char[] chars = data.getChars(line);
			assertTrue(chars == null || !new String(chars).contains("frame"), "line " + line);
		}
		// back on the normal screen, clearing it keeps it again
		run("\u001b[2J");
		assertEquals(height + 2, data.getHeight());
	}

	@Test
	public void testFaint() {
		// SGR 2 makes the text faint; 22 takes both bold and faint off, as in xterm
		run("\u001b[2mfaint\u001b[22m normal \u001b[1;2mboth\u001b[22mplain");
		assertTrue(data.getStyle(0, 0).isDim());
		assertFalse(data.getStyle(0, 0).isBold());
		assertFalse(data.getStyle(0, 6).isDim());
		assertTrue(data.getStyle(0, 13).isDim());
		assertTrue(data.getStyle(0, 13).isBold());
		assertFalse(data.getStyle(0, 17).isDim());
		assertFalse(data.getStyle(0, 17).isBold());
		// 21 is double underline in ECMA-48, not normal intensity: it takes bold off as before, faint stays
		run("\r\n\u001b[1;2ma\u001b[21mb");
		assertTrue(data.getStyle(1, 1).isDim());
		assertFalse(data.getStyle(1, 1).isBold());
	}

	@Test
	public void testModifiedSpecialKeys() {
		// xterm's spelling: CSI number ; modifier final, with 2 shift, 3 alt, 5 control and their sums
		Map<Integer, String> keys = Map.ofEntries(Map.entry(SWT.ARROW_UP, "1;%dA"), Map.entry(SWT.ARROW_DOWN, "1;%dB"),
				Map.entry(SWT.ARROW_RIGHT, "1;%dC"), Map.entry(SWT.ARROW_LEFT, "1;%dD"), Map.entry(SWT.HOME, "1;%dH"),
				Map.entry(SWT.END, "1;%dF"), Map.entry(SWT.INSERT, "2;%d~"), Map.entry(SWT.PAGE_UP, "5;%d~"),
				Map.entry(SWT.PAGE_DOWN, "6;%d~"), Map.entry(SWT.F1, "1;%dP"), Map.entry(SWT.F2, "1;%dQ"),
				Map.entry(SWT.F3, "1;%dR"), Map.entry(SWT.F4, "1;%dS"), Map.entry(SWT.F5, "15;%d~"),
				Map.entry(SWT.F6, "17;%d~"), Map.entry(SWT.F7, "18;%d~"), Map.entry(SWT.F8, "19;%d~"),
				Map.entry(SWT.F9, "20;%d~"), Map.entry(SWT.F10, "21;%d~"), Map.entry(SWT.F11, "23;%d~"),
				Map.entry(SWT.F12, "24;%d~"));
		Map<Integer, Integer> modifiers = Map.of(SWT.SHIFT, 2, SWT.ALT, 3, SWT.CTRL, 5, SWT.CTRL | SWT.SHIFT, 6,
				SWT.CTRL | SWT.ALT | SWT.SHIFT, 8);
		keys.forEach((key, spelling) -> modifiers.forEach((mask, modifier) -> assertEquals(
				"\u001b[" + spelling.formatted(modifier), VT100TerminalControl.modifiedSpecialKey(key, mask))));
		// no modifier held that xterm can spell, or a key it does not spell this way: not this way
		assertEquals(null, VT100TerminalControl.modifiedSpecialKey(SWT.ARROW_UP, 0));
		assertEquals(null, VT100TerminalControl.modifiedSpecialKey(SWT.TAB, SWT.CTRL));
		// the Mac's Cmd, alone or with others, stays in Eclipse: Cmd+Shift+Left is not Shift+Left
		assertEquals(null, VT100TerminalControl.modifiedSpecialKey(SWT.ARROW_LEFT, SWT.COMMAND));
		assertEquals(null, VT100TerminalControl.modifiedSpecialKey(SWT.ARROW_LEFT, SWT.COMMAND | SWT.SHIFT));
	}

	@Test
	public void testDecPrivateModes() {
		// the cursor hidden and shown again by a program
		run("\u001b[?25l");
		assertFalse(control.isCursorShown());
		run("\u001b[?25h");
		assertTrue(control.isCursorShown());
		// several modes in one sequence, as ncurses sends them: every one of them counts
		run("\u001b[?2004;25l");
		assertAll(() -> assertFalse(control.isCursorShown()), () -> assertFalse(control.isBracketedPaste()));
		run("\u001b[?25;2004h");
		assertAll(() -> assertTrue(control.isCursorShown()), () -> assertTrue(control.isBracketedPaste()));
	}

	@Test
	public void testResetStateEndsDecPrivateModes() {
		run("\u001b[?25l\u001b[?2004h");
		assertAll(() -> assertFalse(control.isCursorShown()), () -> assertTrue(control.isBracketedPaste()));
		// the connection ends while the program has them set; the next one starts clean
		emulator.resetState();
		assertAll(() -> assertTrue(control.isCursorShown()), () -> assertFalse(control.isBracketedPaste()));
	}

	@Test
	public void testEmptyDecPrivateModeNamesNoMode() {
		// CSI ? 2004 ; h: the empty parameter is no mode, not 1 (application cursor keys)
		run("\u001b[?2004;h");
		assertAll(() -> assertTrue(control.isBracketedPaste()), () -> assertFalse(control.isApplicationCursorKeys()));
	}

	@Test
	public void testModesPastTheSixteenthAreDropped() {
		// 17 modes: the first 16 are taken, the 17th is dropped, and the sequence does not
		// stop the text after it
		run("\u001b[?25l\u001b[?" + "2004;".repeat(16) + "25hx");
		assertAll(() -> assertTrue(control.isBracketedPaste()), () -> assertFalse(control.isCursorShown()));
		run("\u001b[?25h\u001b[?" + "2004;".repeat(16) + "25ly");
		assertAll(() -> assertFalse(control.isBracketedPaste()), () -> assertTrue(control.isCursorShown()),
				() -> assertTextEquals("xy"));
	}

	@Test
	public void testFullResetEndsDecPrivateModes() {
		run("\u001b[?25l\u001b[?2004h");
		// ESC c (RIS) resets every mode a program set, as a new connection does
		run("\u001bc");
		assertAll(() -> assertTrue(control.isCursorShown()), () -> assertFalse(control.isBracketedPaste()));
	}

	@Test
	public void testSynchronizedOutput() {
		// a program begins and ends a screen with mode 2026
		run("\u001b[?2026h");
		assertTrue(control.isSynchronizedOutput());
		run("\u001b[?2026l");
		assertFalse(control.isSynchronizedOutput());
		// a program that began one and was cut off or reset does not leave the view frozen
		run("\u001b[?2026h");
		emulator.resetState();
		assertFalse(control.isSynchronizedOutput());
		run("\u001b[?2026h\u001bc");
		assertFalse(control.isSynchronizedOutput());
	}

	@Test
	public void testPasteText() {
		// each line break goes as a CR, as the Enter key sends it: a Windows CR LF is one break
		assertEquals("a\rb\rc\rd", VT100TerminalControl.pasteText("a\r\nb\nc\rd", false));
		String start = "\033[200~", end = "\033[201~";
		assertEquals(start + "a\rb" + end, VT100TerminalControl.pasteText("a\r\nb", true));
		// the text cannot end the paste early: every ESC and C1 CSI goes
		assertEquals(start + "a[201~b" + end, VT100TerminalControl.pasteText("a\033[201~b", true));
		// not even with a marker put together from the pieces left after taking one out
		assertEquals(start + "[20[201~1~x" + end, VT100TerminalControl.pasteText("\033[20\033[201~1~x", true));
		assertEquals(start + "201~x" + end, VT100TerminalControl.pasteText("\u009b201~x", true));
	}
}
