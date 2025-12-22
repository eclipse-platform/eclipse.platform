/*******************************************************************************
 * Copyright (c) 2011, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Max Weninger (Wind River) - [361363] [TERMINALS] Implement "Pin&Clone" for the "Terminals" view
 * Christoph Läubrich - extract to interface
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.launcher;

import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.ui.ITerminalsView;
import org.eclipse.terminal.view.ui.TerminalViewId;

public interface ITerminalConsoleViewManager {

	/**
	 * Returns the console view if available within the active workbench window page.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * </p>
	 *
	 * @param tvid The terminals console view id. To specify reuse of most recent terminal view use special value of
	 *        {@link ITerminalsConnectorConstants#LAST_ACTIVE_SECONDARY_ID} for its secondary part.
	 *
	 * @return an {@link Optional} describing the console view instance if available or an empty {@link Optional} otherwise.
	 */
	Optional<ITerminalsView> findConsoleView(TerminalViewId tvid);

	/**
	 * Show the terminals console view specified by the given id.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param tvid The terminals console view id.
	 * @throws CoreException if the requested console cannot be opened
	 * @return opened terminal console view part
	 */
	ITerminalsView showConsoleView(TerminalViewId tvid) throws CoreException;

	/**
	 * Opens the console with the given title and connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param tvid The terminals console view id. To specify reuse of most recent terminal view use special value of
	 *        {@link ITerminalsConnectorConstants#LAST_ACTIVE_SECONDARY_ID} for its secondary part.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param encoding The terminal encoding or <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 * @param flags The flags controlling how the console is opened or <code>null</code> to use defaults.
	 * @throws CoreException if the requested console cannot be opened
	 * @return opened terminal console widget
	 */
	Widget openConsole(TerminalViewId tvid, String title, String encoding, ITerminalConnector connector, Object data,
			Map<String, Boolean> flags) throws CoreException;

	/**
	 * Lookup a console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param tvid The terminals console view id.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 *
	 * @return An {@link Optional} describing the corresponding console tab item or an empty optional if not found.
	 */
	Optional<Widget> findConsole(TerminalViewId tvid, String title, ITerminalConnector connector, Object data);

	/**
	 * Lookup a console which is assigned with the given terminal control.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param control The terminal control. Must not be <code>null</code>.
	 * @return An {@link Optional} describing the corresponding console tab item or an empty optional if not found.
	 */
	Optional<Widget> findConsole(ITerminalControl control);

	/**
	 * Close the console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param tvid The terminals console view id.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 */
	void closeConsole(TerminalViewId tvid, String title, ITerminalConnector connector, Object data);

	/**
	 * Terminate (disconnect) the console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param tvid The terminals console view id.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 */
	void terminateConsole(TerminalViewId tvid, String title, ITerminalConnector connector, Object data);

}