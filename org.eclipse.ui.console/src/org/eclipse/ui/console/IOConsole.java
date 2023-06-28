/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - Bug 214424 IOConsole(String, String, ImageDescriptor, String, boolean) constructor is missing api javadoc
 *******************************************************************************/

package org.eclipse.ui.console;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.WorkbenchEncoding;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.internal.console.IOConsolePartitioner;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A console that displays text from I/O streams. An I/O console can have multiple
 * output streams connected to it and provides one input stream connected to the
 * keyboard.
 * <p>
 * Clients may instantiate and subclass this class.
 * </p>
 * @since 3.1
 */
public class IOConsole extends TextConsole {
	/**
	 * The document partitioner
	 */
	private final IOConsolePartitioner partitioner;

	/**
	 * The stream from which user input may be read
	 */
	private InputStream inputStream;

	/**
	 * A collection of open streams connected to this console.
	 */
	private final List<Closeable> openStreams = Collections.synchronizedList(new ArrayList<>());

	/**
	 * The encoding used to for displaying console output.
	 */
	private final Charset charset;


	/**
	 * Constructs a console with the given name, type, image, and lifecycle, with the
	 * workbench's default encoding.
	 *
	 * @param name name to display for this console
	 * @param consoleType console type identifier or <code>null</code>
	 * @param imageDescriptor image to display for this console or <code>null</code>
	 * @param autoLifecycle whether lifecycle methods should be called automatically
	 *  when this console is added/removed from the console manager
	 */
	public IOConsole(String name, String consoleType, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		this(name, consoleType, imageDescriptor, (String)null, autoLifecycle);
	}

	/**
	 * Constructs a console with the given name, type, image, encoding and lifecycle.
	 *
	 * @param name name to display for this console
	 * @param consoleType console type identifier or <code>null</code>
	 * @param imageDescriptor image to display for this console or <code>null</code>
	 * @param encoding the encoding that should be used to render the text, or <code>null</code>
	 * 	if the system default encoding should be used
	 * @param autoLifecycle whether lifecycle methods should be called automatically
	 *  when this console is added/removed from the console manager
	 */
	public IOConsole(String name, String consoleType, ImageDescriptor imageDescriptor, String encoding, boolean autoLifecycle) {
		this(name, consoleType, imageDescriptor,
				encoding == null
				? Charset.forName(WorkbenchEncoding.getWorkbenchDefaultEncoding())
						: Charset.forName(encoding),
						autoLifecycle);
	}

	/**
	 * Constructs a console with the given name, type, image, encoding and
	 * lifecycle.
	 *
	 * @param name name to display for this console
	 * @param consoleType console type identifier or <code>null</code>
	 * @param imageDescriptor image to display for this console or
	 *            <code>null</code>
	 * @param charset the encoding that should be used to render the text, must
	 *            not be <code>null</code>
	 * @param autoLifecycle whether lifecycle methods should be called
	 *            automatically when this console is added/removed from the
	 *            console manager
	 * @since 3.7
	 */
	public IOConsole(String name, String consoleType, ImageDescriptor imageDescriptor, Charset charset, boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, autoLifecycle);
		this.charset = charset;
		synchronized (openStreams) {
			inputStream = new IOConsoleInputStream(this);
			openStreams.add(inputStream);
		}
		partitioner = new IOConsolePartitioner(this);
		final IDocument document = getDocument();
		if (document != null) {
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
	}

	/**
	 * Constructs a console with the given name, type, and image with the workbench's
	 * default encoding. Lifecycle methods will be called when this console is
	 * added/removed from the console manager.
	 *
	 * @param name name to display for this console
	 * @param consoleType console type identifier or <code>null</code>
	 * @param imageDescriptor image to display for this console or <code>null</code>
	 */
	public IOConsole(String name, String consoleType, ImageDescriptor imageDescriptor) {
		this(name, consoleType, imageDescriptor, true);
	}

