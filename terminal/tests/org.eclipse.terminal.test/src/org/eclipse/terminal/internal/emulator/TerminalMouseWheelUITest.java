/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
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
 * The wheel as a program hears it: SWT's wheel events, however they split the
 * lines, make one report per notch of three lines.
 */
public class TerminalMouseWheelUITest {

	// the button of each report: where on the screen the cell is depends on the size the
	// shell happens to get, which differs between platforms
	private static final String UP = "\u001b[<64", DOWN = "\u001b[<65"; //$NON-NLS-1$ //$NON-NLS-2$

	private Display display;
	private Shell shell;
	private VT100TerminalControl terminal;
	private final ByteArrayOutputStream sent = new ByteArrayOutputStream();

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
		AbstractTerminalConnector connector = new AbstractTerminalConnector() {
			@Override
			public void connect(ITerminalControl control) {
				super.connect(control);
				control.setState(TerminalState.CONNECTED);
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
		terminal.enableMouseReporting(1000);
		terminal.enableSgrMouseEncoding(true);
	}

	@AfterEach
	public void dispose() {
		terminal.disposeTerminal();
		shell.dispose();
		if (display != null) {
			display.dispose();
		}
	}

	/** wheel events of these line counts, then what the program was sent */
	private String wheel(int... counts) {
		return wheelEvents(SWT.SCROLL_LINE, counts);
	}

	private String wheelEvents(int detail, int... counts) {
		Control canvas = terminal.getControl();
		for (int count : counts) {
			Event event = new Event();
			event.detail = detail;
			event.x = 1;
			event.y = 1;
			event.count = count;
			canvas.notifyListeners(SWT.MouseWheel, event);
		}
		String text = sent.toString(StandardCharsets.ISO_8859_1);
		sent.reset();
		return text.replaceAll(";\\d+;\\d+M", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void threeLinesMakeOneNotch() {
		assertEquals("", wheel(1, 1)); //$NON-NLS-1$
		assertEquals(UP, wheel(1));
		assertEquals(UP + UP, wheel(6));
		assertEquals(DOWN, wheel(-3));
	}

	@Test
	public void whatIsLeftOverCountsTowardTheNextNotch() {
		assertEquals(UP, wheel(4));
		assertEquals(UP, wheel(2));
		assertEquals("", wheel(2)); //$NON-NLS-1$
		assertEquals(UP, wheel(1));
	}

	@Test
	public void turningTheWheelBackForgetsTheLinesLeftOver() {
		assertEquals("", wheel(2, -1)); //$NON-NLS-1$
		assertEquals(DOWN, wheel(-2));
		// the two lines up were dropped at the turn: one more up is not a notch
		assertEquals("", wheel(1)); //$NON-NLS-1$
	}

	@Test
	public void aNewModeForgetsTheLinesLeftOver() {
		assertEquals("", wheel(2)); //$NON-NLS-1$
		terminal.enableMouseReporting(1000);
		assertEquals("", wheel(1)); //$NON-NLS-1$
		terminal.enableMouseReporting(0);
		terminal.enableMouseReporting(1002);
		assertEquals("", wheel(2)); //$NON-NLS-1$
		assertEquals(UP, wheel(1));
	}

	@Test
	public void aPageAtATimeCountsNotches() {
		// Windows set to scroll a page at a time gives the notches, not lines
		assertEquals(UP, wheelEvents(SWT.SCROLL_PAGE, 1));
		assertEquals(DOWN + DOWN, wheelEvents(SWT.SCROLL_PAGE, -2));
	}
}
