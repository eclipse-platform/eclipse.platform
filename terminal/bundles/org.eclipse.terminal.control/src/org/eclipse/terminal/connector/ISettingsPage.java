/*******************************************************************************
 * Copyright (c) 2006, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - fixed copyright headers and beautified
 *******************************************************************************/
package org.eclipse.terminal.connector;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface ISettingsPage {

	public interface Listener {

		/**
		 * Invoked by settings page controls to signal that the settings page
		 * changed and page container may update their state.
		 *
		 * @param control The control which triggered the event or <code>null</code>
		 */
		public void onSettingsPageChanged(Control control);
	}

	/**
	 * Create a page to be shown in a dialog or wizard to setup the connection.
	 * @param parent
	 */
	void createControl(Composite parent);

	/**
	 * Called before the page is shown. Loads the state from the {@link ITerminalConnector}.
	 */
	void loadSettings();

	/**
	 * Called when the OK button is pressed.
	 */
	void saveSettings();

	/**
	 * @return true if the
	 */
	boolean validateSettings();

	/**
	 * Adds the given listener.
	 * <p>
	 * Has not effect if the same listener is already registered.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public void addListener(Listener listener);

	/**
	 * Removes the given listener.
	 * <p>
	 * Has no effect if the same listener was not registered.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public void removeListener(Listener listener);
}
