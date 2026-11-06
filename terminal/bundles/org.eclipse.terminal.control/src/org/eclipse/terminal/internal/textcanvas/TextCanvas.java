/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Michael Scharf (Wind River) - [240098] The cursor should not blink when the terminal is disconnected
 * Uwe Stieber (Wind River) - [281328] The very first few characters might be missing in the terminal control if opened and connected programmatically
 * Martin Oberhuber (Wind River) - [294327] After logging in, the remote prompt is hidden
 * Anton Leherbauer (Wind River) - [294468] Fix scroller and text line rendering
 * Uwe Stieber (Wind River) - [205486] Fix ScrollLock always moving to line 1
 * Anton Leherbauer (Wind River) - [219589] Copy an entire line selection
 * Anton Leherbauer (Wind River) - [196465] Resizing Terminal changes Scroller location
 * Anton Leherbauer (Wind River) - [324608] Terminal has strange scrolling behaviour
 * Martin Oberhuber (Wind River) - [265352][api] Allow setting fonts programmatically
 * Anton Leherbauer (Wind River) - [434749] UnhandledEventLoopException when copying to clipboard while the selection is empty
 * Davy Landman (CWI) - [475267][api] Allow custom mouse listeners
 * Philipp Kurrle (Advantest) - [2131] Implement word and line selection
 *******************************************************************************/
package org.eclipse.terminal.internal.textcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.terminal.control.ITerminalMouseListener;
import org.eclipse.terminal.internal.model.CharWidth;
import org.eclipse.terminal.model.ITerminalTextDataReadOnly;
import org.eclipse.terminal.model.TerminalColor;

/**
 * A cell oriented Canvas. Maintains a list of "cells".
 * It can either be vertically or horizontally scrolled.
 * The CellRenderer is responsible for painting the cell.
 */
public class TextCanvas extends GridCanvas {
	protected final ITextCanvasModel fCellCanvasModel;
	/** Renders the cells */
	private final ILinelRenderer fCellRenderer;
	private boolean fScrollLock;
	private Point fDraggingStart;
	/** -1 above the top, 1 below the bottom, 0 while the pointer is inside */
	private boolean fDragScrolling;
	private static final int DRAG_SCROLL_INTERVAL = 60;
	private Point fDraggingEnd;
	private boolean fHasSelection;
	private ResizeListener fResizeListener;
	private final List<ITerminalMouseListener> fMouseListeners;
	private SelectionMode fSelMode = SelectionMode.NONE;
	private IMouseWheelHandler fWheelHandler;
	private IMouseButtonHandler fButtonHandler;
	// the buttons whose press went to the program, as bits 1 << button: their releases go there too
	private int fReportedButtons;
	// whether the last press went to the program: its double-click and context menu are its too
	private boolean fPressReported;

	private enum SelectionMode {
		NONE, DRAG, WORD, LINE
	}

	// The minSize is meant to determine the minimum size of the backing store
	// (grid) into which remote data is rendered. If the viewport is smaller
	// than that minimum size, the backing store size remains at the minSize,
	// and a scrollbar is shown instead. In reality, this has the following
	// issues or effects today:
	//  (a) Bug 281328: For very early data coming in before the widget is
	//      realized, the minSize determines into what initial grid that is
	//      rendered. See also @link{#addResizeHandler(ResizeListener)}.
	//  (b) Bug 294468: Since we have redraw and size computation problems
	//      with horizontal scrollers, for now the minColumns must be small
	//      enough to avoid a horizontal scroller appearing in most cases.
	//  (b) Bug 294327: since we have problems with the vertical scroller
	//      showing the correct location, minLines must be small enough
	//      to avoid a vertical scroller or new data may be rendered off-screen.
	// As a compromise, we have been working with a 20x4 since the Terminal
	// inception, though many users would want a 80x24 minSize and backing
	// store. Pros and cons of the small minsize:
	//   + consistent "remote size==viewport size", vi works as expected
	//   - dumb terminals which expect 80x24 render garbled on small viewport.
	// If bug 294468 were resolved, an 80 wide minSize would be preferrable
	// since it allows switching the terminal viewport small/large as needed,
	// without destroying the backing store. For a complete solution,
	// Bug 196462 tracks the request for a user-defined fixed-widow-size-mode.
	private int fMinColumns = 80;
	private int fMinLines = 4;
	private boolean fCursorEnabled;
	private boolean fResizing;

