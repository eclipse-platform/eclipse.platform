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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the unchanged-region folds of the unified diff do not leave the
 * folding ruler with fold indicators that cannot do anything: the folding ruler
 * paints a fold whose first line is hidden on the first line of it that is still
 * visible, but toggles it by its start line, so clicking such an indicator has
 * no effect.
 */
@SuppressWarnings("restriction")
public class UnifiedDiffShadowedFoldsTest {

	private static final UnifiedDiffMode MODE = UnifiedDiffMode.OVERLAY_READ_ONLY_MODE;

	private Display display;
	private Shell shell;
	private ProjectionViewer viewer;

	@BeforeEach
	public void setUp() {
		display = Display.getDefault();
		assertNotNull(display, "the test needs a display");
		shell = new Shell(display);
	}

	@AfterEach
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	/**
	 * The case reported on the pull request: a fold of the editor that starts inside
	 * a collapsed region and reaches out of it. Its indicator would be painted on a
	 * line the folding ruler does not associate with it, so it has to be taken out
	 * of the model while the region is collapsed.
	 */
	@Test
	public void testAFoldReachingOutOfACollapsedRegionIsTakenOut() throws Exception {
		IDocument document = numberedLines(100);
		ProjectionAnnotationModel projectionModel = openViewer(document);
		ProjectionAnnotation foldOfTheEditor = addFoldOfTheEditor(projectionModel, document, 10, 70);

		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(changeOnLine(document, 60)), MODE, 3);

