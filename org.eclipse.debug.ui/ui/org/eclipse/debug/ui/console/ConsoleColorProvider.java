/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.ui.console;


import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.swt.graphics.Color;

/**
 * Default console color provider for a process. Colors output to standard
 * out, in, and error, as specified by user preferences.
 * <p>
 * Clients implementing a console color provider should subclass this class.
 * </p>
 * @since 2.1
 */
public class ConsoleColorProvider implements IConsoleColorProvider {

	private IProcess fProcess;
	private IConsole fConsole;

	@Override
	public void connect(IProcess process, IConsole console) {
		fProcess = process;
		fConsole = console;
		IStreamsProxy streamsProxy = fProcess.getStreamsProxy();
		if (streamsProxy != null) {
			fConsole.connect(streamsProxy);
		}
	}

	@Override
	public void disconnect() {
		fConsole = null;
		fProcess = null;
	}

	@Override
	public boolean isReadOnly() {
		return fProcess == null || fProcess.isTerminated();
	}

	@Override
	public Color getColor(String streamIdentifer) {
		if (IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM.equals(streamIdentifer)) {
			return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR);
		}
		if (IDebugUIConstants.ID_STANDARD_ERROR_STREAM.equals(streamIdentifer)) {
			return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR);
		}
		if (IDebugUIConstants.ID_STANDARD_INPUT_STREAM.equals(streamIdentifer)) {
			return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_IN_COLOR);
		}
		return null;
	}

	/**
	 * Returns the process this color provider is providing color for, or
	 * <code>null</code> if none.
	 *
	 * @return the process this color provider is providing color for, or
	 * <code>null</code> if none
	 */
	protected IProcess getProcess() {
		return fProcess;
	}

	/**
	 * Returns the console this color provider is connected to, or
	 * <code>null</code> if none.
	 *
	 * @return IConsole the console this color provider is connected to, or
	 * <code>null</code> if none
	 */
	protected IConsole getConsole() {
		return fConsole;
	}
}
