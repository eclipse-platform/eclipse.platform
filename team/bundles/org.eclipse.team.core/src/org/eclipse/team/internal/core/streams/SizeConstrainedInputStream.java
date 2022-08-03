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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Simulates a stream that represents only a portion of the underlying stream.
 * Will report EOF when this portion has been fully read and prevent further reads.
 * The underlying stream is not closed on close(), but the remaining unread input
 * may optionally be skip()'d.
 *
 * Supports resuming partially completed operations after an InterruptedIOException
 * if the underlying stream does.  Check the bytesTransferred field to determine how
 * much of the operation completed; conversely, at what point to resume.
 */
public class SizeConstrainedInputStream extends FilterInputStream {
	private boolean discardOnClose;
	private long bytesRemaining;

	/**
	 * Creates a size constrained input stream.
	 * @param in the underlying input stream, never actually closed by this filter
	 * @param size the maximum number of bytes of the underlying input stream that
	 *             can be read through this filter
	 * @param discardOnClose if true, discards remaining unread bytes on close()
	 */
	public SizeConstrainedInputStream(InputStream in, long size, boolean discardOnClose) {
		super(in);
		this.bytesRemaining = size;
		this.discardOnClose = discardOnClose;
	}

	/**
	 * Prevents further reading from the stream but does not close the underlying stream.
	 * If discardOnClose, skip()'s over any remaining unread bytes in the constrained region.
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public void close() throws IOException {
		try {
			if (discardOnClose) {
				while (bytesRemaining != 0 && skip(bytesRemaining) != 0);
			}
		} catch (OperationCanceledException e) {
			// The receiver is likely wrapping a PollingInputStream which could throw
			// an OperationCanceledException on a skip.
			// Since we're closing, just ignore the cancel and let the caller check the monitor
		} finally {
			bytesRemaining = 0;
		}
	}

	/**
	 * Wraps the underlying stream's method.
	 * Simulates an end-of-file condition if the end of the constrained region has been reached.
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int available() throws IOException {
		int amount = in.available();
		if (amount > bytesRemaining) amount = (int) bytesRemaining;
		return amount;
	}

	/**
	 * Wraps the underlying stream's method.
	 * Simulates an end-of-file condition if the end of the constrained region has been reached.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int read() throws IOException {
		if (bytesRemaining == 0) return -1;
		int b = in.read();
		if (b != -1) bytesRemaining -= 1;
		return b;
	}

	/**
	 * Wraps the underlying stream's method.
	 * Simulates an end-of-file condition if the end of the constrained region has been reached.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred may be non-zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (length > bytesRemaining) {
			if (bytesRemaining == 0) return -1;
			length = (int) bytesRemaining;
		}
		try {
			int count = in.read(buffer, offset, length);
			if (count != -1) bytesRemaining -= count;
			return count;
		} catch (InterruptedIOException e) {
			bytesRemaining -= e.bytesTransferred;
			throw e;
		}
	}

	/**
	 * Wraps the underlying stream's method.
	 * Simulates an end-of-file condition if the end of the constrained region has been reached.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred may be non-zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public long skip(long amount) throws IOException {
		if (amount > bytesRemaining) amount = bytesRemaining;
		try {
			long count = in.skip(amount);
			bytesRemaining -= count;
			return count;
		} catch (InterruptedIOException e) {
			bytesRemaining -= e.bytesTransferred;
			throw e;
		}
	}

	/**
	 * Mark is not supported by the wrapper even if the underlying stream does, returns false.
	 */
	@Override
	public boolean markSupported() {
		return false;
	}
}