		assertFalse(isInModel(projectionModel, foldOfTheEditor),
				"a fold starting on a hidden line must not keep an indicator that does nothing");
	}

	/** Expanding the region has to give the fold of the editor back to the user. */
	@Test
	public void testAFoldComesBackWhenTheRegionIsExpanded() throws Exception {
		IDocument document = numberedLines(100);
		ProjectionAnnotationModel projectionModel = openViewer(document);
		ProjectionAnnotation foldOfTheEditor = addFoldOfTheEditor(projectionModel, document, 10, 70);
		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(changeOnLine(document, 60)), MODE, 3);
		assertFalse(isInModel(projectionModel, foldOfTheEditor), "the fold has to be taken out first");

		viewer.doOperation(ProjectionViewer.EXPAND_ALL);
		waitForPendingWork();

		assertTrue(isInModel(projectionModel, foldOfTheEditor),
				"the fold of the editor must be usable again once nothing hides its first line");
	}

	/**
	 * A fold whose first line stays visible works as it always did and must be left
	 * alone.
	 */
	@Test
	public void testAFoldStartingOnAVisibleLineIsKept() throws Exception {
		IDocument document = numberedLines(100);
		ProjectionAnnotationModel projectionModel = openViewer(document);
		// line 58 is one of the context lines kept visible above the change
		ProjectionAnnotation foldOfTheEditor = addFoldOfTheEditor(projectionModel, document, 58, 90);

		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(changeOnLine(document, 60)), MODE, 3);

		assertTrue(isInModel(projectionModel, foldOfTheEditor), "a fold the user can still click must be kept");
	}

	/**
	 * A collapsed fold of the editor hides itself completely, so it has no
	 * indicator on a foreign line. Taking it out would show its content again.
	 */
	@Test
	public void testACollapsedFoldOfTheEditorIsLeftAlone() throws Exception {
		IDocument document = numberedLines(100);
		ProjectionAnnotationModel projectionModel = openViewer(document);
		ProjectionAnnotation foldOfTheEditor = new ProjectionAnnotation(true);
		projectionModel.replaceAnnotations(null, Map.of(foldOfTheEditor, lines(document, 10, 70)));

		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(changeOnLine(document, 60)), MODE, 3);

		assertTrue(isInModel(projectionModel, foldOfTheEditor),
				"taking a collapsed fold out would unfold what the user folded");
	}

	/**
	 * The editor contributes its folds asynchronously, so a fold arriving after the
	 * regions are collapsed has to be taken out as well.
	 */
	@Test
	public void testAFoldContributedAfterTheRegionsAreCollapsedIsTakenOut() throws Exception {
		IDocument document = numberedLines(100);
		ProjectionAnnotationModel projectionModel = openViewer(document);
		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(changeOnLine(document, 60)), MODE, 3);
		waitForPendingWork();

		ProjectionAnnotation foldOfTheEditor = addFoldOfTheEditor(projectionModel, document, 10, 70);
		waitForPendingWork();

		assertFalse(isInModel(projectionModel, foldOfTheEditor),
				"a fold the editor adds later must not keep an indicator that does nothing");
	}

	/**
	 * Out of the projection model a position no longer follows the document, so a
	 * fold taken out before an edit must not be put back at what it described then.
	 */
	@Test
	public void testAFoldIsNotPutBackAfterTheDocumentWasEdited() throws Exception {
		IDocument document = numberedLines(100);
		ProjectionAnnotationModel projectionModel = openViewer(document);
		ProjectionAnnotation foldOfTheEditor = addFoldOfTheEditor(projectionModel, document, 10, 70);
		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(changeOnLine(document, 60)), MODE, 3);

		document.replace(document.getLineOffset(59), 0, "inserted\n");
		viewer.doOperation(ProjectionViewer.EXPAND_ALL);
		waitForPendingWork();

		assertFalse(isInModel(projectionModel, foldOfTheEditor),
				"a stale fold would put its indicator on the wrong line");
	}

	/** The collapsed region really has to hide its lines, not just be recorded. */
	@Test
	public void testTheCollapsedRegionHidesItsLines() throws Exception {
		IDocument document = numberedLines(100);
		openViewer(document);

		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(changeOnLine(document, 60)), MODE, 3);

		String visible = viewer.getTextWidget().getText();
		assertFalse(visible.contains("line 30"), "a line far from the change must not be shown");
		assertTrue(visible.contains("line 60"), "the changed line must be shown");
	}

	/**
	 * A line deleted in one place and added again further down: the deletion is
	 * shown on the lines it covers and the addition on the line it is anchored to,
	 * so the collapsed regions have to leave both of them on screen.
	 */
	@Test
	public void testAMovedLineIsShownAtBothEndsWhenCollapsed() throws Exception {
		IDocument document = numberedLines(100);
		openViewer(document);
		UnifiedDiff deletedHere = deletionOfLine(document, 20);
		UnifiedDiff addedThere = additionBeforeLine(document, 70, "line 20\n");

		UnifiedDiffManager.foldUnchangedRegions(viewer, document, List.of(deletedHere, addedThere), MODE, 3);

		String visible = viewer.getTextWidget().getText();
		assertTrue(visible.contains("line 20"), "the deleted line must be shown");
		assertTrue(visible.contains("line 70"), "the line the addition is anchored to must be shown");
		assertFalse(visible.contains("line 45"), "a line far from both ends of the move must not be shown");
	}

	/** A fold starting on the caption line of a region competes with that region. */
	@Test
	public void testAFoldOnTheCaptionLineOfARegionIsShadowed() throws Exception {
		IDocument document = numberedLines(100);
		List<Position> collapsed = collapsedRegions(document, 3, changeOnLine(document, 60));
		Position captionLineFold = new Position(collapsed.get(0).getOffset(), document.getLineLength(0));

		assertTrue(UnifiedDiffManager.isShadowedByCollapsedRegion(captionLineFold, collapsed),
				"two indicators on one line would toggle whichever the ruler happens to find first");
	}

	/** A fold below the last collapsed region is untouched. */
	@Test
	public void testAFoldOutsideEveryRegionIsNotShadowed() throws Exception {
		IDocument document = numberedLines(100);
		List<Position> collapsed = collapsedRegions(document, 3, changeOnLine(document, 60));

		assertFalse(UnifiedDiffManager.isShadowedByCollapsedRegion(lines(document, 58, 62), collapsed),
				"a fold starting in the visible context around a change must be left alone");
	}

	/** Without a collapsed region nothing is shadowed. */
	@Test
	public void testNothingIsShadowedWithoutCollapsedRegions() throws Exception {
		IDocument document = numberedLines(100);

		assertFalse(UnifiedDiffManager.isShadowedByCollapsedRegion(lines(document, 10, 70), List.of()));
	}

	// ------------------------------------------------------------------ helpers

	private ProjectionAnnotationModel openViewer(IDocument document) {
		viewer = new ProjectionViewer(shell, null, null, false, SWT.V_SCROLL);
		viewer.setDocument(document, new AnnotationModel());
		viewer.enableProjection();
		ProjectionAnnotationModel projectionModel = viewer.getProjectionAnnotationModel();
		assertNotNull(projectionModel, "the viewer must provide a projection model");
		return projectionModel;
	}

	private static ProjectionAnnotation addFoldOfTheEditor(ProjectionAnnotationModel projectionModel,
			IDocument document, int firstLine, int lastLine) throws BadLocationException {
		ProjectionAnnotation fold = new ProjectionAnnotation();
		projectionModel.replaceAnnotations(null, Map.of(fold, lines(document, firstLine, lastLine)));
		return fold;
	}

	private static List<Position> collapsedRegions(IDocument document, int contextLines, UnifiedDiff... diffs) {
		return UnifiedDiffManager.unchangedFoldRegions(document, List.of(diffs), MODE, contextLines);
	}

	private static boolean isInModel(ProjectionAnnotationModel projectionModel, Annotation annotation) {
		for (Iterator<Annotation> it = projectionModel.getAnnotationIterator(); it.hasNext();) {
			if (it.next() == annotation) {
				return true;
			}
		}
		return false;
	}

	/** Folds are put back from a runnable posted to the display. */
	private void waitForPendingWork() {
		while (display.readAndDispatch()) {
			// keep going until the queue is empty
		}
	}

	private static Position lines(IDocument document, int firstLine, int lastLine) throws BadLocationException {
		int offset = document.getLineOffset(firstLine);
		int end = document.getLineOffset(lastLine) + document.getLineLength(lastLine);
		return new Position(offset, end - offset);
	}

	/** A one line change of the given document line, as the manager records it. */
	private static UnifiedDiff changeOnLine(IDocument document, int line) throws BadLocationException {
		int offset = document.getLineOffset(line);
		int length = document.getLineLength(line);
		return new UnifiedDiff(document, offset, offset + length, document.get(offset, length), document, offset,
				offset + length, "changed\n", new ArrayList<>(), MODE);
	}

	/** A deletion: the document holds the line, the other side does not. */
	private static UnifiedDiff deletionOfLine(IDocument document, int line) throws BadLocationException {
		int offset = document.getLineOffset(line);
		int length = document.getLineLength(line);
		return new UnifiedDiff(document, offset, offset + length, document.get(offset, length), document, offset,
				offset, "", new ArrayList<>(), MODE);
	}

	/** An addition: the other side holds a line the document does not. */
	private static UnifiedDiff additionBeforeLine(IDocument document, int line, String added)
			throws BadLocationException {
		int offset = document.getLineOffset(line);
		return new UnifiedDiff(document, offset, offset, "", document, offset, offset + added.length(), added,
				new ArrayList<>(), MODE);
	}

	private static IDocument numberedLines(int count) {
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < count; i++) {
			content.append("line ").append(i).append('\n');
		}
		return new Document(content.toString());
	}
}
