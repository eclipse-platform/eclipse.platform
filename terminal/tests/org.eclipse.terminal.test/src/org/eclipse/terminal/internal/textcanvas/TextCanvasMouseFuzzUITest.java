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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.internal.model.TerminalTextData;
import org.eclipse.terminal.model.ITerminalTextDataSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Presses, releases, moves and double-clicks in random order, with and without
 * shift, while a program that takes some presses and not others listens: whatever
 * the order, a gesture stays on one side. The program gets a release for every
 * press it got and for no other, and a drag only of a button whose press it got.
 */
public class TextCanvasMouseFuzzUITest {

	private static final int[] BUTTON_MASKS = { 0, SWT.BUTTON1, SWT.BUTTON2, SWT.BUTTON3 };

	private Display display;
	private Shell shell;
	private TextCanvas canvas;
	private final Random random = new Random(2902);
	// the buttons whose press the program got and whose release it has not
	private int programHolds;
	private boolean shift;

	@BeforeEach
	public void createCanvas() {
		display = Display.getCurrent() != null ? null : new Display();
		shell = new Shell();
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(24, 80);
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(false);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		canvas = new TextCanvas(shell, model, SWT.NONE, new TextLineRenderer(() -> canvas, model));
		canvas.setSize(400, 300);
		canvas.setMouseButtonHandler(new TextCanvas.IMouseButtonHandler() {
			@Override
			public boolean mouseButton(int button, int modifiers, Point cell, boolean pressed) {
				int bit = 1 << button;
				if (pressed) {
					assertFalse(shift, "a press with shift is the terminal's");
					assertEquals(0, programHolds & bit, "a second press of a button the program holds");
					// a program that does not listen at the moment declines it
					if (random.nextInt(5) == 0) {
						return false;
					}
					programHolds |= bit;
				} else {
					assertTrue((programHolds & bit) != 0, "a release of a press the program never got");
					programHolds &= ~bit;
				}
				return true;
			}

			@Override
			public boolean mouseMoved(int button, int modifiers, Point cell) {
				if (button != 0) {
					assertTrue((programHolds & 1 << button) != 0, "a drag of a button whose press was the terminal's");
				}
				return true;
			}
		});
	}

	@AfterEach
	public void dispose() {
		shell.dispose();
		if (display != null) {
			display.dispose();
		}
	}

	@Test
	public void aGestureStaysOnOneSide() {
		int down = 0; // the buttons physically down
		for (int step = 0; step < 20000; step++) {
			shift = random.nextInt(4) == 0;
			int button = 1 + random.nextInt(3);
			int bit = 1 << button;
			switch (random.nextInt(4)) {
			case 0:
				if ((down & bit) == 0) {
					send(SWT.MouseDown, button, down);
					down |= bit;
					if (random.nextBoolean()) {
						send(SWT.MouseDoubleClick, button, down);
					}
				}
				break;
			case 1:
				if ((down & bit) != 0) {
					send(SWT.MouseUp, button, down);
					down &= ~bit;
				}
				break;
			default:
				send(SWT.MouseMove, 0, down);
			}
		}
		shift = false;
		for (int button = 1; button <= 3; button++) {
			if ((down & 1 << button) != 0) {
				send(SWT.MouseUp, button, down);
				down &= ~(1 << button);
			}
		}
		assertEquals(0, programHolds, "presses the program never got the release of");
	}

	private void send(int type, int button, int down) {
		Event event = new Event();
		event.button = button;
		int state = shift ? SWT.SHIFT : 0;
		for (int b = 1; b <= 3; b++) {
			if ((down & 1 << b) != 0) {
				state |= BUTTON_MASKS[b];
			}
		}
		event.stateMask = state;
		event.x = random.nextInt(400);
		event.y = random.nextInt(300);
		canvas.notifyListeners(type, event);
	}
}