	/**
	 * Create a new CellCanvas with the given SWT style bits.
	 * (SWT.H_SCROLL and SWT.V_SCROLL are automatically added).
	 */
	public TextCanvas(Composite parent, ITextCanvasModel model, int style, ILinelRenderer cellRenderer) {
		super(parent, style | SWT.H_SCROLL | SWT.V_SCROLL | SWT.DOUBLE_BUFFERED);
		fCellRenderer = cellRenderer;
		setCellWidth(fCellRenderer.getCellWidth());
		setCellHeight(fCellRenderer.getCellHeight());
		fCellCanvasModel = model;
		fCellCanvasModel.addCellCanvasModelListener(new ITextCanvasModelListener() {
			@Override
			public void rangeChanged(int col, int line, int width, int height) {
				if (isDisposed()) {
					return;
				}
				repaintRange(col, line, width, height);
			}

			@Override
			public void dimensionsChanged(int cols, int rows) {
				if (isDisposed()) {
					return;
				}
				calculateGrid();
			}

			@Override
			public void terminalDataChanged() {
				if (isDisposed()) {
					return;
				}

				// scroll to end (unless scroll lock is active)
				if (!fResizing) {
					calculateGrid();
					scrollToEnd();
				}
			}
		});
		// let the cursor blink if the text canvas gets the focus...
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				fCellCanvasModel.setCursorEnabled(fCursorEnabled);
			}

