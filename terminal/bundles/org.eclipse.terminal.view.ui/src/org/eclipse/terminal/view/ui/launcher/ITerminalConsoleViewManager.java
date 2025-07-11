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
 *******************************************************************************/
package org.eclipse.terminal.view.ui.launcher;

import java.util.Map;
import java.util.Optional;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.ui.ITerminalsView;
import org.eclipse.ui.IViewPart;

public interface ITerminalConsoleViewManager {

	/**
	 * Returns the console view if available within the active workbench window page.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param id The terminals console view id or <code>null</code> to show the default terminals console view.
	 * @param secondaryId The terminal console secondary id, which may be <code>null</code> which is the secondary id of
	 *        the first terminal view opened. To specify reuse of most recent terminal view use special value of
	 *        {@link ITerminalsConnectorConstants#LAST_ACTIVE_SECONDARY_ID}.
	 *
	 * @return an {@link Optional} describing the console view instance if available or an empty {@link Optional} otherwise.
	 */
	Optional<ITerminalsView> findConsoleView(String id, String secondaryId);

	/**
	 * Return a new secondary id to use, based on the number of open terminal views.
	 *
	 * @param id The terminals console view id. Must not be <code>null</code>.
	 * @return The next secondary id, or <code>null</code> if it is the first one
	 * @since 4.1
	 */
	String getNextTerminalSecondaryId(String id);

	/**
	 * Show the terminals console view specified by the given id.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param id The terminals console view id or <code>null</code> to show the default terminals console view.
	 */
	IViewPart showConsoleView(String id, String secondaryId);

	/**
	 * Opens the console with the given title and connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param id The terminals console view id or <code>null</code> to show the default terminals console view.
	 * @param secondaryId The terminal console secondary id, which may be <code>null</code> which is the secondary id of
	 *        the first terminal view opened. To specify reuse of most recent terminal view use special value of
	 *        {@link ITerminalsConnectorConstants#LAST_ACTIVE_SECONDARY_ID}.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param encoding The terminal encoding or <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 * @param flags The flags controlling how the console is opened or <code>null</code> to use defaults.
	 */
	Widget openConsole(String id, String secondaryId, String title, String encoding, ITerminalConnector connector,
			Object data, Map<String, Boolean> flags);

	/**
	 * Lookup a console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param id The terminals console view id or <code>null</code> to show the default terminals console view.
	 * @param secondaryId The terminals console view secondary id or <code>null</code>.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 *
	 * @return An {@link Optional} describing the corresponding console tab item or <an empty optional if not found.
	 */
	Optional<Widget> findConsole(String id, String secondaryId, String title, ITerminalConnector connector,
			Object data);

	/**
	 * Lookup a console which is assigned with the given terminal control.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param control The terminal control. Must not be <code>null</code>.
	 * @return An {@link Optional} describing the corresponding console tab item or <an empty optional if not found.
	 */
	Optional<Widget> findConsole(ITerminalControl control);

	/**
	 * Close the console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 */
	void closeConsole(String id, String title, ITerminalConnector connector, Object data);

	/**
	 * Terminate (disconnect) the console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 */
	void terminateConsole(String id, String title, ITerminalConnector connector, Object data);

}