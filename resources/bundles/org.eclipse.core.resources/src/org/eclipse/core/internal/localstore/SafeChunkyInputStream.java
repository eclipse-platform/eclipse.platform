/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.localstore;

import java.io.*;

/**
 * @see SafeChunkyOutputStream
 */
public class SafeChunkyInputStream extends InputStream {
	protected static final int BUFFER_SIZE = 8192;
	protected byte[] buffer;
	protected int bufferLength = 0;
	protected byte[] chunk;
	protected int chunkLength = 0;
	protected boolean endOfFile = false;
	protected InputStream input;
	protected int nextByteInBuffer = 0;
	protected int nextByteInChunk = 0;

	public SafeChunkyInputStream(File target) throws IOException {
		this(target, BUFFER_SIZE);
	}

	public SafeChunkyInputStream(File target, int bufferSize) throws IOException {
		input = new FileInputStream(target);
		buffer = new byte[bufferSize];
	}

	protected void accumulate(byte[] data, int start, int end) {
		byte[] result = new byte[chunk.length + end - start];
		System.arraycopy(chunk, 0, result, 0, chunk.length);
		System.arraycopy(data, start, result, chunk.length, end - start);
		chunk = result;
		chunkLength = chunkLength + end - start;
	}

	@Override
	public int available() {
		return chunkLength - nextByteInChunk;
	}

	protected void buildChunk() throws IOException {
		//read buffer loads of data until an entire chunk is accumulated
		while (true) {
			if (nextByteInBuffer + ILocalStoreConstants.CHUNK_DELIMITER_SIZE > bufferLength)
				shiftAndFillBuffer();
			int end = find(ILocalStoreConstants.END_CHUNK, nextByteInBuffer, bufferLength, true);
			if (end != -1) {
				accumulate(buffer, nextByteInBuffer, end);
				nextByteInBuffer = end + ILocalStoreConstants.CHUNK_DELIMITER_SIZE;
				return;
			}
			accumulate(buffer, nextByteInBuffer, bufferLength);
			bufferLength = input.read(buffer);
			nextByteInBuffer = 0;
			if (bufferLength == -1) {
				endOfFile = true;
				return;
			}
		}
	}

	@Override
	public void close() throws IOException {
		input.close();
	}

	protected boolean compare(byte[] source, byte[] target, int startIndex) {
		for (byte element : target) {
			if (source[startIndex] != element)
				return false;
			startIndex++;
		}
		return true;
	}

	protected int find(byte[] pattern, int startIndex, int endIndex, boolean accumulate) throws IOException {
		int pos = findByte(pattern[0], startIndex, endIndex);
		if (pos == -1)
			return -1;
		if (pos + ILocalStoreConstants.CHUNK_DELIMITER_SIZE > bufferLength) {
			if (accumulate)
				accumulate(buffer, nextByteInBuffer, pos);
			nextByteInBuffer = pos;
			pos = 0;
			shiftAndFillBuffer();
		}
		if (compare(buffer, pattern, pos))
			return pos;
		return find(pattern, pos + 1, endIndex, accumulate);
	}

	protected int findByte(byte target, int startIndex, int endIndex) {
		while (startIndex < endIndex) {
			if (buffer[startIndex] == target)
				return startIndex;
			startIndex++;
		}
		return -1;
	}

	protected void findChunkStart() throws IOException {
		if (nextByteInBuffer + ILocalStoreConstants.CHUNK_DELIMITER_SIZE > bufferLength)
			shiftAndFillBuffer();
		int begin = find(ILocalStoreConstants.BEGIN_CHUNK, nextByteInBuffer, bufferLength, false);
		if (begin != -1) {
			nextByteInBuffer = begin + ILocalStoreConstants.CHUNK_DELIMITER_SIZE;
			return;
		}
		bufferLength = input.read(buffer);
		nextByteInBuffer = 0;
		if (bufferLength == -1) {
			resetChunk();
			endOfFile = true;
			return;
		}
		findChunkStart();
	}

	@Override
	public int read() throws IOException {
		if (endOfFile)
			return -1;
		// if there are bytes left in the chunk, return the first available
		if (nextByteInChunk < chunkLength)
			return chunk[nextByteInChunk++] & 0xFF;
		// Otherwise the chunk is empty so clear the current one, get the next
		// one and recursively call read.  Need to recur as the chunk may be
		// real but empty.
		resetChunk();
		findChunkStart();
		if (endOfFile)
			return -1;
		buildChunk();
		refineChunk();
		return read();
	}

	/**
	 * Skip over any begin chunks in the current chunk.  This could be optimized
	 * to skip at the same time as we are scanning the buffer.
	 */
	protected void refineChunk() {
		int start = chunkLength - ILocalStoreConstants.CHUNK_DELIMITER_SIZE;
		if (start < 0)
			return;
		for (int i = start; i >= 0; i--) {
			if (compare(chunk, ILocalStoreConstants.BEGIN_CHUNK, i)) {
				nextByteInChunk = i + ILocalStoreConstants.CHUNK_DELIMITER_SIZE;
				return;
			}
		}
	}

	protected void resetChunk() {
		chunk = new byte[0];
		chunkLength = 0;
		nextByteInChunk = 0;
	}

	protected void shiftAndFillBuffer() throws IOException {
		int length = bufferLength - nextByteInBuffer;
		System.arraycopy(buffer, nextByteInBuffer, buffer, 0, length);
		nextByteInBuffer = 0;
		bufferLength = length;
		int read = input.read(buffer, bufferLength, buffer.length - bufferLength);
		if (read != -1)
			bufferLength += read;
		else {
			resetChunk();
			endOfFile = true;
		}
	}
}
