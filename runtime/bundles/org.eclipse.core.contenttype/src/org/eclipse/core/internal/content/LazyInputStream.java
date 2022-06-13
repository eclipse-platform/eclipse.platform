/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;

public class LazyInputStream extends InputStream implements ILazySource {
	private int blockCapacity;
	byte[][] blocks = {};
	private int bufferSize;
	private InputStream in;
	private int mark;
	private int offset;

	public LazyInputStream(InputStream in, int blockCapacity) {
		this.in = in;
		this.blockCapacity = blockCapacity;
	}

	@Override
	public int available() throws IOException {
		try {
			return bufferSize - offset + in.available();
		} catch (IOException ioe) {
			throw new LowLevelIOException(ioe);
		}
	}

	private int computeBlockSize(int blockIndex) {
		if (blockIndex < blocks.length - 1)
			return blockCapacity;
		int blockSize = bufferSize % blockCapacity;
		return blockSize == 0 ? blockCapacity : blockSize;
	}

	private int copyFromBuffer(byte[] userBuffer, int userOffset, int needed) {
		int copied = 0;
		int current = offset / blockCapacity;
		while ((needed - copied) > 0 && current < blocks.length) {
			int blockSize = computeBlockSize(current);
			int offsetInBlock = offset % blockCapacity;
			int availableInBlock = blockSize - offsetInBlock;
			int toCopy = Math.min(availableInBlock, needed - copied);
			System.arraycopy(blocks[current], offsetInBlock, userBuffer, userOffset + copied, toCopy);
			copied += toCopy;
			current++;
			offset += toCopy;
		}
		return copied;
	}

	private void ensureAvailable(long bytesToRead) throws IOException {
		int loadedBlockSize = blockCapacity;
		while (bufferSize < offset + bytesToRead && loadedBlockSize == blockCapacity) {
			try {
				loadedBlockSize = loadBlock();
			} catch (IOException e) {
				throw new LowLevelIOException(e);
			}
			bufferSize += loadedBlockSize;
		}
	}

	// for testing purposes
	protected int getBlockCount() {
		return blocks.length;
	}

	// for testing purposes
	protected int getBufferSize() {
		return bufferSize;
	}

	// for testing purposes
	protected int getMark() {
		return mark;
	}

	// for testing purposes
	protected int getOffset() {
		return offset;
	}

	@Override
	public boolean isText() {
		return false;
	}

	private int loadBlock() throws IOException {
		// read a block from the underlying stream
		byte[] newBlock = new byte[blockCapacity];
		int readCount = in.read(newBlock);
		if (readCount == -1)
			return 0;
		// expand blocks array
		byte[][] tmpBlocks = new byte[blocks.length + 1][];
		System.arraycopy(blocks, 0, tmpBlocks, 0, blocks.length);
		blocks = tmpBlocks;
		blocks[blocks.length - 1] = newBlock;
		return readCount;
	}

	@Override
	public synchronized void mark(int readlimit) {
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
		int nextByte = 0xFF & blocks[offset / blockCapacity][offset % blockCapacity];
		offset++;
		return nextByte;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ensureAvailable(len);
		int copied = copyFromBuffer(b, off, len);
		return copied == 0 ? -1 : copied;
	}

	@Override
	public synchronized void reset() {
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
		offset += skipped;
		return skipped;
	}
}
