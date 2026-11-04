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
		canvas.setMouseButtonHandler(new TextCanvas.IMouseButtonHandler() {
			@Override
			public boolean mouseButton(int button, int modifiers, Point cell, boolean pressed) {
				reported.add(button + (pressed ? " down" : " up"));
				return true;
			}

			@Override
			public boolean mouseMoved(int button, int modifiers, Point cell) {
				return true;
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
		Event event = new Event();
		event.button = button;
		event.stateMask = stateMask;
		event.x = 1;
		event.y = 1;
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
}
