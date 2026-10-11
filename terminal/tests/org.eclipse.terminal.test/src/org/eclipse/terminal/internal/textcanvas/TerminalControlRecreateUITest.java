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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalListener;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.eclipse.terminal.internal.emulator.VT100TerminalControl;
import org.eclipse.terminal.internal.preferences.ITerminalConstants;
import org.eclipse.terminal.model.TerminalColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests recreating the terminal controls in a new parent, as moving a terminal tab to another
 * terminals view does.
 */
public class TerminalControlRecreateUITest {

	private static final RGB BACKGROUND = new RGB(1, 2, 3);

	private Display display;
	private Shell shell;
	private VT100TerminalControl terminal;

	@BeforeEach
	public void createTerminal() {
		display = Display.getCurrent() != null ? null : new Display();
		shell = new Shell();
		shell.setLayout(new FillLayout());
		PreferenceStore preferences = new PreferenceStore();
		preferences.setValue(ITerminalConstants.getPrefForTerminalColor(TerminalColor.BACKGROUND),
				"1,2,3"); //$NON-NLS-1$
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
		terminal = new VT100TerminalControl(listener, new Composite(shell, 0), new ITerminalConnector[0], preferences);
	}

	@AfterEach
	public void dispose() {
		// as the terminals view does when a terminal tab is closed
		terminal.disposeTerminal();
		shell.dispose();
		if (display != null) {
			display.dispose();
		}
	}

	private void recreateInNewParent() {
		terminal.setupTerminal(new Composite(shell, 0));
	}

	@Test
	public void recreatingDoesNotUseTheDisposedCanvas() {
		assertDoesNotThrow(this::recreateInNewParent);
	}

	@Test
	public void recreatedCanvasKeepsThePreferences() {
		assertEquals(BACKGROUND, ((TextCanvas) terminal.getControl()).getCellRenderer().getDefaultBackgroundColor().getRGB());
		recreateInNewParent();
		assertEquals(BACKGROUND, ((TextCanvas) terminal.getControl()).getCellRenderer().getDefaultBackgroundColor().getRGB());
	}
}
