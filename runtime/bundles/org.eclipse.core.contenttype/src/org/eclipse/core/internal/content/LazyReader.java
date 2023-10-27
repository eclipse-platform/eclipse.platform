/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

public class LazyReader extends Reader implements ILazySource {
	private final int blockCapacity;
	char[][] blocks = {};
	private long bufferSize;
	private final Reader in;
	private long mark;
	private long offset;
	private final long maxBufferSize;

	public LazyReader(Reader in, int blockCapacity) {
		this.in = in;
		this.blockCapacity = blockCapacity;
		// Since Arrays in Java are limited in size (to Integer.MAX_VALUE), the size of
		// the buffer may never exceed ...
		maxBufferSize = (long) blockCapacity * Integer.MAX_VALUE;
	}

	@Override
	public void close() {
		// we don't close the underlying stream
	}

	private int computeBlockSize(int blockIndex) {
		if (blockIndex < blocks.length - 1)
			return blockCapacity;
		int blockSize = (int) (bufferSize % blockCapacity);
		return blockSize == 0 ? blockCapacity : blockSize;
	}

	private int copyFromBuffer(char[] userBuffer, int userOffset, int needed) throws LowLevelIOException {
		int copied = 0;
		int current = getCurrentBlockIndex();
		while ((needed - copied) > 0 && current < blocks.length) {
			int blockSize = computeBlockSize(current);
			int offsetInBlock = getOffsetInCurrentBlock();
			int availableInBlock = blockSize - offsetInBlock;
			int toCopy = Math.min(availableInBlock, needed - copied);
			System.arraycopy(blocks[current], offsetInBlock, userBuffer, userOffset + copied, toCopy);
			copied += toCopy;
			current++;
			increaseOffset(toCopy);
		}
		return copied;
	}

	private int getCurrentBlockIndex() {
		return Math.toIntExact(offset / blockCapacity);
	}

	private int getOffsetInCurrentBlock() {
		return (int) offset % blockCapacity;
	}

	private void increaseOffset(long inc) throws LowLevelIOException {
		checkOffsetIncrease(inc);
		offset += inc;
	}

	private void checkOffsetIncrease(long inc) throws LowLevelIOException {
		if (offset + inc >= maxBufferSize) {
			// Increasing the offset would go over the maximum capacity
			throw new LowLevelIOException(
					new EOFException("This would bring the current offset over the limit: " + maxBufferSize)); //$NON-NLS-1$
		}
	}

	private void ensureAvailable(long charsToRead) throws IOException {
		// Make sure that we won't go over the maximum capacity of the buffer
		// before even trying to load it
		checkOffsetIncrease(charsToRead);

		int loadedBlockSize = blockCapacity;
		while (bufferSize < offset + charsToRead && loadedBlockSize == blockCapacity) {
			try {
				loadedBlockSize = loadBlock();
			} catch (IOException ioe) {
				throw new LowLevelIOException(ioe);
			}
			// no need to check this like we did before by calling "checkOffsetIncrease"
			// since checkOffsetIncrease already makes sure that there will be no overflow
			bufferSize += loadedBlockSize;
		}
	}

	// for testing purposes
	protected int getBlockCount() {
		return blocks.length;
	}

	// for testing purposes
	protected long getBufferSize() {
		return bufferSize;
	}

	// for testing purposes
	protected void setBufferSize(long bufferSize) {
		this.bufferSize = bufferSize;
	}

	// for testing purposes
	protected long getMark() {
		return mark;
	}

	// for testing purposes
	protected long getOffset() {
		return offset;
	}

	// for testing purposes
	protected void setOffset(long offset) {
		this.offset = offset;
	}

	@Override
	public boolean isText() {
		return true;
	}

	private int loadBlock() throws IOException {
		// read a block from the underlying stream
		char[] newBlock = new char[blockCapacity];
		int readCount = in.read(newBlock);
		if (readCount == -1)
			return 0;
		// expand blocks array
		char[][] tmpBlocks = new char[blocks.length + 1][];
		System.arraycopy(blocks, 0, tmpBlocks, 0, blocks.length);
		blocks = tmpBlocks;
		blocks[blocks.length - 1] = newBlock;
		return readCount;
	}

	@Override
	public void mark(int readlimit) {
		mark = offset;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public int read() throws IOException {
		ensureAvailable(1);
		if (bufferSize <= offset)
			return -1;
		char nextChar = blocks[getCurrentBlockIndex()][getOffsetInCurrentBlock()];
		increaseOffset(1);
		return nextChar;
	}

	@Override
	public int read(char[] c) throws IOException {
		return read(c, 0, c.length);
	}

	@Override
	public int read(char[] c, int off, int len) throws IOException {
		ensureAvailable(len);
		int copied = copyFromBuffer(c, off, len);
		return copied == 0 ? -1 : copied;
	}

	@Override
	public boolean ready() throws IOException {
		try {
			return (bufferSize - offset) > 0 || in.ready();
		} catch (IOException ioe) {
			throw new LowLevelIOException(ioe);
		}
	}

	@Override
	public void reset() {
		offset = mark;
	}

	@Override
	public void rewind() {
		mark = 0;
		offset = 0;
	}

	@Override
	public long skip(long toSkip) throws IOException {
		if (toSkip <= 0)
			return 0;
		ensureAvailable(toSkip);
		long skipped = Math.min(toSkip, bufferSize - offset);
		increaseOffset(skipped);
		return skipped;
	}
}
