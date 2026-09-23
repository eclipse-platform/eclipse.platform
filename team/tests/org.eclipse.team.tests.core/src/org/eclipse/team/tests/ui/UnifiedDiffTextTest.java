/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP - initial implementation
 *******************************************************************************/
package org.eclipse.team.tests.ui;

import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.clampDetailedDiffLength;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.countLines;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.mapOffsetToTabExpanded;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.mergeStyleRanges;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.replaceTabWithSpaces;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.UnifiedDiffLineHeaderCodeMining;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.UnifiedDiffLineHeaderCodeMining.RangeInfo;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the geometry and text helpers behind the unified diff code minings.
 * These functions decide where the detailed-diff highlight rectangles are
 * painted, so an off-by-one here shows up as a misplaced highlight that is easy
 * to miss in a manual test.
 */
@SuppressWarnings("restriction")
public class UnifiedDiffTextTest {

	private static final Color BACKGROUND_1 = systemColor(SWT.COLOR_RED);
	private static final Color BACKGROUND_2 = systemColor(SWT.COLOR_GREEN);
	private static final Color FOREGROUND_1 = systemColor(SWT.COLOR_BLUE);
	private static final Color FOREGROUND_2 = systemColor(SWT.COLOR_YELLOW);

	private Shell shell;
	private StyledText styledText;
	private GC gc;

	@BeforeEach
	public void setUp() {
		shell = new Shell(Display.getDefault());
		styledText = new StyledText(shell, SWT.NONE);
		gc = new GC(styledText);
	}

