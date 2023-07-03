/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.help.internal.search.HTMLDocParser;

/**
 * Filters output stream and inserts specified bytes before the end of HEAD
 * element (before &lt;/html&gt; tag) and immediately after the BODY
 * element tag of HTML in the stream.
 */
public class FilterHTMLHeadAndBodyOutputStream extends FilterOutputStream {
	private static final int STATE_START = 0;

	private static final int STATE_LT = 1;

	private static final int STATE_LT_SLASH = 2;

	private static final int STATE_LT_SLASH_H = 3;

	private static final int STATE_LT_SLASH_HE = 4;

	private static final int STATE_LT_SLASH_HEA = 5;

	private static final int STATE_LT_SLASH_HEAD = 6;

	private static final int STATE_AFTER_HEAD = 7;

	private static final int STATE_LT_B = 8;

	private static final int STATE_LT_BO = 9;

	private static final int STATE_LT_BOD = 10;

	private static final int STATE_LT_BODY = 11;

	private static final int STATE_DONE = 17;

	private static final int STATE_LT_M = 18;
	private static final int STATE_LT_ME = 19;
	private static final int STATE_LT_MET = 20;
	private static final int STATE_LT_META = 21;

	private int areaState = STATE_START;

	private int state = STATE_START;

	private byte[] toHead;

	private String bodyContent;

	private String charset;

	ByteArrayOutputStream buffer = new ByteArrayOutputStream(7);

	/**
	 * Constructor.
	 *
	 * @param out
	 *            sink output stream
	 * @param bytesForHead
	 *            bytes to insert at the end of head of HTML or
	 *            <code>null</code>
	 * @param bytesToInsert
	 *            bytes to insert at the begining of body of HTML or
	 *            <code>null</code>
	 * @param bytesToAppend
	 *            bytes to append at the end of body of HTML or
	 *            <code>null</code>
	 */
	public FilterHTMLHeadAndBodyOutputStream(OutputStream out,
			byte[] bytesForHead, String bodyContent) {
		super(out);
		toHead = bytesForHead;
		this.bodyContent = bodyContent;
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
		case STATE_AFTER_HEAD:
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
			} else if (b == 'b' || b == 'B') {
				state = STATE_LT_B;
			} else if (b == 'm' || b== 'M') {
				state = STATE_LT_M;
			} else {
				reset();
			}
			break;
		case STATE_LT_M:
			buffer.write(b);
			if (b == 'e' || b=='E') {
				state = STATE_LT_ME;
			}
			else {
				reset();
			}
			break;
		case STATE_LT_ME:
			buffer.write(b);
			if (b == 't' || b=='T') {
				state = STATE_LT_MET;
			}
			else {
				reset();
			}
			break;
		case STATE_LT_MET:
			buffer.write(b);
			if (b == 'a' || b=='A') {
				state = STATE_LT_META;
			}
			else {
				reset();
			}
			break;
		case STATE_LT_META:
			buffer.write(b);
			if (b=='>') {
				parseMetaTag(buffer);
				reset();
			}
			break;
		case STATE_LT_B:
			buffer.write(b);
			if (b == 'o' || b == 'O') {
				state = STATE_LT_BO;
			} else {
				reset();
			}
			break;
		case STATE_LT_BO:
			buffer.write(b);
			if (b == 'd' || b == 'D') {
				state = STATE_LT_BOD;
			} else {
				reset();
			}
			break;
		case STATE_LT_BOD:
			buffer.write(b);
			if (b == 'y' || b == 'Y') {
				state = STATE_LT_BODY;
			} else {
				reset();
			}
			break;
		case STATE_LT_BODY:
			buffer.write(b);
			if (b == '>') {
				out.write(buffer.toByteArray());
				buffer.reset();
				if (bodyContent != null) {
					String encoding = charset!=null?charset:"ASCII"; //$NON-NLS-1$
					byte [] bodyBytes = bodyContent.getBytes(encoding);
					String bodyRecoded = new String(bodyBytes, encoding);
					if (bodyRecoded.equals(bodyContent)) {
						out.write('\n');
						out.write(bodyBytes);
						out.write('\n');
					} else {
						// Some characters could not be encoded
						// Write one character at a time using an entity if necessary
						out.write('\n');
						for (int i = 0; i < bodyContent.length(); i++) {
							String nextChar  = bodyContent.substring(i, i+1);
							byte[] codedChar = nextChar.getBytes(encoding);
							String decodedChar = new String(codedChar, encoding);
							if (decodedChar.equals(nextChar)) {
								out.write(codedChar);
							} else {
								int value = bodyContent.charAt(i);
								String code = "&#" + value + ';'; //$NON-NLS-1$
								out.write(code.getBytes());
							}
						}
						out.write('\n');
					}
				}
				areaState = STATE_DONE;
				state = STATE_DONE;
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
				if (toHead != null) {
					out.write(toHead);
					out.write('\n');
				}
				areaState = STATE_AFTER_HEAD;
				state = STATE_AFTER_HEAD;
				out.write(buffer.toByteArray());
				buffer.reset();
			} else {
				reset();
			}
			break;
		default: // (case STATE_DONE):
			out.write(b);
			break;
		}
	}

	private void parseMetaTag(ByteArrayOutputStream buffer) {
		try (ByteArrayInputStream is = new ByteArrayInputStream(buffer.toByteArray())) {
			String value = HTMLDocParser.getCharsetFromHTML(is);
			if (value != null)
				this.charset = value;
		} catch (IOException e) {
		}
	}

	private void reset() throws IOException {
		out.write(buffer.toByteArray());
		buffer.reset();
		state = areaState;
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