	/**
	 * Constructs a console with the given name and image. Lifecycle methods
	 * will be called when this console is added/removed from the console manager.
	 * This console will have an unspecified (<code>null</code>) type.
	 *
	 * @param name name to display for this console
	 * @param imageDescriptor image to display for this console or <code>null</code>
	 */
	public IOConsole(String name, ImageDescriptor imageDescriptor) {
		this(name, null, imageDescriptor);
	}

	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		return new IOConsolePage(this, view);
	}

	/**
	 * Creates and returns a new output stream which may be used to write to this console.
	 * A console may be connected to more than one output stream at once. Clients are
	 * responsible for closing any output streams created on this console.
	 * <p>
	 * Clients should avoid writing large amounts of output to this stream in the UI
	 * thread. The console needs to process the output in the UI thread and if the client
	 * hogs the UI thread writing output to the console, the console will not be able
	 * to process the output.
	 * </p>
	 * @return a new output stream connected to this console
	 */
	public IOConsoleOutputStream newOutputStream() {
		IOConsoleOutputStream outputStream = new IOConsoleOutputStream(this, this.charset);
		addOpenStream(outputStream);
		return outputStream;
	}

	/**
	 * Returns the input stream connected to the keyboard.
	 * <p>
	 * Note: It returns the stream connected to keyboard. There is no guarantee to
	 * get the stream last set with {@link #setInputStream(InputStream)}. The return
	 * value might be <code>null</code> if the current input stream is not connected
	 * to the keyboard.
	 * </p>
	 *
	 * @return the input stream connected to the keyboard.
	 */
	public IOConsoleInputStream getInputStream() {
		if (inputStream instanceof IOConsoleInputStream) {
			return (IOConsoleInputStream) inputStream;
		}
		return null;
	}

	/**
	 * Sets the new input stream.
	 *
	 * @param inputStream the input stream
	 * @since 3.6
	 */
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	/**
	 * Returns this console's document partitioner.
	 *
	 * @return this console's document partitioner
	 */
	@Override
	protected IConsoleDocumentPartitioner getPartitioner() {
		return partitioner;
	}

	/**
	 * Returns the maximum number of characters that the console will display at
	 * once. This is analogous to the size of the text buffer this console
	 * maintains.
	 *
	 * @return the maximum number of characters that the console will display
	 */
	public int getHighWaterMark() {
		return partitioner.getHighWaterMark();
	}

	/**
	 * Returns the number of characters that will remain in this console
	 * when its high water mark is exceeded.
	 *
	 * @return the number of characters that will remain in this console
	 *  when its high water mark is exceeded
	 */
	public int getLowWaterMark() {
		return partitioner.getLowWaterMark();
	}

	/**
	 * Sets the text buffer size for this console. The high water mark indicates the
	 * maximum number of characters stored in the buffer. The low water mark
	 * indicates the number of characters remaining in the buffer when the high
	 * water mark is exceeded.
	 *
	 * @param low  the number of characters remaining in the buffer when the high
	 *             water mark is exceeded (if -1 the console does not limit output)
	 * @param high the maximum number of characters this console will cache in its
	 *             text buffer (if -1 the console does not limit output)
	 * @exception IllegalArgumentException if low &gt;= high &amp; low != -1
	 */
	public void setWaterMarks(int low, int high) {
		if (low >= 0) {
			if (low >= high) {
				throw new IllegalArgumentException("High water mark must be greater than low water mark"); //$NON-NLS-1$
			}
		}
		partitioner.setWaterMarks(low, high);
	}

	/**
	 * Check if all streams connected to this console are closed. If so,
	 * notify the partitioner that this console is finished.
	 */
	private void checkFinished() {
		if (openStreams.isEmpty()) {
			partitioner.streamsClosed();
		}
	}

	/**
	 * Notification that an output stream connected to this console has been closed.
	 *
	 * @param stream stream that closed
	 */
	void streamClosed(IOConsoleOutputStream stream) {
		synchronized (openStreams) {
			openStreams.remove(stream);
			checkFinished();
		}
	}

	/**
	 * Notification that the input stream connected to this console has been closed.
	 *
	 * @param stream stream that closed
	 */
	void streamClosed(IOConsoleInputStream stream) {
		synchronized (openStreams) {
			openStreams.remove(stream);
			checkFinished();
		}
	}

	@Override
	public void clearConsole() {
		partitioner.clearBuffer();
	}

	/**
	 * Disposes this console.
	 */
	@Override
	protected void dispose() {
		// Get lock on ourself before closing. Closing streams check for console finish.
		// Finish check need lock on (this) console. Since closing also lock on stream
		// this can deadlock with other threads (double) closing streams at same time
		// but already own a lock on console. (bug 551902)
		// Long story short. Before closing IOConsole...Stream get lock from associated
		// console to prevent deadlocks.
		synchronized (this) {
			// make a copy of the open streams and close them all
			// a copy is needed as close the streams results in a callback that
			// removes the streams from the openStreams collection (bug 152794)
			List<Closeable> list = new ArrayList<>(openStreams);
			for (Closeable closable : list) {
				try {
					closable.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
			inputStream = null;
		}

		final IDocument document = partitioner.getDocument();
		partitioner.disconnect();
		document.setDocumentPartitioner(null);

		super.dispose();
	}

	/**
	 * Returns the encoding for this console.
	 *
	 * @return the encoding set for this console
	 * @since 3.3
	 */
	public String getEncoding() {
		return this.charset.name();
	}

	/**
	 * Returns the Charset for this console.
	 *
	 * @return the Charset for this console
	 * @since 3.7
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * Check if console currently interprets ASCII control characters.
	 *
	 * @return <code>true</code> if console interprets ASCII control characters
	 * @since 3.9
	 */
	public boolean isHandleControlCharacters() {
		return partitioner.isHandleControlCharacters();
	}

	/**
	 * Enable or disable interpretation of ASCII control characters like backspace
	 * (<code>\b</code>).
	 *
	 * @param handleControlCharacters interpret control characters if
	 *                                <code>true</code>
	 * @since 3.9
	 */
	public void setHandleControlCharacters(boolean handleControlCharacters) {
		partitioner.setHandleControlCharacters(handleControlCharacters);
	}

	/**
	 * Check if carriage returns (<code>\r</code>) in console output are interpreted
	 * as control characters. They are also not interpreted if general control
	 * character handling is disabled.
	 *
	 * @return if <code>true</code> carriage returns are interpreted as control
	 *         characters.
	 * @see #isHandleControlCharacters()
	 * @since 3.9
	 */
	public boolean isCarriageReturnAsControlCharacter() {
		return partitioner.isCarriageReturnAsControlCharacter();
	}

	/**
	 * If control characters in console output are interpreted by this console
	 * carriage returns (<code>\r</code>) are either ignored (<code>false</code>)
	 * and usually handled as line break by connected console document or if
	 * <code>true</code> interpreted with there control character meaning.
	 * <p>
	 * Note: this option has no effect if control character interpretation is
	 * disabled in general.
	 * </p>
	 *
	 * @param carriageReturnAsControlCharacter set <code>false</code> to exclude
	 *                                         carriage return from control
	 *                                         character interpretation
	 * @see #setHandleControlCharacters(boolean)
	 * @since 3.9
	 */
	public void setCarriageReturnAsControlCharacter(boolean carriageReturnAsControlCharacter) {
		partitioner.setCarriageReturnAsControlCharacter(carriageReturnAsControlCharacter);
	}

	/**
	 * Registers a stream that will be managed by this console.
	 *
	 * @param stream The stream which will be closed on {@link #dispose()}.
	 */
	void addOpenStream(Closeable stream) {
		synchronized (openStreams) {
			openStreams.add(stream);
		}
	}
}