	@AfterEach
	public void tearDown() {
		if (gc != null && !gc.isDisposed()) {
			gc.dispose();
		}
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	// ---------------------------------------------------------------- countLines

	/**
	 * The line a detailed diff is painted on. A diff that starts right after a line
	 * delimiter belongs to the following line, not to the one that ended, which is
	 * what {@code String#split("\n")} gets wrong: it drops trailing empty strings
	 * and must therefore never be used to count lines.
	 */
	@Test
	public void testCountLinesForDetailedDiffStartOffsets() {
		String diff = "first\nsecond\nthird";
		assertAll( //
				() -> assertEquals(1, countLines(diff.substring(0, 0)), "offset 0 is on line 1"), //
				() -> assertEquals(1, countLines(diff.substring(0, 3)), "offset inside line 1"), //
				() -> assertEquals(1, countLines(diff.substring(0, 5)), "offset at end of line 1"), //
				() -> assertEquals(2, countLines(diff.substring(0, 6)), "offset at start of line 2"), //
				() -> assertEquals(3, countLines(diff.substring(0, 13)), "offset at start of line 3"), //
				() -> assertEquals(2, countLines("first\r\nsecond"), "CRLF is one delimiter"));
	}

	/**
	 * The prefix overload counts the same lines as the copied substring would.
	 */
	@Test
	public void testCountLinesOfPrefixMatchesSubstring() {
		String diff = "first\nsecond\r\nthird\n";
		assertAll( //
				() -> assertEquals(1, countLines(diff, 0), "empty prefix is one line"), //
				() -> assertEquals(1, countLines(diff, 5), "prefix ending before the delimiter"), //
				() -> assertEquals(2, countLines(diff, 6), "prefix ending after the delimiter"), //
				() -> assertEquals(3, countLines(diff, 14), "CRLF is one delimiter"), //
				() -> assertEquals(countLines(diff), countLines(diff, diff.length()), "whole string"));
	}

	// ------------------------------------------------------------ tab expansion

	@Test
	public void testMapOffsetToTabExpanded() {
		String source = "\ta\tb";
		assertAll( //
				() -> assertEquals(0, mapOffsetToTabExpanded(source, 0, 4), "offset 0 never shifts"), //
				() -> assertEquals(4, mapOffsetToTabExpanded(source, 1, 4), "after the first tab"), //
				() -> assertEquals(5, mapOffsetToTabExpanded(source, 2, 4)), //
				() -> assertEquals(9, mapOffsetToTabExpanded(source, 3, 4), "after the second tab"), //
				() -> assertEquals(3, mapOffsetToTabExpanded(source, 3, 1), "a tab width of 1 is the identity"), //
				() -> assertEquals(2, mapOffsetToTabExpanded("ab\tc", 2, 4), "a tab behind the offset is ignored"));
	}

	/**
	 * The highlight offsets are computed on the raw diff text while the label is
	 * painted from the tab expanded text, so both must agree for every offset.
	 */
	@Test
	public void testMapOffsetToTabExpandedMatchesReplaceTabWithSpaces() {
		for (String source : List.of("\ta\tb", "no tabs at all", "\t\t\t", "a\tb\nc\td", "")) {
			for (int tabWidth : new int[] { 1, 2, 4, 8 }) {
				for (int offset = 0; offset <= source.length(); offset++) {
					String prefix = source.substring(0, offset);
					int expected = replaceTabWithSpaces(prefix, tabWidth).length();
					int actual = mapOffsetToTabExpanded(source, offset, tabWidth);
					assertEquals(expected, actual, "offset " + offset + " of '" + source.replace("\t", "\\t")
							+ "' with tab width " + tabWidth);
				}
			}
		}
	}

	@Test
	public void testMapOffsetToTabExpandedClampsOffsetsBehindTheText() {
		// callers pass detailedDiffStart + detailedDiffLength, which may run past the
		// text once trailing new lines have been stripped
		assertEquals(10, mapOffsetToTabExpanded("\tab", 7, 4), "tabs are only counted within the text");
	}

	// -------------------------------------------------------- mergeStyleRanges

	@Test
	public void testMergeStyleRangesWithoutBackgroundsReturnsForegrounds() {
		List<StyleRange> foregrounds = List.of(foreground(0, 5, FOREGROUND_1));
		assertSame(foregrounds, mergeStyleRanges(List.of(), foregrounds));
	}

	@Test
	public void testMergeStyleRangesWithoutForegroundsReturnsBackgrounds() {
		List<StyleRange> backgrounds = List.of(background(0, 5, BACKGROUND_1));
		assertSame(backgrounds, mergeStyleRanges(backgrounds, List.of()));
	}

	@Test
	public void testMergeStyleRangesSplitsForegroundAroundBackground() {
		List<StyleRange> result = mergeStyleRanges(List.of(background(3, 3, BACKGROUND_1)),
				List.of(foreground(0, 10, FOREGROUND_1)));

		assertRanges(result, //
				"0+3 fg=BLUE bg=null", //
				"3+3 fg=BLUE bg=RED", //
				"6+4 fg=BLUE bg=null");
		assertTilesForegrounds(result, List.of(foreground(0, 10, FOREGROUND_1)));
	}

	@Test
	public void testMergeStyleRangesKeepsForegroundStylingOfEverySegment() {
		StyleRange bold = foreground(0, 10, FOREGROUND_1);
		bold.fontStyle = SWT.BOLD;

		List<StyleRange> result = mergeStyleRanges(List.of(background(3, 3, BACKGROUND_1)), List.of(bold));

		for (StyleRange range : result) {
			assertEquals(SWT.BOLD, range.fontStyle, "font style must survive the split");
			assertSame(FOREGROUND_1, range.foreground, "foreground must survive the split");
		}
	}

	@Test
	public void testMergeStyleRangesSpansSeveralForegrounds() {
		List<StyleRange> foregrounds = List.of(foreground(0, 5, FOREGROUND_1), foreground(5, 5, FOREGROUND_2));

		List<StyleRange> result = mergeStyleRanges(List.of(background(3, 4, BACKGROUND_1)), foregrounds);

		assertRanges(result, //
				"0+3 fg=BLUE bg=null", //
				"3+2 fg=BLUE bg=RED", //
				"5+2 fg=YELLOW bg=RED", //
				"7+3 fg=YELLOW bg=null");
		assertTilesForegrounds(result, foregrounds);
	}

	@Test
	public void testMergeStyleRangesWithSeveralBackgroundsInOneForeground() {
		List<StyleRange> foregrounds = List.of(foreground(0, 20, FOREGROUND_1));

		List<StyleRange> result = mergeStyleRanges(
				List.of(background(2, 3, BACKGROUND_1), background(10, 2, BACKGROUND_2)), foregrounds);

		assertRanges(result, //
				"0+2 fg=BLUE bg=null", //
				"2+3 fg=BLUE bg=RED", //
				"5+5 fg=BLUE bg=null", //
				"10+2 fg=BLUE bg=GREEN", //
				"12+8 fg=BLUE bg=null");
		assertTilesForegrounds(result, foregrounds);
	}

	@Test
	public void testMergeStyleRangesWithCongruentRanges() {
		List<StyleRange> foregrounds = List.of(foreground(0, 5, FOREGROUND_1));

		List<StyleRange> result = mergeStyleRanges(List.of(background(0, 5, BACKGROUND_1)), foregrounds);

		assertRanges(result, "0+5 fg=BLUE bg=RED");
	}

	@Test
	public void testMergeStyleRangesIgnoresBackgroundsOutsideTheForegrounds() {
		List<StyleRange> foregrounds = List.of(foreground(10, 5, FOREGROUND_1));

		assertRanges(mergeStyleRanges(List.of(background(0, 5, BACKGROUND_1)), foregrounds), //
				"10+5 fg=BLUE bg=null");
		assertRanges(mergeStyleRanges(List.of(background(30, 5, BACKGROUND_1)), foregrounds), //
				"10+5 fg=BLUE bg=null");
	}

	@Test
	public void testMergeStyleRangesWithGapsBetweenForegrounds() {
		// a background reaching into the gap between two foregrounds must not produce
		// a range covering unstyled text
		List<StyleRange> foregrounds = List.of(foreground(0, 4, FOREGROUND_1), foreground(8, 4, FOREGROUND_2));

		List<StyleRange> result = mergeStyleRanges(List.of(background(2, 8, BACKGROUND_1)), foregrounds);

		assertRanges(result, //
				"0+2 fg=BLUE bg=null", //
				"2+2 fg=BLUE bg=RED", //
				"8+2 fg=YELLOW bg=RED", //
				"10+2 fg=YELLOW bg=null");
		assertTilesForegrounds(result, foregrounds);
	}

	// ------------------------------------------- clampDetailedDiffLength

	@Test
	public void testClampDetailedDiffLengthEndExactlyAtTrimmedEnd() {
		// diff ends exactly at the last char of the trimmed string — must not clip
		assertEquals(4, clampDetailedDiffLength(22, 4, 26));
	}

	@Test
	public void testClampDetailedDiffLengthEndBeyondTrimmedEnd() {
		// diff overshoots by 1 (e.g. includes a stripped trailing newline) — clip by 1
		assertEquals(4, clampDetailedDiffLength(22, 5, 26));
	}

	@Test
	public void testClampDetailedDiffLengthFullyOutside() {
		// diff is entirely in the stripped region — result is <= 0
		assertTrue(clampDetailedDiffLength(26, 1, 26) <= 0);
	}

	@Test
	public void testClampDetailedDiffLengthNoOvershoot() {
		// diff well within bounds — unchanged
		assertEquals(3, clampDetailedDiffLength(5, 3, 20));
	}

	// ------------------------------------------- getPositionForOffset

	@Test
	public void testGetPositionForOffsetResetsXAfterNewlineAtRangeStart() throws Exception {
		// Label: "abc\nxyz" — two lines, 3 chars each.
		// Range [0, 3): "abc" — first line only, no newline.
		// Range [3, 7): "\nxyz" — starts with \n at index 0 (lfIdx == 0).
		// Querying the position of offset 7 (end of "xyz") must return x=width("xyz"),
		// not x=width("abc")+width("xyz"), because the \n resets the x accumulator.
		String str = "abc\nxyz";
		List<StyleRange> ranges = List.of(styledRange(0, 3), styledRange(3, 4));

		Document doc = new Document(str);
		UnifiedDiff diff = new UnifiedDiff(doc, 0, str.length(), str, doc, 0, str.length(), str,
				List.of(), UnifiedDiffMode.REPLACE_MODE);
		UnifiedDiffLineHeaderCodeMining mining = new UnifiedDiffLineHeaderCodeMining(
				new Position(0, 1), null, diff, 4, null, null, null);

		gc.setFont(styledText.getFont());
		Point result = mining.getPositionForOffset(styledText, gc, 7, str, ranges, new RangeInfo(-1, -1, null));

		assertNotNull(result, "position must not be null");
		Point expected = gc.stringExtent("xyz");
		assertEquals(expected.x, result.x,
				"x must equal the width of 'xyz' alone, not width('abc')+width('xyz')");
	}

	// ------------------------------------------------------------------ helpers

	/**
	 * The merged ranges are handed to {@code StyledText#setStyleRanges}, which
	 * rejects overlapping ranges. They must therefore tile the foregrounds exactly:
	 * same extent, ascending, no gaps, no overlaps, no empty ranges.
	 */
	private static void assertTilesForegrounds(List<StyleRange> result, List<StyleRange> foregrounds) {
		int resultIndex = 0;
		for (StyleRange fg : foregrounds) {
			int expected = fg.start;
			while (resultIndex < result.size() && result.get(resultIndex).start < fg.start + fg.length) {
				StyleRange range = result.get(resultIndex);
				assertEquals(expected, range.start, "ranges must be contiguous and ascending: " + describe(result));
				assertTrue(range.length > 0, "empty ranges are not allowed: " + describe(result));
				expected += range.length;
				resultIndex++;
			}
			assertEquals(fg.start + fg.length, expected,
					"the foreground extent must be covered completely: " + describe(result));
		}
		assertEquals(result.size(), resultIndex, "no ranges outside the foregrounds: " + describe(result));
	}

	private static void assertRanges(List<StyleRange> actual, String... expected) {
		assertEquals(List.of(expected), describe(actual));
	}

	private static List<String> describe(List<StyleRange> ranges) {
		return ranges.stream()
				.map(r -> r.start + "+" + r.length + " fg=" + name(r.foreground) + " bg=" + name(r.background)).toList();
	}

	private static String name(Color color) {
		if (color == null) {
			return "null";
		}
		if (color.equals(BACKGROUND_1)) {
			return "RED";
		}
		if (color.equals(BACKGROUND_2)) {
			return "GREEN";
		}
		if (color.equals(FOREGROUND_1)) {
			return "BLUE";
		}
		if (color.equals(FOREGROUND_2)) {
			return "YELLOW";
		}
		return color.toString();
	}

	private static StyleRange foreground(int start, int length, Color color) {
		StyleRange range = new StyleRange();
		range.start = start;
		range.length = length;
		range.foreground = color;
		return range;
	}

	private static StyleRange background(int start, int length, Color color) {
		StyleRange range = new StyleRange();
		range.start = start;
		range.length = length;
		range.background = color;
		return range;
	}

	private static Color systemColor(int id) {
		return Display.getDefault().getSystemColor(id);
	}

	private static StyleRange styledRange(int start, int length) {
		StyleRange range = new StyleRange();
		range.start = start;
		range.length = length;
		range.foreground = systemColor(SWT.COLOR_BLUE);
		return range;
	}
}
