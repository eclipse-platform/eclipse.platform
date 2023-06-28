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
 *     Patrik Suzzi <psuzzi@gmail.com> - Bug 507661
 *******************************************************************************/
package org.eclipse.ui.console;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.eclipse.debug.internal.core.StreamDecoder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.WorkbenchEncoding;
import org.eclipse.ui.internal.console.IOConsolePartitioner;

/**
 * OutputStream used to write to an IOConsole.
 * <p>
 * Clients are not intended to instantiate this class directly, instead
 * use <code>IOConsole.newOutputStream()</code>.
 * </p>
 * <p>
 * Clients should avoid writing large amounts of output to this stream in the UI
 * thread. The console needs to process the output in the UI thread and if the client
 * hogs the UI thread writing output to the console, the console will not be able
 * to process the output.
 * </p>
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class IOConsoleOutputStream extends OutputStream {
	/**
	 * Flag indicating whether this stream has been closed.
	 */
	private volatile boolean closed;

	/**
	 * The console's document partitioner.
	 */
	private IOConsolePartitioner partitioner;

	/**
	 * The console this stream is attached to.
	 */
	private final IOConsole console;

	/**
	 * Flag indicating that the console should be activated when data
	 * is written to this stream.
	 */
	private volatile boolean activateOnWrite;

	/**
	 * The color used to decorate data written to this stream.
	 */
	private volatile Color color;

	/**
	 * The font style used to decorate data written to this stream.
	 */
	private volatile int fontStyle;

	/** synchronized access for atomic update **/
	private StreamDecoder decoder;

	/** synchronized access for atomic update **/
	private boolean prependCR;

	/**
	 * Constructs a new output stream on the given console.
	 *
	 * @param console I/O console
	 * @param charset the encoding used to write to console
	 */
	IOConsoleOutputStream(IOConsole console, Charset charset) {
		this.decoder = new StreamDecoder(charset);
		this.console = console;
		this.partitioner = (IOConsolePartitioner) console.getPartitioner();
	}

	/**
	 * Returns the font style used to decorate data written to this stream.
	 *
	 * @return the font style used to decorate data written to this stream
	 */
	public int getFontStyle() {
		return fontStyle;
	}

	/**
	 * Sets the font style to be used to decorate data written to this stream.
	 *
	 * @param newFontStyle the font style to be used to decorate data written to this stream
	 */
	public void setFontStyle(int newFontStyle) {
		if (newFontStyle != fontStyle) {
			int old = fontStyle;
			fontStyle = newFontStyle;
			console.firePropertyChange(this, IConsoleConstants.P_FONT_STYLE, Integer.valueOf(old), Integer.valueOf(fontStyle));
		}
	}

	/**
	 * Returns whether the console this stream is writing to will be activated when this stream
	 * is written to.
	 *
	 * @return whether the console this stream is writing to will be activated when this stream
	 * is written to.
	 */
	public boolean isActivateOnWrite() {
		return activateOnWrite;
	}

	/**
	 * Sets whether to activate the console this stream is writing to when this stream
	 * is written to.
	 *
	 * @param activateOnWrite whether the console this stream is writing to will be activated when this stream
	 * is written to.
	 */
	public void setActivateOnWrite(boolean activateOnWrite) {
		this.activateOnWrite = activateOnWrite;
	}

	/**
	 * Sets the color of this stream. Use <code>null</code> to indicate
	 * the default color.
	 *
	 * @param newColor color of this stream, or <code>null</code>
	 */
	public void setColor(Color newColor) {
		Color old = color;
		if (old == null || !old.equals(newColor)) {
			color = newColor;
			console.firePropertyChange(this, IConsoleConstants.P_STREAM_COLOR, old, newColor);
		}
	}

	/**
	 * Returns the color of this stream, or <code>null</code>
	 * if default.
	 *
	 * @return the color of this stream, or <code>null</code>
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Returns true if the stream has been closed
	 * @return true is the stream has been closed, false otherwise.
	 */
	public boolean isClosed() {
		return closed;
	}

	@Override
	public synchronized void close() throws IOException {
		if(closed) {
			// Closeable#close() has no effect if already closed
			return;
		}
		if (prependCR) { // force writing of last /r
			prependCR = false;
			notifyParitioner("\r"); //$NON-NLS-1$
		}
		String s = this.decoder.finish();
		if (s.length() > 0) {
			notifyParitioner(s);
		}
		console.streamClosed(this);
		closed = true;
		partitioner = null;
		decoder = null;
	}

	@Override
	public void flush() throws IOException {
		if(closed) {
			throw new IOException("Output Stream is closed"); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		if (closed) {
			throw new IOException("Output Stream is closed"); //$NON-NLS-1$
		}
		String s = this.decoder.decode(b, off, len);
		encodedWrite(s);
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] {(byte)b}, 0, 1);
	}

	/**
	 * Writes a character array to the attached console.
	 *
	 * @param buffer the char array to write to the attached console
	 * @throws IOException if the stream is closed
	 * @since 3.7
	 */
	public void write(char[] buffer) throws IOException {
		String str = new String(buffer);
		this.encodedWrite(str);
	}

	/**
	 * Writes a character array using specified offset and length to the
	 * attached console.
	 *
	 * @param buffer the char array to write to the attached console.
	 * @param off the initial offset
	 * @param len the length
	 * @throws IOException if the stream is closed
	 * @since 3.7
	 */
	public void write(char[] buffer, int off, int len) throws IOException {
		String str = new String(buffer, off, len);
		this.encodedWrite(str);
	}

	/**
	 * Writes a character sequence to the attached console.
	 *
	 * @param chars the string/characters to write to the attached console.
	 * @throws IOException if the stream is closed.
	 * @since 3.7
	 */
	public void write(CharSequence chars) throws IOException {
		String str = chars.toString();
		encodedWrite(str);
	}

	/**
	 * Writes a string to the attached console.
	 *
	 * @param str the string to write to the attached console
	 * @throws IOException if the stream is closed
	 */
	public void write(String str) throws IOException {
		encodedWrite(str);
	}

	private synchronized void encodedWrite(String encodedString) throws IOException {
		if(closed) {
			throw new IOException("Output Stream is closed"); //$NON-NLS-1$
		}
		String newencoding = encodedString;
		if (prependCR){
			newencoding = "\r" + newencoding; //$NON-NLS-1$
			prependCR=false;
		}
		if (newencoding.endsWith("\r")) { //$NON-NLS-1$
			prependCR = true;
			newencoding = newencoding.substring(0, newencoding.length() - 1);
		}
		notifyParitioner(newencoding);
	}

	private void notifyParitioner(String encodedString) throws IOException {
		try {
			partitioner.streamAppended(this, encodedString);

			if (activateOnWrite) {
				console.activate();
			} else {
				ConsolePlugin.getDefault().getConsoleManager().warnOfContentChange(console);
			}
		} catch (IOException e) {
			if (!closed) {
				close();
			}
			throw e;
		}
	}

	/**
	 * Sets the character encoding used to interpret characters written to this steam.
	 *
	 * @param encoding encoding identifier
	 */
	public void setEncoding(String encoding) {
		String charsetName;
		if (encoding == null) {
			charsetName = WorkbenchEncoding.getWorkbenchDefaultEncoding();
		} else {
			charsetName = encoding;
		}
		Charset charset = Charset.forName(charsetName);
		try {
			this.setCharset(charset);
		} catch (IOException ioe) {
			// ignore exception while writing final characters
			// to avoid API break
		}
	}

	/**
	 * @param charset set the Charset for the attached console
	 * @throws IOException if the stream is closed
	 * @since 3.7
	 */
	public synchronized void setCharset(Charset charset) throws IOException {
		if (closed) {
			throw new IOException("Output Stream is closed"); //$NON-NLS-1$
		}
		String s = this.decoder.finish();
		if (s.length() > 0) {
			this.encodedWrite(s);
		}
		this.decoder = new StreamDecoder(charset);
	}

}
