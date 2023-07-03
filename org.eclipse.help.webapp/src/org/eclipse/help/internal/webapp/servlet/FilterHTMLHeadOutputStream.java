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

package org.eclipse.help.internal.webapp.servlet;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Filters output stream and inserts specified bytes before the end of HEAD
 * element (before &lt;/html&gt; tag) of HTML in the stream.
 */
public class FilterHTMLHeadOutputStream extends FilterOutputStream {
	private static final int STATE_START = 0;

	private static final int STATE_LT = 1;

	private static final int STATE_LT_SLASH = 2;

	private static final int STATE_LT_SLASH_H = 3;

	private static final int STATE_LT_SLASH_HE = 4;

	private static final int STATE_LT_SLASH_HEA = 5;

	private static final int STATE_LT_SLASH_HEAD = 6;

	private static final int STATE_DONE = 7;

	private int state = STATE_START;

	private byte[] toInsert;

	ByteArrayOutputStream buffer = new ByteArrayOutputStream(7);

	/**
	 * Constructor.
	 *
	 * @param out
	 *            sink output stream
	 * @param bytesToInsert
	 *            bytes to insert in head of HTML
	 */
	public FilterHTMLHeadOutputStream(OutputStream out, byte[] bytesToInsert) {
		super(out);
		toInsert = bytesToInsert;
	}

	/**
	 * Writes the specified <code>byte</code> to this output stream.
	 * <p>
	 * The underlying stream might have a more bytes written to it, following
	 * the &lt;head&gt; HTML element.
	 * <p>
	 * Implements the abstract <tt>write</tt> method of <tt>OutputStream</tt>.
	 *
	 * @param b
	 *            the <code>byte</code>.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	@Override
	public final void write(int b) throws IOException {
		switch (state) {
		case STATE_START:
			if (b == '<') {
				buffer.write(b);
				state = STATE_LT;
			} else {
				out.write(b);
			}
			break;
		case STATE_LT:
			buffer.write(b);
			if (b == '/') {
				state = STATE_LT_SLASH;
			} else {
				reset();
			}
			break;
		case STATE_LT_SLASH:
			buffer.write(b);
			if (b == 'h' || b == 'H') {
				state = STATE_LT_SLASH_H;
			} else {
				reset();
			}
			break;
		case STATE_LT_SLASH_H:
			buffer.write(b);
			if (b == 'e' || b == 'E') {
				state = STATE_LT_SLASH_HE;
			} else {
				reset();
			}
			break;
		case STATE_LT_SLASH_HE:
			buffer.write(b);
			if (b == 'a' || b == 'A') {
				state = STATE_LT_SLASH_HEA;
			} else {
				reset();
			}
			break;
		case STATE_LT_SLASH_HEA:
			buffer.write(b);
			if (b == 'd' || b == 'D') {
				state = STATE_LT_SLASH_HEAD;
			} else {
				reset();
			}
			break;
		case STATE_LT_SLASH_HEAD:
			buffer.write(b);
			if (b == '>') {
				out.write(toInsert);
				out.write('\n');
				reset();
				state = STATE_DONE;
			} else {
				reset();
			}
			break;
		default: // (case STATE_DONE):
			out.write(b);
			break;
		}
	}

	private void reset() throws IOException {
		out.write(buffer.toByteArray());
		buffer.reset();
		state = STATE_START;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (state == STATE_DONE) {
			out.write(b, off, len);
		} else {
			for (int i = 0; i < len; i++) {
				write(b[off + i]);
			}
		}
	}

	@Override
	public void close() throws IOException {
		reset();
		super.close();
	}
}
