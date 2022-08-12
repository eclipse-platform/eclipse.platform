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
 * Converts LFs in the underlying input stream to CR/LF.
 *
 * Supports resuming partially completed operations after an InterruptedIOException
 * if the underlying stream does.  Check the bytesTransferred field to determine how
 * much of the operation completed; conversely, at what point to resume.
 */
public class LFtoCRLFInputStream extends FilterInputStream {
	private boolean mustReturnLF = false;

	/**
	 * Creates a new filtered input stream.
	 * @param in the underlying input stream
	 */
	public LFtoCRLFInputStream(InputStream in) {
		super(in);
	}

	/**
	 * Wraps the underlying stream's method.
	 * Translates LFs to CR/LF sequences transparently.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int read() throws IOException {
		if (mustReturnLF) {
			mustReturnLF = false;
			return '\n';
		}
		int b = in.read(); // ok if this throws
		if (b == '\n') {
			mustReturnLF = true;
			b = '\r';
		}
		return b;
	}

	/**
	 * Wraps the underlying stream's method.
	 * Translates LFs to CR/LF sequences transparently.
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
		// prefix with remembered \n from last read, but don't expand it a second time
		int count = 0;
		if (mustReturnLF) {
			mustReturnLF = false;
			buffer[off++] = '\n';
			--len;
			count = 1;
			if (len < 2) return count; // is there still enough room to expand more?
		}
		// read some bytes from the stream into the back half of the buffer
		// this guarantees that there is always room to expand
		len /= 2;
		int j = off + len;
		InterruptedIOException iioe = null;
		try {
			len = in.read(buffer, j, len);
			if (len == -1) {
				return (count == 0) ? -1 : count;
			}
		} catch (InterruptedIOException e) {
			len = e.bytesTransferred;
			iioe = e;
		}
		count += len;
		// copy bytes from the middle to the front of the array, expanding LF->CR/LF
		while (len-- > 0) {
			byte b = buffer[j++];
			if (b == '\n') {
				buffer[off++] = '\r';
				count++;
			}
			buffer[off++] = b;
		}
		if (iioe != null) {
			iioe.bytesTransferred = count;
			throw iioe;
		}
		return count;
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
	 * possible translation of LFs to CR/LF sequences in these bytes.
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int available() throws IOException {
		return in.available(); // we can guarantee at least this amount after expansion
	}

	/**
	 * Mark is not supported by the wrapper even if the underlying stream does, returns false.
	 */
	@Override
	public boolean markSupported() {
		return false;
	}
}
