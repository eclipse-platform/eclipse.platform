/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.textcanvas;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalListener;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.eclipse.terminal.internal.emulator.VT100TerminalControl;
import org.eclipse.terminal.internal.model.TerminalTextData;
import org.eclipse.terminal.model.ITerminalTextDataSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A click a program listening to the mouse takes is its alone: the terminal's own
 * listeners (the middle-click paste, the copy on release, a double-click) and its
 * context menu do not get it too. Shift keeps the click for the terminal.
 */
public class TextCanvasMouseUITest {

	private Display display;
	private Shell shell;
	private VT100TerminalControl terminal;
	private TextCanvas canvas;
	private final List<String> reported = new ArrayList<>();
	private final List<Integer> seenByOthers = new ArrayList<>();

	@BeforeEach
	public void createTerminal() {
		display = Display.getCurrent() != null ? null : new Display();
		shell = new Shell();
		shell.setLayout(new FillLayout());
		ITerminalListener listener = new ITerminalListener() {
			@Override
			public void setState(TerminalState state) {
			}

			@Override
			public void setTerminalSelectionChanged() {
			}

			@Override
			public void setTerminalTitle(String title, TerminalTitleRequestor requestor) {
			}
		};
		terminal = new VT100TerminalControl(listener, new Composite(shell, 0), new ITerminalConnector[0],
				new PreferenceStore());
		canvas = (TextCanvas) terminal.getControl();
		// a program listening to the mouse
		canvas.setMouseHandler(new TextCanvas.IMouseHandler() {
			@Override
			public boolean button(int button, int modifiers, Point cell, boolean pressed) {
				reported.add(button + (pressed ? " down" : " up"));
				return true;
			}

			@Override
			public boolean moved(int button, int modifiers, Point cell) {
				return true;
			}

			@Override
			public boolean wheel(int lines, int modifiers, Point cell) {
				return false;
			}
		});
		// as the middle-click paste of the terminals view listens: added after the canvas's own
		for (int type : new int[] { SWT.MouseDown, SWT.MouseUp, SWT.MouseDoubleClick }) {
			canvas.addListener(type, e -> seenByOthers.add(e.type));
		}
	}

	@AfterEach
	public void dispose() {
		terminal.disposeTerminal();
		shell.dispose();
		if (display != null) {
			display.dispose();
		}
	}

	private Event send(int type, int button, int stateMask) {
		return send(type, button, stateMask, 1, 1);
	}

	private Event send(int type, int button, int stateMask, int x, int y) {
		Event event = new Event();
		event.button = button;
		event.stateMask = stateMask;
		event.x = x;
		event.y = y;
		event.detail = SWT.MENU_MOUSE;
		canvas.notifyListeners(type, event);
		return event;
	}

	@Test
	public void aClickTheProgramTakesGoesNoFurther() {
		for (int button : new int[] { 1, 2, 3 }) {
			send(SWT.MouseDown, button, 0);
			send(SWT.MouseDoubleClick, button, 0);
			send(SWT.MouseUp, button, 0);
		}
		assertEquals(List.of("1 down", "1 up", "2 down", "2 up", "3 down", "3 up"), reported);
		assertTrue(seenByOthers.isEmpty(), "seen by the terminal's own listeners: " + seenByOthers);
		// and the right-click opens no context menu, while the menu key still does
		assertFalse(send(SWT.MenuDetect, 3, 0).doit);
		Event key = new Event();
		key.detail = SWT.MENU_KEYBOARD;
		canvas.notifyListeners(SWT.MenuDetect, key);
		assertTrue(key.doit);
	}

	@Test
	public void shiftKeepsTheClickForTheTerminal() {
		send(SWT.MouseDown, 2, SWT.SHIFT);
		send(SWT.MouseUp, 2, SWT.SHIFT);
		assertTrue(reported.isEmpty());
		assertEquals(List.of(SWT.MouseDown, SWT.MouseUp), seenByOthers);
		assertTrue(send(SWT.MenuDetect, 3, SWT.SHIFT).doit);
	}

	@Test
	public void aGestureTheTerminalStartedStaysItsOwn() {
		// a shift-drag of the terminal's own, then the right button with shift let go
		send(SWT.MouseDown, 1, SWT.SHIFT);
		send(SWT.MouseDown, 3, SWT.BUTTON1);
		send(SWT.MouseUp, 3, SWT.BUTTON1 | SWT.BUTTON3);
		send(SWT.MouseUp, 1, SWT.BUTTON1);
		assertTrue(reported.isEmpty(), "reported: " + reported);
	}

	@Test
	public void theMenuFollowsTheRightPressOnly() {
		// the program took the right press; a shift-click while it is held is the terminal's
		send(SWT.MouseDown, 3, 0);
		send(SWT.MouseDown, 1, SWT.SHIFT | SWT.BUTTON3);
		send(SWT.MouseUp, 1, SWT.SHIFT | SWT.BUTTON1 | SWT.BUTTON3);
		send(SWT.MouseUp, 3, SWT.BUTTON3);
		assertEquals(List.of("3 down", "3 up"), reported);
		// Windows asks for the menu after the release: still not the terminal's to open
		assertFalse(send(SWT.MenuDetect, 3, 0).doit);
	}

	@Test
	public void aShiftDragStartsAtTheLastLeftClickTheProgramTook() {
		// a canvas of its own, whose model the test can ask for the selection
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(24, 80);
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(false);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		canvas = new TextCanvas(shell, model, SWT.NONE, new TextLineRenderer(() -> canvas, model));
		canvas.setSize(400, 300);
		canvas.setMouseHandler(new TextCanvas.IMouseHandler() {
			@Override
			public boolean button(int button, int modifiers, Point cell, boolean pressed) {
				return true;
			}

			@Override
			public boolean moved(int button, int modifiers, Point cell) {
				return true;
			}

			@Override
			public boolean wheel(int lines, int modifiers, Point cell) {
				return false;
			}
		});
		int w = canvas.getCellWidth(), h = canvas.getCellHeight();
		// the program took a left click in the first cell, and a right one further on
		send(SWT.MouseDown, 1, 0, 1, 1);
		send(SWT.MouseUp, 1, SWT.BUTTON1, 1, 1);
		send(SWT.MouseDown, 3, 0, 10 * w + 1, 1);
		send(SWT.MouseUp, 3, SWT.BUTTON3, 10 * w + 1, 1);
		// a shift-click on the third cell selects from the left click, not the right one
		send(SWT.MouseDown, 1, SWT.SHIFT, 2 * w + 1, 1);
		send(SWT.MouseUp, 1, SWT.SHIFT | SWT.BUTTON1, 2 * w + 1, 1);
		assertEquals(new Point(0, 0), model.getSelectionStart());
		canvas.clearSelection();
		// once the program stops listening (the control then tells the canvas), its last
		// click is no anchor any more
		send(SWT.MouseDown, 1, 0, 1, 1);
		send(SWT.MouseUp, 1, SWT.BUTTON1, 1, 1);
		canvas.forgetProgramClick();
		send(SWT.MouseDown, 1, SWT.SHIFT, 2 * w + 1, h + 1);
		send(SWT.MouseUp, 1, SWT.SHIFT | SWT.BUTTON1, 2 * w + 1, h + 1);
		assertEquals(null, model.getSelectionStart());
	}
}
