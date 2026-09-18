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
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.countLines;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.mapOffsetToTabExpanded;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.mergeStyleRanges;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.removeLeadingNewLines;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.removeTrailingNewLines;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffText.replaceTabWithSpaces;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.DocumentFooterCodeMining;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.inlined.LineFooterAnnotation;
import org.eclipse.jface.text.source.inlined.LineHeaderAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class UnifiedDiffCodeMiningProvider extends AbstractCodeMiningProvider {

	private Color deletionBackgroundColor;
	private Color detailedDiffColor;
	private Color foldSeparatorColor;
	private Color foldButtonColor;
	private boolean lastIsOverlay;

	@Override
	public void dispose() {
		try {
			if (deletionBackgroundColor != null && !deletionBackgroundColor.isDisposed()) {
				deletionBackgroundColor.dispose();
			}
			deletionBackgroundColor = null;
			if (detailedDiffColor != null && !detailedDiffColor.isDisposed()) {
				detailedDiffColor.dispose();
			}
			detailedDiffColor = null;
			if (foldSeparatorColor != null && !foldSeparatorColor.isDisposed()) {
				foldSeparatorColor.dispose();
			}
			foldSeparatorColor = null;
			if (foldButtonColor != null && !foldButtonColor.isDisposed()) {
				foldButtonColor.dispose();
			}
			foldButtonColor = null;
		} finally {
			super.dispose();
		}
	}

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		List<UnifiedDiff> diffs = UnifiedDiffManager.get(viewer);
		if (diffs == null || diffs.size() == 0) {
			return CompletableFuture.completedFuture(new ArrayList<>());
		}
		boolean isOverlay = isOverlay(diffs);
		if (Display.getCurrent() != null && (this.deletionBackgroundColor == null || isOverlay != lastIsOverlay)) {
			// check class ColorPalette
			RGB background = getBackground();
			String colorName;
			if (isOverlay) {
				colorName = "ADDITION_COLOR"; //$NON-NLS-1$
			} else {
				colorName = "DELETION_COLOR"; //$NON-NLS-1$
			}
			RGB deletionColor = JFaceResources.getColorRegistry().getRGB(colorName);
			// dispose any previously allocated colors before replacing them so the
			// native handles are not leaked when the mode (isOverlay) changes
			if (this.detailedDiffColor != null && !this.detailedDiffColor.isDisposed()) {
				this.detailedDiffColor.dispose();
			}
			if (this.deletionBackgroundColor != null && !this.deletionBackgroundColor.isDisposed()) {
				this.deletionBackgroundColor.dispose();
			}
			if (this.foldSeparatorColor != null && !this.foldSeparatorColor.isDisposed()) {
				this.foldSeparatorColor.dispose();
			}
			if (this.foldButtonColor != null && !this.foldButtonColor.isDisposed()) {
				this.foldButtonColor.dispose();
			}
			// the word-level diff is tinted stronger so it stands out against the band
			this.detailedDiffColor = new Color(interpolate(deletionColor, background, 0.8));
			this.deletionBackgroundColor = new Color(interpolate(deletionColor, background, 0.9));
			this.foldSeparatorColor = new Color(separatorBackground(background));
			this.foldButtonColor = new Color(buttonBackground(background));
			lastIsOverlay = isOverlay;
		}
		if (viewer instanceof ISourceViewer sv) {
			List<ICodeMining> attached = attachedMiningsOf(sv, diffs);
			if (attached != null) {
				// the expander minings are recreated instead of reused so that they
				// reflect the current expansion state of the folds
				createFoldRegionCodeMinings(viewer, attached);
				return CompletableFuture.completedFuture(attached);
			}
		}

		int tabWidth = getTabWidth(viewer);
		// take an immutable snapshot so the async iteration cannot observe
		// concurrent modifications when accept/hide actions mutate the live list
		List<UnifiedDiff> diffsSnapshot = List.copyOf(diffs);
		// created on the calling thread because it reads the projection annotation model
		List<ICodeMining> foldMinings = new ArrayList<>();
		createFoldRegionCodeMinings(viewer, foldMinings);
		return CompletableFuture.supplyAsync(() -> {
			List<ICodeMining> minings = new ArrayList<>();
			createLineHeaderCodeMinings(diffsSnapshot, minings, viewer, tabWidth);
			minings.addAll(foldMinings);
			return minings;
		});
	}

	public static RGB getBackground() {
		RGB background = null;
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		boolean isUsingSystemBackground = store
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
		if (!isUsingSystemBackground) {
			background = createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
		}
		if (background != null) {
			return background;
		}
		return Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
	}

	private static RGB createColor(IPreferenceStore store, String key) {
		if (!store.contains(key)) {
			return null;
		}
		if (store.isDefault(key)) {
			return PreferenceConverter.getDefaultColor(store, key);
		}
		return PreferenceConverter.getColor(store, key);
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

	/** Whether the diff shows content of the other side, which takes a mining. */
	public static boolean needsCodeMining(UnifiedDiff diff) {
		return diff.mode.equals(UnifiedDiffMode.REPLACE_MODE) ? !diff.leftStr.isEmpty() : !diff.rightStr.isEmpty();
	}

	/**
	 * Returns the attached minings of the diffs, one per diff that takes one, or
	 * {@code null} when a diff has none and they have to be rebuilt. Minings of
	 * diffs that are no longer shown are left out, so what an earlier request
	 * attached never decides what is shown.
	 */
	private static List<ICodeMining> attachedMiningsOf(ISourceViewer sv, List<UnifiedDiff> diffs) {
		IAnnotationModel model = sv.getAnnotationModel();
		IDocument doc = sv.getDocument();
		if (model == null || doc == null) {
			return null;
		}
		Map<UnifiedDiff, ICodeMining> attached = new IdentityHashMap<>();
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation next = it.next();
			UnifiedDiff diff;
			ICodeMining mining;
			if (next instanceof LineHeaderAnnotation header) {
				List<ICodeMining> m = header.getMinings();
				if (m.size() != 1 || !(m.get(0) instanceof UnifiedDiffLineHeaderCodeMining lineHeaderMining)) {
					continue;
				}
				try {
					// the annotation followed the edits of the document, the mining has not
					int line = doc.getLineOfOffset(header.getPosition().offset);
					lineHeaderMining.getPosition().offset = doc.getLineOffset(line);
				} catch (BadLocationException e) {
					error(e);
					continue;
				}
				diff = lineHeaderMining.getUnifiedDiff();
				mining = lineHeaderMining;
			} else if (next instanceof LineFooterAnnotation footer) {
				List<ICodeMining> m = footer.getMinings();
				if (m.size() != 1 || !(m.get(0) instanceof UnifiedDiffFooterCodeMining footerMining)) {
					continue;
				}
				footerMining.getPosition().offset = doc.getLength();
				diff = footerMining.getUnifiedDiff();
				mining = footerMining;
			} else {
				continue;
			}
			if (attached.put(diff, mining) != null) {
				return null;
			}
		}
		List<ICodeMining> minings = new ArrayList<>();
		for (UnifiedDiff diff : diffs) {
			if (!needsCodeMining(diff)) {
				continue;
			}
			ICodeMining mining = attached.get(diff);
			if (mining == null) {
				return null;
			}
			minings.add(mining);
		}
		return minings;
	}

	private void createLineHeaderCodeMinings(List<UnifiedDiff> diffs, List<ICodeMining> minings, ITextViewer tv,
			int tabWidth) {
		if (diffs == null) {
			return;
		}
		IDocument doc = tv.getDocument();
		for (UnifiedDiff diff : diffs) {
			if (!needsCodeMining(diff)) {
				continue;
			}
			// an overlay sits on the line after the range it stands for
			boolean overlay = diff.mode.equals(UnifiedDiffMode.OVERLAY_MODE)
					|| diff.mode.equals(UnifiedDiffMode.OVERLAY_READ_ONLY_MODE);
			int offset = overlay ? diff.leftStart + diff.leftLength : diff.leftStart;
			try {
				minings.add(createMining(doc, diff, offset, tabWidth, tv));
			} catch (BadLocationException e) {
				error(e);
			}
		}
	}

	/**
	 * A line header mining reserves its height as the vertical indent of its line,
	 * so the viewer can scroll over it. A footer mining reserves nothing and is
	 * therefore only used where no line start is available: behind the last,
	 * non-empty line of the document.
	 */
	private ICodeMining createMining(IDocument doc, UnifiedDiff diff, int offset, int tabWidth, ITextViewer tv)
			throws BadLocationException {
		int end = doc.getLength();
		if (offset >= end && !startsLine(doc, end)) {
			return new UnifiedDiffFooterCodeMining(doc, this, diff, tabWidth, this.deletionBackgroundColor, this.detailedDiffColor, tv);
		}
		// a position must not reach beyond the document, otherwise the annotation model
		// silently drops it
		int start = Math.min(offset, end);
		return new UnifiedDiffLineHeaderCodeMining(new Position(start, start < end ? 1 : 0), this, diff, tabWidth,
				this.deletionBackgroundColor, this.detailedDiffColor, tv);
	}

	private static boolean startsLine(IDocument doc, int offset) throws BadLocationException {
		return doc.getLineOffset(doc.getLineOfOffset(offset)) == offset;
	}

	/**
	 * Creates one clickable mining per unchanged-region fold, which shows the
	 * region while it is collapsed and hides it again while it is expanded.
	 */
	private void createFoldRegionCodeMinings(ITextViewer viewer, List<ICodeMining> minings) {
		IDocument doc = viewer.getDocument();
		if (doc == null) {
			return;
		}
		Map<Annotation, Position> folds = UnifiedDiffManager.getFoldRegions(viewer);
		for (Map.Entry<Annotation, Position> fold : folds.entrySet()) {
			Position position = fold.getValue();
			boolean collapsed = fold.getKey() instanceof ProjectionAnnotation p && p.isCollapsed();
			try {
				int firstLine = doc.getLineOfOffset(position.getOffset());
				int lastLine = position.getLength() > 0
						? doc.getLineOfOffset(position.getOffset() + position.getLength() - 1)
						: firstLine;
				// the first line of the region stays visible as the fold's caption
				int foldableLines = lastLine - firstLine;
				if (foldableLines <= 0) {
					continue;
				}
				// The band keeps the same line whether the region is collapsed or not, so
				// that toggling it does not move it away from the pointer that just
				// clicked it. A line header mining is drawn above its line, so the line
				// after the region puts the band in the gap while the region is collapsed
				// and directly below the block once it is expanded. The only other line
				// visible in both states is the caption, and a band above the caption
				// would claim the gap is before a line that is still there.
				int anchorLine = lastLine + 1;
				if (anchorLine >= doc.getNumberOfLines()) {
					continue;
				}
				int anchor = doc.getLineOffset(anchorLine);
				if (anchor >= doc.getLength()) {
					// the empty last line of a document ending in a line delimiter has
					// neither a character nor a delimiter to repaint, so an annotation
					// there never reaches the drawing strategy
					continue;
				}
				Position anchorPosition = new Position(anchor, 1);
				minings.add(new FoldedRegionCodeMining(anchorPosition, this, viewer, fold.getKey(), foldableLines,
						collapsed, this.foldSeparatorColor, this.foldButtonColor));
			} catch (BadLocationException e) {
				error(e);
			}
		}
	}

	/**
	 * A band that stands out from the surrounding text, so the collapsed region
	 * reads as a break between two hunks rather than as another line of the file.
	 */
	private static RGB separatorBackground(RGB background) {
		return interpolate(contrasting(background), background, 0.88);
	}

	/** The expander reads as a control, so it is tinted more strongly than its band. */
	private static RGB buttonBackground(RGB background) {
		return interpolate(contrasting(background), background, 0.72);
	}

	private static RGB contrasting(RGB background) {
		boolean dark = background != null && (background.red + background.green + background.blue) / 3 < 128;
		return dark ? new RGB(255, 255, 255) : new RGB(0, 0, 0);
	}

	public static class FoldedRegionCodeMining extends LineHeaderCodeMining {

		/** Width of the expander button, in multiples of the band height. */
		private static final int BUTTON_WIDTHS = 2;

		private final String bandLabel;
		private final boolean collapsed;
		private final Color separatorColor;
		private final Color buttonColor;

		public FoldedRegionCodeMining(Position position, ICodeMiningProvider provider, ITextViewer viewer,
				Annotation foldAnnotation, int foldableLines, boolean collapsed, Color separatorColor,
				Color buttonColor) throws BadLocationException {
			super(position, provider, e -> {
				if (collapsed) {
					UnifiedDiffManager.expandFoldRegion(viewer, foldAnnotation);
				} else {
					UnifiedDiffManager.collapseFoldRegion(viewer, foldAnnotation);
				}
			});
			this.bandLabel = label(foldableLines, collapsed);
			this.collapsed = collapsed;
			this.separatorColor = separatorColor;
			this.buttonColor = buttonColor;
		}

		private static String label(int foldableLines, boolean collapsed) {
			if (collapsed) {
				return foldableLines == 1 ? CompareMessages.UnifiedDiff_showUnchangedLine
						: NLS.bind(CompareMessages.UnifiedDiff_showUnchangedLines, Integer.valueOf(foldableLines));
			}
			return foldableLines == 1 ? CompareMessages.UnifiedDiff_hideUnchangedLine
					: NLS.bind(CompareMessages.UnifiedDiff_hideUnchangedLines, Integer.valueOf(foldableLines));
		}

		/** Whether clicking this band shows the region rather than hiding it again. */
		public boolean isExpander() {
			return this.collapsed;
		}

		@Override
		public String getLabel() {
			return this.bandLabel;
		}

		@Override
		public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
			if (this.separatorColor == null || this.separatorColor.isDisposed() || this.buttonColor == null
					|| this.buttonColor.isDisposed()) {
				return super.draw(gc, textWidget, color, x, y);
			}
			gc.setFont(textWidget.getFont());
			String label = getLabel();
			Point extent = gc.stringExtent(label);
			int height = extent.y;
			int width = textWidget.getBounds().width;
			int buttonWidth = BUTTON_WIDTHS * height;
			gc.setBackground(this.separatorColor);
			gc.fillRectangle(0, y, width, height);
			gc.setBackground(this.buttonColor);
			gc.fillRectangle(0, y, buttonWidth, height);
			gc.setForeground(textWidget.getForeground());
			drawChevrons(gc, buttonWidth / 2, y + height / 2, height, this.collapsed);
			gc.drawString(label, x + buttonWidth, y, true);
			return new Point(buttonWidth + extent.x, height);
		}

		/**
		 * Two chevrons, pointing apart to show the region and together to hide it
		 * again. Drawn rather than loaded as an icon so that they follow the
		 * editor's foreground colour in every theme.
		 */
		private static void drawChevrons(GC gc, int centerX, int centerY, int height, boolean apart) {
			int arm = Math.max(2, height / 4);
			// Tips that point at each other need more room between them than tips that
			// point away, otherwise the two chevrons meet in the middle and read as a
			// cross rather than as a pair.
			int gap = apart ? Math.max(1, height / 6) : Math.max(3, height / 5);
			int tip = apart ? gap + arm : gap;
			int base = apart ? gap : gap + arm;
			int previousWidth = gc.getLineWidth();
			gc.setLineWidth(Math.max(1, height / 8));
			gc.drawPolyline(new int[] { centerX - arm, centerY - base, centerX, centerY - tip, centerX + arm,
					centerY - base });
			gc.drawPolyline(new int[] { centerX - arm, centerY + base, centerX, centerY + tip, centerX + arm,
					centerY + base });
			gc.setLineWidth(previousWidth);
		}
	}

	interface IUnifiedDiffCodeMining {
		Rectangle getLastRectangle();
		Color getDeletionBackgroundColor();
		Color getDetailedDiffColor();
		int getTabWidth();
		UnifiedDiff getUnifiedDiff();
		String getLabel();
	}

	static class MouseClickConsumer implements Consumer<MouseEvent> {

		private final ITextViewer viewer;
		private IUnifiedDiffCodeMining mining;

		public MouseClickConsumer(ITextViewer viewer) {
			this.viewer = viewer;
		}

		public void setCodeMining(IUnifiedDiffCodeMining mining) {
			this.mining = mining;
		}

		@Override
		public void accept(MouseEvent t) {
			if (mining == null || viewer == null || mining.getLastRectangle() == null) {
				return;
			}
			StyledText st = viewer.getTextWidget();
			StyledText overlay = new StyledText(st, SWT.NONE);
			overlay.setBounds(mining.getLastRectangle());
			overlay.setFont(st.getFont());
			overlay.setBackground(mining.getDeletionBackgroundColor());
			overlay.setLineSpacing(st.getLineSpacing());
			String txt = mining.getLabel().stripTrailing();
			overlay.setText(txt);
			overlay.setFocus();
			List<StyleRange> backgrounds = createDetailedDiffBackgroundRanges(mining.getUnifiedDiff(),
					mining.getTabWidth(), mining.getDetailedDiffColor());
			List<StyleRange> foregrounds = computeStyleRanges(viewer, mining.getUnifiedDiff().leftStart, txt);
			List<StyleRange> ranges = mergeStyleRanges(backgrounds, foregrounds);
			overlay.setStyleRanges(ranges.toArray(new StyleRange[] {}));
			openOverlay(overlay, viewer);
		}
	}

	public static class UnifiedDiffFooterCodeMining extends DocumentFooterCodeMining implements IUnifiedDiffCodeMining {
		private final String unifiedDiffLabel;
		private final Color deletionBackgroundColor;
		private final Color detailedDiffColor;
		private final int tabWidth;
		private final ITextViewer viewer;
		private UnifiedDiff diff;
		private List<StyleRange> styleRanges;
		private final HashMap<Font, Map<Integer, Font>> styledFonts = new HashMap<>();
		private Rectangle lastRectangle;
		private Font cachedFont;

		public UnifiedDiffFooterCodeMining(IDocument document, ICodeMiningProvider provider,
				UnifiedDiff diff, int tabWidth, Color deletionBackgroundColor, Color detailedDiffColor,
				ITextViewer viewer) {
			super(document, provider, new MouseClickConsumer(viewer));
			this.deletionBackgroundColor = deletionBackgroundColor;
			this.detailedDiffColor = detailedDiffColor;
			this.tabWidth = tabWidth;
			this.viewer = viewer;
			if (diff.mode.equals(UnifiedDiffMode.REPLACE_MODE)) {
				this.unifiedDiffLabel = removeTrailingNewLines(replaceTabWithSpaces(diff.leftStr, tabWidth));
			} else {
				this.unifiedDiffLabel = removeTrailingNewLines(replaceTabWithSpaces(diff.rightStr, tabWidth));
			}
			this.diff = diff;
			((MouseClickConsumer) getAction()).setCodeMining(this);
		}

		@Override
		public Rectangle getLastRectangle() {
			return lastRectangle;
		}

		@Override
		public Color getDeletionBackgroundColor() {
			return deletionBackgroundColor;
		}

		@Override
		public Color getDetailedDiffColor() {
			return detailedDiffColor;
		}

		@Override
		public int getTabWidth() {
			return tabWidth;
		}

		@Override
		public UnifiedDiff getUnifiedDiff() {
			return diff;
		}

		@Override
		public String getLabel() {
			return this.unifiedDiffLabel;
		}

		@Override
		public void dispose() {
			styleRanges = null;
			lastRectangle = null;
			cachedFont = null;
			clearStyledFonts();
			super.dispose();
		}

		private void clearStyledFonts() {
			styledFonts.forEach((font, map) -> map.forEach((style, f) -> f.dispose()));
			styledFonts.clear();
		}

		private List<StyleRange> styleRanges(String label) {
			if (styleRanges == null) {
				styleRanges = computeStyleRanges(viewer, diff.leftStart, label);
			}
			return styleRanges;
		}

		@Override
		public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
			gc.setBackground(this.deletionBackgroundColor);
			Color c = textWidget.getForeground();
			gc.setForeground(c);
			Font font = textWidget.getFont();
			gc.setFont(font);
			if (cachedFont != null && (cachedFont.isDisposed() || !cachedFont.equals(font))) {
				// font might have been changed in the meantime - drop the derived fonts
				// keyed on the old base font so their native handles are not leaked
				clearStyledFonts();
			}
			cachedFont = font;
			// first run to get width and height for label
			// change from https://github.com/eclipse-platform/eclipse.platform.ui/pull/3651
			// is required so that background correctly drawn with line spacing > 0
			Point result = super.draw(gc, textWidget, color, x, y);
			lastRectangle = new Rectangle(x, y, result.x, result.y);
			// draw background
			// vs code is drawing the background to the top right of the editor - we do here
			// the same!
			gc.fillRectangle(0, y, textWidget.getBounds().width /* result.x */, result.y);

			String label = getLabel();
			List<StyleRange> ranges = styleRanges(label);
			if (ranges.isEmpty()) {
				// no syntax coloring available; fall back to plain rendering
				result = super.draw(gc, textWidget, color, x, y);
				return result;
			}

			// paint the word-level detailed-diff backgrounds first, then draw the
			// syntax-colored text transparently on top - same order as the header band
			fillDetailedDiffBackgrounds(gc, textWidget, label, ranges, x, y);
			gc.setFont(font);
			drawStyleRanges(gc, textWidget, ranges, label, styledFonts, x, y, null);
			return result;
		}

		/**
		 * Fills the darker {@code detailedDiffColor} rectangles behind the word-level
		 * changes. {@link #drawStyleRanges} draws its text transparently and so cannot
		 * paint a range background itself.
		 */
		private void fillDetailedDiffBackgrounds(GC gc, StyledText textWidget, String label, List<StyleRange> ranges,
				int x, int y) {
			List<StyleRange> backgrounds = createDetailedDiffBackgroundRanges(diff, tabWidth, detailedDiffColor);
			if (backgrounds.isEmpty()) {
				return;
			}
			int lineHeight = textWidget.getLineHeight();
			int lineSpacing = textWidget.getLineSpacing();
			gc.setBackground(this.detailedDiffColor);
			for (StyleRange bg : backgrounds) {
				int pos = bg.start;
				int end = bg.start + bg.length;
				// a detailed diff may span lines; fill each line segment on its own line
				while (pos < end) {
					int lineStart = label.lastIndexOf('\n', pos - 1) + 1;
					int lineEnd = label.indexOf('\n', pos);
					int segmentEnd = lineEnd == -1 ? end : Math.min(end, lineEnd);
					int lineIndex = (int) label.substring(0, pos).chars().filter(ch -> ch == '\n').count();
					int startX = styledWidth(gc, label, ranges, lineStart, pos);
					int width = styledWidth(gc, label, ranges, pos, segmentEnd);
					if (width > 0) {
						int rectX = x + startX;
						int rectY = y + lineIndex * (lineHeight + lineSpacing);
						gc.fillRectangle(rectX, rectY, width, lineHeight);
					}
					pos = segmentEnd == lineEnd ? segmentEnd + 1 : segmentEnd;
				}
			}
		}

		/**
		 * Measures {@code label[from, to)}, which must lie within a single line, the
		 * way {@link #drawStyleRanges} paints it. Measuring in the base font would put
		 * the highlight left of the changed word whenever bold or italic text precedes
		 * it on the same line.
		 */
		private int styledWidth(GC gc, String label, List<StyleRange> ranges, int from, int to) {
			if (to <= from) {
				return 0;
			}
			Font base = gc.getFont();
			int width = 0;
			try {
				int cursor = from;
				for (StyleRange range : ranges) {
					int rangeEnd = range.start + range.length;
					if (rangeEnd <= cursor || range.start >= to) {
						continue;
					}
					if (range.start > cursor) {
						width += measure(gc, base, label, cursor, range.start);
						cursor = range.start;
					}
					// drawStyleRanges keeps the base font for whitespace-only ranges
					boolean blank = label.substring(range.start, Math.min(rangeEnd, label.length())).isBlank();
					StyleRange rangeWithFont = blank ? range : transformFontStyleToFont(styledFonts, base, range);
					Font font = rangeWithFont.font != null ? rangeWithFont.font : base;
					int segmentEnd = Math.min(rangeEnd, to);
					width += measure(gc, font, label, cursor, segmentEnd);
					cursor = segmentEnd;
					if (cursor >= to) {
						break;
					}
				}
				if (cursor < to) {
					width += measure(gc, base, label, cursor, to);
				}
			} finally {
				gc.setFont(base);
			}
			return width;
		}

		private int measure(GC gc, Font font, String label, int from, int to) {
			gc.setFont(font);
			// gc.stringExtent ignores tabs, and drawStyleRanges drops the CR of a CRLF
			String text = replaceTabWithSpaces(label.substring(from, to), tabWidth).replace("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
			return gc.stringExtent(text).x;
		}
	}

	static List<StyleRange> createDetailedDiffBackgroundRanges(UnifiedDiff diff, int tabWidth, Color detailedDiffColor) {
		List<StyleRange> ranges = new ArrayList<>();
		String diffStr = diff.mode.equals(UnifiedDiffMode.REPLACE_MODE) ? diff.leftStr : diff.rightStr;
		String trimmedDiffStr = removeTrailingNewLines(diffStr);
		int labelLength = replaceTabWithSpaces(trimmedDiffStr, tabWidth).stripTrailing().length();
		for (var detailedDiff : diff.detailedDiffs) {
			int detailedDiffStart;
			int detailedDiffLength;
			String detailedDiffStr;
			if (diff.mode.equals(UnifiedDiffMode.REPLACE_MODE)) {
				detailedDiffStart = detailedDiff.leftStart;
				detailedDiffLength = detailedDiff.leftLength;
				detailedDiffStr = detailedDiff.leftStr;
			} else {
				detailedDiffStart = detailedDiff.rightStart;
				detailedDiffLength = detailedDiff.rightLength;
				detailedDiffStr = detailedDiff.rightStr;
			}
			if (detailedDiffStr.trim().length() == 0) {
				continue;
			}
			if (detailedDiffStart + detailedDiffLength >= trimmedDiffStr.length()) {
				int delta = diffStr.length() - trimmedDiffStr.length();
				if (detailedDiffLength <= delta) {
					continue;
				}
				detailedDiffLength -= delta;
			}
			int expandedStart = mapOffsetToTabExpanded(diffStr, detailedDiffStart, tabWidth);
			int expandedEnd = mapOffsetToTabExpanded(diffStr, detailedDiffStart + detailedDiffLength, tabWidth);
			int expandedLength = expandedEnd - expandedStart;
			if (expandedStart >= 0 && expandedLength > 0 && expandedStart + expandedLength <= labelLength) {
				StyleRange bgRange = new StyleRange();
				bgRange.start = expandedStart;
				bgRange.length = expandedLength;
				bgRange.background = detailedDiffColor;
				ranges.add(bgRange);
			}
		}
		return ranges;
	}

	record ForegroundInfo(int x, int y, String str, Font font, Color background, Color foreground) {
	}

	/**
	 * Draws the syntax-colored label using the given style ranges, advancing the
	 * cursor position range by range. The {@code onForeground} consumer is called
	 * for each drawn segment and may be {@code null}; the header mining uses it to
	 * populate its foreground cache so subsequent repaints skip this path.
	 */
	static void drawStyleRanges(GC gc, StyledText textWidget, List<StyleRange> ranges, String label,
			HashMap<Font, Map<Integer, Font>> styledFonts, int x, int y,
			Consumer<ForegroundInfo> onForeground) {
		Font font = gc.getFont();
		int textWidgetLineHeight = textWidget.getLineHeight();
		int cx = x;
		int cy = y;
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
				var rangeWithFont = transformFontStyleToFont(styledFonts, currentFont, range);
				if (rangeWithFont.font != null) {
					gc.setFont(rangeWithFont.font);
				}
				String[] lines = sub.split("\n"); //$NON-NLS-1$
				if (lines.length > 1) {
					for (int i = 0; i < lines.length; i++) {
						String line = lines[i].replace("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
						gc.drawString(line, cx, cy, true);
						if (onForeground != null) {
							onForeground.accept(new ForegroundInfo(cx - x, cy - y, line, gc.getFont(),
									gc.getBackground(), gc.getForeground()));
						}
						Point p = gc.stringExtent(line);
						if (i < lines.length - 1) {
							cy += textWidgetLineHeight + textWidget.getLineSpacing();
							cx = x;
						} else {
							if (sub.endsWith("\n")) { //$NON-NLS-1$
								cy += textWidgetLineHeight + textWidget.getLineSpacing();
								cx = x;
							} else {
								cx += p.x;
							}
						}
					}
				} else {
					gc.drawString(sub, cx, cy, true);
					if (onForeground != null) {
						onForeground.accept(new ForegroundInfo(cx - x, cy - y, sub, gc.getFont(),
								gc.getBackground(), gc.getForeground()));
					}
					Point p = gc.stringExtent(sub);
					if (sub.endsWith("\n")) { //$NON-NLS-1$
						cy += textWidgetLineHeight + textWidget.getLineSpacing();
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
					cy += lfCount * (textWidgetLineHeight + textWidget.getLineSpacing());
					cx = x;
				}
				cx += p.x;
			}
		}
		gc.setFont(font);
	}

	private static List<StyleRange> computeStyleRanges(ITextViewer v, int offset, String source) {
		List<StyleRange> result = new ArrayList<>();
		if (!(v instanceof SourceViewer sv)) {
			return result;
		}
		IDocument originalDocument = sv.getDocument();
		if (originalDocument == null) {
			return result;
		}
		try {
			String prefix = originalDocument.get(0, offset /* diff.leftStart */);
			IDocument document = new Document(prefix + source);
			IRegion damage = new Region(prefix.length(), source.length());
			result = sv.computeStyleRanges(document, damage);
			int startOffset = prefix.length();
			for (StyleRange next : result) {
				if (next.start < startOffset) {
					throw new IllegalStateException(
							"Invalid presentation with style range starting before source offset"); //$NON-NLS-1$
				}
				next.start -= startOffset;
			}
			return result;
		} catch (BadLocationException e) {
			return result;
		}
	}

	public static class UnifiedDiffLineHeaderCodeMining extends LineHeaderCodeMining implements IUnifiedDiffCodeMining {
		private final String unifiedDiffLabel;
		private final Color deletionBackgroundColor;
		private final Color detailedDiffColor;
		private final UnifiedDiff diff;
		private final int tabWidth;
		private ITextViewer viewer;

		private final List<DetailedDiffRange> detailedDiffRanges;

		private Rectangle lastRectangle;
		private List<Rectangle> backgrounds;
		private List<ForegroundInfo> foregrounds;
		private List<StyleRange> styleRanges;
		private Font cachedFont;
		private final HashMap<Font, Map<Integer /* style */, Font>> styledFonts = new HashMap<>();

		public UnifiedDiffLineHeaderCodeMining(Position position, ICodeMiningProvider provider, UnifiedDiff diff,
				int tabWidth, Color deletionBackgroundColor, Color detailedDiffColor, ITextViewer viewer)
				throws BadLocationException {
			super(position, provider, new MouseClickConsumer(viewer));
			if (diff.mode.equals(UnifiedDiffMode.REPLACE_MODE)) {
				this.unifiedDiffLabel = removeTrailingNewLines(replaceTabWithSpaces(diff.leftStr, tabWidth));
			} else {
				this.unifiedDiffLabel = removeTrailingNewLines(replaceTabWithSpaces(diff.rightStr, tabWidth));
			}
			this.deletionBackgroundColor = deletionBackgroundColor;
			this.detailedDiffColor = detailedDiffColor;
			this.diff = diff;
			this.tabWidth = tabWidth;
			this.viewer = viewer;
			this.detailedDiffRanges = computeDetailedDiffRanges(diff);
			((MouseClickConsumer) getAction()).setCodeMining(this);
		}

		/**
		 * Line range of a detailed diff, resolved once so painting does not scan
		 * the hunk text.
		 */
		private record DetailedDiffRange(String diffStr, int start, int length, int fromLine, int toLine) {
		}

		private static List<DetailedDiffRange> computeDetailedDiffRanges(UnifiedDiff diff) {
			boolean useRight = diff.mode.equals(UnifiedDiffMode.OVERLAY_MODE)
					|| diff.mode.equals(UnifiedDiffMode.OVERLAY_READ_ONLY_MODE)
					|| diff.mode.equals(UnifiedDiffMode.REVERT_MODE);
			String fullDiffStr = useRight ? diff.rightStr : diff.leftStr;
			String diffStr = removeTrailingNewLines(fullDiffStr);
			int diffStrDelta = fullDiffStr.length() - diffStr.length();
			List<DetailedDiffRange> result = new ArrayList<>();
			for (var detailedDiff : diff.detailedDiffs) {
				String detailedDiffStr = useRight ? detailedDiff.rightStr : detailedDiff.leftStr;
				int detailedDiffStart = useRight ? detailedDiff.rightStart : detailedDiff.leftStart;
				int detailedDiffLength = useRight ? detailedDiff.rightLength : detailedDiff.leftLength;
				if (detailedDiffStr.trim().length() == 0) {
					continue;
				}
				if (detailedDiffStart + detailedDiffLength >= diffStr.length()) {
					if (detailedDiffLength <= diffStrDelta) {
						continue;
					}
					detailedDiffLength -= diffStrDelta;
				}
				// String#split drops trailing empty strings, so it must not be used to
				// count lines: a prefix ending with \n starts the next line
				int fromLine = countLines(diffStr, detailedDiffStart);
				int toLine = countLines(diffStr, detailedDiffStart + detailedDiffLength);
				result.add(new DetailedDiffRange(diffStr, detailedDiffStart, detailedDiffLength, fromLine, toLine));
			}
			return result;
		}

		@Override
		public String getLabel() {
			return this.unifiedDiffLabel;
		}

		@Override
		public UnifiedDiff getUnifiedDiff() {
			return this.diff;
		}

		@Override
		public Rectangle getLastRectangle() {
			return lastRectangle;
		}

		@Override
		public Color getDeletionBackgroundColor() {
			return deletionBackgroundColor;
		}

		@Override
		public Color getDetailedDiffColor() {
			return detailedDiffColor;
		}

		@Override
		public int getTabWidth() {
			return tabWidth;
		}

		/**
		 * Highlighting depends on the document content only, so unlike the drawing
		 * caches it survives a font change. Recomputing it means re-partitioning the
		 * whole file prefix once per mining.
		 */
		private List<StyleRange> styleRanges(String label) {
			if (styleRanges == null) {
				styleRanges = computeStyleRanges(viewer, diff.leftStart, label);
			}
			return styleRanges;
		}

		@Override
		public void dispose() {
			cleanCachedData();
			styleRanges = null;
			super.dispose();
		}

		private void cleanCachedData() {
			foregrounds = null;
			backgrounds = null;
			lastRectangle = null;
			cachedFont = null;
			clearStyledFonts();
		}

		private void clearStyledFonts() {
			styledFonts.forEach((font1, styledFonts1) -> {
				styledFonts1.forEach((style, styledFont) -> {
					styledFont.dispose();
				});
				styledFonts1.clear();
			});
			styledFonts.clear();
		}

		@Override
		public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
			gc.setBackground(this.deletionBackgroundColor);
			Color c = textWidget.getForeground();
			gc.setForeground(c);
			Font font = textWidget.getFont();
			gc.setFont(font);
			if (cachedFont != null && (cachedFont.isDisposed() || !cachedFont.equals(font))) {
				// font might have been changed in the meantime - remove cache
				cleanCachedData();
			}
			cachedFont = font;
			if (lastRectangle != null && backgrounds != null && foregrounds != null) {
				boolean fontIsDisposed = false;
				// draw background
				// vs code is drawing the background to the top right of the editor - we do here
				// the same!
				gc.fillRectangle(0, y, textWidget.getBounds().width, lastRectangle.height);

				// backgrounds
				gc.setBackground(this.detailedDiffColor);
				for (var b : backgrounds) {
					int rectY = y + b.y;
					if (rectY < 0) {
						continue;
					}
					gc.fillRectangle(x + b.x, rectY, b.width, b.height);
				}
				// foregrounds
				for (var f : foregrounds) {
					if (y + f.y() < 0) {
						continue;
					}
					if (f.font() == null) {
						gc.setFont(cachedFont);
					} else if (!f.font().isDisposed()) {
						gc.setFont(f.font());
					} else {
						cleanCachedData();
						cachedFont = font;
						fontIsDisposed = true;
						break;
					}
					gc.setBackground(f.background());
					gc.setForeground(f.foreground());
					gc.drawString(f.str(), x + f.x(), y + f.y(), true);
				}
				if (!fontIsDisposed) {
					lastRectangle = new Rectangle(x, y, lastRectangle.width, lastRectangle.height);
					return new Point(lastRectangle.width, lastRectangle.height);
				}
			}
			// first run to get width and height for label
			Point result = super.draw(gc, textWidget, color, x, y);
			lastRectangle = new Rectangle(x, y, result.x, result.y);

			// draw background
			// vs code is drawing the background to the top right of the editor - we do here
			// the same!
			gc.fillRectangle(0, y, textWidget.getBounds().width /* result.x */, result.y);

			String label = getLabel();
			List<StyleRange> ranges = styleRanges(label);

			// draw darker background for detailed diff
			gc.setBackground(this.detailedDiffColor);
			backgrounds = new ArrayList<>();
			String[] diffLines = null;
			Document diffStrDoc = null;
			for (var range : this.detailedDiffRanges) {
				String diffStr = range.diffStr();
				int detailedDiffStart = range.start();
				int detailedDiffLength = range.length();
				int fromLine = range.fromLine();
				int toLine = range.toLine();
				try {
					var rangeInfo = new RangeInfo(-1, -1, null);
					if (fromLine == toLine) {
						int starty = getYForLine(fromLine - 1, y, gc, textWidget);
						Point start = getPositionForOffset(textWidget, gc, detailedDiffStart, diffStr, ranges,
								rangeInfo);
						Point curr = getPositionForOffset(textWidget, gc, detailedDiffStart + detailedDiffLength,
								diffStr, ranges, rangeInfo);
						if (start != null && curr != null && curr.x > start.x) {
							int rectX = x + start.x;
							int rectY = starty;
							int rectWidth = curr.x - start.x;
							int rectHeight = curr.y;
							if (starty >= 0) {
								gc.fillRectangle(rectX, rectY, rectWidth, rectHeight);
							}
							backgrounds.add(new Rectangle(rectX - x, rectY - y, rectWidth, rectHeight));
						}
					} else {
						// mark first line until end
						if (diffLines == null) {
							diffLines = diffStr.split("\n"); //$NON-NLS-1$
							diffStrDoc = new Document(diffStr);
						}
						String firstLine = diffLines[fromLine - 1];
						int starty = getYForLine(fromLine - 1, y, gc, textWidget);
						int idx = getOffsetAtLine(diffStr, detailedDiffStart);
						int fromLineOffset;
						try {
							fromLineOffset = diffStrDoc.getLineOffset(fromLine - 1);
						} catch (BadLocationException e) {
							error(e);
							continue;
						}
						boolean ignoreFirstLine = false;
						int remainingOnLine = firstLine.length() - idx;
						if (remainingOnLine == 0) {
							// detailed diff starts exactly at end-of-line; nothing visible to highlight
							ignoreFirstLine = true;
						} else if (remainingOnLine == 1 && diffStr.charAt(fromLineOffset + idx) == '\r') {
							// detailed diff covers only the CR of a CRLF terminator
							ignoreFirstLine = true;
						}
						if (!ignoreFirstLine) {
							Point start = getPositionForOffset(textWidget, gc, fromLineOffset + idx, diffStr, ranges,
									rangeInfo);
							Point curr = getPositionForOffset(textWidget, gc, fromLineOffset + firstLine.length(),
									diffStr, ranges, rangeInfo);
							if (start != null && curr != null && curr.x > 0) {
								int rectX = x + start.x;
								int rectY = starty;
								int rectWidth = curr.x - start.x;
								int rectHeight = curr.y;
								gc.fillRectangle(rectX, rectY, rectWidth, rectHeight);
								backgrounds.add(new Rectangle(rectX - x, rectY - y, rectWidth, rectHeight));
							}
						}
						// all the lines between first and last line
						for (int middleLine = fromLine + 1; middleLine < toLine; middleLine++) {
							starty = getYForLine(middleLine - 1, y, gc, textWidget);
							try {
								int currentLineEndOffset = diffStrDoc.getLineOffset(middleLine - 1)
										+ diffStrDoc.getLineLength(middleLine - 1)
										- getLineDelimiterLength(diffStrDoc, middleLine);
								Point curr = getPositionForOffset(textWidget, gc, currentLineEndOffset, diffStr, ranges,
										rangeInfo);
								if (curr != null && curr.x > 0) {
									int rectX = x;
									int rectY = starty;
									int rectWidth = curr.x;
									int rectHeight = curr.y;
									if (starty >= 0) {
										gc.fillRectangle(rectX, rectY, rectWidth, rectHeight);
									}
									backgrounds.add(new Rectangle(rectX - x, rectY - y, rectWidth, rectHeight));
								}
							} catch (BadLocationException e) {
								error(e);
							}
						}
						// last line
						starty = getYForLine(toLine - 1, y, gc, textWidget);
						Point curr = getPositionForOffset(textWidget, gc, detailedDiffStart + detailedDiffLength,
								diffStr, ranges, rangeInfo);
						if (curr != null && curr.x > 0) {
							int rectX = x;
							int rectY = starty;
							int rectWidth = curr.x;
							int rectHeight = curr.y;
							if (starty >= 0) {
								gc.fillRectangle(rectX, rectY, rectWidth, rectHeight);
							}
							backgrounds.add(new Rectangle(rectX - x, rectY - y, rectWidth, rectHeight));
						}
					}
				} catch (IllegalArgumentException e) {
					error(e);
				}
			}
			// draw foreground again
			if (ranges.size() == 0) {
				// no syntax coloring available (e.g. plain-text editor without a
				// presentation reconciler); fall back to super.draw() so the label is
				// still rendered. backgrounds/foregrounds are intentionally left unset
				// so the next paint takes the full slow path again.
				result = super.draw(gc, textWidget, color, x, y);
				return result;
			}
			foregrounds = new ArrayList<>();
			gc.setFont(cachedFont);
			drawStyleRanges(gc, textWidget, ranges, label, styledFonts, x, y, foregrounds::add);
			return result;
		}

		private int getLineDelimiterLength(Document diffStrDoc, int middleLine) throws BadLocationException {
			String delim = diffStrDoc.getLineDelimiter(middleLine - 1);
			if (delim == null) {
				return 0;
			}
			return delim.length();
		}

		private static class RangeInfo {
			int rangeIndex;
			int offset;
			Point position;

			RangeInfo(int rangeIndex, int offset, Point position) {
				this.rangeIndex = rangeIndex;
				this.offset = offset;
				this.position = position;
			}
		}

		private Point getPositionForOffset(StyledText tw, GC gc, int offset, String str, List<StyleRange> ranges,
				RangeInfo rangeInfo) {
			String sub = str.substring(0, offset);
			Point result = null;
			var before = gc.getFont();
			int twLineHeight = tw.getLineHeight();
			try {
				int i = 0;
				if (rangeInfo.rangeIndex != -1 && offset >= rangeInfo.offset) {
					i = rangeInfo.rangeIndex + 1;
					result = rangeInfo.position;
				}
				for (; i < ranges.size(); i++) {
					var range = ranges.get(i);
					if (range.start <= offset) {
						boolean rangeEndBeforeOffset = true;
						int rangeEnd = range.start + range.length;
						if (rangeEnd > offset) {
							rangeEnd = offset;
							rangeEndBeforeOffset = false;
						}
						sub = str.substring(range.start, rangeEnd);
						if (offset == rangeEnd && offset == str.length()) {
							while (sub.endsWith("\n")) { //$NON-NLS-1$
								sub = sub.substring(0, sub.length() - 1);
							}
						}
						int lfIdx = sub.lastIndexOf("\n"); //$NON-NLS-1$
						if (lfIdx > 0) {
							if (lfIdx == sub.length() - 1 && isLastForCurrentOffset(ranges, i, offset)) {
								sub = sub.substring(0, lfIdx);
							} else {
								sub = sub.substring(lfIdx + 1);
								result = null;
							}
						}
						var rangeWithFont = transformFontStyleToFont(before, range);
						if (rangeWithFont.font != null) {
							gc.setFont(rangeWithFont.font);
						} else {
							gc.setFont(before);
						}
						// gc.stringExtent does not consider tabs - we need to replace them with spaces
						// to get correct width
						Point extent = gc.stringExtent(removeLeadingNewLines(replaceTabWithSpaces(sub, tabWidth)));
						if (result == null) {
							result = extent;
							result.y = twLineHeight;
						} else {
							result.x += extent.x;
						}
						if (rangeEndBeforeOffset) {
							rangeInfo.offset = rangeEnd;
							rangeInfo.rangeIndex = i;
							rangeInfo.position = new Point(result.x, result.y);
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

		private boolean isLastForCurrentOffset(List<StyleRange> ranges, int i, int offset) {
			int nextIdx = i + 1;
			if (nextIdx >= ranges.size()) {
				return false;
			}
			StyleRange next = ranges.get(nextIdx);
			if (next.start >= offset) {
				return true;
			}
			return false;
		}

		private StyleRange transformFontStyleToFont(Font baseFont, StyleRange styleRange) {
			return UnifiedDiffCodeMiningProvider.transformFontStyleToFont(styledFonts, baseFont, styleRange);
		}

		private int getOffsetAtLine(String str, int off) {
			Document doc;
			if (off == str.length()) {
				doc = new Document(str);
			} else {
				doc = new Document(str.stripTrailing());
			}
			try {
				if (off > doc.getLength()) {
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
			int textWidgetLineHeight = textWidget.getLineHeight();
			y += line * (textWidgetLineHeight + textWidget.getLineSpacing());
			return y;
		}
	}

	static void openOverlay(StyledText overlay, ITextViewer viewer) {
		overlay.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				overlay.dispose();
				setTextEditorActionsActivated(viewer, true);
			}
		});
		overlay.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ESC) {
					overlay.dispose();
					setTextEditorActionsActivated(viewer, true);
				}
				e.doit = false;
			}
		});
		setTextEditorActionsActivated(viewer, false);
	}

	static void setTextEditorActionsActivated(ITextViewer viewer, boolean state) {
		IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (part instanceof MultiPageEditorPart multiPageEditorPart) {
			Object page = multiPageEditorPart.getSelectedPage();
			if (page instanceof IEditorPart editorPart) {
				part = editorPart;
			}
		}
		if (!(part instanceof AbstractTextEditor) || part.getSite().getWorkbenchWindow().isClosing()) {
			return;
		}
		if (UnifiedDiffManager.isViewerInPart(part, viewer)) {
			try {
				Method method = AbstractTextEditor.class.getDeclaredMethod("setActionActivation", //$NON-NLS-1$
						boolean.class);
				method.setAccessible(true);
				method.invoke(part, Boolean.valueOf(state));
			} catch (IllegalArgumentException | ReflectiveOperationException ex) {
				error(ex);
			}
		}
	}

	/**
	 * Returns a {@link StyleRange} whose {@code font} carries the range's font
	 * style. Fonts are cached in the caller-owned {@code styledFonts} map so the
	 * cache lifetime stays tied to the owning mining instance.
	 */
	static StyleRange transformFontStyleToFont(Map<Font, Map<Integer, Font>> styledFonts, Font baseFont,
			StyleRange styleRange) {
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
}
