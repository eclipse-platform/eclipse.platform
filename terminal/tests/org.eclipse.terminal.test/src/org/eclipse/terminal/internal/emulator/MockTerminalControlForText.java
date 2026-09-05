/*******************************************************************************
 * Copyright (c) 2021 Kichwa Coders Canada Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.emulator;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.eclipse.terminal.internal.control.impl.ITerminalControlForText;

public class MockTerminalControlForText implements ITerminalControlForText {
	private List<String> allTitles = new ArrayList<>();

	@Override
	public TerminalState getState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setState(TerminalState state) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTerminalTitle(String title, TerminalTitleRequestor requestor) {
		if (requestor == TerminalTitleRequestor.ANSI) {
			allTitles.add(title);
		}
	}

	public List<String> getAllTitles() {
		return Collections.unmodifiableList(allTitles);
	}

	@Override
	public ITerminalConnector getTerminalConnector() {
		return null;
	}

	@Override
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException();

	}

	private boolean cursorShown = true;
	private boolean bracketedPaste;

	@Override
	public void showCursor(boolean show) {
		cursorShown = show;
	}

	public boolean isCursorShown() {
		return cursorShown;
	}

	@Override
	public void enableBracketedPaste(boolean enable) {
		bracketedPaste = enable;
	}

	public boolean isBracketedPaste() {
		return bracketedPaste;
	}

	private int mouseMode;
	private boolean sgrMouse;
	private boolean focusReporting;

	@Override
	public void enableMouseReporting(int mode) {
		mouseMode = mode;
	}

	public int getMouseMode() {
		return mouseMode;
	}

	@Override
	public void enableSgrMouseEncoding(boolean enable) {
		sgrMouse = enable;
	}

	public boolean isSgrMouseEncoding() {
		return sgrMouse;
	}

	@Override
	public void enableFocusReporting(boolean enable) {
		focusReporting = enable;
	}

	public boolean isFocusReporting() {
		return focusReporting;
	}

	@Override
	public void enableApplicationCursorKeys(boolean enable) {
		throw new UnsupportedOperationException();

	}

}
