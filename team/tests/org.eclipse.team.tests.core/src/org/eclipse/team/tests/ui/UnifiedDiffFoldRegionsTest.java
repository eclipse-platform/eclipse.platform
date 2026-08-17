/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.junit.jupiter.api.Test;

/**
 * Tests which unchanged regions the unified diff collapses. The assertions are
 * expressed in the lines the user would no longer see: a collapsed region keeps
 * its first line visible as the caption and hides the rest.
 */
@SuppressWarnings("restriction")
public class UnifiedDiffFoldRegionsTest {

	/**
	 * The bulk of an unchanged file has to be folded away while the change and the
	 * requested context around it stay visible.
	 */
	@Test
	public void testContextLinesAroundAChangeStayVisible() throws Exception {
		IDocument document = numberedLines(40);

		Set<Integer> hidden = hiddenLines(document, 3, changeOnLine(document, 20));

		assertThat(hidden).as("the lines far from the change must be folded away").isNotEmpty();
		assertVisible(hidden, 20, "the changed line");
		for (int line = 17; line <= 23; line++) {
			assertVisible(hidden, line, "context line");
		}
		assertTrue(hidden.contains(Integer.valueOf(5)), "line 5 is far from the change and must be hidden");
		assertTrue(hidden.contains(Integer.valueOf(34)), "line 34 is far from the change and must be hidden");
	}

	/**
	 * The context is what the caller asked for, not a fixed amount: a larger context
	 * has to keep strictly more lines visible.
	 */
	@Test
	public void testALargerContextKeepsMoreLinesVisible() throws Exception {
		IDocument document = numberedLines(40);

		Set<Integer> withOne = hiddenLines(document, 1, changeOnLine(document, 20));
		Set<Integer> withFive = hiddenLines(document, 5, changeOnLine(document, 20));

		assertThat(withFive).as("a larger context hides fewer lines").isSubsetOf(withOne);
		assertThat(withOne).as("a larger context hides fewer lines").isNotEqualTo(withFive);
		for (int line = 15; line <= 25; line++) {
			assertVisible(withFive, line, "context line");
		}
	}

	/**
	 * Two changes must each keep their own context, and the gap between them has to
	 * be folded. A single fold across both would hide a change.
	 */
	@Test
	public void testEveryChangeKeepsItsOwnContext() throws Exception {
		IDocument document = numberedLines(60);

		Set<Integer> hidden = hiddenLines(document, 2, changeOnLine(document, 10), changeOnLine(document, 45));

		for (int changedLine : new int[] { 10, 45 }) {
			assertVisible(hidden, changedLine, "changed line");
			for (int line = changedLine - 2; line <= changedLine + 2; line++) {
				assertVisible(hidden, line, "context line");
			}
		}
		assertTrue(hidden.contains(Integer.valueOf(28)), "the gap between both changes must be folded");
		assertTrue(hidden.contains(Integer.valueOf(2)), "the region before the first change must be folded");
		assertTrue(hidden.contains(Integer.valueOf(55)), "the region after the last change must be folded");
	}

	/**
	 * A change on the very first line has no region above it, so no context must be
	 * reserved there and the fold below it still has to appear.
	 */
	@Test
	public void testChangeOnTheFirstLine() throws Exception {
		IDocument document = numberedLines(30);

		Set<Integer> hidden = hiddenLines(document, 3, changeOnLine(document, 0));

		assertVisible(hidden, 0, "the changed first line");
		for (int line = 1; line <= 3; line++) {
			assertVisible(hidden, line, "context line");
		}
		assertTrue(hidden.contains(Integer.valueOf(20)), "the unchanged rest of the file must be folded");
	}

	/**
	 * A region that would not hide a single line below its caption costs a fold
	 * marker and a click without saving anything, so it must not be created.
	 */
	@Test
	public void testNoFoldWhenNothingWouldBeHidden() throws Exception {
		IDocument document = numberedLines(7);

		List<Position> regions = UnifiedDiffManager.unchangedFoldRegions(document,
				List.of(changeOnLine(document, 3)), UnifiedDiffMode.OVERLAY_READ_ONLY_MODE, 3);

		assertThat(regions).as("the context covers the whole file, so there is nothing to fold").isEmpty();
	}

	/** Without a change there is nothing to fold around, so nothing is collapsed. */
	@Test
	public void testNoDiffsMeansNoFolds() {
		List<Position> regions = UnifiedDiffManager.unchangedFoldRegions(numberedLines(40), List.of(),
				UnifiedDiffMode.OVERLAY_READ_ONLY_MODE, 3);

		assertThat(regions).isEmpty();
	}

	/**
	 * At least one context line is kept even when none was asked for, so that the
	 * expander of a fold and the code mining of the following change cannot end up
	 * on the same line.
	 */
	@Test
	public void testAtLeastOneContextLineIsKept() throws Exception {
		IDocument document = numberedLines(40);

		Set<Integer> hidden = hiddenLines(document, 0, changeOnLine(document, 20));

		assertVisible(hidden, 19, "the line above the change");
		assertVisible(hidden, 21, "the line below the change");
	}

