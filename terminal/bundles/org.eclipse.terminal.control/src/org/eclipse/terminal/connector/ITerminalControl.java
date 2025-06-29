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
 * Martin Oberhuber (Wind River) - [204796] Terminal should allow setting the encoding to use
 * Martin Oberhuber (Wind River) - [261486][api][cleanup] Mark @noimplement interfaces as @noextend
 * Anton Leherbauer (Wind River) - [433751] Add option to enable VT100 line wrapping mode
 *******************************************************************************/
package org.eclipse.terminal.connector;

import java.io.OutputStream;
import java.nio.charset.Charset;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.control.TerminalTitleRequestor;

/**
 * Represents the terminal view as seen by a terminal connection.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITerminalControl {

	/**
	 * @return the current state of the connection
	 */
	TerminalState getState();

	/**
	 * @param state
	 */
	void setState(TerminalState state);

	/**
	 * Setup the terminal control within the given parent composite.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	void setupTerminal(Composite parent);

	/**
	 * A shell to show dialogs.
	 * @return the shell in which the terminal is shown.
	 */
	Shell getShell();

	/**
	 * Set the charset that the Terminal uses to decode bytes from the
	 * Terminal-to-remote-Stream into Unicode Characters used in Java; or, to
	 * encode Characters typed by the user into bytes sent over the wire to the
	 * remote.
	 *
	 * By default, the local Platform Default charset is used. Also note that
	 * the encoding must not be applied in case the terminal stream is processed
	 * by some data transfer protocol which requires binary data.
	 *
	 * Validity of the charset set here is not checked. Since some encodings do
	 * not cover the entire range of Unicode characters, it can happen that a
	 * particular Unicode String typed in by the user can not be encoded into a
	 * byte Stream with the encoding specified. and UnsupportedEncodingException
	 * will be thrown in this case at the time the String is about to be
	 * processed.
	 *
	 * The concrete encoding to use can either be specified manually by a user,
	 * by means of a dialog, or a connector can try to obtain it automatically
	 * from the remote side e.g. by evaluating an environment variable such as
	 * LANG on UNIX systems.
	 *
	 * @param charset Charset to use, or <code>null</code> for platform's default charset.
	 *
	 * @since 5.3
	 */
	void setCharset(Charset charset);

	/**
	 * Return the current charset.
	 *
	 * @return the non-<code>null</code> current charset of the Terminal
	 * @since 5.3
	 */
	Charset getCharset();

	/**
	 * Show a text in the terminal. If puts newlines at the beginning and the
	 * end.
	 *
	 * @param text TODO: Michael Scharf: Is this really needed?
	 */
	void displayTextInTerminal(String text);

	/**
	 * @return a stream used to write to the terminal. Any bytes written to this
	 * stream appear in the terminal or are interpreted by the emulator as
	 * control sequences. The stream in the opposite direction, terminal
	 * to remote is in {@link ITerminalConnector#getTerminalToRemoteStream()}.
	 */
	OutputStream getRemoteToTerminalOutputStream();

	/**
	 * Set the title of the terminal view.
	 * @param title Termianl title.
	 * @param requestor Item that requests terminal title update.
	 * @since 5.5
	 */
	void setTerminalTitle(String title, TerminalTitleRequestor requestor);

	/**
	 * Show an error message during connect.
	 * @param msg
	 * TODO: Michael Scharf: Should be replaced by a better error notification mechanism!
	 */
	void setMsg(String msg);

	/**
	 * Sets if or if not the terminal view control should try to reconnect
	 * the terminal connection if the user hits ENTER in a closed terminal.
	 * <p>
	 * Reconnect on ENTER if terminal is closed is enabled by default.
	 *
	 * @param on <code>True</code> to enable the reconnect, <code>false</code> to disable it.
	 */
	void setConnectOnEnterIfClosed(boolean on);

	/**
	 * Returns if or if not the terminal view control should try to reconnect
	 * the terminal connection if the user hits ENTER in a closed terminal.
	 *
	 * @return <code>True</code> the reconnect is enabled, <code>false</code> if disabled.
	 */
	boolean isConnectOnEnterIfClosed();

	/**
	 * Enables VT100 line wrapping mode (default is off).
	 * This corresponds to the VT100 'eat_newline_glitch' terminal capability.
	 * If enabled, writing to the rightmost column does not cause
	 * an immediate wrap to the next line. Instead the line wrap occurs on the
	 * next output character.
	 *
	 * @param enable  whether to enable or disable VT100 line wrapping mode
	 */
	void setVT100LineWrapping(boolean enable);

	/**
	 * @return whether VT100 line wrapping mode is enabled
	 */
	boolean isVT100LineWrapping();
}
