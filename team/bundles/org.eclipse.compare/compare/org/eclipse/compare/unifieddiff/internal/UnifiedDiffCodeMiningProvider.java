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
 * SAP - initial implementation
 *******************************************************************************/
package org.eclipse.compare.unifieddiff.internal;

import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.error;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.isOverlay;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.DocumentFooterCodeMining;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationReconcilerExtension;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.inlined.LineFooterAnnotation;
import org.eclipse.jface.text.source.inlined.LineHeaderAnnotation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class UnifiedDiffCodeMiningProvider extends AbstractCodeMiningProvider {

	private Color deletionBackgroundColor;
	private Color detailedDiffColor;
	private boolean lastIsOverlay;

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		List<UnifiedDiff> diffs = UnifiedDiffManager.get(viewer);
		if (diffs == null || diffs.size() == 0) {
			return CompletableFuture.completedFuture(new ArrayList<>());
		}
		boolean isOverlay = isOverlay(diffs);
		if (this.deletionBackgroundColor == null || isOverlay != lastIsOverlay) {
			// check class ColorPalette
			RGB background = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
			String colorName;
			if (isOverlay) {
				colorName = "ADDITION_COLOR"; //$NON-NLS-1$
			} else {
				colorName = "DELETION_COLOR"; //$NON-NLS-1$
			}
			RGB deletionColor = JFaceResources.getColorRegistry().getRGB(colorName);
			this.detailedDiffColor = new Color(interpolate(deletionColor, background, 0.9));
			this.deletionBackgroundColor = new Color(interpolate(deletionColor, background, 0.8));
			lastIsOverlay = isOverlay;
			// TODO (tm) remove me - add annotation extension with color 204, 229, 204
//			RGB additionColor = JFaceResources.getColorRegistry().getRGB("ADDITION_COLOR");
//			additionColor = interpolate(additionColor, background, 0.9);
		}
		if (viewer instanceof ISourceViewer sv && UnifiedDiffManager.get(viewer) != null) {
			List<ICodeMining> existingMinings = new ArrayList<>();
			IAnnotationModel model = sv.getAnnotationModel();
			Iterator<Annotation> it = model.getAnnotationIterator();
			while (it.hasNext()) {
				Annotation next = it.next();
				if (next instanceof LineHeaderAnnotation n) {
					try {
						List<ICodeMining> m = n.getMinings();
						if (m != null && m.size() == 1 && m.get(0) instanceof UnifiedDiffLineHeaderCodeMining idlhcm) {
							Position p = n.getPosition();
							IDocument doc = sv.getDocument();
							int line = doc.getLineOfOffset(p.offset);
							idlhcm.getPosition().offset = doc.getLineOffset(line); // we need to recalculate the line
																					// offset for the scenario where
																					// source is modified at the
																					// beginning of the line
							existingMinings.add(idlhcm);
						}
					} catch (BadLocationException e) {
						error(e);
					}
				} else if (next instanceof LineFooterAnnotation footer) {
					List<ICodeMining> m = footer.getMinings();
					if (m != null && m.size() == 1 && m.get(0) instanceof UnifiedDiffFooterCodeMining idlhcm) {
						IDocument doc = sv.getDocument();
						idlhcm.getPosition().offset = doc.getLength();
						existingMinings.add(idlhcm);
					}
				}
			}
			if (existingMinings.size() > 0) {
				return CompletableFuture.completedFuture(existingMinings);
			}
		}

		int tabWidth = getTabWidth(viewer);
		return CompletableFuture.supplyAsync(() -> {
			List<ICodeMining> minings = new ArrayList<>();
			createLineHeaderCodeMinings(diffs, minings, viewer, tabWidth);
			return minings;
		});
	}

	private int getTabWidth(ITextViewer viewer) {
		int tabWidth = -1;
		if (viewer != null && Display.getCurrent() != null) {
			StyledText tw = viewer.getTextWidget();
			tabWidth = tw.getTabs();
			if (tabWidth > 0) {
				return tabWidth;
			}
		}
		if (tabWidth == -1) {
			IPreferenceStore store = EditorsUI.getPreferenceStore();
			tabWidth = store.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
		}
		return tabWidth;
	}

	private void createLineHeaderCodeMinings(List<UnifiedDiff> diffs, List<ICodeMining> minings, ITextViewer tv,
			int tabWidth) {
		if (diffs == null) {
			return;
		}
		IDocument doc = tv.getDocument();
		for (UnifiedDiff diff : diffs) {
			if (diff.mode.equals(UnifiedDiffMode.REPLACE_MODE)) {
				if (diff.leftStr.isEmpty()) {
					continue;
				}
				try {
					ICodeMining mining;
					if (diff.leftStart == doc.getLength()) {
						mining = new UnifiedDiffFooterCodeMining(doc, this, null, diff, tabWidth,
								this.deletionBackgroundColor);
					} else {
						mining = new UnifiedDiffLineHeaderCodeMining(new Position(diff.leftStart, 1), this, diff,
								tabWidth, this.detailedDiffColor, this.deletionBackgroundColor, tv);
					}
					minings.add(mining);
				} catch (BadLocationException e) {
					error(e);
				}
			} else if (diff.mode.equals(UnifiedDiffMode.OVERLAY_MODE)
					|| diff.mode.equals(UnifiedDiffMode.OVERLAY_READ_ONLY_MODE)) {
				if (diff.rightStr.isEmpty()) {
					continue;
				}
				try {
					ICodeMining mining;
					if (diff.leftStart == doc.getLength()) {
						mining = new UnifiedDiffFooterCodeMining(doc, this, null, diff, tabWidth,
								this.deletionBackgroundColor);
					} else {
						mining = new UnifiedDiffLineHeaderCodeMining(new Position(diff.leftStart + diff.leftLength, 1),
								this, diff, tabWidth, this.detailedDiffColor, this.deletionBackgroundColor, tv);
					}
					minings.add(mining);
				} catch (BadLocationException e) {
					error(e);
				}
			}
		}
	}

	static class UnifiedDiffFooterCodeMining extends DocumentFooterCodeMining {
		private final String unifiedDiffLabel;
		private final Color deletionBackgroundColor;
		private UnifiedDiff diff;

		public UnifiedDiffFooterCodeMining(IDocument document, ICodeMiningProvider provider,
				Consumer<MouseEvent> action, UnifiedDiff diff, int tabWidth, Color deletionBackgroundColor) {
			super(document, provider, action);
			this.deletionBackgroundColor = deletionBackgroundColor;
			if (diff.mode.equals(UnifiedDiffMode.REPLACE_MODE)) {
				this.unifiedDiffLabel = replaceTabWithSpaces(diff.leftStr, tabWidth);
			} else {
				this.unifiedDiffLabel = replaceTabWithSpaces(diff.rightStr, tabWidth);
			}
			this.diff = diff;
		}

		@Override
		public String getLabel() {
			return this.unifiedDiffLabel;
		}

		public UnifiedDiff getUnifiedDiff() {
			return this.diff;
		}

		@Override
		public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
			gc.setBackground(this.deletionBackgroundColor);
			Color c = textWidget.getForeground();
			gc.setForeground(c);
			Font font = textWidget.getFont();
			gc.setFont(font);
			// first run to get width and height for label
			// change from https://github.com/eclipse-platform/eclipse.platform.ui/pull/3651
			// is required so that background correctly drawn with line spacing > 0
			Point result = super.draw(gc, textWidget, color, x, y);
			// draw background
			// vs code is drawing the background to the top right of the editor - we do here
			// the same!
			gc.fillRectangle(0, y, textWidget.getBounds().width /* result.x */, result.y);
			// draw foreground again
			result = super.draw(gc, textWidget, color, x, y);
			return result;
		}
	}

	public static class UnifiedDiffLineHeaderCodeMining extends LineHeaderCodeMining {
		private final String unifiedDiffLabel;
		private final Color deletionBackgroundColor;
		private final Color detailedDiffColor;
		private final UnifiedDiff diff;
		private final int tabWidth;
		private ITextViewer viewer;
		private Rectangle lastRectangle;

		public UnifiedDiffLineHeaderCodeMining(Position position, ICodeMiningProvider provider, UnifiedDiff diff,
				int tabWidth, Color deletionBackgroundColor, Color detailedDiffColor, ITextViewer viewer)
				throws BadLocationException {
			super(position, provider, new MouseClickConsumer(viewer));
			if (diff.mode.equals(UnifiedDiffMode.REPLACE_MODE)) {
				this.unifiedDiffLabel = replaceTabWithSpaces(diff.leftStr, tabWidth);
			} else {
				this.unifiedDiffLabel = replaceTabWithSpaces(diff.rightStr, tabWidth);
			}
			this.deletionBackgroundColor = deletionBackgroundColor;
			this.detailedDiffColor = detailedDiffColor;
			this.diff = diff;
			this.tabWidth = tabWidth;
			this.viewer = viewer;
			((MouseClickConsumer) getAction()).setCodeMining(this);
		}

		private static class MouseClickConsumer implements Consumer<MouseEvent> {

			private final ITextViewer viewer;
			private UnifiedDiffLineHeaderCodeMining mining;

			public MouseClickConsumer(ITextViewer viewer) {
				this.viewer = viewer;
			}

			public void setCodeMining(UnifiedDiffLineHeaderCodeMining mining) {
				this.mining = mining;
			}

			@Override
			public void accept(MouseEvent t) {
				if (mining == null || viewer == null || mining.lastRectangle == null) {
					return;
				}
				StyledText st = viewer.getTextWidget();
				StyledText overlay = new StyledText(st, SWT.NONE);
				overlay.setBounds(mining.lastRectangle);
				overlay.setFont(st.getFont());
				overlay.setBackground(mining.deletionBackgroundColor);
				overlay.setText(mining.getLabel().stripTrailing());
				overlay.setFocus();
				// TODO (tm) style ranges missing
				// TODO (tm) common keyboard shortcuts like ctrl-a, ctrl-right are not captured
				// by this text control if in focus
				overlay.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						overlay.dispose();
					}
				});
			}
		}

		@Override
		public String getLabel() {
			return this.unifiedDiffLabel;
		}

		public UnifiedDiff getUnifiedDiff() {
			return this.diff;
		}

		@Override
		public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
			gc.setBackground(this.deletionBackgroundColor);
			Color c = textWidget.getForeground();
			gc.setForeground(c);
			Font font = textWidget.getFont();
			gc.setFont(font);
			// first run to get width and height for label
			Point result = super.draw(gc, textWidget, color, x, y);
			lastRectangle = new Rectangle(x, y, result.x, result.y);
			// draw background
			// vs code is drawing the background to the top right of the editor - we do here
			// the same!
			gc.fillRectangle(0, y, textWidget.getBounds().width /* result.x */, result.y);

			String label = getLabel();
			List<StyleRange> ranges = getStyleRanges(viewer, label);
			HashMap<Font, Map<Integer /* style */, Font>> styledFonts = new HashMap<>();

			// draw darker background for detailed diff
			gc.setBackground(this.detailedDiffColor);
			for (var detailedDiff : this.diff.detailedDiffs) {
				String diffStr = this.diff.leftStr;
				String detailedDiffStr = detailedDiff.leftStr;
				int detailedDiffStart = detailedDiff.leftStart;
				int detailedDiffLength = detailedDiff.leftLength;
				if (diff.mode.equals(UnifiedDiffMode.OVERLAY_MODE)
						|| diff.mode.equals(UnifiedDiffMode.OVERLAY_READ_ONLY_MODE)) {
					diffStr = this.diff.rightStr;
					detailedDiffStr = detailedDiff.rightStr;
					detailedDiffStart = detailedDiff.rightStart;
					detailedDiffLength = detailedDiff.rightLength;
				}
				if (detailedDiffStr.trim().length() == 0) {
					continue;
				}
				try {
					String l = diffStr.substring(0, detailedDiffStart);
					int fromLine = l.split("\n").length; //$NON-NLS-1$
					int toLine = diffStr.substring(0, detailedDiffStart + detailedDiffLength).split("\n").length; //$NON-NLS-1$
					if (fromLine == toLine) {
						int starty = getYForLine(fromLine - 1, y, gc, textWidget);
						Point start = getPositionForOffset(gc, detailedDiffStart, diffStr, ranges, styledFonts);
						Point curr = getPositionForOffset(gc, detailedDiffStart + detailedDiffLength, diffStr, ranges,
								styledFonts);
						gc.fillRectangle(x + start.x, starty, curr.x - start.x, curr.y);
					} else {
						// mark first line until end
						String[] lines = diffStr.split("\n"); //$NON-NLS-1$
						String firstLine = lines[fromLine - 1];
						int starty = getYForLine(fromLine - 1, y, gc, textWidget);
						int idx = getOffsetAtLine(diffStr, detailedDiffStart);
						Point start = getPositionForOffset(gc, idx, diffStr, ranges, styledFonts);
						Point curr = getPositionForOffset(gc, firstLine.length(), diffStr, ranges, styledFonts);
						if (curr.x > 0) {
							gc.fillRectangle(x + start.x, starty, curr.x - start.x, curr.y);
						}
						// all the lines between first and last line
						var diffStrDoc = new Document(diffStr);
						for (int middleLine = fromLine + 1; middleLine < toLine; middleLine++) {
							starty = getYForLine(middleLine - 1, y, gc, textWidget);
							try {
								int currentLineEndOffset = diffStrDoc.getLineOffset(middleLine - 1)
										+ diffStrDoc.getLineLength(middleLine - 1)
										- getLineDelimiterLength(diffStrDoc, middleLine);
								curr = getPositionForOffset(gc, currentLineEndOffset, diffStr, ranges, styledFonts);
								if (curr.x > 0) {
									gc.fillRectangle(x, starty, curr.x, curr.y);
								}
							} catch (BadLocationException e) {
								error(e);
							}
						}
						// last line
						starty = getYForLine(toLine - 1, y, gc, textWidget);
						curr = getPositionForOffset(gc, detailedDiffStart + detailedDiffLength, diffStr, ranges,
								styledFonts);
						if (curr.x > 0) {
							gc.fillRectangle(x, starty, curr.x, curr.y);
						}
					}
				} catch (IllegalArgumentException e) {
					error(e);
				}
			}
			// draw foreground again
			int cx = x;
			int cy = y;
			try {
				for (StyleRange range : ranges) {
					String sub = label.substring(range.start, range.start + range.length);
					if (sub.trim().length() > 0) {
						if (range.background != null) {
							gc.setBackground(range.background);
						}
						if (range.foreground != null) {
							gc.setForeground(range.foreground);
						}
						Font currentFont = gc.getFont();
						var rangeWithFont = transformFontStyleToFont(currentFont, range, styledFonts);
						if (rangeWithFont.font != null) {
							gc.setFont(rangeWithFont.font);
						}
						String[] lines = sub.split("\n"); //$NON-NLS-1$
						if (lines.length > 1) {
							for (int i = 0; i < lines.length; i++) {
								String line = lines[i].replace("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
								gc.drawString(line, cx, cy, true);
								Point p = gc.stringExtent(line);
								if (i < lines.length - 1) {
									cy += p.y + textWidget.getLineSpacing();
									cx = x;
								} else {
									cx += p.x;
								}
							}
						} else {
							gc.drawString(sub, cx, cy, true);
							Point p = gc.stringExtent(sub);
							if (sub.endsWith("\n")) { //$NON-NLS-1$
								cy += p.y + textWidget.getLineSpacing();
								cx = x;
							} else {
								cx += p.x;
							}
						}
						gc.setFont(currentFont);
					} else {
						int lfCount = 0;
						if (sub.contains("\n")) { //$NON-NLS-1$
							lfCount = sub.split("\n", -1).length - 1; //$NON-NLS-1$
							sub = sub.substring(sub.lastIndexOf("\n") + 1); //$NON-NLS-1$
						}
						Point p = gc.stringExtent(sub);
						if (lfCount > 0) {
							cy += lfCount * (p.y + textWidget.getLineSpacing());
							cx = x;
						}
						cx += p.x;
					}
				}
			} finally {
				styledFonts.forEach((font1, styledFonts1) -> {
					styledFonts1.forEach((style, styledFont) -> {
						styledFont.dispose();
					});
					styledFonts1.clear();
				});
			}
			return result;
		}

		private int getLineDelimiterLength(Document diffStrDoc, int middleLine) throws BadLocationException {
			String delim = diffStrDoc.getLineDelimiter(middleLine - 1);
			if (delim == null) {
				return 0;
			}
			return delim.length();
		}

		private Point getPositionForOffset(GC gc, int offset, String str, List<StyleRange> ranges,
				HashMap<Font, Map<Integer, Font>> styledFonts) {
			String sub = str.substring(0, offset);
			int tabCount = sub.split("\t", -1).length - 1; //$NON-NLS-1$
			offset += tabCount * tabWidth - tabCount;
			str = replaceTabWithSpaces(str, tabWidth);
			Point result = null;
			var before = gc.getFont();
			try {
				for (var range : ranges) {
					if (range.start <= offset) {
						int rangeEnd = range.start + range.length;
						if (rangeEnd > offset) {
							rangeEnd = offset;
						}
						sub = str.substring(range.start, rangeEnd);
						if (offset == rangeEnd && offset == str.length()) {
							while (sub.endsWith("\n")) { //$NON-NLS-1$
								sub = sub.substring(0, sub.length() - 1);
							}
						}
						int lfIdx = sub.lastIndexOf("\n"); //$NON-NLS-1$
						if (lfIdx > 0) {
							sub = sub.substring(lfIdx + 1);
							result = null;
						}
						var rangeWithFont = transformFontStyleToFont(before, range, styledFonts);
						if (rangeWithFont.font != null) {
							gc.setFont(rangeWithFont.font);
						} else {
							gc.setFont(before);
						}
						Point extent = gc.stringExtent(sub);
						if (result == null) {
							result = extent;
						} else {
							result.x += extent.x;
						}
					} else {
						break;
					}
				}
			} finally {
				gc.setFont(before);
			}
			return result;
		}

		private StyleRange transformFontStyleToFont(Font baseFont, StyleRange styleRange,
				HashMap<Font, Map<Integer /* style */, Font>> styledFonts) {
			// as per the StyleRange contract, only consider fontStyle if font is not
			// already set
			if (styleRange.font == null && styleRange.fontStyle > 0) {
				StyleRange newRange = (StyleRange) styleRange.clone();
				newRange.font = styledFonts.computeIfAbsent(baseFont, f -> new HashMap<>())
						.computeIfAbsent(Integer.valueOf(styleRange.fontStyle), s -> {
							FontData[] fontDatas = baseFont.getFontData();
							for (FontData fontData : fontDatas) {
								fontData.setStyle(styleRange.fontStyle);
							}
							return new Font(baseFont.getDevice(), fontDatas);
						});
				return newRange;
			}
			return styleRange;
		}

		private List<StyleRange> getStyleRanges(ITextViewer v, String source) {
			List<StyleRange> result = new ArrayList<>();
			if (!(v instanceof SourceViewer sv)) {
				return result;
			}
			try {
				// TODO (tm) API needed to access IPresentationReconciler
				Field f = SourceViewer.class.getDeclaredField("fPresentationReconciler"); //$NON-NLS-1$
				f.setAccessible(true);
				var reconciler = (IPresentationReconciler) f.get(sv);
				// TODO (tm) should we better cache the presentation and don't calculate it each
				// time?
				result = createPresentation(reconciler, sv.getDocument(), source);
			} catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
				error(e);
			}
			return result;
		}

		private List<StyleRange> createPresentation(IPresentationReconciler reconciler, IDocument originalDocument,
				String source) {
			try {
				String prefix = originalDocument.get(0, diff.leftStart);
				IDocument document = new Document(prefix + source);
				IRegion damage = new Region(prefix.length(), source.length());
				String partition = IDocumentExtension3.DEFAULT_PARTITIONING;
				if (reconciler instanceof IPresentationReconcilerExtension ext) {
					String extPartition = ext.getDocumentPartitioning();
					if (extPartition != null && !extPartition.isEmpty()) {
						partition = extPartition;
					}
				}
				IDocumentPartitioner partitioner = originalDocument.getDocumentPartitioner();
				document.setDocumentPartitioner(partitioner);
				IDocumentPartitioner originalDocumentPartitioner = null;
				if (document instanceof IDocumentExtension3 ext
						&& originalDocument instanceof IDocumentExtension3 originalExt) {
					originalDocumentPartitioner = originalExt.getDocumentPartitioner(partition);
					if (originalDocumentPartitioner != null) {
						// set temporarily another document in partitioner so that presentation can be
						// created for given source
						originalDocumentPartitioner.disconnect();
						originalDocumentPartitioner.connect(document);
						ext.setDocumentPartitioner(partition, originalDocumentPartitioner);
					}
				}
				TextPresentation presentation = new TextPresentation(damage, 1000);

				ITypedRegion[] partitioning = TextUtilities.computePartitioning(document, partition, damage.getOffset(),
						damage.getLength(), false);
				for (ITypedRegion r : partitioning) {
					IPresentationRepairer repairer = reconciler.getRepairer(r.getType());
					if (repairer != null) {
						repairer.setDocument(document);
						repairer.createPresentation(presentation, r);
						repairer.setDocument(originalDocument);
					}
				}
				if (originalDocumentPartitioner != null) {
					originalDocumentPartitioner.connect(originalDocument);
				}
				List<StyleRange> result = new ArrayList<>();
				var it = presentation.getAllStyleRangeIterator();
				int startOffset = prefix.length();
				while (it.hasNext()) {
					StyleRange next = it.next();
					if (next.start < startOffset) {
						throw new IllegalStateException(
								"Invalid presentation with style range starting before source offset"); //$NON-NLS-1$
					}
					next.start -= startOffset;
					result.add(next);
				}
				return result;
			} catch (BadLocationException x) {
				return null;
			}
		}

		private int getOffsetAtLine(String str, int off) {
			Document doc;
			if (off == str.length()) {
				doc = new Document(str);
			} else {
				doc = new Document(str.stripTrailing()); // TODO (tm) strange: when do we need to stripTrailing?
			}
			try {
				if (off > doc.getLength()) {
					// TODO (tm) don't get it - when is this branch needed
					int line = doc.getLineOfOffset(doc.getLength());
					int lineOffset = doc.getLineOffset(line);
					int resultOff = doc.getLength() - lineOffset;
					return resultOff;
				}
				int line = doc.getLineOfOffset(off);
				int lineOffset = doc.getLineOffset(line);
				int resultOff = off - lineOffset;
				return resultOff;
			} catch (BadLocationException e) {
				error(e);
			}
			return -1;
		}

		private int getYForLine(int line, int y, GC gc, StyledText textWidget) {
			Point ext = gc.stringExtent("A"); //$NON-NLS-1$
			y += line * (ext.y + textWidget.getLineSpacing());
			return y;
		}
	}

	// from inner class ColorPalette in TextMergeViewer
	static RGB interpolate(RGB fg, RGB bg, double scale) {
		if (fg != null && bg != null) {
			return new RGB((int) ((1.0 - scale) * fg.red + scale * bg.red),
					(int) ((1.0 - scale) * fg.green + scale * bg.green),
					(int) ((1.0 - scale) * fg.blue + scale * bg.blue));
		}
		if (fg != null) {
			return fg;
		}
		if (bg != null) {
			return bg;
		}
		return new RGB(128, 128, 128); // a gray
	}

	private static String replaceTabWithSpaces(String leftStr, int tabWidth) {
		if (leftStr == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int s = 0; s < tabWidth; s++) {
			sb.append(' ');
		}
		String result = leftStr.replace("\t", sb); //$NON-NLS-1$
		return result;
	}
}
