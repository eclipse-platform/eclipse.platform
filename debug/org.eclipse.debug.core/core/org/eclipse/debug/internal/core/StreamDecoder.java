/*******************************************************************************
 * Copyright (c) 2017 Andreas Loth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andreas Loth - initial API and implementation
 *******************************************************************************/

package org.eclipse.debug.internal.core;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Set;

/**
 * Wraps CharsetDecoder to decode a byte stream statefully to characters.
 *
 * @since 3.7 org.eclipse.ui.console
 */
public class StreamDecoder {
	// For more context see https://bugs.eclipse.org/bugs/show_bug.cgi?id=507664

	/** size of java.io.BufferedInputStream.DEFAULT_BUFFER_SIZE **/
	private static final int INPUT_BUFFER_SIZE = 8192;

	private final Charset charset;
	private final CharsetDecoder decoder;
	private final ByteBuffer inputBuffer;
	private final CharBuffer outputBuffer;
	private volatile boolean finished;

	/**
	 * Incomplete list of known Single Byte Character Sets (see
	 * sun.nio.cs.SingleByte) which do not need a buffer
	 **/
	Set<String> singlebyteCharsetNames = Set.of("ISO_8859_1", "US_ASCII", "windows-1250", "windows-1251", "windows-1252", "windows-1253", "windows-1254", "windows-1255", "windows-1256", "windows-1257", "windows-1258"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$

	public StreamDecoder(Charset charset) {
		this.charset = charset;
		CharsetDecoder d = charset.newDecoder();
		d.onMalformedInput(CodingErrorAction.REPLACE);
		d.onUnmappableCharacter(CodingErrorAction.REPLACE);
		boolean unbuffered = singlebyteCharsetNames.contains(charset.name());
		this.decoder = unbuffered ? null : d;
		this.inputBuffer = unbuffered ? null : ByteBuffer.allocate(StreamDecoder.INPUT_BUFFER_SIZE).flip();
		this.outputBuffer = unbuffered ? null : CharBuffer.allocate((int) (StreamDecoder.INPUT_BUFFER_SIZE * d.maxCharsPerByte()));
		this.finished = false;
	}

	private void consume(StringBuilder consumer) {
		this.outputBuffer.flip();
		consumer.append(this.outputBuffer);
		this.outputBuffer.clear();
	}

	private void internalDecode(StringBuilder consumer, byte[] buffer, int offset, int length) {
		assert (offset >= 0);
		assert (length >= 0);
		int position = offset;
		int end = offset + length;
		assert (end <= buffer.length);
		boolean finishedReading = false;
		do {
			CoderResult result = this.decoder.decode(this.inputBuffer, this.outputBuffer, false);
			if (result.isOverflow()) {
				this.consume(consumer);
			} else if (result.isUnderflow()) {
				this.inputBuffer.compact();
				int remaining = this.inputBuffer.remaining();
				assert (remaining > 0);
				int read = Math.min(remaining, end - position);
				if (read > 0) {
					this.inputBuffer.put(buffer, position, read);
					position += read;
				} else {
					finishedReading = true;
				}
				this.inputBuffer.flip();
			} else {
				assert false;
			}
		} while (!finishedReading);
	}

	public String decode(byte[] buffer, int offset, int length) {
		if (this.decoder == null) {
			// fast path for single byte encodings
			return new String(buffer, offset, length, charset);
		}
		StringBuilder builder = new StringBuilder();
		decode(builder, buffer, offset, length);
		return builder.toString();
	}

	private void decode(StringBuilder consumer, byte[] buffer, int offset, int length) {
		this.internalDecode(consumer, buffer, offset, length);
		this.consume(consumer);
	}

	public String finish() {
		if (this.decoder == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder builder = new StringBuilder();
		finish(builder);
		return builder.toString();
	}

	private void finish(StringBuilder consumer) {
		if (this.finished) {
			return;
		}
		this.finished = true;
		CoderResult result;
		result = this.decoder.decode(this.inputBuffer, this.outputBuffer, true);
		assert (result.isOverflow() || result.isUnderflow());
		do {
			result = this.decoder.flush(this.outputBuffer);
			if (result.isOverflow()) {
				this.consume(consumer);
			} else {
				assert result.isUnderflow();
			}
		} while (!result.isUnderflow());
		this.consume(consumer);
	}

}
