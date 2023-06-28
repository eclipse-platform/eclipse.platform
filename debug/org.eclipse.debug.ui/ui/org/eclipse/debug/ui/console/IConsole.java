/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.IPatternMatchListener;

/**
 * A console that displays output and writes input to a process. Implementors of
 * <code>IConsoleColorProvider</code> should connect streams to a console
 * document when connected to.
 * @see org.eclipse.debug.ui.console.IConsoleColorProvider
 * @since 2.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IConsole {

	/**
	 * Connects this console to the given streams proxy. This associates the
	 * standard in, out, and error streams with the console. Keyboard input will
	 * be written to the given proxy.
	 *
	 * @param streamsProxy the proxy to connect this console to
	 */
	void connect(IStreamsProxy streamsProxy);

	/**
	 * Connects this console to the given stream monitor, uniquely identified by
	 * the given identifier. This allows for more than the standard (in, out,
	 * error) streams to be connected to the console.
	 *
	 * @param streamMonitor the monitor to connect this console to
	 * @param streamIdentifer the stream identifier to connect this console to
	 */
	void connect(IStreamMonitor streamMonitor, String streamIdentifer);

	/**
	 * Adds the given hyperlink to this console. The link will be notified when
	 * entered, exited, and activated.
	 * <p>
	 * If the link's region (offset/length) is within the console's document
	 * current bounds, it is added immediately. Otherwise, the link is added
	 * when the console's document grows to contain the link's region.
	 * </p>
	 * @param link the hyperlink to add
	 * @param offset the character offset within the console document where the
	 * text associated with the hyperlink begins
	 * @param length the length of the associated hyperlink text
	 * @deprecated replaced with addLink(IHyperlink link, int offset, int length)
	 */
	@Deprecated void addLink(IConsoleHyperlink link, int offset, int length);

	/**
	 * Adds the given hyperlink to this console. The link will be notified when
	 * entered, exited, and activated.
	 * <p>
	 * If the link's region (offset/length) is within the console's document
	 * current bounds, it is added immediately. Otherwise, the link is added
	 * when the console's document grows to contain the link's region.
	 * </p>
	 * @param link the hyperlink to add
	 * @param offset the character offset within the console document where the
	 * text associated with the hyperlink begins
	 * @param length the length of the associated hyperlink text
	 * @since 3.1
	 */
	void addLink(IHyperlink link, int offset, int length);

	/**
	 * Returns the region of text associated with the given hyperlink, or
	 * <code>null</code> if the given hyperlink is not contained in this
	 * console.
	 *
	 * @param link a console hyperlink
	 * @return region of text associated with the hyperlink, or <code>null</code>
	 * @deprecated replaced with getRegion(IHyperlink link) instead
	 */
	@Deprecated IRegion getRegion(IConsoleHyperlink link);

	/**
	 * Returns the region of text associated with the given hyperlink, or
	 * <code>null</code> if the given hyperlink is not contained in this
	 * console.
	 *
	 * @param link a console hyperlink
	 * @return region of text associated with the hyperlink, or <code>null</code>
	 * @since 3.1
	 */
	IRegion getRegion(IHyperlink link);

	/**
	 * Returns the document associated with this console.
	 *
	 * @return document
	 */
	IDocument getDocument();

	/**
	 * Returns the process associated with this console.
	 *
	 * @return the process associated with this console
	 */
	IProcess getProcess();

	/**
	 * Adds the given pattern match listener to this console. The listener will
	 * be connected and receive match notifications.
	 * @param matchListener the listener to add
	 * @since 3.1
	 */
	void addPatternMatchListener(IPatternMatchListener matchListener);

	/**
	 * Removes the given pattern match listener from this console. The listener will be
	 * disconnected and will no longer receive match notifications.
	 * @param matchListener the pattern match listener to remove.
	 * @since 3.1
	 */
	void removePatternMatchListener(IPatternMatchListener matchListener);

	/**
	 * Returns the stream associated with the specified stream identifier.
	 * @param streamIdentifier Uniquely identifies the required stream
	 * @return The stream or <code>null</code> if none found with matching streamIdentifier
	 * @since 3.1
	 */
	IOConsoleOutputStream getStream(String streamIdentifier);
}
