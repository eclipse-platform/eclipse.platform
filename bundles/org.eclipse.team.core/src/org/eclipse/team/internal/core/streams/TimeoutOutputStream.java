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
package org.eclipse.team.internal.core.streams;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.eclipse.team.internal.core.Messages;

/**
 * Wraps an output stream that blocks indefinitely to simulate timeouts on write(),
 * flush(), and close().  The resulting output stream is buffered and supports
 * retrying operations that failed due to an InterruptedIOException.
 *
 * Supports resuming partially completed operations after an InterruptedIOException
 * REGARDLESS of whether the underlying stream does unless the underlying stream itself
 * generates InterruptedIOExceptions in which case it must also support resuming.
 * Check the bytesTransferred field to determine how much of the operation completed;
 * conversely, at what point to resume.
 */
public class TimeoutOutputStream extends FilterOutputStream {
	// unsynchronized variables
	private final long writeTimeout; // write() timeout in millis
	private final long closeTimeout; // close() timeout in millis, or -1

	// requests for the thread (synchronized)
	private byte[] iobuffer; // circular buffer
	private int head = 0; // points to first unwritten byte
	private int length = 0; // number of remaining unwritten bytes
	private boolean closeRequested = false; // if true, close requested
	private boolean flushRequested = false; // if true, flush requested

	// responses from the thread (synchronized)
	private Thread thread;
	private boolean waitingForClose = false; // if true, the thread is waiting for close()
	private IOException ioe = null;

