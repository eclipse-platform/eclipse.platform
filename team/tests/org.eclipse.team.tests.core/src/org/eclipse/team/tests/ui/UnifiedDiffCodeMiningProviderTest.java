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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.FoldedRegionCodeMining;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.UnifiedDiffFooterCodeMining;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.UnifiedDiffLineHeaderCodeMining;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.inlined.AbstractInlinedAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the code minings of the unified diff describe the diffs and the
 * collapsed regions shown at the time they are computed, whatever the code
 * mining framework still has attached to the viewer from an earlier request.
 */
@SuppressWarnings("restriction")
public class UnifiedDiffCodeMiningProviderTest {

	private static final UnifiedDiffMode MODE = UnifiedDiffMode.OVERLAY_READ_ONLY_MODE;
	private static final int CONTEXT_LINES = 3;
	private static final int TIMEOUT_SECONDS = 10;

	private Display display;
	private Shell shell;
	private ProjectionViewer viewer;
	private IDocument document;
	private IAnnotationModel model;
	private UnifiedDiffCodeMiningProvider provider;
	private final List<CompletableFuture<List<? extends ICodeMining>>> pendingRequests = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		display = Display.getDefault();
		assertNotNull(Display.getCurrent(), "the test drives the viewer from the UI thread");
		shell = new Shell(display);
		document = numberedLines(60);
		model = new AnnotationModel();
		viewer = new ProjectionViewer(shell, null, null, false, SWT.V_SCROLL);
		viewer.configure(syntaxColoringConfiguration());
		viewer.setDocument(document, model);
		viewer.enableProjection();
		DocumentUndoManagerRegistry.connect(document);
		provider = new UnifiedDiffCodeMiningProvider();
	}

	@AfterEach
	public void tearDown() {
		DocumentUndoManagerRegistry.disconnect(document);
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
		waitForPendingWork();
	}

	/**
	 * Opening a diff on a viewer that already shows one clears the shown diffs in
	 * place and puts the new ones while the minings of the old ones are still
	 * attached. The framework's requests are asynchronous and each one cancels the
	 * one before it, so the provider can be asked at any point in between and must
	 * answer from the current diffs and folds, never from what it finds attached.
	 */
	@Test
	public void testMiningsFollowTheDiffsAndFoldsThroughAReopen() throws Exception {
		installCodeMinings(provider);
		assertTrue(open(withChangedLines(10, 45)).isOK());
		List<UnifiedDiff> first = UnifiedDiffManager.get(viewer);
		assertEquals(2, first.size(), "one diff per changed line");
		waitForAttachedMinings(first, allRegions().size());
		viewer.doOperation(ProjectionViewer.EXPAND_ALL);
		waitForPendingWork();
		assertThat(collapsedRegions()).as("everything is expanded again").isEmpty();
		assertThat(allRegions()).as("the regions are still there, ready to be hidden again").isNotEmpty();

		assertMinings(provide(), first);

		UnifiedDiffManager.foldUnchangedRegions(viewer, document, first, MODE, CONTEXT_LINES);
		waitForPendingWork();
		assertThat(collapsedRegions()).as("the unchanged regions are collapsed").hasSize(3);
		assertMinings(provide(), first);

		// what a reopen does before the framework caught up: the old list is cleared
		// in place and a new one with the same diffs is put
		List<UnifiedDiff> second = diffsOnLines(10, 45);
		first.clear();
		UnifiedDiffManager.put(viewer, second);
		assertMinings(provide(), second);

		// a reopen showing fewer diffs than there are minings attached
		List<UnifiedDiff> third = diffsOnLines(45);
		second.clear();
		UnifiedDiffManager.put(viewer, third);
		assertMinings(provide(), third);

		// the framework caught up, then lost the mining of one diff again
		viewer.updateCodeMinings();
		waitForAttachedMinings(third, allRegions().size());
		List<UnifiedDiff> fourth = diffsOnLines(10, 45);
		third.clear();
		UnifiedDiffManager.put(viewer, fourth);
		viewer.updateCodeMinings();
		waitForAttachedMinings(fourth, allRegions().size());
		model.removeAnnotation(attachedMiningAnnotationOf(fourth.get(0)));
		assertMinings(provide(), fourth);
	}

	/**
	 * The expander stands for the lines a region hides, and those lie below the
	 * region's own first line, which stays visible as the fold's caption. Drawn
	 * above the caption it would claim the gap is before a line that is still
	 * there, so it belongs to the first line after the region.
	 */
	@Test
	public void testTheExpanderSitsInTheGapAndNotAboveTheCaption() throws Exception {
		installCodeMinings(provider);
		assertTrue(open(withChangedLines(10, 45)).isOK());
		List<Position> collapsed = collapsedRegions();
		assertThat(collapsed).as("the unchanged regions are collapsed").isNotEmpty();

		for (ICodeMining mining : provide()) {
			if (!(mining instanceof FoldedRegionCodeMining expander)) {
				continue;
			}
			assertTrue(expander.isExpander(), "every band of a collapsed region shows it rather than hides it");
			int anchor = expander.getPosition().getOffset();
			Position region = regionEndingBefore(collapsed, anchor);
			assertNotNull(region, "every expander belongs to a collapsed region, but none ends before " + anchor);
			int captionLine = document.getLineOfOffset(region.getOffset());
			int anchorLine = document.getLineOfOffset(anchor);
			assertTrue(anchorLine > captionLine,
					"the expander must be drawn below the caption line " + captionLine + ", not above it at line "
							+ anchorLine);
			assertEquals(document.getLineOfOffset(region.getOffset() + region.getLength() - 1) + 1, anchorLine,
					"the expander belongs to the first line after the region");
		}
	}

	/**
	 * A region the user expanded can be hidden again: its band stays where it was
	 * and turns into a collapsing one.
	 */
	@Test
	public void testAnExpandedRegionOffersToHideItselfAgain() throws Exception {
		installCodeMinings(provider);
		assertTrue(open(withChangedLines(10, 45)).isOK());
		assertThat(collapsedRegions()).as("the regions start collapsed").isNotEmpty();
		int regions = allRegions().size();

		viewer.doOperation(ProjectionViewer.EXPAND_ALL);
		waitForPendingWork();
		assertThat(collapsedRegions()).as("nothing is collapsed any more").isEmpty();

		List<FoldedRegionCodeMining> bands = new ArrayList<>();
		for (ICodeMining mining : provide()) {
			if (mining instanceof FoldedRegionCodeMining band) {
				bands.add(band);
			}
		}
		assertEquals(regions, bands.size(), "every expanded region keeps a band to hide it again");
		for (FoldedRegionCodeMining band : bands) {
			assertFalse(band.isExpander(), "an expanded region's band hides it rather than showing it");
			assertThat(band.getLabel()).as("the label offers to hide the lines").contains("Hide");
			int anchorLine = document.getLineOfOffset(band.getPosition().getOffset());
			Position region = regionEndingBefore(allRegions().stream().map(FoldRegion::position).toList(),
					band.getPosition().getOffset());
			assertNotNull(region, "the band of an expanded region belongs to a region ending before it");
			assertThat(anchorLine).as("the band sits below the block it would hide")
					.isGreaterThan(document.getLineOfOffset(region.getOffset()));
		}
	}

	/**
	 * Expanding a region must not move its band. The band is what the user just
	 * clicked, and the only two lines of a region that are visible in both states
	 * are its caption and the line after it, so a band that changes ends jumps by
	 * the whole height of the region; for the region at the start of the file that
	 * lands on the first line of the document.
	 */
	@Test
	public void testTogglingARegionLeavesItsBandWhereItWas() throws Exception {
		installCodeMinings(provider);
		assertTrue(open(withChangedLines(45)).isOK());
		assertThat(collapsedRegions()).as("the regions start collapsed").isNotEmpty();
		Map<Integer, Integer> collapsedAnchors = bandAnchorsByRegion();
		assertThat(collapsedAnchors).as("there is a band to follow").isNotEmpty();

		viewer.doOperation(ProjectionViewer.EXPAND_ALL);
		waitForPendingWork();
		assertThat(collapsedRegions()).as("nothing is collapsed any more").isEmpty();

		assertEquals(collapsedAnchors, bandAnchorsByRegion(), "every band stays on the line it was clicked on");
	}

	/**
	 * The line each region's band currently sits on, keyed by the region's own first
	 * line. A band can only be on one of the two lines of its region that are
	 * visible while it is collapsed, so both are accepted here and it is the
	 * comparison across a toggle that decides whether the band moved.
	 */
	private Map<Integer, Integer> bandAnchorsByRegion() throws Exception {
		Map<Integer, Integer> anchors = new LinkedHashMap<>();
		for (ICodeMining mining : provide()) {
			if (!(mining instanceof FoldedRegionCodeMining band)) {
				continue;
			}
			int anchorLine = document.getLineOfOffset(band.getPosition().getOffset());
			Integer captionLine = null;
			for (FoldRegion region : allRegions()) {
				Position position = region.position();
				int first = document.getLineOfOffset(position.getOffset());
				int last = document.getLineOfOffset(position.getOffset() + position.getLength() - 1);
				if (anchorLine == first || anchorLine == last + 1) {
					captionLine = Integer.valueOf(first);
				}
			}
			assertNotNull(captionLine, "the band on line " + anchorLine + " belongs to no region");
			anchors.put(captionLine, Integer.valueOf(anchorLine));
		}
		return anchors;
	}

	/** Hiding a region again puts it back the way the first open had it. */
	@Test
	public void testHidingAnExpandedRegionCollapsesItAgain() throws Exception {
		installCodeMinings(provider);
		assertTrue(open(withChangedLines(10, 45)).isOK());
		int collapsedAtFirst = collapsedRegions().size();
		assertThat(collapsedAtFirst).isPositive();

		viewer.doOperation(ProjectionViewer.EXPAND_ALL);
		waitForPendingWork();
		assertThat(collapsedRegions()).isEmpty();

		for (ICodeMining mining : provide()) {
			if (mining instanceof FoldedRegionCodeMining band) {
				band.getAction().accept(null);
			}
		}
		waitForPendingWork();

		assertEquals(collapsedAtFirst, collapsedRegions().size(), "every region is collapsed again");
	}

	/**
	 * A document ending in a line delimiter has an empty last line. It has neither
	 * a character nor a line delimiter to repaint, so an annotation there never
	 * reaches the drawing strategy and its band stays invisible. The region running
	 * to the end of the document therefore has to stop above the last line that
	 * carries content.
	 */
	@Test
	public void testTheBandOfTheLastRegionSitsOnALineThatCanBePainted() throws Exception {
		assertEquals(0, document.getLineLength(document.getNumberOfLines() - 1),
				"the document under test ends in a line delimiter");
		installCodeMinings(provider);
		assertTrue(open(withChangedLines(10)).isOK());
		assertThat(collapsedRegions()).as("the unchanged regions are collapsed").isNotEmpty();

		List<FoldedRegionCodeMining> bands = new ArrayList<>();
		for (ICodeMining mining : provide()) {
			if (mining instanceof FoldedRegionCodeMining band) {
				bands.add(band);
			}
		}
		assertEquals(allRegions().size(), bands.size(), "every region keeps its band");
		for (FoldedRegionCodeMining band : bands) {
			int anchor = band.getPosition().getOffset();
			assertTrue(anchor < document.getLength(),
					"a band anchored at the end of the document is never painted, but one sits at " + anchor);
			assertThat(document.getLineLength(document.getLineOfOffset(anchor)))
					.as("the band's line has something to repaint").isPositive();
		}
	}

	/** The collapsed region whose last line is the one before the given offset. */
	private Position regionEndingBefore(List<Position> collapsed, int anchor) throws BadLocationException {
		for (Position region : collapsed) {
			int lastLine = document.getLineOfOffset(region.getOffset() + region.getLength() - 1);
			if (document.getLineOffset(lastLine + 1) == anchor) {
				return region;
			}
		}
		return null;
	}

	/**
	 * The case seen in the IDE: an editor with another code mining provider that
	 * answers asynchronously, such as the Java editor. Each request the reopen
	 * issues cancels the one before it, so the request that would have dropped the
	 * old minings never renders, and the framework keeps believing they are
	 * attached. The rebuilt minings must still all reach the editor.
	 */
	@Test
	public void testReopeningNextToAnAsynchronousProviderKeepsEveryMining() throws Exception {
		installCodeMinings(provider, new PendingProvider());
		String changed = withChangedLines(10, 45);

		assertTrue(open(changed).isOK());
		answerPendingRequests();
		List<UnifiedDiff> diffs = UnifiedDiffManager.get(viewer);
		assertEquals(2, diffs.size(), "one diff per changed line");
		assertThat(collapsedRegions()).as("the unchanged regions are collapsed").hasSize(3);
		waitForAttachedMinings(diffs, allRegions().size());

		assertTrue(open(changed).isOK());
		answerPendingRequests();
		diffs = UnifiedDiffManager.get(viewer);
		assertEquals(2, diffs.size(), "one diff per changed line");
		assertThat(collapsedRegions()).as("the unchanged regions are collapsed again").hasSize(3);
		waitForAttachedMinings(diffs, allRegions().size());
	}

	/**
	 * When the document does not end with a newline, the diff at the end cannot
	 * anchor a line-header mining (there is no following line to indent). A footer
	 * mining must be created instead.
	 */
	@Test
	public void testFooterMiningIsCreatedWhenDocumentHasNoTrailingNewline() throws Exception {
		switchToDocument(new Document("line 0\nline 1 changed"));

		IStatus status = UnifiedDiffManager.open(viewer, document, model, null, "line 0\nline 1\n", MODE, null, null,
				null, true, CONTEXT_LINES);
		assertTrue(status.isOK(), "open() should succeed: " + status);

		List<ICodeMining> minings = provide();
		List<UnifiedDiffFooterCodeMining> footers = new ArrayList<>();
		for (ICodeMining mining : minings) {
			if (mining instanceof UnifiedDiffFooterCodeMining footer) {
				footers.add(footer);
			}
		}
		assertThat(footers).as("a footer mining is used for the diff at the end of a document without trailing newline")
				.isNotEmpty();
	}

	/**
	 * A word-level footer highlight following bold text must start after the bold
	 * text, not where the base font would put it. Inspects the pixels
	 * {@link UnifiedDiffFooterCodeMining#draw(GC, org.eclipse.swt.custom.StyledText, Color, int, int)}
	 * paints.
	 */
	@Test
	public void testFooterMiningDetailedDiffUsesStyledTextPosition() throws Exception {
		// several bold keywords, so the bold/regular difference clearly exceeds a space
		String keywords = "public public public public public public public public";
		switchToDocument(new Document("line 0\n" + keywords));

		IStatus status = UnifiedDiffManager.open(viewer, document, model, null, "line 0\n" + keywords + " changed\n",
				MODE, null, null, null, true, CONTEXT_LINES);
		assertTrue(status.isOK(), "open() should succeed: " + status);

		List<ICodeMining> minings = provide();
		UnifiedDiffFooterCodeMining footer = null;
		for (ICodeMining mining : minings) {
			if (mining instanceof UnifiedDiffFooterCodeMining f) {
				footer = f;
			}
		}
		assertNotNull(footer, "a footer mining must be present");

		Color deletionColor = new Color(display, 11, 22, 33);
		Color detailedDiffColor = new Color(display, 44, 55, 66);
		UnifiedDiffFooterCodeMining paintingFooter = new UnifiedDiffFooterCodeMining(document, provider,
				footer.getUnifiedDiff(), 4, deletionColor, detailedDiffColor, viewer);
		Image image = new Image(display, 1200, 100);
		GC gc = new GC(image);
		Font boldFont = null;
		try {
			paintingFooter.draw(gc, viewer.getTextWidget(), null, 0, 0);

			Font baseFont = viewer.getTextWidget().getFont();
			FontData[] boldData = baseFont.getFontData();
			for (FontData fontData : boldData) {
				fontData.setStyle(SWT.BOLD);
			}
			boldFont = new Font(display, boldData);
			// stringExtent measures at the display zoom, getImageData() at 100%
			int zoom = display.getPrimaryMonitor().getZoom();
			gc.setFont(baseFont);
			int baseWidth = gc.stringExtent(keywords + " ").x * 100 / zoom;
			gc.setFont(boldFont);
			int boldWidth = gc.stringExtent(keywords).x * 100 / zoom;
			assumeTrue(boldWidth > baseWidth, "the viewer font renders bold no wider than regular, "
					+ "so this test cannot tell the two measurements apart");

			int firstDetailedColumn = firstColumnWith(image, detailedDiffColor);
			assertThat(firstDetailedColumn).as("draw() must paint the word-level detailed-diff background")
					.isGreaterThanOrEqualTo(0);
			assertThat(firstDetailedColumn)
					.as("the highlight must be positioned with the bold font, not the narrower base font")
					.isGreaterThan(baseWidth);
		} finally {
			gc.dispose();
			image.dispose();
			paintingFooter.dispose();
			if (boldFont != null) {
				boldFont.dispose();
			}
			deletionColor.dispose();
			detailedDiffColor.dispose();
		}
	}

	/**
	 * A detailed diff spanning several lines must restart each line's word-level
	 * highlight at the line start instead of accumulating the width of the
	 * preceding lines, which would make the highlight cascade to the right.
	 */
	@Test
	public void testFooterMiningMultiLineDetailedDiffResetsPerLine() throws Exception {
		switchToDocument(new Document("line 0\nx"));

		String target = "line 0\nalpha alpha\nbravo bravo\ncarol carol\n";
		IStatus status = UnifiedDiffManager.open(viewer, document, model, null, target, MODE, null, null, null, true,
				CONTEXT_LINES);
		assertTrue(status.isOK(), "open() should succeed: " + status);

		List<ICodeMining> minings = provide();
		UnifiedDiffFooterCodeMining footer = null;
		for (ICodeMining mining : minings) {
			if (mining instanceof UnifiedDiffFooterCodeMining f) {
				footer = f;
			}
		}
		assertNotNull(footer, "a footer mining must be present");

		Color deletionColor = new Color(display, 11, 22, 33);
		Color detailedDiffColor = new Color(display, 44, 55, 66);
		UnifiedDiffFooterCodeMining paintingFooter = new UnifiedDiffFooterCodeMining(document, provider,
				footer.getUnifiedDiff(), 4, deletionColor, detailedDiffColor, viewer);
		Image image = new Image(display, 400, 200);
		GC gc = new GC(image);
		try {
			paintingFooter.draw(gc, viewer.getTextWidget(), null, 0, 0);

			int lineHeight = viewer.getTextWidget().getLineHeight();
			int lineSpacing = viewer.getTextWidget().getLineSpacing();
			// the line metrics are at the display zoom, so read the pixels at that zoom
			int zoom = display.getPrimaryMonitor().getZoom();
			ImageData data = image.getImageData(zoom);
			RGB detailedRgb = detailedDiffColor.getRGB();

			List<Integer> paintedLastColumns = new ArrayList<>();
			for (int lineIndex = 0; lineIndex < 8; lineIndex++) {
				int yTop = lineIndex * (lineHeight + lineSpacing);
				int lastColumn = lastColumnWithInBand(data, detailedRgb, yTop, yTop + lineHeight);
				if (lastColumn >= 0) {
					paintedLastColumns.add(lastColumn);
				}
			}
			assertThat(paintedLastColumns).as("the multi-line detailed-diff background must span several lines")
					.hasSizeGreaterThanOrEqualTo(2);
			// cascading would widen each line by about a full line width
			int min = paintedLastColumns.stream().mapToInt(Integer::intValue).min().getAsInt();
			int max = paintedLastColumns.stream().mapToInt(Integer::intValue).max().getAsInt();
			assertThat(max).as("later lines must not cascade to the right").isLessThanOrEqualTo(min * 2);
		} finally {
			gc.dispose();
			image.dispose();
			paintingFooter.dispose();
			deletionColor.dispose();
			detailedDiffColor.dispose();
		}
	}

	/**
	 * Returns the rightmost column in {@code [yTop, yBottom)} holding
	 * {@code target}, or {@code -1}. Matches with a small tolerance against
	 * anti-aliasing.
	 */
	private static int lastColumnWithInBand(ImageData data, RGB target, int yTop, int yBottom) {
		int bottom = Math.min(yBottom, data.height);
		int last = -1;
		for (int x = 0; x < data.width; x++) {
			for (int y = Math.max(0, yTop); y < bottom; y++) {
				RGB rgb = data.palette.getRGB(data.getPixel(x, y));
				if (Math.abs(rgb.red - target.red) <= 2 && Math.abs(rgb.green - target.green) <= 2
						&& Math.abs(rgb.blue - target.blue) <= 2) {
					last = x;
					break;
				}
			}
		}
		return last;
	}

	/**
	 * Returns the leftmost column holding {@code color}, or {@code -1}. Matches
	 * with a small tolerance against anti-aliasing.
	 */
	private static int firstColumnWith(Image image, Color color) {
		ImageData data = image.getImageData();
		RGB target = color.getRGB();
		for (int x = 0; x < data.width; x++) {
			for (int y = 0; y < data.height; y++) {
				RGB rgb = data.palette.getRGB(data.getPixel(x, y));
				if (Math.abs(rgb.red - target.red) <= 2 && Math.abs(rgb.green - target.green) <= 2
						&& Math.abs(rgb.blue - target.blue) <= 2) {
					return x;
				}
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------ helpers

	/**
	 * A test configuration that colors all text and renders {@code public} in bold.
	 */
	private static SourceViewerConfiguration syntaxColoringConfiguration() {
		return new SourceViewerConfiguration() {
			@Override
			public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
				PresentationReconciler reconciler = new PresentationReconciler();
				RuleBasedScanner scanner = new RuleBasedScanner();
				Color fg = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
				scanner.setDefaultReturnToken(new Token(new TextAttribute(fg)));
				WordRule publicKeyword = new WordRule(new IWordDetector() {
					@Override
					public boolean isWordStart(char character) {
						return Character.isJavaIdentifierStart(character);
					}

					@Override
					public boolean isWordPart(char character) {
						return Character.isJavaIdentifierPart(character);
					}
				});
				publicKeyword.addWord("public", new Token(new TextAttribute(fg, null, SWT.BOLD)));
				scanner.setRules(new IRule[] { publicKeyword });
				DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
				reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
				reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
				return reconciler;
			}
		};
	}

	/** A provider that answers only when the test lets it, like a slow editor. */
	private final class PendingProvider implements ICodeMiningProvider {

		@Override
		public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer textViewer,
				IProgressMonitor monitor) {
			CompletableFuture<List<? extends ICodeMining>> request = new CompletableFuture<>();
			pendingRequests.add(request);
			return request;
		}

		@Override
		public void dispose() {
			// nothing to release
		}
	}

	private void installCodeMinings(ICodeMiningProvider... providers) {
		viewer.setCodeMiningProviders(providers);
		AnnotationPainter painter = new AnnotationPainter(viewer, null);
		viewer.setCodeMiningAnnotationPainter(painter);
		viewer.addPainter(painter);
	}

	private IStatus open(String source) {
		return UnifiedDiffManager.open(viewer, document, model, null, source, MODE, null, null, null, true,
				CONTEXT_LINES);
	}

	/**
	 * Lets the other provider answer every request made so far, once the fold
	 * listener had its turn and cancelled the requests it supersedes.
	 */
	private void answerPendingRequests() {
		waitForPendingWork();
		List<CompletableFuture<List<? extends ICodeMining>>> requests = new ArrayList<>(pendingRequests);
		pendingRequests.clear();
		for (CompletableFuture<List<? extends ICodeMining>> request : requests) {
			request.complete(List.of());
		}
		waitForPendingWork();
	}

	private List<ICodeMining> provide() throws Exception {
		return new ArrayList<>(
				provider.provideCodeMinings(viewer, new NullProgressMonitor()).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
	}

	/**
	 * One overlay mining per diff, showing that very diff, and one expander per
	 * collapsed region, anchored to the first line after it.
	 */
	private void assertMinings(List<ICodeMining> minings, List<UnifiedDiff> diffs) {
		List<UnifiedDiff> shown = new ArrayList<>();
		List<Integer> expanders = new ArrayList<>();
		for (ICodeMining mining : minings) {
			if (mining instanceof UnifiedDiffLineHeaderCodeMining overlay) {
				shown.add(overlay.getUnifiedDiff());
			} else if (mining instanceof UnifiedDiffFooterCodeMining footer) {
				shown.add(footer.getUnifiedDiff());
			} else if (mining instanceof FoldedRegionCodeMining expander) {
				expanders.add(Integer.valueOf(expander.getPosition().getOffset()));
			} else {
				fail("unexpected mining " + mining);
			}
		}
		assertThat(shown).as("one overlay mining per shown diff").containsExactlyInAnyOrderElementsOf(diffs);
		assertThat(expanders).as("one band per region, wherever its state puts it")
				.containsExactlyInAnyOrderElementsOf(allRegions().stream().map(this::expectedAnchorOf).toList());
	}

	/**
	 * Where the band of a region belongs, in either state. A collapsed region keeps
	 * its own first line visible as the fold's caption, so the line after the region
	 * is the one line that is visible whether the region is collapsed or not, and
	 * anchoring there keeps the band still while the user toggles it.
	 */
	private Integer expectedAnchorOf(FoldRegion region) {
		try {
			int lastLine = document
					.getLineOfOffset(region.position().getOffset() + region.position().getLength() - 1);
			return Integer.valueOf(document.getLineOffset(lastLine + 1));
		} catch (BadLocationException e) {
			return fail("the region reaches past the document", e);
		}
	}

	/** An unchanged-region fold and whether it is currently collapsed. */
	private record FoldRegion(Position position, boolean collapsed) {
	}

	/** Every unchanged-region fold of the viewer, collapsed or expanded. */
	private List<FoldRegion> allRegions() {
		List<FoldRegion> result = new ArrayList<>();
		ProjectionAnnotationModel projectionModel = viewer.getProjectionAnnotationModel();
		for (Iterator<Annotation> it = projectionModel.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation = it.next();
			if (annotation instanceof ProjectionAnnotation fold) {
				result.add(new FoldRegion(projectionModel.getPosition(annotation), fold.isCollapsed()));
			}
		}
		return result;
	}

	/** Waits until the framework attached exactly the minings of the diffs and regions. */
	private void waitForAttachedMinings(List<UnifiedDiff> diffs, int regions) {
		waitUntil(() -> {
			List<UnifiedDiff> shown = new ArrayList<>();
			int expanders = 0;
			for (ICodeMining mining : attachedMinings()) {
				if (mining instanceof UnifiedDiffLineHeaderCodeMining overlay) {
					shown.add(overlay.getUnifiedDiff());
				} else if (mining instanceof UnifiedDiffFooterCodeMining footer) {
					shown.add(footer.getUnifiedDiff());
				} else if (mining instanceof FoldedRegionCodeMining) {
					expanders++;
				}
			}
			return shown.size() == diffs.size() && shown.containsAll(diffs) && diffs.containsAll(shown)
					&& expanders == regions;
		}, () -> "the minings of " + diffs.size() + " diffs and " + regions
				+ " regions are attached, but the model holds " + attachedMinings());
	}

	private List<ICodeMining> attachedMinings() {
		List<ICodeMining> result = new ArrayList<>();
		for (Iterator<Annotation> it = model.getAnnotationIterator(); it.hasNext();) {
			if (it.next() instanceof AbstractInlinedAnnotation inlined) {
				result.addAll(inlined.getMinings());
			}
		}
		return result;
	}

	private Annotation attachedMiningAnnotationOf(UnifiedDiff diff) {
		for (Iterator<Annotation> it = model.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation = it.next();
			if (annotation instanceof AbstractInlinedAnnotation inlined) {
				for (ICodeMining mining : inlined.getMinings()) {
					if (mining instanceof UnifiedDiffLineHeaderCodeMining overlay && overlay.getUnifiedDiff() == diff) {
						return annotation;
					}
				}
			}
		}
		return fail("no mining is attached for the diff on line " + diff.leftStart);
	}

	private List<Position> collapsedRegions() {
		List<Position> result = new ArrayList<>();
		ProjectionAnnotationModel projectionModel = viewer.getProjectionAnnotationModel();
		for (Iterator<Annotation> it = projectionModel.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation = it.next();
			if (annotation instanceof ProjectionAnnotation fold && fold.isCollapsed()) {
				result.add(projectionModel.getPosition(annotation));
			}
		}
		return result;
	}

	private void waitUntil(BooleanSupplier condition, Supplier<String> what) {
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline) {
				fail("timed out waiting until " + what.get());
			}
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					fail("interrupted");
				}
			}
		}
	}

	private void waitForPendingWork() {
		while (display.readAndDispatch()) {
			// keep going until the queue is empty
		}
	}

	/** The document with the given lines changed, as the other side of the diff. */
	private String withChangedLines(int... lines) {
		String content = document.get();
		for (int line : lines) {
			content = content.replace("line " + line + "\n", "line " + line + " changed\n");
		}
		return content;
	}

	/** The diffs the manager would record for the given changed lines. */
	private List<UnifiedDiff> diffsOnLines(int... lines) throws BadLocationException {
		List<UnifiedDiff> diffs = new ArrayList<>();
		for (int line : lines) {
			int offset = document.getLineOffset(line);
			int length = document.getLineLength(line);
			diffs.add(new UnifiedDiff(document, offset, offset + length, document.get(offset, length), document,
					offset, offset + length + 8, "line " + line + " changed\n", diffs, MODE));
		}
		return diffs;
	}

	private static IDocument numberedLines(int count) {
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < count; i++) {
			content.append("line ").append(i).append('\n');
		}
		return new Document(content.toString());
	}

	/**
	 * Replaces the viewer's document mid-test. Disconnects the undo manager from
	 * the old document, connects it to the new one, and re-wires the viewer.
	 */
	private void switchToDocument(IDocument newDocument) {
		DocumentUndoManagerRegistry.disconnect(document);
		document = newDocument;
		model = new AnnotationModel();
		viewer.setDocument(document, model);
		DocumentUndoManagerRegistry.connect(document);
		installCodeMinings(provider);
	}
}
