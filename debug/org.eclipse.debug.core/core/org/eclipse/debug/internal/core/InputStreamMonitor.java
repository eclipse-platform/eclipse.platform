/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Paul Pazderski  - Bug 558463: add handling of raw stream content instead of strings
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Vector;

import org.eclipse.debug.core.DebugPlugin;

/**
 * Writes to the input stream of a system process, queuing output if the stream
 * is blocked.
 *
 * The input stream monitor writes to system in via an output stream.
 */
public class InputStreamMonitor {

	/**
	 * The stream which is being written to (connected to system in).
	 */
	private OutputStream fStream;

	/**
	 * The queue of output.
	 */
	private Vector<byte[]> fQueue;

	/**
	 * The thread which writes to the stream.
	 */
	private Thread fThread;

	/**
	 * A lock for ensuring that writes to the queue are contiguous
	 */
	private Object fLock;

	/**
	 * Whether the underlying output stream has been closed
	 */
	private boolean fClosed = false;

	/**
	 * The charset of the input stream.
	 */
	private Charset fCharset;

	/**
	 * Creates an input stream monitor which writes to system in via the given output stream.
	 *
	 * @param stream output stream
	 */
	public InputStreamMonitor(OutputStream stream) {
		this(stream, (Charset) null);
	}

	/**
	 * Creates an input stream monitor which writes to system in via the given
	 * output stream.
	 *
	 * @param stream output stream
	 * @param charset stream charset or <code>null</code> for system default
	 */
	public InputStreamMonitor(OutputStream stream, Charset charset) {
		fStream = stream;
		fQueue = new Vector<>();
		fLock = new Object();
		fCharset = charset;
	}

	/**
	 * Creates an input stream monitor which writes to system in via the given
	 * output stream.
	 *
	 * @param stream output stream
	 * @param encoding stream encoding or <code>null</code> for system default
	 * @deprecated use {@link #InputStreamMonitor(OutputStream, Charset)}
	 *             instead
	 */
	@Deprecated
	public InputStreamMonitor(OutputStream stream, String encoding) {
		this(stream, Charset.forName(encoding));
	}

	/**
	 * Appends the given text to the stream, or queues the text to be written at
	 * a later time if the stream is blocked.
	 *
	 * @param text text to append
	 */
	public void write(String text) {
		synchronized (fLock) {
			fQueue.add(fCharset == null ? text.getBytes() : text.getBytes(fCharset));
			fLock.notifyAll();
		}
	}

	/**
	 * Appends the given binary data to the stream, or queues the text to be
	 * written at a later time if the stream is blocked.
	 *
	 * @param data data to append; not <code>null</code>
	 * @param offset start offset in data
	 * @param length number of bytes in data
	 */
	public void write(byte[] data, int offset, int length) {
		synchronized (fLock) {
			byte[] copy = new byte[length];
			System.arraycopy(data, offset, copy, 0, length);
			fQueue.add(copy);
			fLock.notifyAll();
		}
	}

	public void startMonitoring() {
		startMonitoring("Input Stream Monitor"); //$NON-NLS-1$
	}

	/**
	 * Starts a thread which writes the stream.
	 *
	 * @param threadName Thread name
	 */
	public void startMonitoring(String threadName) {
		synchronized (this) {
			if (fThread == null) {
				fThread = new Thread((Runnable) this::write, threadName);
				fThread.setDaemon(true);
				fThread.start();
			}
		}
	}

	/**
	 * Close all communications between this
	 * monitor and the underlying stream.
	 */
	public void close() {
		Thread thread = null;
		synchronized (this) {
			thread = fThread;
			fThread = null;
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

	/**
	 * Continuously writes to the stream.
	 */
	protected void write() {
		while (fThread != null) {
			writeNext();
		}
		if (!fClosed) {
			try {
				fStream.close();
			} catch (IOException e) {
				DebugPlugin.log(e);
			}
		}
	}

	/**
	 * Write the text in the queue to the stream.
	 */
	protected void writeNext() {
		while (!fQueue.isEmpty() && !fClosed) {
			byte[] data = fQueue.firstElement();
			fQueue.removeElementAt(0);
			try {
				fStream.write(data);
				fStream.flush();
			} catch (IOException e) {
				DebugPlugin.log(e);
			}
		}
		try {
			synchronized(fLock) {
				// Queue could receive more input between last empty check and
				// lock acquire. See https://bugs.eclipse.org/550834
				if (fQueue.isEmpty()) {
					fLock.wait();
				}
			}
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Closes the output stream attached to the standard input stream of this
	 * monitor's process.
	 *
	 * @exception IOException if an exception occurs closing the input stream or
	 *                stream is already closed
	 */
	public void closeInputStream() throws IOException {
		if (!fClosed) {
			fClosed = true;
			fStream.close();
		} else {
			throw new IOException();
		}

	}
}