	/**
	 * Creates a timeout wrapper for an output stream.
	 * @param out the underlying input stream
	 * @param bufferSize the buffer size in bytes; should be large enough to mitigate
	 *        Thread synchronization and context switching overhead
	 * @param writeTimeout the number of milliseconds to block for a write() or flush() before
	 *        throwing an InterruptedIOException; 0 blocks indefinitely
	 * @param closeTimeout the number of milliseconds to block for a close() before throwing
	 *        an InterruptedIOException; 0 blocks indefinitely, -1 closes the stream in the background
	 */
	public TimeoutOutputStream(OutputStream out, int bufferSize, long writeTimeout, long closeTimeout) {
		super(new BufferedOutputStream(out, bufferSize));
		this.writeTimeout = writeTimeout;
		this.closeTimeout = closeTimeout;
		this.iobuffer = new byte[bufferSize];
		thread = new Thread((Runnable) this::runThread, "TimeoutOutputStream");//$NON-NLS-1$
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Wraps the underlying stream's method.
	 * It may be important to wait for a stream to actually be closed because it
	 * holds an implicit lock on a system resoure (such as a file) while it is
	 * open.  Closing a stream may take time if the underlying stream is still
	 * servicing a previous request.
	 * @throws InterruptedIOException if the timeout expired, bytesTransferred will
	 *         reflect the number of bytes flushed from the buffer
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public void close() throws IOException {
		Thread oldThread;
		synchronized (this) {
			if (thread == null) return;
			oldThread = thread;
			closeRequested = true;
			thread.interrupt();
			checkError();
		}
		if (closeTimeout == -1) return;
		try {
			oldThread.join(closeTimeout);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // we weren't expecting to be interrupted
		}
		synchronized (this) {
			checkError();
			if (thread != null) throw new InterruptedIOException();
		}
	}

	/**
	 * Writes a byte to the stream.
	 * @throws InterruptedIOException if the timeout expired and no data was sent,
	 *         bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public synchronized void write(int b) throws IOException {
		syncCommit(true);
		iobuffer[(head + length) % iobuffer.length] = (byte) b;
		length++;
		notify();
	}

	/**
	 * Writes multiple bytes to the stream.
	 * @throws InterruptedIOException if the timeout expired, bytesTransferred will
	 *         reflect the number of bytes sent
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public synchronized void write(byte[] buffer, int off, int len) throws IOException {
		int amount = 0;
		try {
			do {
				syncCommit(true);
				while (amount < len && length != iobuffer.length) {
					iobuffer[(head + length) % iobuffer.length] = buffer[off++];
					length++;
					amount++;
				}
			} while (amount < len);
		} catch (InterruptedIOException e) {
			e.bytesTransferred = amount;
			throw e;
		}
		notify();
	}

	/**
	 * Flushes the stream.
	 * @throws InterruptedIOException if the timeout expired, bytesTransferred will
	 *         reflect the number of bytes flushed from the buffer
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public synchronized void flush() throws IOException {
		int oldLength = length;
		flushRequested = true;
		try {
			syncCommit(false);
		} catch (InterruptedIOException e) {
			e.bytesTransferred = oldLength - length;
			throw e;
		}
		notify();
	}

	/**
	 * Waits for the buffer to drain if it is full.
	 * @param partial if true, waits until the buffer is partially empty, else drains it entirely
	 * @throws InterruptedIOException if the buffer could not be drained as requested
	 */
	private void syncCommit(boolean partial) throws IOException {
		checkError(); // check errors before allowing the addition of new bytes
		if (partial && length != iobuffer.length || length == 0) return;
		if (waitingForClose) throw new IOException(Messages.TimeoutOutputStream_cannotWriteToStream);
		notify();
		try {
			wait(writeTimeout);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // we weren't expecting to be interrupted
		}
		checkError(); // check errors before allowing the addition of new bytes
		if (partial && length != iobuffer.length || length == 0) return;
		throw new InterruptedIOException();
	}

	/**
	 * If an exception is pending, throws it.
	 */
	private void checkError() throws IOException {
		if (ioe != null) {
			IOException e = ioe;
			ioe = null;
			throw e;
		}
	}

	/**
	 * Runs the thread in the background.
	 */
	private void runThread() {
		try {
			writeUntilDone();
		} catch (IOException e) {
			synchronized (this) { ioe = e; }
		} finally {
			waitUntilClosed();
			try {
				out.close();
			} catch (IOException e) {
				synchronized (this) { ioe = e; }
			} finally {
				synchronized (this) {
					thread = null;
					notify();
				}
			}
		}
	}

	/**
	 * Waits until we have been requested to close the stream.
	 */
	private synchronized void waitUntilClosed() {
		waitingForClose = true;
		notify();
		while (! closeRequested) {
			try {
				wait();
			} catch (InterruptedException e) {
				closeRequested = true; // alternate quit signal
			}
		}
	}

	/**
	 * Writes bytes from the buffer until closed and buffer is empty
	 */
	private void writeUntilDone() throws IOException {
		int bytesUntilFlush = -1; // if > 0, then we will flush after that many bytes have been written
		for (;;) {
			int off, len;
			synchronized (this) {
				for (;;) {
					if (closeRequested && length == 0) return; // quit signal
					if (length != 0 || flushRequested) break;
					try {
						wait();
					} catch (InterruptedException e) {
						closeRequested = true; // alternate quit signal
					}
				}
				off = head;
				len = iobuffer.length - head;
				if (len > length) len = length;
				if (flushRequested && bytesUntilFlush < 0) {
					flushRequested = false;
					bytesUntilFlush = length;
				}
			}

			// If there are bytes to be written, write them
			if (len != 0) {
				// write out all remaining bytes from the buffer before flushing
				try {
					// the i/o operation might block without releasing the lock,
					// so we do this outside of the synchronized block
					out.write(iobuffer, off, len);
				} catch (InterruptedIOException e) {
					len = e.bytesTransferred;
				}
			}

			// If there was a pending flush, do it
			if (bytesUntilFlush >= 0) {
				bytesUntilFlush -= len;
				if (bytesUntilFlush <= 0) {
					// flush the buffer now
					try {
						out.flush();
					} catch (InterruptedIOException e) {
					}
					bytesUntilFlush = -1; // might have been 0
				}
			}

			// If bytes were written, update the circular buffer
			if (len != 0) {
				synchronized (this) {
					head = (head + len) % iobuffer.length;
					length -= len;
					notify();
				}
			}
		}
	}
}
