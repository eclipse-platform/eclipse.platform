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

/**
 * Converts CR/LFs in the underlying input stream to LF.
 *
 * Supports resuming partially completed operations after an InterruptedIOException
 * if the underlying stream does.  Check the bytesTransferred field to determine how
 * much of the operation completed; conversely, at what point to resume.
 */
public class CRLFtoLFInputStream extends FilterInputStream {
	private boolean pendingByte = false;
	private int lastByte = -1;

	/**
	 * Creates a new filtered input stream.
	 * @param in the underlying input stream
	 */
	public CRLFtoLFInputStream(InputStream in) {
		super(in);
	}

	/**
	 * Wraps the underlying stream's method.
	 * Translates CR/LF sequences to LFs transparently.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int read() throws IOException {
		if (! pendingByte) {
			lastByte = in.read(); // ok if this throws
			pendingByte = true; // remember the byte in case we throw an exception later on
		}
		if (lastByte == '\r') {
			lastByte = in.read(); // ok if this throws
			if (lastByte != '\n') {
				if (lastByte == -1) pendingByte = false;
				return '\r'; // leaves the byte pending for later
			}
		}
		pendingByte = false;
		return lastByte;
	}

	/**
	 * Wraps the underlying stream's method.
	 * Translates CR/LF sequences to LFs transparently.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred may be non-zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		// handle boundary cases cleanly
		if (len == 0) {
			return 0;
		} else if (len == 1) {
			int b = read();
			if (b == -1) return -1;
			buffer[off] = (byte) b;
			return 1;
		}
		// read some bytes from the stream
		// prefix with pending byte from last read if any
		int count = 0;
		if (pendingByte) {
			buffer[off] = (byte) lastByte;
			pendingByte = false;
			count = 1;
		}
		InterruptedIOException iioe = null;
		try {
			len = in.read(buffer, off + count, len - count);
			if (len == -1) {
				return (count == 0) ? -1 : count;
			}
		} catch (InterruptedIOException e) {
			len = e.bytesTransferred;
			iioe = e;
		}
		count += len;
		// strip out CR's in CR/LF pairs
		// pendingByte will be true iff previous byte was a CR
		int j = off;
		for (int i = off; i < off + count; ++i) { // invariant: j <= i
			lastByte = buffer[i];
			if (lastByte == '\r') {
				if (pendingByte) {
					buffer[j++] = '\r'; // write out orphan CR
				} else {
					pendingByte = true;
				}
			} else {
				if (pendingByte) {
					if (lastByte != '\n') buffer[j++] = '\r'; // if LF, don't write the CR
					pendingByte = false;
				}
				buffer[j++] = (byte) lastByte;
			}
		}
		if (iioe != null) {
			iioe.bytesTransferred = j - off;
			throw iioe;
		}
		return j - off;
	}

	/**
	 * Calls read() to skip the specified number of bytes
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred may be non-zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public long skip(long count) throws IOException {
		int actualCount = 0; // assumes count < Integer.MAX_INT
		try {
			while (count-- > 0 && read() != -1) actualCount++; // skip the specified number of bytes
			return actualCount;
		} catch (InterruptedIOException e) {
			e.bytesTransferred = actualCount;
			throw e;
		}
	}

	/**
	 * Wraps the underlying stream's method.
	 * Returns the number of bytes that can be read without blocking; accounts for
	 * possible translation of CR/LF sequences to LFs in these bytes.
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int available() throws IOException {
		return in.available() / 2; // we can guarantee at least this amount after contraction
	}

	/**
	 * Mark is not supported by the wrapper even if the underlying stream does, returns false.
	 */
	@Override
	public boolean markSupported() {
		return false;
	}
}
