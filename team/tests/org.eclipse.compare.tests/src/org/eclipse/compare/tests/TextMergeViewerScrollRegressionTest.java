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
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.MergeSourceViewer;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TextMergeViewerScrollRegressionTest {
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
		textWidget.setLineSpacing(50);
		shell.open();
	}

	@AfterEach
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void testRepaintAfterFontHeightChangesWhileScrolled() throws Exception {
		int initialHeight = heightBetweenLines(0, 80);
		Font originalFont = textWidget.getFont();
		FontData[] fontData = originalFont.getFontData();
		for (FontData data : fontData) {
			data.setHeight(data.getHeight() + 8);
		}
		Font largerFont = new Font(textWidget.getDisplay(), fontData);
		try {
			textWidget.setFont(largerFont);
			textWidget.setTopIndex(70);
			repaint();
			int expectedHeight = textWidget.getLinePixel(80) - textWidget.getLinePixel(0);
			assertTrue(expectedHeight > initialHeight);
			assertEquals(expectedHeight, heightBetweenLines(0, 80));
		} finally {
			textWidget.setFont(originalFont);
			largerFont.dispose();
		}
	}

	@Test
	public void testRepaintDetectsStalePrefixWithUnchangedBaseMetrics() throws Exception {
		int initialHeight = heightBetweenLines(0, 80);
		int baseHeight = textWidget.getLineHeight();
		textWidget.setLineVerticalIndent(75, 13);
		textWidget.setTopIndex(70);
		assertEquals(baseHeight, textWidget.getLineHeight());
		repaint();
		assertEquals(initialHeight + 13, heightBetweenLines(0, 80));
		assertEquals(textWidget.getLinePixel(80) - textWidget.getLinePixel(0), heightBetweenLines(0, 80));
	}

	@Test
	public void testBalancedIndentRelocationUpdatesIntermediateLineY() throws Exception {
		textWidget.setLineVerticalIndent(10, 13);
		int initialHeight = heightBetweenLines(0, 80);
		int initialLineY = lineY(20);
		textWidget.setLineVerticalIndent(10, 0);
		textWidget.setLineVerticalIndent(75, 13);
		repaint();
		assertEquals(initialHeight, heightBetweenLines(0, 80));
		assertEquals(initialLineY - 13, lineY(20));
		assertLineYMatchesWidget(20);
		assertEquals(textWidget.getLinePixel(20) - textWidget.getLinePixel(0), heightBetweenLines(0, 20));
	}

	@Test
	public void testScrolledLineYWithPopulatedCacheAndWithoutCaching() throws Exception {
		for (int spacing : new int[] { 50, 0 }) {
			sourceViewer.getSourceViewer().setDocument(new Document("unchanged context\n".repeat(100)));
			textWidget.setLineSpacing(spacing);
			textWidget.setTopMargin(19);
			textWidget.setLineVerticalIndent(10, 13);
			heightBetweenLines(0, 80);
			textWidget.setLineVerticalIndent(10, 0);
			textWidget.setLineVerticalIndent(75, 13);
			textWidget.setTopIndex(70);
			repaint();
			for (int line : new int[] { 0, 20, 75, 80, 100 }) {
				assertLineYMatchesWidget(line);
			}
		}
	}

	@Test
	public void testScrolledLineYBeforeCachePopulation() throws Exception {
		textWidget.setTopMargin(19);
		textWidget.setLineVerticalIndent(10, 13);
		textWidget.setLineVerticalIndent(75, 13);
		textWidget.setTopIndex(70);
		for (int line : new int[] { 20, 75, 80, 100 }) {
			assertLineYMatchesWidget(line);
		}
	}

	private int lineY(int line) throws Exception {
		return viewer.getAdapter(IMergeViewerTestAdapter.class).getLineY(sourceViewer, line);
	}

	private void assertLineYMatchesWidget(int line) throws Exception {
		int actualY = lineY(line);
		assertEquals(textWidget.getLinePixel(line) - textWidget.getTopMargin()
				+ sourceViewer.getSourceViewer().getTopInset(), actualY);
	}

	private int heightBetweenLines(int fromLine, int toLine) throws Exception {
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
