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
 * Martin Oberhuber (Wind River) - [261486][api][cleanup] Mark @noimplement interfaces as @noextend
 * Uwe Stieber (Wind River) - [282996] [terminal][api] Add "hidden" attribute to terminal connector extension point
 *******************************************************************************/
package org.eclipse.terminal.connector;

import java.io.OutputStream;
import java.util.Optional;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.terminal.connector.provider.AbstractTerminalConnector;
import org.eclipse.terminal.control.ITerminalViewControl;

/**
 * A contributed connection type to manage a single connection.
 *
 * Implementations of this class are contributed through the
 * <code>org.eclipse.terminal.control.connectors</code> extension point. This
 * class gives access to the static markup of a terminal connector extension as
 * well as providing the lifecycle management for the dynamically loaded
 * {@link AbstractTerminalConnector} instance, which performs the actual
 * communications. This pattern allows for lazy initialization, bundle
 * activation and class loading of the actual {@link AbstractTerminalConnector}
 * instance.
 *
 * Clients can get terminal connector instances from the
 * {@link TerminalConnectorExtension} class, or from
 * {@link ITerminalViewControl#getTerminalConnector()} when running inside an
 * active terminal widget.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 *
 */
public interface ITerminalConnector extends IAdaptable {
	/**
	 * @return an ID of this connector. The id from the plugin.xml.
	 */
	String getId();

	/**
	 * @return <code>null</code> the name (as specified in the plugin.xml)
	 */
	String getName();

	/**
	 * @return <code>True</code> if the connector is not visible in user
	 *         selections.
	 */
	boolean isHidden();

	/**
	 * @return true if the {@link AbstractTerminalConnector} has been initialized.
	 * If there was an initialization error, {@link #getInitializationErrorMessage()}
	 * returns the error message.
	 */
	boolean isInitialized();

	/**
	 * This method initializes the connector if it is not initialized!
	 * If the connector was initialized successfully, <code>null</code> is
	 * returned. Otherwise an error message describing the problem is returned.
	 * @return <code>null</code> or a localized error message.
	 */
	String getInitializationErrorMessage();

	/**
	 * Connect using the current state of the settings.
	 * @param control Used to inform the UI about state changes and messages from the connection.
	 */
	void connect(ITerminalControl control);

	/**
	 * Disconnect if connected. Else do nothing.
	 */
	void disconnect();

	/**
	 * @return true if a local echo is needed.
	 */
	boolean isLocalEcho();

	/**
	 * Notify the remote site that the size of the terminal has changed.
	 * @param newWidth
	 * @param newHeight
	 */
	void setTerminalSize(int newWidth, int newHeight);

	/**
	 * @return the terminal to remote stream (bytes written to this stream will
	 * be sent to the remote site). For the stream in the other direction (remote to
	 * terminal see {@link ITerminalControl#getRemoteToTerminalOutputStream()}
	 */
	OutputStream getTerminalToRemoteStream();

	/**
	 * Load the state of this connection. Is typically called before
	 * {@link #connect(ITerminalControl)}.
	 *
	 * @param store a string based data store. Short keys like "foo" can be used to
	 * store the state of the connection.
	 */
	void load(ISettingsStore store);

	/**
	 * When the view or dialog containing the terminal is closed,
	 * the state of the connection is saved into the settings store <code>store</code>
	 * @param store
	 */
	void save(ISettingsStore store);

	/**
	 * Set or reset the settings store to the default values.
	 */
	void setDefaultSettings();

	/**
	 * @return A string that represents the settings of the connection. This representation
	 * may be shown in the status line of the terminal view.
	 */
	String getSettingsSummary();

	/**
	 * @return An optional with the absolute path if available of the current working dir, empty otherwise.
	 * @since 5.2
	 */
	default Optional<String> getWorkingDirectory() {
		return Optional.empty();
	}

}
