/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.terminal.ui;

import java.io.OutputStream;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.Spawner;
import org.eclipse.terminal.internal.provisional.api.ISettingsStore;
import org.eclipse.terminal.internal.provisional.api.ITerminalConnector;
import org.eclipse.terminal.internal.provisional.api.ITerminalControl;
import org.eclipse.terminal.internal.provisional.api.TerminalState;

class ConsoleConnector implements ITerminalConnector {

	private final Spawner process;
	private ITerminalControl control;

	public ConsoleConnector(Spawner process) {
		this.process = process;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.terminal"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return "Eclipse Terminal Console"; //$NON-NLS-1$
	}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public String getInitializationErrorMessage() {
		return null;
	}

	@Override
	public void connect(ITerminalControl control) {
		this.control = control;
		control.setState(TerminalState.CONNECTED);

	}

	@Override
	public void disconnect() {
		control.setState(TerminalState.CLOSED);
	}

	@Override
	public boolean isLocalEcho() {
		return false;
	}

	@Override
	public void setTerminalSize(int newWidth, int newHeight) {
		PTY pty = process.pty();
		if (pty != null) {
			pty.setTerminalSize(newWidth, newHeight);
		}
	}

	@Override
	public OutputStream getTerminalToRemoteStream() {
		return process.getOutputStream();
	}

	@Override
	public void load(ISettingsStore store) {

	}

	@Override
	public void save(ISettingsStore store) {

	}

	@Override
	public void setDefaultSettings() {

	}

	@Override
	public String getSettingsSummary() {
		return ""; //$NON-NLS-1$
	}

}
