/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.metadata;

import java.io.*;

/**
 * An input stream that counts how many bytes have been read.
 */
public class MeteredInputStream extends PushbackInputStream {

	/** The current number of bytes read. */
	private long offset;

	/**
	 * Constructs a metered input stream chained to the given source input stream.
	 *
	 * @param in the input stream from where to read data
	 */
	public MeteredInputStream(InputStream in) {
		super(in);
	}

	/**
	 * Forwards the call to the super class, incrementing the offset if a
	 * byte is successfully read.
	 *
	 * @return the next byte or <code>-1</code> if there is more data to be read.
	 * @throws IOException if a failure occurs while reading the input stream
	 * @see java.io.PushbackInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		int byteRead = super.read();
		if (byteRead >= 0)
			offset++;
		return byteRead;
	}

	/**
	 * Forwards the call to the super class, incrementing the current offset
	 * by the number of bytes read.
	 *
	 * @param b an array containing bytes to be read
	 * @return the number of bytes read.
	 * @throws IOException if a failure occurs while reading the input stream
	 * @see java.io.PushbackInputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * Forwards the call to the super class, incrementing the current offset
	 * by the number of bytes read.
	 *
	 * @param b an array containing bytes to be read
	 * @param off the array offset where bytes will be read to
	 * @param len the number of bytes to be read
	 * @return the number of bytes read
	 * @throws IOException if a failure occurs while reading the input stream
	 * @see java.io.PushbackInputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = super.read(b, off, len);
		if (read > 0)
			offset += read;
		return read;
	}

	/**
	 * Returns the current offset value.
	 *
	 * @return long the current number of bytes read
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * Forwards the call to the underlying input stream, decrementing the offset by
	 * the number of bytes unread.
	 *
	 * @param b an array containing bytes to be unread
	 * @throws IOException if a failure occurs
	 * @see java.io.PushbackInputStream#unread(byte)
	 */
	@Override
	public void unread(byte[] b) throws IOException {
		super.unread(b);
		offset -= b.length;
	}

	/**
	 * Forwards the call to the underlying input stream, decrementing the offset by
	 * the number of bytes unread.
	 *
	 * @param b an array containing bytes to be unread
	 * @param off the array offset from where bytes will be unread
	 * @param len the number of bytes to be unread
	 * @throws IOException if a failure occurs
	 * @see PushbackInputStream#unread(byte[], int, int)
	 */
	@Override
	public void unread(byte[] b, int off, int len) throws IOException {
		super.unread(b, off, len);
		offset -= len;
	}

	/**
	 * Forwards the call to the underlying push back input stream, decrementing the
	 * offset if a
	 * byte is successfully unread.
	 *
	 * @param b the byte to be unread
	 * @throws IOException if a failure occurs
	 * @see PushbackInputStream#unread(byte[])
	 */
	@Override
	public void unread(int b) throws IOException {
		super.unread(b);
		offset--;
	}

}
