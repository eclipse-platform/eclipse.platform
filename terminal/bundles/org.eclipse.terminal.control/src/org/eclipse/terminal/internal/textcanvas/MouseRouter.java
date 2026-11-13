/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.textcanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;

/**
 * Whose a gesture of the mouse is: the program's, which asked for the mouse, or
 * the terminal's own (a selection, the paste, the context menu). A press decides
 * it, once, and what follows from that press (its drag, its release, its
 * double-click, its context menu) goes the same way, however the keyboard stands
 * by then. Everything the canvas needs to know of that is kept here, and only
 * here.
 * <p>
 * Shift keeps the mouse for the terminal, which is the convention every terminal
 * uses to still allow a selection while a program is listening. A press while a
 * button of the terminal's own gesture is held is the terminal's too: a gesture
 * is not split between the two.
 */
final class MouseRouter {

	private TextCanvas.IMouseHandler fProgram;
	/** the buttons whose press went to the program, as bits 1 &lt;&lt; button */
	private int fProgramButtons;
	/** whether the last press went to the program: its double-click is its too */
	private boolean fLastPressProgram;
	/** whether the last right press went to the program: its context menu is not opened */
	private boolean fRightPressProgram;
	/** the last left click the program took: where a shift-drag starts while nothing is selected */
	private Point fProgramClick;

	void setProgram(TextCanvas.IMouseHandler program) {
		fProgram = program;
	}

	/**
	 * @param cell the cell under the pointer, null while the canvas has no cells yet
	 * @return whether the program took the press, which the terminal then leaves alone
	 */
	boolean press(int button, int stateMask, Point cell) {
		int bit = 1 << button;
		boolean program = listens(stateMask, cell) && heldButton(stateMask, fProgramButtons) >= 0
				&& fProgram.button(button, modifiers(stateMask), cell, true);
		fLastPressProgram = program;
		if (button == 3) {
			fRightPressProgram = program;
		}
		if (!program) {
			// a release of this button lost earlier (a dialog took it) is not waited for
			fProgramButtons &= ~bit;
			return false;
		}
		fProgramButtons |= bit;
		if (button == 1) {
			fProgramClick = cell;
		}
		return true;
	}

	/** @return whether the release went to the program, as its press did */
	boolean release(int button, int stateMask, Point cell) {
		int bit = 1 << button;
		if ((fProgramButtons & bit) == 0) {
			return false;
		}
		fProgramButtons &= ~bit;
		fProgram.button(button, modifiers(stateMask), cell, false);
		return true;
	}

	/** @return whether the double-click is the program's, as the press before it was */
	boolean doubleClick() {
		return fLastPressProgram;
	}

	/** @return whether the context menu of a right-click stays shut, the program having taken it */
	boolean menuTaken() {
		return fRightPressProgram;
	}

	/**
	 * @param terminalDragging whether the terminal's own selection is being dragged
	 * @return whether the program took the move
	 */
	boolean move(int stateMask, Point cell, boolean terminalDragging) {
		int held = heldButton(stateMask, fProgramButtons);
		// a button held down is a drag of the program's only if its press was the program's
		return !terminalDragging && held >= 0 && listens(stateMask, cell)
				&& fProgram.moved(held, modifiers(stateMask), cell);
	}

	/** @return whether the program took the wheel, which then does not scroll the view */
	boolean wheel(int lines, int stateMask, Point cell) {
		return listens(stateMask, cell) && fProgram.wheel(lines, modifiers(stateMask), cell);
	}

	/**
	 * @param height the lines of the text now
	 * @return where a shift-drag starts while nothing is selected: the last left click
	 *         the program took, unless its line is gone; null for none
	 */
	Point clickAnchor(int height) {
		return fProgramClick != null && fProgramClick.y < height ? fProgramClick : null;
	}

	/**
	 * The last click the program took no longer counts: the program stopped or
	 * started listening (its line may not even be the same line after a switch of
	 * screens), the selection was cleared, or the terminal took a click of its own.
	 */
	void forgetClick() {
		fProgramClick = null;
	}

	private boolean listens(int stateMask, Point cell) {
		return fProgram != null && cell != null && (stateMask & SWT.SHIFT) == 0;
	}

	/**
	 * What xterm adds to a button for the keys held with it: 8 for alt, 16 for
	 * control. Shift is never among them, being what keeps the mouse for the
	 * terminal.
	 */
	static int modifiers(int stateMask) {
		return ((stateMask & SWT.ALT) != 0 ? 8 : 0) | ((stateMask & SWT.CTRL) != 0 ? 16 : 0);
	}

	/**
	 * @param program the buttons whose press went to the program, as bits 1 &lt;&lt; button
	 * @return the first button held whose press went to the program; 0 when none is
	 *         held, -1 when every button held is the terminal's
	 */
	static int heldButton(int stateMask, int program) {
		int[] masks = { SWT.BUTTON1, SWT.BUTTON2, SWT.BUTTON3 };
		boolean held = false;
		for (int button = 1; button <= 3; button++) {
			if ((stateMask & masks[button - 1]) != 0) {
				held = true;
				if ((program & 1 << button) != 0) {
					return button;
				}
			}
		}
		return held ? -1 : 0;
	}
}
