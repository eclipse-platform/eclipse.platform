/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.ui.console;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A console that displays messages. A message console may have one or
 * more streams connected to it (<code>MessageConsoleStream</code>).
 * Text written to streams is buffered and processed in a Job by the
 * console's document partitioner.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MessageConsole extends IOConsole {

	/**
	 * Property constant indicating the font of this console has changed.
	 *
	 * @deprecated use {@link IConsoleConstants#P_FONT}
	 */
	@Deprecated
	public static final String P_FONT = IConsoleConstants.P_FONT;

	/**
	 * Property constant indicating the color of a stream has changed.
	 *
	 * @deprecated use {@link IConsoleConstants#P_STREAM_COLOR}
	 */
	@Deprecated
	public static final String P_STREAM_COLOR = IConsoleConstants.P_STREAM_COLOR;

	/**
	 * Property constant indicating tab size has changed
	 *
	 * @deprecated use {@link IConsoleConstants#P_TAB_SIZE}
	 */
	@Deprecated
	public static final String P_TAB_SIZE = IConsoleConstants.P_TAB_SIZE;

	/**
	 * The default tab size
	 *
	 * @deprecated use {@link IConsoleConstants#DEFAULT_TAB_SIZE}
	 */
	@Deprecated
	public static final int DEFAULT_TAB_SIZE = IConsoleConstants.DEFAULT_TAB_SIZE;

	/**
	 * Constructs a message console with the given name and image.
	 *
	 * @param name console name
	 * @param imageDescriptor console image descriptor or <code>null</code>
	 */
	public MessageConsole(String name, ImageDescriptor imageDescriptor) {
		this(name, imageDescriptor, true);
	}

	/**
	 * Constructs a message console.
	 *
	 * @param name console name
	 * @param imageDescriptor console image descriptor or <code>null</code>
	 * @param autoLifecycle whether lifecycle methods should be called automatically
	 *  when added and removed from the console manager
	 * @since 3.1
	 */
	public MessageConsole(String name, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		this(name, IConsoleConstants.MESSAGE_CONSOLE_TYPE, imageDescriptor, autoLifecycle);
	}

	/**
	 * Constructs a message console with the given name, type, image, and lifecycle.
	 *
	 * @param name console name
	 * @param consoleType console type identifier or <code>null</code>
	 * @param imageDescriptor console image descriptor or <code>null</code>
	 * @param autoLifecycle whether lifecycle methods should be called automatically
	 *  when added and removed from the console manager
	 *
	 * @since 3.4
	 */
	public MessageConsole(String name, String consoleType, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		this(name, consoleType, imageDescriptor, null, autoLifecycle);
	}

	/**
	 * Constructs a message console with the given name, type, image, encoding, and lifecycle specification.
	 *
	 * @param name the name to display for this console
	 * @param consoleType console type identifier or <code>null</code>
	 * @param imageDescriptor console image descriptor or <code>null</code>
	 * @param encoding the encoding that should be used to render the text, or <code>null</code>
	 * 	if the system default encoding should be used
	 * @param autoLifecycle whether lifecycle methods should be called automatically
	 *  when added and removed from the console manager
	 * @since 3.5
	 */
	public MessageConsole(String name, String consoleType, ImageDescriptor imageDescriptor, String encoding, boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, encoding, autoLifecycle);
	}

	/**
	 * Returns a new message stream connected to this console.
	 * <p>
	 * Clients should avoid writing large amounts of output to this stream in the UI
	 * thread. The console needs to process the output in the UI thread and if the client
	 * hogs the UI thread writing output to the console, the console will not be able
	 * to process the output.
	 * </p>
	 * @return a new message stream connected to this console
	 */
	public MessageConsoleStream newMessageStream() {
		MessageConsoleStream messageConsoleStream = new MessageConsoleStream(this, this.getCharset());
		addOpenStream(messageConsoleStream);
		return messageConsoleStream;
	}

	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		IOConsolePage page = (IOConsolePage) super.createPage(view);
		page.setReadOnly();
		return page;
	}

	@Override
	public IOConsoleInputStream getInputStream() {
		throw new UnsupportedOperationException("Message Console does not support user input"); //$NON-NLS-1$
	}


	/**
	 * Appends the given message to this console, from the specified stream.
	 *
	 * @param text message
	 * @param stream stream the message belongs to
	 * @deprecated since 3.1, this method should no longer be called, and has no effect.
	 * Writing to a message console stream updates the document
	 */
	@Deprecated
	protected void appendToDocument(String text, MessageConsoleStream stream) {
	}
}
