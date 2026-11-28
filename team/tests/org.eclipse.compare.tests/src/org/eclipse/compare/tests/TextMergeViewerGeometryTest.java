/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
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
package org.eclipse.compare.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.MergeSourceViewer;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TextMergeViewerGeometryTest {
	private Shell shell;
	private TextMergeViewer viewer;
	private MergeSourceViewer sourceViewer;
	private StyledText textWidget;

	@BeforeEach
	public void setUp() throws Exception {
		shell = new Shell(Display.getDefault());
		shell.setLayout(new FillLayout());
		shell.setSize(800, 400);
		viewer = new TextMergeViewer(shell, SWT.NONE, new CompareConfiguration());
		sourceViewer = (MergeSourceViewer) ReflectionUtils.getField(viewer, "fLeft", true);
		sourceViewer.getSourceViewer().setDocument(new Document("unchanged context\n".repeat(100)));
		textWidget = sourceViewer.getSourceViewer().getTextWidget();
		shell.open();
	}

	@AfterEach
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void testLineHeightChangesWithoutDocumentChanges() throws Exception {
		textWidget.setLineSpacing(50);
		int initialHeight = heightBetweenLines(0, 80);
		Object initialCache = caches().get(sourceViewer);
		Font originalFont = textWidget.getFont();
		FontData[] fontData = originalFont.getFontData();
		for (FontData data : fontData) {
			data.setHeight(data.getHeight() + 8);
		}
		Font largerFont = new Font(textWidget.getDisplay(), fontData);
		try {
			textWidget.setFont(largerFont);
			textWidget.setTopIndex(70);
			int expectedHeight = textWidget.getLinePixel(80) - textWidget.getLinePixel(0);
			assertTrue(expectedHeight > initialHeight);
			assertEquals(expectedHeight, heightBetweenLines(0, 80));
			assertNotSame(initialCache, caches().get(sourceViewer));
			assertEquals(textWidget.getLinePixel(81) - textWidget.getLinePixel(80), heightBetweenLines(80, 81));
		} finally {
			textWidget.setFont(originalFont);
			largerFont.dispose();
		}
	}

	@Test
	public void testLineSpacingAndIndentChangesWhileScrolled() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 80);
		textWidget.setLineSpacing(0);
		textWidget.setLineSpacingProvider(line -> line % 3 == 0 ? 7 : 0);
		textWidget.setLineVerticalIndent(75, 13);
		textWidget.setTopIndex(70);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertEquals(textWidget.getLinePixel(76) - textWidget.getLinePixel(75), heightBetweenLines(75, 76));
		assertEquals(0, heightBetweenLines(80, 80));
		assertEquals(textWidget.getLinePixel(textWidget.getLineCount()) - textWidget.getLinePixel(99),
				heightBetweenLines(99, textWidget.getLineCount()));
		assertEquals(false, ReflectionUtils.getField(caches().get(sourceViewer), "usable"));
	}

	@Test
	public void testOutOfDocumentHeightKeepsExistingEstimate() throws Exception {
		textWidget.setLineVerticalIndent(0, 13);
		for (int spacing : new int[] { 0, 50 }) {
			textWidget.setLineSpacing(spacing);
			int lastLine = textWidget.getLineCount();
			int expectedHeight = 3 * (textWidget.getLineHeight() + textWidget.getLineVerticalIndent(0)) + spacing;
			assertEquals(expectedHeight, heightBetweenLines(lastLine, lastLine + 3));
			assertEquals(0, heightBetweenLines(lastLine + 3, lastLine + 3));
		}
	}

	@Test
	public void testCacheReusedWithUnchangedMetrics() throws Exception {
		textWidget.setLineSpacing(50);
		int initialHeight = heightBetweenLines(0, 80);
		Object initialCache = caches().get(sourceViewer);
		textWidget.setTopIndex(70);
		assertEquals(initialHeight, heightBetweenLines(0, 80));
		assertSame(initialCache, caches().get(sourceViewer));
	}

	@Test
	public void testZeroSpacingWithVerticalIndentsKeepsCache() throws Exception {
		textWidget.setLineSpacing(0);
		textWidget.setLineVerticalIndent(10, 13);
		int initialHeight = heightBetweenLines(0, 80);
		Object initialCache = caches().get(sourceViewer);
		assertNotNull(initialCache, "Vertical indents need caching even with zero line spacing");
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), initialHeight);
		textWidget.setTopIndex(70);
		assertEquals(initialHeight, heightBetweenLines(0, 80));
		assertSame(initialCache, caches().get(sourceViewer));
		textWidget.setLineVerticalIndent(10, 0);
		textWidget.setLineVerticalIndent(75, 13);
		assertEquals(initialHeight, heightBetweenLines(0, 80));
		assertEquals(textWidget.getLinePixel(20) - textWidget.getLinePixel(0), heightBetweenLines(0, 20));
		assertSame(initialCache, caches().get(sourceViewer));
	}

	@Test
	public void testCachedIndentAndSpacingChanges() throws Exception {
		textWidget.setLineSpacing(50);
		int initialHeight = heightBetweenLines(0, 80);
		textWidget.setLineVerticalIndent(75, 13);
		assertEquals(initialHeight + 13, heightBetweenLines(0, 80));
		textWidget.setLineSpacing(60);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
	}

	@Test
	public void testCacheInvalidatedByDocumentChanges() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 80);
		Object initialCache = caches().get(sourceViewer);
		sourceViewer.getSourceViewer().getDocument().replace(0, 0, "inserted line\n");
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertNotSame(initialCache, caches().get(sourceViewer));
	}

	@Test
	public void testCacheInvalidatedBySameLineCountDocumentChanges() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 80);
		Object initialCache = caches().get(sourceViewer);
		int initialLineCount = textWidget.getLineCount();
		sourceViewer.getSourceViewer().getDocument().replace(textWidget.getOffsetAtLine(10), 9, "replacement");
		assertEquals(initialLineCount, textWidget.getLineCount());
		assertNull(caches().get(sourceViewer));
		assertEquals(textWidget.getLinePixel(20) - textWidget.getLinePixel(0), heightBetweenLines(0, 20));
		assertNotSame(initialCache, caches().get(sourceViewer));
	}

	@Test
	public void testWordWrapBypassesCache() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 80);
		textWidget.setWordWrap(true);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertNull(caches().get(sourceViewer));
	}

	@Test
	public void testCacheInvalidatedByPresentationChanges() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 80);
		Object initialCache = caches().get(sourceViewer);
		TextPresentation presentation = new TextPresentation();
		presentation.addStyleRange(new StyleRange(0, 4, null, null, SWT.BOLD));
		sourceViewer.getSourceViewer().changeTextPresentation(presentation, true);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertNotSame(initialCache, caches().get(sourceViewer));
	}

	@Test
	public void testCheckpointOnlyUsesPopulatedPrefix() throws Exception {
		textWidget.setLineSpacing(50);
		int initialHeight = heightBetweenLines(0, 20);
		Object initialCache = caches().get(sourceViewer);
		assertEquals(20, ReflectionUtils.getField(initialCache, "prefixLines"));
		int[] heights = (int[]) ReflectionUtils.getField(initialCache, "heights");
		assertEquals(-1, heights[20]);
		textWidget.setLineVerticalIndent(10, 13);
		assertEquals(initialHeight + 13, heightBetweenLines(0, 20));
		Object refreshedCache = caches().get(sourceViewer);
		assertSame(initialCache, refreshedCache);
		assertEquals(20, ReflectionUtils.getField(refreshedCache, "prefixLines"));
		assertEquals(-1, ((int[]) ReflectionUtils.getField(refreshedCache, "heights"))[20]);
	}

	@Test
	public void testStaleCachePreservesPrefixAndDropsPopulatedSuffix() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 20);
		heightBetweenLines(70, 80);
		Object initialCache = caches().get(sourceViewer);
		int[] heights = (int[]) ReflectionUtils.getField(initialCache, "heights");
		int preservedHeight = heightBetweenLines(0, 10);
		assertTrue(heights[70] > 0);
		textWidget.setLineVerticalIndent(10, 13);
		assertEquals(preservedHeight, heightBetweenLines(0, 10));
		assertSame(initialCache, caches().get(sourceViewer));
		assertEquals(10, ReflectionUtils.getField(initialCache, "prefixLines"));
		assertEquals(preservedHeight, ReflectionUtils.getField(initialCache, "prefixHeight"));
		for (int line = 0; line < heights.length; line++) {
			if (line < 10) {
				assertTrue(heights[line] > 0);
			} else {
				assertEquals(-1, heights[line]);
			}
		}
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
	}

	@Test
	public void testStaleCacheAtFirstLineDropsAllEntries() throws Exception {
		textWidget.setLineSpacing(50);
		int initialHeight = heightBetweenLines(0, 20);
		Object initialCache = caches().get(sourceViewer);
		textWidget.setLineVerticalIndent(0, 13);
		assertEquals(initialHeight + 13, heightBetweenLines(0, 20));
		assertSame(initialCache, caches().get(sourceViewer));
		assertEquals(textWidget.getLinePixel(20) - textWidget.getLinePixel(0), heightBetweenLines(0, 20));
	}

	@Test
	public void testBalancedIndentRelocationValidatesConsumedRange() throws Exception {
		textWidget.setLineSpacing(50);
		textWidget.setLineVerticalIndent(10, 13);
		int initialHeight = heightBetweenLines(0, 80);
		int initialIntermediateHeight = heightBetweenLines(0, 20);
		Object initialCache = caches().get(sourceViewer);
		int[] heights = (int[]) ReflectionUtils.getField(initialCache, "heights");
		int preservedHeight = heightBetweenLines(0, 10);
		textWidget.setLineVerticalIndent(10, 0);
		textWidget.setLineVerticalIndent(75, 13);
		repaint();
		assertEquals(initialHeight, textWidget.getLinePixel(80) - textWidget.getLinePixel(0));
		assertSame(initialCache, caches().get(sourceViewer));
		assertEquals(10, ReflectionUtils.getField(initialCache, "prefixLines"));
		assertEquals(preservedHeight, ReflectionUtils.getField(initialCache, "prefixHeight"));
		for (int line = 0; line < heights.length; line++) {
			if (line < 10) {
				assertTrue(heights[line] > 0);
			} else {
				assertEquals(-1, heights[line]);
			}
		}
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertEquals(initialIntermediateHeight - 13, heightBetweenLines(0, 20));
	}

	@Test
	public void testBalancedIndentRelocationBeyondCacheGap() throws Exception {
		textWidget.setLineSpacing(50);
		textWidget.setLineVerticalIndent(75, 13);
		int preservedHeight = heightBetweenLines(0, 20);
		int initialHeight = heightBetweenLines(70, 80);
		int initialIntermediateHeight = heightBetweenLines(70, 76);
		Object initialCache = caches().get(sourceViewer);
		int[] heights = (int[]) ReflectionUtils.getField(initialCache, "heights");
		textWidget.setLineVerticalIndent(75, 0);
		textWidget.setLineVerticalIndent(78, 13);
		repaint();
		assertEquals(initialHeight, textWidget.getLinePixel(80) - textWidget.getLinePixel(70));
		assertSame(initialCache, caches().get(sourceViewer));
		assertEquals(20, ReflectionUtils.getField(initialCache, "prefixLines"));
		assertEquals(preservedHeight, ReflectionUtils.getField(initialCache, "prefixHeight"));
		for (int line = 0; line < heights.length; line++) {
			if (line < 20 || line >= 70 && line < 75) {
				assertTrue(heights[line] > 0);
			} else {
				assertEquals(-1, heights[line]);
			}
		}
		assertEquals(initialIntermediateHeight - 13, heightBetweenLines(70, 76));
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(70), heightBetweenLines(70, 80));
	}

	@Test
	public void testIndividualStyleHeightChangePreservesPrefix() throws Exception {
		textWidget.setLineSpacing(50);
		int initialHeight = heightBetweenLines(0, 80);
		Object initialCache = caches().get(sourceViewer);
		int preservedHeight = heightBetweenLines(0, 75);
		StyleRange style = new StyleRange(textWidget.getOffsetAtLine(75), 1, null, null);
		style.metrics = new GlyphMetrics(textWidget.getLineHeight() + 13, 0, 8);
		textWidget.setStyleRange(style);
		repaint();
		assertSame(initialCache, caches().get(sourceViewer));
		assertEquals(75, ReflectionUtils.getField(initialCache, "prefixLines"));
		assertEquals(preservedHeight, ReflectionUtils.getField(initialCache, "prefixHeight"));
		assertTrue(heightBetweenLines(0, 80) > initialHeight);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
	}

	@Test
	public void testBalancedVariableHeightStylesInvalidateCache() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 80);
		int unstyledIntermediateHeight = heightBetweenLines(0, 20);
		Object unstyledCache = caches().get(sourceViewer);
		StyleRange style = new StyleRange(textWidget.getOffsetAtLine(10), 1, null, null);
		style.metrics = new GlyphMetrics(textWidget.getLineHeight() + 13, 0, 8);
		TextPresentation presentation = new TextPresentation();
		presentation.setDefaultStyleRange(new StyleRange(0, textWidget.getCharCount(), null, null));
		presentation.addStyleRange(style);
		sourceViewer.getSourceViewer().changeTextPresentation(presentation, true);
		int initialHeight = heightBetweenLines(0, 80);
		int initialIntermediateHeight = heightBetweenLines(0, 20);
		assertTrue(initialIntermediateHeight > unstyledIntermediateHeight);
		assertNotSame(unstyledCache, caches().get(sourceViewer));
		Object styledCache = caches().get(sourceViewer);
		style.start = textWidget.getOffsetAtLine(75);
		presentation = new TextPresentation();
		presentation.setDefaultStyleRange(new StyleRange(0, textWidget.getCharCount(), null, null));
		presentation.addStyleRange(style);
		sourceViewer.getSourceViewer().changeTextPresentation(presentation, true);
		assertEquals(initialHeight, heightBetweenLines(0, 80));
		assertEquals(unstyledIntermediateHeight, heightBetweenLines(0, 20));
		assertNotSame(styledCache, caches().get(sourceViewer));
	}

	@Test
	public void testDynamicLineStylesBypassCache() throws Exception {
		textWidget.setLineSpacing(50);
		heightBetweenLines(0, 80);
		textWidget.addLineStyleListener(event -> {
			StyleRange style = new StyleRange(event.lineOffset, 1, null, null);
			style.metrics = new GlyphMetrics(textWidget.getLineHeight() + 13, 0, 8);
			event.styles = new StyleRange[] { style };
		});
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertNull(caches().get(sourceViewer));
	}

	@Test
	public void testBalancedProviderSpacingKeepsLineCoordinatesAligned() throws Exception {
		textWidget.setLineSpacing(50);
		textWidget.setLineSpacingProvider(line -> line == 0 ? 60 : line == 1 ? 40 : 50);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertEquals(textWidget.getLinePixel(1) - textWidget.getTopMargin()
				+ sourceViewer.getSourceViewer().getTopInset(), viewer.getAdapter(IMergeViewerTestAdapter.class).getLineY(sourceViewer, 1));
	}

	@Test
	public void testGeometryDisagreementBypassesCacheWithoutRefilling() throws Exception {
		textWidget.setLineSpacing(50);
		textWidget.setLineSpacingProvider(_ -> 60);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		Object initialCache = caches().get(sourceViewer);
		assertEquals(false, ReflectionUtils.getField(initialCache, "usable"));
		int[] heights = ((int[]) ReflectionUtils.getField(initialCache, "heights")).clone();
		for (int pass = 0; pass < 3; pass++) {
			assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
			assertSame(initialCache, caches().get(sourceViewer));
			assertEquals(false, ReflectionUtils.getField(initialCache, "usable"));
			assertTrue(java.util.Arrays.equals(heights, (int[]) ReflectionUtils.getField(initialCache, "heights")));
		}
		textWidget.setLineVerticalIndent(50, 13);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertSame(initialCache, caches().get(sourceViewer));
		assertEquals(false, ReflectionUtils.getField(initialCache, "usable"));
		textWidget.setLineSpacingProvider(null);
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
		assertSame(initialCache, caches().get(sourceViewer));
		assertEquals(true, ReflectionUtils.getField(initialCache, "usable"));
	}

	private Map<?, ?> caches() throws Exception {
		return (Map<?, ?>) ReflectionUtils.getField(viewer, "lineHeightsByViewer", true);
	}

	private int heightBetweenLines(int fromLine, int toLine) throws Exception {
		repaint();
		return viewer.getAdapter(IMergeViewerTestAdapter.class).getHeightBetweenLines(sourceViewer, fromLine, toLine);
	}

	private void repaint() throws Exception {
		GC gc = new GC(textWidget);
		try {
			Event event = new Event();
			event.widget = textWidget;
			event.gc = gc;
			viewer.getAdapter(IMergeViewerTestAdapter.class).paint(new PaintEvent(event), sourceViewer);
		} finally {
			gc.dispose();
		}
	}
}