	/**
	 * The gaps are walked front to back, so the result must not depend on the order
	 * in which the caller collected the changes.
	 */
	@Test
	public void testResultDoesNotDependOnTheOrderOfTheDiffs() throws Exception {
		IDocument document = numberedLines(60);
		UnifiedDiff first = changeOnLine(document, 10);
		UnifiedDiff second = changeOnLine(document, 45);

		Set<Integer> inOrder = hiddenLines(document, 2, first, second);
		Set<Integer> reversed = hiddenLines(document, 2, second, first);

		assertEquals(inOrder, reversed, "reversing the diffs must not change which lines are folded");
	}

	/** No fold may cover a changed line, whatever the context and the file size. */
	@Test
	public void testFoldsNeverHideAChange() throws Exception {
		IDocument document = numberedLines(200);
		int[] changedLines = { 0, 7, 8, 50, 120, 121, 199 };
		List<UnifiedDiff> diffs = new ArrayList<>();
		for (int line : changedLines) {
			diffs.add(changeOnLine(document, line));
		}

		for (int context = 0; context <= 4; context++) {
			Set<Integer> hidden = hiddenLines(document, context, diffs.toArray(new UnifiedDiff[0]));
			for (int line : changedLines) {
				assertVisible(hidden, line, "changed line with context " + context + ":");
			}
		}
	}

	/**
	 * In replace mode the document already holds the new content, so the folds have
	 * to give way to the lines the replacement occupies, not to the ones it
	 * replaced.
	 */
	@Test
	public void testReplaceModeMeasuresTheChangeByItsNewContent() throws Exception {
		IDocument document = numberedLines(60);
		// one line of the left side is shown as three lines of the right side
		UnifiedDiff diff = change(document, 20, 1, 3);

		Set<Integer> inReplaceMode = hiddenLines(document, UnifiedDiffMode.REPLACE_MODE, 2, diff);
		Set<Integer> inOverlayMode = hiddenLines(document, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE, 2, diff);

		for (int line = 20; line <= 24; line++) {
			assertVisible(inReplaceMode, line, "the replacement and its context line");
		}
		assertTrue(inOverlayMode.contains(Integer.valueOf(24)),
				"the overlay only occupies the line it is anchored to, so line 24 is foldable there");
	}

	/**
	 * An addition at the very end of a file that does not end with a newline sits
	 * on the last offset of the document. The unchanged lines above it still have
	 * to be folded.
	 */
	@Test
	public void testAnAdditionAtTheEndOfAFileWithoutATrailingNewline() throws Exception {
		IDocument document = new Document(numberedLines(40).get().stripTrailing());
		int end = document.getLength();
		UnifiedDiff addition = new UnifiedDiff(document, end, end, "", document, end, end + 6, "added\n",
				new ArrayList<>(), UnifiedDiffMode.OVERLAY_READ_ONLY_MODE);

		Set<Integer> hidden = hiddenLines(document, 3, addition);

		assertTrue(hidden.contains(Integer.valueOf(10)), "the unchanged lines above the addition must be folded");
	}

	/**
	 * A file ending with a newline has an empty last line. A change close to it
	 * must not leave a fold behind that only reaches that line, because the
	 * expander code mining does not count it and could not expand it again.
	 */
	@Test
	public void testNoFoldForTheEmptyLineOfAFileEndingWithANewline() throws Exception {
		IDocument document = numberedLines(40);

		Set<Integer> hidden = hiddenLines(document, 1, changeOnLine(document, 37));

		assertVisible(hidden, 39, "the last line with content");
	}

	/**
	 * A deletion is not shown by a code mining but by the annotation on the lines
	 * it covers, so those lines have to stay visible.
	 */
	@Test
	public void testAPureDeletionStaysVisible() throws Exception {
		IDocument document = numberedLines(60);

		Set<Integer> hidden = hiddenLines(document, 3, deletionOfLines(document, 30, 2));

		assertVisible(hidden, 30, "the first deleted line");
		assertVisible(hidden, 31, "the second deleted line");
	}

	/**
	 * A pure addition occupies no line of the document. Its code mining is anchored
	 * to the line it is inserted in front of, which therefore has to stay visible.
	 */
	@Test
	public void testAPureAdditionInTheMiddleOfAFileStaysVisible() throws Exception {
		IDocument document = numberedLines(60);

		Set<Integer> hidden = hiddenLines(document, 3, additionBeforeLine(document, 30, "added\n"));

		assertVisible(hidden, 30, "the line the addition is anchored to");
	}

	/**
	 * A line deleted in one place and added again further down produces a deletion
	 * and a separate addition. Both ends of the move have to stay visible.
	 */
	@Test
	public void testALineDeletedHereAndAddedLaterIsVisibleAtBothEnds() throws Exception {
		IDocument document = numberedLines(60);
		UnifiedDiff deletedHere = deletionOfLines(document, 10, 1);
		UnifiedDiff addedThere = additionBeforeLine(document, 40, "line 10\n");

		for (int context = 0; context <= 4; context++) {
			Set<Integer> hidden = hiddenLines(document, context, deletedHere, addedThere);
			assertVisible(hidden, 10, "the line that was deleted, with context " + context + ":");
			assertVisible(hidden, 40, "the line the deleted text was added in front of, with context " + context + ":");
		}
	}

