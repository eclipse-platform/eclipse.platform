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
package org.eclipse.terminal.internal.emulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.connector.provider.AbstractTerminalConnector;
import org.eclipse.terminal.control.ITerminalListener;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.eclipse.terminal.internal.connector.TerminalConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What reaches the program when text is pasted, and when a caller writes it with
 * pasteString() to run a command.
 */
public class TerminalPasteUITest {

	private static final String START = "\u001b[200~", END = "\u001b[201~"; //$NON-NLS-1$ //$NON-NLS-2$

	private Display display;
	private Shell shell;
	private VT100TerminalControl terminal;
	private final ByteArrayOutputStream sent = new ByteArrayOutputStream();
	private boolean localEcho;

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
		terminal = new VT100TerminalControl(listener, new Composite(shell, 0), new ITerminalConnector[0]);
	}

	@AfterEach
	public void dispose() {
		terminal.disposeTerminal();
		shell.dispose();
		if (display != null) {
			display.dispose();
		}
	}

	private void connect(boolean echo) {
		localEcho = echo;
		AbstractTerminalConnector connector = new AbstractTerminalConnector() {
			@Override
			public void connect(ITerminalControl control) {
				super.connect(control);
				control.setState(TerminalState.CONNECTED);
			}

			@Override
			public boolean isLocalEcho() {
				return localEcho;
			}

			@Override
			public OutputStream getTerminalToRemoteStream() {
				return sent;
			}

			@Override
			public String getSettingsSummary() {
				return ""; //$NON-NLS-1$
			}
		};
		ITerminalConnector wrapped = new TerminalConnector(() -> connector, "test", "test", false); //$NON-NLS-1$ //$NON-NLS-2$
		terminal.setConnector(wrapped);
		wrapped.connect(terminal);
		assertTrue(terminal.isConnected());
	}

	private String taken() {
		String text = sent.toString(StandardCharsets.UTF_8);
		sent.reset();
		return text;
	}

	@Test
	public void pasteSendsLineBreaksAsEnterAndBrackets() {
		connect(false);
		terminal.paste("a\r\nb\n"); //$NON-NLS-1$
		assertEquals("a\rb\r", taken()); //$NON-NLS-1$
		terminal.enableBracketedPaste(true);
		terminal.paste("a\nb"); //$NON-NLS-1$
		assertEquals(START + "a\rb" + END, taken()); //$NON-NLS-1$
	}

	@Test
	public void pasteStringWritesTheTextAsItIs() {
		// as CDT runs gdb commands, and the command input field sends its line: never
		// bracketed, so the shell runs the line instead of inserting it
		connect(false);
		terminal.enableBracketedPaste(true);
		terminal.pasteString("info threads\n"); //$NON-NLS-1$
		assertEquals("info threads\n", taken()); //$NON-NLS-1$
	}

	@Test
	public void pasteToALocalEchoConnectorLeavesTheLineBreaks() {
		// InputStreamMonitor converts them for such a connector, from the text as it was
		connect(true);
		terminal.paste("a\nb\n"); //$NON-NLS-1$
		assertEquals("a\nb\n", taken()); //$NON-NLS-1$
	}
}
