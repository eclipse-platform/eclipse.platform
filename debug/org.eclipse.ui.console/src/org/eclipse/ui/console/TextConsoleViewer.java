/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Paul Pazderski  - Bug 550620: reimplementation of style override (to fix existing and future problems with link styling)
 *******************************************************************************/

package org.eclipse.ui.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentAdapter;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.internal.console.ConsoleDocumentAdapter;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Default viewer used to display a <code>TextConsole</code>.
 * <p>
 * Clients may subclass this class.
 * </p>
 *
 * @since 3.1
 */
public class TextConsoleViewer extends SourceViewer implements LineStyleListener, LineBackgroundListener, MouseTrackListener, MouseMoveListener, MouseListener {
	/**
	 * Adapts document to the text widget.
	 */
	private ConsoleDocumentAdapter documentAdapter;

	private IHyperlink hyperlink;

	private Cursor handCursor;

	private Cursor textCursor;

	private int consoleWidth = -1;

	private TextConsole console;

	private boolean consoleAutoScrollLock = true;


	private IPropertyChangeListener propertyChangeListener;

	private IScrollLockStateProvider scrollLockStateProvider;

	private IDocumentListener documentListener = new IDocumentListener() {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			updateLinks(event.fOffset);
		}
	};
	// event listener used to send event to hyperlink for IHyperlink2
	private Listener mouseUpListener = event -> {
		if (hyperlink != null) {
			String selection = getTextWidget().getSelectionText();
			if (selection.length() <= 0) {
				if (event.button == 1) {
					if (hyperlink instanceof IHyperlink2) {
						((IHyperlink2) hyperlink).linkActivated(event);
					} else {
						hyperlink.linkActivated();
					}
				}
			}
		}
	};

	// to store to user scroll lock action
	private AtomicBoolean userHoldsScrollLock = new AtomicBoolean(false);

	WorkbenchJob revealJob = new WorkbenchJob("Reveal End of Document") {//$NON-NLS-1$
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			scrollToEndOfDocument();
			return Status.OK_STATUS;
		}
	};

	// reveal the end of the document
	private void scrollToEndOfDocument() {
		StyledText textWidget = getTextWidget();
		if (textWidget != null && !textWidget.isDisposed()) {
			int lineCount = textWidget.getLineCount();
			textWidget.setTopIndex(lineCount > 0 ? lineCount - 1 : 0);
		}

	}

	// set the scroll Lock setting for Console Viewer and Console View
	private void setScrollLock(boolean lock) {
		userHoldsScrollLock.set(lock);
		if (scrollLockStateProvider != null && scrollLockStateProvider.getAutoScrollLock() != lock) {
			scrollLockStateProvider.setAutoScrollLock(lock);
		}
	}

	/*
	 * Checks if at the end of document
	 */
	private boolean checkEndOfDocument() {
		StyledText textWidget = getTextWidget();
		if (textWidget != null && !textWidget.isDisposed()) {
			int partialBottomIndex = JFaceTextUtil.getPartialBottomIndex(textWidget);
			int lineCount = textWidget.getLineCount();
			int delta = textWidget.getVerticalBar().getIncrement();
			return lineCount - partialBottomIndex < delta;
		}
		return false;
	}

	/*
	 * Check if user preference is enabled for auto scroll lock and the document is empty or the line count is smaller than each
	 * vertical scroll
	 */
	private boolean isAutoScrollLockNotApplicable() {
		if (!consoleAutoScrollLock) {
			return true;
		}
		StyledText textWidget = getTextWidget();
		if (textWidget != null && !textWidget.isDisposed()) {
			return (textWidget.getLineCount() <= textWidget.getVerticalBar().getIncrement());
		}
		return false;
	}

	/*
	 * Checks if at the start of document
	 */
	private boolean checkStartOfDocument() {
		StyledText textWidget = getTextWidget();
		if (textWidget != null && !textWidget.isDisposed()) {
			int partialTopIndex = JFaceTextUtil.getPartialTopIndex(textWidget);
			int lineCount = textWidget.getLineCount();
			int delta = textWidget.getVerticalBar().getIncrement();
			return lineCount - partialTopIndex < delta;
		}
		return false;
	}

	private IPositionUpdater positionUpdater = event -> {
		try {
			IDocument document = getDocument();
			if (document != null) {
				for (Position position : document.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY)) {
					if (position.offset == event.fOffset && position.length <= event.fLength) {
						position.delete();
					}
					if (position.isDeleted) {
						document.removePosition(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY, position);
					}
				}
			}
		} catch (BadPositionCategoryException e) {
		}
	};

	/**
	 * Constructs a new viewer in the given parent for the specified console.
	 *
	 * @param parent the containing composite
	 * @param console the text console
	 * @param scrollLockStateProvider the scroll lock state provider
	 * @since 3.6
	 */
	public TextConsoleViewer(Composite parent, TextConsole console, IScrollLockStateProvider scrollLockStateProvider) {
		this(parent, console);
		this.scrollLockStateProvider = scrollLockStateProvider;


	}

	/**
	 * Constructs a new viewer in the given parent for the specified console.
	 *
	 * @param parent containing widget
	 * @param console text console
	 */
	public TextConsoleViewer(Composite parent, TextConsole console) {
		super(parent, null, SWT.V_SCROLL | SWT.H_SCROLL);
		this.console = console;
		this.consoleAutoScrollLock = console.isConsoleAutoScrollLock();

		IDocument document = console.getDocument();
		setDocument(document);

		StyledText styledText = getTextWidget();
		styledText.setDoubleClickEnabled(true);
		styledText.addLineStyleListener(this);
		styledText.addLineBackgroundListener(this);
		styledText.setEditable(true);
		styledText.setBackground(console.getBackground());
		setFont(console.getFont());
		styledText.addMouseTrackListener(this);
		styledText.addListener(SWT.MouseUp, mouseUpListener);
		// event listener used to send event to vertical scroll bar
		styledText.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (isAutoScrollLockNotApplicable()) {
					return;
				}
				// scroll lock if vertical scroll bar dragged, OR selection on
				// vertical bar used
				if (e.detail == SWT.TOP || e.detail == SWT.HOME) {
					// selecting TOP or HOME should lock
					setScrollLock(true);
				}
				if (e.detail == SWT.ARROW_UP || e.detail == SWT.PAGE_UP) {
					setScrollLock(true);
				} else if (e.detail == SWT.END || e.detail == SWT.BOTTOM) {
					// selecting BOTTOM or END from vertical scroll makes it
					// reveal the end
					setScrollLock(false);
				} else if (e.detail == SWT.DRAG) {
					if (checkEndOfDocument()) {
						setScrollLock(false);
					} else {
						setScrollLock(true);
					}
				} else if ((e.detail == SWT.PAGE_DOWN || e.detail == SWT.ARROW_DOWN) && checkEndOfDocument()) {
					// unlock if Down at the end of document
					setScrollLock(false);
				}
			}
		});
		styledText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (isAutoScrollLockNotApplicable()) {
					return;
				}
				// lock the scroll if PAGE_UP ,HOME or TOP selected
				if (e.keyCode == SWT.HOME || e.keyCode == SWT.TOP) {
					setScrollLock(true);
				} else if ((e.keyCode == SWT.PAGE_UP || e.keyCode == SWT.ARROW_UP) && !checkStartOfDocument()) {
					setScrollLock(true);
				} else if (e.keyCode == SWT.END || e.keyCode == SWT.BOTTOM) {
					setScrollLock(false);// selecting END makes it reveal the
					// end
				} else if ((e.keyCode == SWT.PAGE_DOWN || e.keyCode == SWT.ARROW_DOWN) && checkEndOfDocument()) {
					// unlock if Down at the end of document
					setScrollLock(false);
				}
			}
		});
		styledText.addMouseWheelListener(e -> {
			if (isAutoScrollLockNotApplicable()) {
				return;
			}
			if (e.count < 0) { // Mouse dragged down
				if (checkEndOfDocument()) {
					setScrollLock(false);
				}
			} else if (!userHoldsScrollLock.get()) {
				setScrollLock(true);
			}
		});

		styledText.addVerifyListener(e -> {
			// unlock the auto lock if user starts typing only if it was not manual lock
			if (scrollLockStateProvider != null && !scrollLockStateProvider.getScrollLock()) {
				setScrollLock(false);
			}
		});

		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		propertyChangeListener = new HyperlinkColorChangeListener();
		colorRegistry.addListener(propertyChangeListener);

		revealJob.setSystem(true);
		document.addDocumentListener(documentListener);
		document.addPositionUpdater(positionUpdater);
	}

	/**
	 * Sets the tab width used by this viewer.
	 *
	 * @param tabWidth
	 *            the tab width used by this viewer
	 */
	public void setTabWidth(int tabWidth) {
		StyledText styledText = getTextWidget();
		int oldWidth = styledText.getTabs();
		if (tabWidth != oldWidth) {
			styledText.setTabs(tabWidth);
		}
	}

	/**
	 * Sets the font used by this viewer.
	 *
	 * @param font
	 *            the font used by this viewer
	 */
	public void setFont(Font font) {
		StyledText styledText = getTextWidget();
		Font oldFont = styledText.getFont();
		if (oldFont == font) {
			return;
		}
		if (font == null || !(font.equals(oldFont))) {
			styledText.setFont(font);
		}
	}

	/**
	 * Positions the cursor at the end of the document.
	 */
	protected void revealEndOfDocument() {
		revealJob.schedule(50);
	}

	@Override
	public void lineGetStyle(LineStyleEvent event) {
		IDocument document = getDocument();
		if (document != null && document.getLength() > 0) {
			ArrayList<StyleRange> ranges = new ArrayList<>();
			int offset = event.lineOffset;
			int length = event.lineText.length();

			IDocumentPartitioner partitioner = document.getDocumentPartitioner();
			if (partitioner instanceof IConsoleDocumentPartitioner) {
				StyleRange[] partitionerStyles = ((IConsoleDocumentPartitioner) partitioner).getStyleRanges(offset,
						length);
				if (partitionerStyles != null) {
					Collections.addAll(ranges, partitionerStyles);
				}
			}

			try {
				Position[] positions = getDocument().getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
				Position[] overlap = findPosition(offset, length, positions);
				Color color = JFaceColors.getHyperlinkText(Display.getCurrent());
				if (overlap != null) {
					for (Position position : overlap) {
						StyleRange linkRange = new StyleRange(position.offset, position.length, color, null);
						linkRange.underline = true;
						overrideStyleRange(ranges, linkRange);
					}
				}
			} catch (BadPositionCategoryException e) {
			}

			if (ranges.size() > 0) {
				event.styles = ranges.toArray(new StyleRange[ranges.size()]);
			}
		}
	}

	/**
	 * Apply new style range to list of existing style ranges. If the new style
	 * range overlaps with any of the existing style ranges the new style overrides
	 * the existing one in the affected range by splitting/adjusting the existing
	 * ones.
	 *
	 * @param ranges   list of existing style ranges (will contain the new style
	 *                 range when finished)
	 * @param newRange new style range which should be combined with the existing
	 *                 ranges
	 */
	private static void overrideStyleRange(List<StyleRange> ranges, StyleRange newRange) {
		final int overrideStart = newRange.start;
		final int overrideEnd = overrideStart + newRange.length;
		int insertIndex = ranges.size();
		for (int i = ranges.size() - 1; i >= 0; i--) {
			final StyleRange existingRange = ranges.get(i);
			final int existingStart = existingRange.start;
			final int existingEnd = existingStart + existingRange.length;

			// Find first position to insert where offset of new range is smaller then all
			// offsets before. This way the list is still sorted by offset after insert if
			// it was sorted before and it will not fail if list was not sorted.
			if (overrideStart <= existingStart) {
				insertIndex = i;
			}

			// adjust the existing range if required
			if (overrideStart <= existingStart) { // new range starts before or with existing
				if (overrideEnd < existingStart) {
					// new range lies before existing range. No overlapping.
					// new range: ++++_________
					// existing : ________=====
					// . result : ++++____=====
					// nothing to do
				} else if (overrideEnd < existingEnd) {
					// new range overlaps start of existing.
					// new range: ++++++++_____
					// existing : _____========
					// . result : ++++++++=====
					final int overlap = overrideEnd - existingStart;
					existingRange.start += overlap;
					existingRange.length -= overlap;
				} else {
					// new range completely overlaps existing.
					// new range: ___++++++++++
					// existing : ___======____
					// . result : ___++++++++++
					ranges.remove(i);
				}
			} else { // new range starts inside or after existing
				if (existingEnd < overrideStart) {
					// new range lies after existing range. No overlapping.
					// new range: _________++++
					// existing : =====________
					// . result : =====____++++
					// nothing to do
				} else if (overrideEnd >= existingEnd) {
					// new range overlaps end of existing.
					// new range: _____++++++++
					// existing : ========_____
					// . result : =====++++++++
					existingRange.length -= existingEnd - overrideStart;
				} else {
					// new range lies inside existing range but not overrides all of it
					// (and does not touch first or last offset of existing)
					// new range: ____+++++____
					// existing : =============
					// . result : ====+++++====
					final StyleRange clonedRange = (StyleRange) existingRange.clone();
					existingRange.length = overrideStart - existingStart;
					clonedRange.start = overrideEnd;
					clonedRange.length = existingEnd - overrideEnd;
					ranges.add(i + 1, clonedRange);
				}
			}
		}
		ranges.add(insertIndex, newRange);
	}

	/**
	 * Binary search for the positions overlapping the given range
	 *
	 * @param offset    the offset of the range
	 * @param length    the length of the range
	 * @param positions the positions to search
	 * @return the positions overlapping the given range, or <code>null</code>
	 */
	private Position[] findPosition(int offset, int length, Position[] positions) {

		if (positions.length == 0) {
			return null;
		}

		int rangeEnd = offset + length;
		int left = 0;
		int right = positions.length - 1;
		int mid = 0;
		Position position = null;

		while (left < right) {

			mid = (left + right) / 2;

			position = positions[mid];
			if (rangeEnd < position.getOffset()) {
				if (left == mid) {
					right = left;
				} else {
					right = mid - 1;
				}
			} else if (offset > (position.getOffset() + position.getLength() - 1)) {
				if (right == mid) {
					left = right;
				} else {
					left = mid + 1;
				}
			} else {
				left = right = mid;
			}
		}

		List<Position> list = new ArrayList<>();
		int index = left - 1;
		if (index >= 0) {
			position = positions[index];
			while (index >= 0 && (position.getOffset() + position.getLength()) > offset) {
				index--;
				if (index > 0) {
					position = positions[index];
				}
			}
		}
		index++;
		position = positions[index];
		while (index < positions.length && (position.getOffset() < rangeEnd)) {
			list.add(position);
			index++;
			if (index < positions.length) {
				position = positions[index];
			}
		}

		if (list.isEmpty()) {
			return null;
		}
		return list.toArray(new Position[list.size()]);
	}

	@Override
	public void lineGetBackground(LineBackgroundEvent event) {
		event.lineBackground = null;
	}

	/**
	 * Returns the hand cursor.
	 *
	 * @return the hand cursor
	 */
	protected Cursor getHandCursor() {
		if (handCursor == null) {
			handCursor = ConsolePlugin.getStandardDisplay().getSystemCursor(SWT.CURSOR_HAND);
		}
		return handCursor;
	}

	/**
	 * Returns the text cursor.
	 *
	 * @return the text cursor
	 */
	protected Cursor getTextCursor() {
		if (textCursor == null) {
			textCursor = ConsolePlugin.getStandardDisplay().getSystemCursor(SWT.CURSOR_IBEAM);
		}
		return textCursor;
	}

	/**
	 * Notification a hyperlink has been entered.
	 *
	 * @param link
	 *            the link that was entered
	 */
	protected void linkEntered(IHyperlink link) {
		Control control = getTextWidget();
		if (hyperlink != null) {
			linkExited(hyperlink);
		}
		hyperlink = link;
		hyperlink.linkEntered();
		control.setCursor(getHandCursor());
		control.redraw();
		control.addMouseListener(this);
	}

	/**
	 * Notification a link was exited.
	 *
	 * @param link
	 *            the link that was exited
	 */
	protected void linkExited(IHyperlink link) {
		link.linkExited();
		hyperlink = null;
		Control control = getTextWidget();
		control.setCursor(getTextCursor());
		control.redraw();
		control.removeMouseListener(this);
	}

	@Override
	public void mouseEnter(MouseEvent e) {
		getTextWidget().addMouseMoveListener(this);
	}

	@Override
	public void mouseExit(MouseEvent e) {
		getTextWidget().removeMouseMoveListener(this);
		if (hyperlink != null) {
			linkExited(hyperlink);
		}
	}

	@Override
	public void mouseHover(MouseEvent e) {
	}

	@Override
	public void mouseMove(MouseEvent e) {
		Point p = new Point(e.x, e.y);
		int offset = getTextWidget().getOffsetAtPoint(p);
		updateLinks(offset);
	}

	/**
	 * The cursor has just be moved to the given offset, the mouse has hovered over
	 * the given offset. Update link rendering.
	 *
	 * @param offset The offset cursor has been moved to.
	 */
	protected void updateLinks(int offset) {
		if (offset >= 0) {
			IHyperlink link = getHyperlink(offset);
			if (link != null) {
				if (link.equals(hyperlink)) {
					return;
				}
				linkEntered(link);
				return;
			}
		}
		if (hyperlink != null) {
			linkExited(hyperlink);
		}
	}

	/**
	 * Returns the currently active hyperlink or <code>null</code> if none.
	 *
	 * @return the currently active hyperlink or <code>null</code> if none
	 */
	public IHyperlink getHyperlink() {
		return hyperlink;
	}

	/**
	 * Returns the hyperlink at the specified offset, or <code>null</code> if
	 * none.
	 *
	 * @param offset offset at which a hyperlink has been requested
	 * @return hyperlink at the specified offset, or <code>null</code> if none
	 */
	public IHyperlink getHyperlink(int offset) {
		if (offset >= 0 && console != null) {
			return console.getHyperlink(offset);
		}
		return null;
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
	}

	@Override
	public void mouseDown(MouseEvent e) {
	}

	@Override
	public void mouseUp(MouseEvent e) {
	}

	@Override
	protected IDocumentAdapter createDocumentAdapter() {
		if (documentAdapter == null) {
			documentAdapter = new ConsoleDocumentAdapter(consoleWidth = -1);
		}
		return documentAdapter;
	}

	/**
	 * Sets the user preference for console auto scroll lock.
	 *
	 * @param autoScrollLock user preference for console auto scroll lock
	 * @since 3.8
	 */
	public void setConsoleAutoScrollLock(boolean autoScrollLock) {
		if (consoleAutoScrollLock != autoScrollLock) {
			consoleAutoScrollLock = autoScrollLock;
		}
	}

	/**
	 * Sets the console to have a fixed character width. Use -1 to indicate that
	 * a fixed width should not be used.
	 *
	 * @param width
	 *            fixed character width of the console, or -1
	 */
	public void setConsoleWidth(int width) {
		if (consoleWidth != width) {
			consoleWidth = width;
			ConsolePlugin.getStandardDisplay().asyncExec(() -> {
				if (documentAdapter != null) {
					documentAdapter.setWidth(consoleWidth);
				}
			});
		}
	}

	@Override
	protected void handleDispose() {
		IDocument document = getDocument();
		if (document != null) {
			document.removeDocumentListener(documentListener);
			document.removePositionUpdater(positionUpdater);
		}

		StyledText styledText = getTextWidget();
		styledText.removeLineStyleListener(this);
		styledText.removeLineBackgroundListener(this);
		styledText.removeMouseTrackListener(this);

		hyperlink = null;
		console = null;

		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		colorRegistry.removeListener(propertyChangeListener);

		super.handleDispose();
	}

	class HyperlinkColorChangeListener implements IPropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(JFacePreferences.ACTIVE_HYPERLINK_COLOR) || event.getProperty().equals(JFacePreferences.HYPERLINK_COLOR)) {
				getTextWidget().redraw();
			}
		}

	}

	/*
	 * work around to memory leak in TextViewer$WidgetCommand
	 */
	@Override
	protected void updateTextListeners(WidgetCommand cmd) {
		super.updateTextListeners(cmd);
		cmd.preservedText = null;
		cmd.event = null;
		cmd.text = null;
	}

	@Override
	protected void internalRevealRange(int start, int end) {
		StyledText textWidget = getTextWidget();
		int startLine = documentAdapter.getLineAtOffset(start);
		int endLine = documentAdapter.getLineAtOffset(end);

		int top = textWidget.getTopIndex();
		if (top > -1) {
			// scroll vertically
			@SuppressWarnings("deprecation")
			int lines = getVisibleLinesInViewport();
			int bottom = top + lines;

			// two lines at the top and the bottom should always be left
			// if window is smaller than 5 lines, always center position is
			// chosen
			int bufferZone = 2;
			if (startLine >= top + bufferZone && startLine <= bottom - bufferZone && endLine >= top + bufferZone && endLine <= bottom - bufferZone) {

				// do not scroll at all as it is already visible
			} else {
				int delta = Math.max(0, lines - (endLine - startLine));
				textWidget.setTopIndex(startLine - delta / 3);
				updateViewportListeners(INTERNAL);
			}

			// scroll horizontally
			if (endLine < startLine) {
				endLine += startLine;
				startLine = endLine - startLine;
				endLine -= startLine;
			}

			int startPixel = -1;
			int endPixel = -1;

			if (endLine > startLine) {
				// reveal the beginning of the range in the start line
				IRegion extent = getExtent(start, start);
				startPixel = extent.getOffset() + textWidget.getHorizontalPixel();
				endPixel = startPixel;
			} else {
				IRegion extent = getExtent(start, end);
				startPixel = extent.getOffset() + textWidget.getHorizontalPixel();
				endPixel = startPixel + extent.getLength();
			}

			int visibleStart = textWidget.getHorizontalPixel();
			int visibleEnd = visibleStart + textWidget.getClientArea().width;

			// scroll only if not yet visible
			if (startPixel < visibleStart || visibleEnd < endPixel) {
				// set buffer zone to 10 pixels
				bufferZone = 10;
				int newOffset = visibleStart;
				int visibleWidth = visibleEnd - visibleStart;
				int selectionPixelWidth = endPixel - startPixel;

				if (startPixel < visibleStart) {
					newOffset = startPixel;
				} else if (selectionPixelWidth + bufferZone < visibleWidth) {
					newOffset = endPixel + bufferZone - visibleWidth;
				} else {
					newOffset = startPixel;
				}

				float index = ((float) newOffset) / ((float) getAverageCharWidth());

				textWidget.setHorizontalIndex(Math.round(index));
			}

		}
	}


}
