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
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.io.*;

/**
 * High performance reader. Assumes the input stream is ASCII.
 */
public class ASCIIReader extends Reader {
	private InputStream stream;
	int bufSize;
	byte[] buf;
	/**
	 * @param stream
	 *            InputStream
	 * @param bufSize
	 *            size of internal buffer
	 */
	public ASCIIReader(InputStream stream, int bufSize) {
		this.stream = stream;
		this.bufSize = bufSize;
		buf = new byte[bufSize];
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int n = stream.read(buf, 0, Math.min(bufSize, len));
		for (int i = 0; i < n; i++) {
			cbuf[off + i] = (char) buf[i];
		}
		return n;
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

}