	/** The same move, with the addition above the deletion in the document. */
	@Test
	public void testALineDeletedHereAndAddedEarlierIsVisibleAtBothEnds() throws Exception {
		IDocument document = numberedLines(60);
		UnifiedDiff addedThere = additionBeforeLine(document, 10, "line 40\n");
		UnifiedDiff deletedHere = deletionOfLines(document, 40, 1);

		Set<Integer> hidden = hiddenLines(document, 3, addedThere, deletedHere);

		assertVisible(hidden, 10, "the line the deleted text was added in front of");
		assertVisible(hidden, 40, "the line that was deleted");
	}

	/**
	 * A reused set of code minings is only complete when it holds one mining per
	 * diff that shows content of the other side. Reusing fewer would drop a diff
	 * for good, because nothing recomputes it from the diffs afterwards.
	 */
	@Test
	public void testOnlyDiffsShowingTheOtherSideNeedACodeMining() throws Exception {
		IDocument document = numberedLines(60);
		UnifiedDiff shownAsOverlay = additionBeforeLine(document, 10, "line 40\n");
		UnifiedDiff shownOnItsOwnLines = deletionOfLines(document, 40, 1);

		assertEquals(1, UnifiedDiffCodeMiningProvider
				.expectedMiningCount(List.of(shownAsOverlay, shownOnItsOwnLines)),
				"only the overlay needs a mining, the deletion is drawn on the lines it covers");
		assertEquals(2, UnifiedDiffCodeMiningProvider
				.expectedMiningCount(List.of(shownAsOverlay, changeOnLine(document, 50))),
				"a replacement shows the other side as an overlay too");
	}

	// ------------------------------------------------------------------ helpers

	/**
	 * The lines that the given folds would hide. A collapsed region keeps its first
	 * line visible as the caption.
	 */
	private static Set<Integer> hiddenLines(IDocument document, int contextLines, UnifiedDiff... diffs)
			throws BadLocationException {
		return hiddenLines(document, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE, contextLines, diffs);
	}

	private static Set<Integer> hiddenLines(IDocument document, UnifiedDiffMode mode, int contextLines,
			UnifiedDiff... diffs) throws BadLocationException {
		List<Position> regions = UnifiedDiffManager.unchangedFoldRegions(document, Arrays.asList(diffs), mode,
				contextLines);
		Set<Integer> hidden = new LinkedHashSet<>();
		for (Position region : regions) {
			int firstLine = document.getLineOfOffset(region.getOffset());
			int lastLine = document.getLineOfOffset(region.getOffset() + region.getLength() - 1);
			assertTrue(lastLine > firstLine, "a fold that hides nothing must not be created");
			for (int line = firstLine + 1; line <= lastLine; line++) {
				assertTrue(hidden.add(Integer.valueOf(line)), "folds must not overlap on line " + line);
			}
		}
		return hidden;
	}

	private static void assertVisible(Set<Integer> hidden, int line, String what) {
		assertThat(hidden).as(what + " " + line + " must stay visible").doesNotContain(Integer.valueOf(line));
	}

	/** A one line change of the given document line, as the manager records it. */
	private static UnifiedDiff changeOnLine(IDocument document, int line) throws BadLocationException {
		return change(document, line, 1, 1);
	}

	/** A deletion: the document holds the lines, the other side does not. */
	private static UnifiedDiff deletionOfLines(IDocument document, int line, int lines) throws BadLocationException {
		int offset = document.getLineOffset(line);
		int end = document.getLineOffset(line + lines);
		return new UnifiedDiff(document, offset, end, document.get(offset, end - offset), document, offset, offset, "",
				new ArrayList<>(), UnifiedDiffMode.OVERLAY_READ_ONLY_MODE);
	}

	/** An addition: the other side holds a line the document does not. */
	private static UnifiedDiff additionBeforeLine(IDocument document, int line, String added)
			throws BadLocationException {
		int offset = document.getLineOffset(line);
		return new UnifiedDiff(document, offset, offset, "", document, offset, offset + added.length(), added,
				new ArrayList<>(), UnifiedDiffMode.OVERLAY_READ_ONLY_MODE);
	}

	/** A change of {@code leftLines} shown as {@code rightLines}. */
	private static UnifiedDiff change(IDocument document, int line, int leftLines, int rightLines)
			throws BadLocationException {
		int offset = document.getLineOffset(line);
		int leftEnd = document.getLineOffset(line + leftLines);
		int rightEnd = document.getLineOffset(line + rightLines);
		return new UnifiedDiff(document, offset, leftEnd, document.get(offset, leftEnd - offset), document, offset,
				rightEnd, "changed\n", new ArrayList<>(), UnifiedDiffMode.OVERLAY_READ_ONLY_MODE);
	}

	private static IDocument numberedLines(int count) {
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < count; i++) {
			content.append("line ").append(i).append('\n');
		}
		return new Document(content.toString());
	}
}