			@Override
			public void focusLost(FocusEvent e) {
				fCellCanvasModel.setCursorEnabled(false);
			}
		});
		// Before every other mouse listener: a press the program takes is the program's
		// alone. The terminal's own use of it (a selection, the middle-click paste, a
		// double-click, the copy on release) must not happen too, so the event goes no
		// further (SWT delivers no more listeners once the type is None). Shift keeps
		// the mouse for the terminal.
		Listener reporter = event -> {
			switch (event.type) {
			case SWT.MouseDown:
				fPressReported = wouldReport(event.stateMask) && report(event, event.button, true);
				if (!fPressReported) {
					// a release of this button lost earlier (a dialog took it) is not waited for
					fReportedButtons &= ~(1 << event.button);
					return;
				}
				fReportedButtons |= 1 << event.button;
				// The click went to the program, so whatever the terminal had
				// selected is no longer what the pointer is pointing at.
				fCellCanvasModel.setSelection(-1, -1, -1, -1);
				fCellCanvasModel.expandHoverSelectionAt(-1, -1);
				fDraggingStart = null;
				break;
			case SWT.MouseUp:
				// a release goes wherever its press went, however the keyboard stands by then
				if ((fReportedButtons & 1 << event.button) == 0) {
					return;
				}
				fReportedButtons &= ~(1 << event.button);
				report(event, event.button, false);
				break;
			default: // SWT.MouseDoubleClick
				if (!fPressReported) {
					return;
				}
			}
			event.type = SWT.None;
		};
		addListener(SWT.MouseDown, reporter);
		addListener(SWT.MouseUp, reporter);
		addListener(SWT.MouseDoubleClick, reporter);
		// the context menu of a right-click the program took; the menu key still opens it
		addListener(SWT.MenuDetect, event -> {
			if (event.detail == SWT.MENU_MOUSE && fPressReported) {
				event.doit = false;
			}
		});
		fMouseListeners = new ArrayList<>();
		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (fMouseListeners.size() > 0) {
					Point pt = screenPointToCell(e.x, e.y);
					if (pt != null) {
						for (ITerminalMouseListener l : fMouseListeners) {
							l.mouseDoubleClick(fCellCanvasModel.getTerminalText(), pt.y, pt.x, e.button, e.stateMask);
						}
						selectWord(pt);
					}
				}
			}

			/**
			 * Select the word at the given point.
			 * If no word is detected, select the single character at the point.
			 *
			 * @param pt
			 * 		  the point in cell coordinates
			 */
			private void selectWord(Point pt) {
				fSelMode = SelectionMode.WORD;

				fCellCanvasModel.expandHoverSelectionAt(pt.y, pt.x);
				Point start = fCellCanvasModel.getHoverSelectionStart();
				Point end = fCellCanvasModel.getHoverSelectionEnd();
				if (start != null && end != null) {
					fHasSelection = true;
					fDraggingStart = start; // anchor at word start for drag-extend by words
					fCellCanvasModel.setSelectionAnchor(start);
					fCellCanvasModel.setSelection(start.y, end.y, start.x, end.x);
				} else {
					fHasSelection = true;
					fDraggingStart = pt;
					fCellCanvasModel.setSelectionAnchor(pt);
					fCellCanvasModel.setSelection(pt.y, pt.y, pt.x, pt.x);
				}
				// clear hover highlight after using it
				fCellCanvasModel.expandHoverSelectionAt(-1, -1);
			}

			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) { // left button
					fSelMode = SelectionMode.DRAG;
					fDraggingStart = screenPointToCell(e.x, e.y);
					fHasSelection = false;
					if ((e.stateMask & SWT.SHIFT) != 0) {
						Point anchor = fCellCanvasModel.getSelectionAnchor();
						if (anchor != null) {
							fDraggingStart = anchor;
						}
					} else {
						fCellCanvasModel.setSelectionAnchor(fDraggingStart);
					}
					fDraggingEnd = null;
				}
				if (fMouseListeners.size() > 0) {
					Point pt = screenPointToCell(e.x, e.y);
					if (pt != null) {
						for (ITerminalMouseListener l : fMouseListeners) {
							l.mouseDown(fCellCanvasModel.getTerminalText(), pt.y, pt.x, e.button, e.stateMask);
						}
					}
				}
				if (e.count == 3) {
					selectLine(e);
				}
			}

			private void selectLine(MouseEvent e) {
				fSelMode = SelectionMode.LINE;
				Point pt = screenPointToCell(e.x, e.y);
				if (pt != null) {
					ITerminalTextDataReadOnly term = fCellCanvasModel.getTerminalText();
					int row = Math.max(0, Math.min(pt.y, term.getHeight() - 1));
					int startCol = 0;
					int endCol = Math.max(0, term.getWidth() - 1); // inclusive end column
					Point start = new Point(startCol, row);
					Point end = new Point(endCol, row);
					fHasSelection = true;
					// keep anchor at the start of the clicked line for drag-extend by lines
					fDraggingStart = start;
					fCellCanvasModel.setSelectionAnchor(start);
					fCellCanvasModel.setSelection(start.y, end.y, start.x, end.x);
					// notify custom listeners
					if (fMouseListeners.size() > 0) {
						for (ITerminalMouseListener l : fMouseListeners) {
							l.mouseDown(term, pt.y, pt.x, e.button, e.stateMask);
						}
					}
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 1) { // left button
					if (fSelMode == SelectionMode.DRAG) {
						updateHasSelection(e);
						if (fHasSelection) {
							setSelection(screenPointToCell(e.x, e.y));
						} else {
							fCellCanvasModel.setSelection(-1, -1, -1, -1);
						}
					}
					fDraggingStart = null;
					fSelMode = SelectionMode.NONE;
				}
				if (fMouseListeners.size() > 0) {
					Point pt = screenPointToCell(e.x, e.y);
					if (pt != null) {
						for (ITerminalMouseListener l : fMouseListeners) {
							l.mouseUp(fCellCanvasModel.getTerminalText(), pt.y, pt.x, e.button, e.stateMask);
						}
					}
				}
			}
		});
		addMouseMoveListener(e -> {
			if (fDraggingStart == null && wouldReport(e.stateMask)
					&& fButtonHandler.mouseMoved(heldButton(e.stateMask), modifiers(e.stateMask), cell(e.x, e.y))) {
				return;
			}
			if (fDraggingStart != null) {
				// Dragging past an edge keeps the lines beyond it coming into view.
				if (dragPast(e.y) != 0 && !fDragScrolling) {
					fDragScrolling = true;
					getDisplay().timerExec(DRAG_SCROLL_INTERVAL, this::dragScroll);
				}
				updateHasSelection(e);
				extendSelection(screenPointToCell(e.x, e.y));
			} else if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD1) {
				// highlight (underline) word that would be used by MOD1 + mouse click
				Point pt = screenPointToCell(e.x, e.y);
				fCellCanvasModel.expandHoverSelectionAt(pt.y, pt.x);
			} else {
				fCellCanvasModel.expandHoverSelectionAt(-1, -1);
			}
			redraw();
		});
		setVerticalBarVisible(true);
		setHorizontalBarVisible(false);
	}

	/**
	 * A drag moves the end of the selection to the cell, by characters, words or
	 * lines as it began: the one way a drag does, whether the pointer moved or the
	 * view scrolled under it.
	 */
	private void extendSelection(Point cell) {
		switch (fSelMode) {
		case WORD:
			updateWordSelection(cell);
			break;
		case LINE:
			updateLineSelection(cell);
			break;
		case DRAG:
		default:
			setSelection(cell);
			break;
		}
		fCellCanvasModel.expandHoverSelectionAt(-1, -1);
	}

	/** @return -1 above the canvas, 1 below it, 0 on it */
	private int dragPast(int y) {
		return y < 0 ? -1 : y >= getClientArea().height ? 1 : 0;
	}

	private void dragScroll() {
		Point p = isDisposed() ? null : toControl(getDisplay().getCursorLocation());
		// where the pointer is now, not where the last move left it: a new click may have
		// come in between
		int past = p == null ? 0 : dragPast(p.y);
		if (p == null || fDraggingStart == null || past == 0) {
			fDragScrolling = false;
			return;
		}
		scrollYDelta(past * getCellHeight());
		extendSelection(screenPointToCell(p.x, p.y));
		redraw();
		getDisplay().timerExec(DRAG_SCROLL_INTERVAL, this::dragScroll);
	}

	private static class Range {
		final Point start;
		final Point end;

		Range(Point start, Point end) {
			this.start = start;
			this.end = end;
		}

		boolean isValid() {
			return start != null && end != null;
		}
	}

	private Range wordRangeAt(Point pt) {
		fCellCanvasModel.expandHoverSelectionAt(pt.y, pt.x);
		return new Range(fCellCanvasModel.getHoverSelectionStart(), fCellCanvasModel.getHoverSelectionEnd());
	}

	private void setNormalizedSelection(Point start, Point end) {
		if (start == null || end == null) {
			return;
		}

		Point s = start;
		Point e = end;
		if (compare(e, s) < 0) { // normalize order
			s = end;
			e = start;
		}
		int sCol = Math.max(0, s.x);
		int sRow = Math.max(0, s.y);
		fCellCanvasModel.setSelection(sRow, e.y, sCol, e.x);
	}

	private void updateWordSelection(Point curr) {
		if (fDraggingStart == null || curr == null) {
			return;
		}
		// Determine word boundaries at anchor and current positions

		Range anchor = wordRangeAt(fDraggingStart);
		Range current = wordRangeAt(curr);
		if (!anchor.isValid() || !current.isValid()) {
			return;
		}

		if (compare(current.start, anchor.start) < 0) {
			setNormalizedSelection(current.start, anchor.end);
		} else {
			setNormalizedSelection(anchor.start, current.end);
		}
		fHasSelection = true;
		// clear hover underline
		fCellCanvasModel.expandHoverSelectionAt(-1, -1);
	}

	private void updateLineSelection(Point curr) {
		if (fDraggingStart == null || curr == null) {
			return;
		}
		ITerminalTextDataReadOnly term = fCellCanvasModel.getTerminalText();
		int startRow = Math.max(0, Math.min(fDraggingStart.y, curr.y));
		int endRow = Math.max(0, Math.max(fDraggingStart.y, curr.y));
		int endCol = Math.max(0, term.getWidth() - 1);
		setNormalizedSelection(new Point(0, startRow), new Point(endCol, endRow));
		fCellCanvasModel.setSelectionAnchor(new Point(0, fDraggingStart.y));
		fHasSelection = true;
	}

	/**
	 * The user has to drag the mouse to at least one character to make a selection.
	 * Once this is done, even a one char selection is OK.
	 *
	 * @param e
	 */
	private void updateHasSelection(MouseEvent e) {
		if (fDraggingStart != null) {
			Point p = screenPointToCell(e.x, e.y);
			if (fDraggingStart.x != p.x || fDraggingStart.y != p.y) {
				fHasSelection = true;
			}
		}
	}

	void setSelection(Point p) {
		if (fDraggingStart != null && !p.equals(fDraggingEnd)) {
			fDraggingEnd = p;
			if (compare(p, fDraggingStart) < 0) {
				// bug 219589 - make sure selection start coordinates are non-negative
				int startColumn = Math.max(0, p.x);
				int startRow = Math.max(p.y, 0);
				fCellCanvasModel.setSelection(startRow, fDraggingStart.y, startColumn, fDraggingStart.x);
			} else {
				fCellCanvasModel.setSelection(fDraggingStart.y, p.y, fDraggingStart.x, p.x);

			}
		}
	}

	int compare(Point p1, Point p2) {
		if (p1.equals(p2)) {
			return 0;
		}
		if (p1.y == p2.y) {
			if (p1.x > p2.x) {
				return 1;
			} else {
				return -1;
			}
		}
		if (p1.y > p2.y) {
			return 1;
		} else {
			return -1;
		}
	}

	public ILinelRenderer getCellRenderer() {
		return fCellRenderer;
	}

	public int getMinColumns() {
		return fMinColumns;
	}

	public void setMinColumns(int minColumns) {
		fMinColumns = minColumns;
	}

	public int getMinLines() {
		return fMinLines;
	}

	public void setMinLines(int minLines) {
		fMinLines = minLines;
	}

	protected void onResize(boolean init) {
		if (fResizeListener != null) {
			Rectangle bonds = getClientArea();
			int cellHeight = getCellHeight();
			int cellWidth = getCellWidth();
			int lines = bonds.height / cellHeight;
			int columns = bonds.width / cellWidth;
			// when the view is minimised, its size is set to 0
			// we don't sent this to the terminal!
			if ((lines > 0 && columns > 0) || init) {
				if (columns < fMinColumns) {
					if (!isHorizontalBarVisble()) {
						setHorizontalBarVisible(true);
						bonds = getClientArea();
						lines = bonds.height / cellHeight;
					}
					columns = fMinColumns;
				} else if (columns >= fMinColumns && isHorizontalBarVisble()) {
					setHorizontalBarVisible(false);
					bonds = getClientArea();
					lines = bonds.height / cellHeight;
					columns = bonds.width / cellWidth;
				}
				if (lines < fMinLines) {
					lines = fMinLines;
				}
				fResizeListener.sizeChanged(lines, columns);
			}
		}
		super.onResize();
		calculateGrid();
	}

	@Override
	protected void onResize() {
		fResizing = true;
		try {
			onResize(false);
		} finally {
			fResizing = false;
		}
	}

	private void calculateGrid() {
		Rectangle virtualBounds = getVirtualBounds();
		setRedraw(false);
		try {
			setVirtualExtend(getCols() * getCellWidth(), getRows() * getCellHeight());
			getParent().layout();
			if (fResizing) {
				// scroll to end if view port was near last line
				Rectangle viewRect = getViewRectangle();
				if (virtualBounds.height - (viewRect.y + viewRect.height) < getCellHeight() * 2) {
					scrollToEnd();
				}
			}
		} finally {
			setRedraw(true);
		}
		// NO_REDRAW_RESIZE paints only what a resize uncovers. When the grid gets
		// narrower, what was drawn in the columns now past its edge stays there.
		if (getVirtualBounds().width < virtualBounds.width) {
			redraw();
		}
	}

	void scrollToEnd() {
		if (!fScrollLock) {
			int y = -(getRows() * getCellHeight() - getClientArea().height);
			if (y > 0) {
				y = 0;
			}
			Rectangle v = getViewRectangle();
			if (v.y != -y) {
				setVirtualOrigin(v.x, y);
			}
			// make sure the scroll area is correct:
			scrollY(getVerticalBar());
			scrollX(getHorizontalBar());
		}
	}

	/**
	 *
	 * @return true if the cursor should be shown on output....
	 */
	public boolean isScrollLock() {
		return fScrollLock;
	}

	/**
	 * If set then if the size changes
	 */
	public void setScrollLock(boolean scrollLock) {
		fScrollLock = scrollLock;
	}

	protected void repaintRange(int col, int line, int width, int height) {
		Point origin = cellToOriginOnScreen(col, line);
		Rectangle r = new Rectangle(origin.x, origin.y, width * getCellWidth(), height * getCellHeight());
		repaint(r);
	}

	/**
	 * Hands the wheel to whoever can pass it on to the program being run.
	 */
	public interface IMouseWheelHandler {
		/**
		 * @param count lines the wheel asked for, positive when scrolling up
		 * @param cell  the cell of the text under the pointer, history lines included
		 * @return whether the program took it
		 */
		boolean mouseWheel(int count, int modifiers, Point cell);
	}

	/**
	 * Hands a button or the pointer moving to whoever can pass it on to the program
	 * being run, with the cell of the text under the pointer, history lines included.
	 */
	public interface IMouseButtonHandler {
		/** @return whether the program took it */
		boolean mouseButton(int button, int modifiers, Point cell, boolean pressed);

		/** @return whether the program took it */
		boolean mouseMoved(int button, int modifiers, Point cell);
	}

	public void setMouseButtonHandler(IMouseButtonHandler handler) {
		fButtonHandler = handler;
	}

	/**
	 * Holding shift keeps the event for the terminal, which is how a selection is
	 * still made with the mouse while a program is listening for it. Asked of a
	 * press and of the pointer moving; a release is not its own decision but goes
	 * wherever the press it ends went, however the keyboard stands by then.
	 */
	private boolean wouldReport(int stateMask) {
		return fButtonHandler != null && (stateMask & SWT.SHIFT) == 0 && getCellWidth() > 0 && getCellHeight() > 0;
	}

	private boolean report(Event e, int button, boolean pressed) {
		return fButtonHandler.mouseButton(button, modifiers(e.stateMask), cell(e.x, e.y), pressed);
	}

	/** the cell of the text under the point: its line counts the history above the screen too */
	private Point cell(int x, int y) {
		return screenPointToCell(Math.max(0, x), Math.max(0, y));
	}

	/**
	 * What xterm adds to a button for the keys held with it: 8 for alt, 16 for
	 * control. Shift is never among them, being what keeps the mouse for the
	 * terminal.
	 */
	static int modifiers(int stateMask) {
		return ((stateMask & SWT.ALT) != 0 ? 8 : 0) | ((stateMask & SWT.CTRL) != 0 ? 16 : 0);
	}

	private static int heldButton(int stateMask) {
		return (stateMask & SWT.BUTTON1) != 0 ? 1 : (stateMask & SWT.BUTTON2) != 0 ? 2
				: (stateMask & SWT.BUTTON3) != 0 ? 3 : 0;
	}

	public void setMouseWheelHandler(IMouseWheelHandler handler) {
		fWheelHandler = handler;
	}

	@Override
	protected boolean handleMouseWheel(int x, int y, int count, int stateMask) {
		// shift keeps the wheel for the terminal's own scrolling, as it keeps the buttons
		if (fWheelHandler == null || (stateMask & SWT.SHIFT) != 0 || getCellWidth() <= 0 || getCellHeight() <= 0) {
			return false;
		}
		return fWheelHandler.mouseWheel(count, modifiers(stateMask), cell(x, y));
	}

	@Override
	protected void drawLine(GC gc, int line, int x, int y, int colFirst, int colLast) {
		// A wide character spans two cells and has to be drawn from the first of
		// them. When the damaged area starts on the second one, widen the range so
		// the glyph is drawn from its own origin. Clipping then keeps only the half
		// that was actually damaged, which is the half that needed repainting.
		if (isFillerCell(line, colFirst)) {
			colFirst--;
			x -= getCellWidth();
		}
		// Same at the other end: a wide character starting in the last cell of the
		// range would be cut in half by the edge of the range.
		if (isFillerCell(line, colLast)) {
			colLast++;
		}
		fCellRenderer.drawLine(fCellCanvasModel, gc, line, x, y, colFirst, colLast);
	}

	/**
	 * @return whether the cell holds the second half of a wide character
	 */
	private boolean isFillerCell(int line, int col) {
		ITerminalTextDataReadOnly text = fCellCanvasModel.getTerminalText();
		if (col <= 0 || col >= text.getWidth() || line < 0 || line >= text.getHeight()) {
			return false;
		}
		char c = text.getChar(line, col);
		char before = text.getChar(line, col - 1);
		// the filler of a wide character of the BMP, or the low half of a surrogate
		// pair, whose high half sits in the cell before
		if (Character.isLowSurrogate(c)) {
			return Character.isHighSurrogate(before);
		}
		return c == '\000' && !Character.isSurrogate(before) && CharWidth.of(before) == 2;
	}

	@Override
	protected Color getTerminalBackgroundColor(Device device) {
		return fCellRenderer.getDefaultBackgroundColor();
	}

	@Override
	protected void visibleCellRectangleChanged(int x, int y, int width, int height) {
		fCellCanvasModel.setVisibleRectangle(y, x, height, width);
		update();
	}

	@Override
	protected int getCols() {
		return fCellCanvasModel.getTerminalText().getWidth();
	}

	@Override
	protected int getRows() {
		return fCellCanvasModel.getTerminalText().getHeight();
	}

	public String getSelectionText() {
		// TODO -- create a hasSelectionMethod!
		return fCellCanvasModel.getSelectedText();
	}

	public void copy() {
		String selectionText = getSelectionText();
		if (selectionText != null && selectionText.length() > 0) {
			Clipboard clipboard = new Clipboard(getDisplay());
			clipboard.setContents(new Object[] { selectionText }, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		}
	}

	public void selectAll() {
		fCellCanvasModel.setSelection(0, fCellCanvasModel.getTerminalText().getHeight(), 0,
				fCellCanvasModel.getTerminalText().getWidth());
		fCellCanvasModel.setSelectionAnchor(new Point(0, 0));
	}

	/**
	 * @since 4.1
	 */
	public void clearSelection() {
		fCellCanvasModel.setSelection(-1, -1, -1, -1);
	}

	/**
	 * Collect and return all text present in the widget.
	 *
	 * <p>Individual lines of the returned text are separated by '\n'.
	 *
	 * <p>The method is primarily designed for test automation. Tests need
	 * to check what happens in a terminal (e.g. if and how a CDT Debugger
	 * Console reacts in a GDB session) and this method allows to read the
	 * text present in the terminal.
	 *
	 * @since 4.4
	 */
	public String getAllText() {
		return fCellCanvasModel.getAllText();
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * Gets notified when the visible size of the terminal changes.
	 * This should update the model!
	 *
	 */
	public interface ResizeListener {
		void sizeChanged(int lines, int columns);
	}

	/**
	 * @param listener this listener gets notified, when the size of
	 * the widget changed. It should change the dimensions of the underlying
	 * terminaldata
	 */
	public void addResizeHandler(ResizeListener listener) {
		if (fResizeListener != null) {
			throw new IllegalArgumentException("There can be at most one listener at the moment!"); //$NON-NLS-1$
		}
		fResizeListener = listener;

		// Bug 281328: [terminal] The very first few characters might be missing in
		//             the terminal control if opened and connected programmatically
		//
		// In case the terminal had not been visible yet or is too small (less than one
		// line visible), the terminal should have a minimum size to avoid RuntimeExceptions.
		Rectangle bonds = getClientArea();
		if (bonds.height < getCellHeight() || bonds.width < getCellWidth()) {
			//Widget not realized yet, or minimized to < 1 item:
			//Just tell the listener our min size
			fResizeListener.sizeChanged(getMinLines(), getMinColumns());
		} else {
			//Widget realized: compute actual size and force telling the listener
			onResize(true);
		}
	}

	public void updateFont(String fontName) {
		fCellRenderer.updateFont(fontName);
		setCellWidth(fCellRenderer.getCellWidth());
		setCellHeight(fCellRenderer.getCellHeight());
		calculateGrid();
	}

	public void updateColors(Map<TerminalColor, Color> map) {
		fCellRenderer.updateColors(map);
		redraw();
	}

	public void setInvertedColors(boolean invert) {
		fCellRenderer.setInvertedColors(invert);
		redraw();
	}

	public boolean isInvertedColors() {
		return fCellRenderer.isInvertedColors();
	}

	/**
	 * @return true if the cursor is enabled (blinking). By default the cursor is not enabled.
	 */
	public boolean isCursorEnabled() {
		return fCursorEnabled;
	}

	/**
	 * @param enabled enabling means that the cursor blinks
	 */
	public void setCursorEnabled(boolean enabled) {
		if (enabled != fCursorEnabled) {
			fCursorEnabled = enabled;
			fCellCanvasModel.setCursorEnabled(fCursorEnabled);
		}

	}

	public void addTerminalMouseListener(final ITerminalMouseListener listener) {
		fMouseListeners.add(listener);
	}

	public void removeTerminalMouseListener(ITerminalMouseListener listener) {
		fMouseListeners.remove(listener);
	}

	public String getHoverSelection() {
		return fCellCanvasModel.getHoverSelectionText();
	}
}
