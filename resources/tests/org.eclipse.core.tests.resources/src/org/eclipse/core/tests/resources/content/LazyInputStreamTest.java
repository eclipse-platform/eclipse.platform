/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.internal.content.LazyInputStream;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LazyInputStream}.
 */
public class LazyInputStreamTest {

	/**
	 * Opens up protected methods from LazyInputStream.
	 */
	private static class OpenLazyInputStream extends LazyInputStream {

		public OpenLazyInputStream(InputStream in, int blockCapacity) {
			super(in, blockCapacity);
		}

		@Override
		public int getBlockCount() {
			return super.getBlockCount();
		}

		@Override
		public long getBufferSize() {
			return super.getBufferSize();
		}

		@Override
		public void setBufferSize(long bufferSize) {
			super.setBufferSize(bufferSize);
		}

		@Override
		public long getMark() {
			return super.getMark();
		}

		@Override
		public long getOffset() {
			return super.getOffset();
		}

		@Override
		protected void setOffset(long offset) {
			super.setOffset(offset);
		}
	}

	private final static String DATA = "012345678901234567890123456789";

	private final static int[] VARIOUS_INTS = {0xFF, 0xFE, 0xA0, 0x7F, 0x70, 0x10, 0x00};

	@Test
	public void testReadSingleByte() throws IOException {
		ByteArrayInputStream underlying = new ByteArrayInputStream(DATA.getBytes());
		OpenLazyInputStream stream = new OpenLazyInputStream(underlying, 7);
		assertEquals('0', stream.read());
		assertEquals('1', stream.read());
		stream.skip(10);
		assertEquals('2', stream.read());
		assertEquals(13, stream.getOffset());
		stream.close();
	}

	@Test
	public void testReadBlock() throws IOException {
		ByteArrayInputStream underlying = new ByteArrayInputStream(DATA.getBytes());
		OpenLazyInputStream stream = new OpenLazyInputStream(underlying, 7);
		stream.skip(4);
		byte[] buffer = new byte[7];
		int read = stream.read(buffer);
		assertThat(buffer).hasSize(read);
		assertEquals(DATA.substring(4, 4 + buffer.length), new String(buffer));
		assertEquals(11, stream.getOffset());
		read = stream.read(buffer, 3, 4);
		assertEquals(4, read);
		assertEquals(DATA.substring(11, 11 + read), new String(buffer, 3, read));
		assertEquals(15, stream.getOffset());
		stream.mark(0);
		buffer = new byte[100];
		read = stream.read(buffer);
		assertEquals(DATA.length() - 15, read);
		assertEquals(DATA.substring(15, 15 + read), new String(buffer, 0, read));
		assertEquals(0, stream.available());
		stream.reset();
		assertEquals(15, stream.getOffset());
		read = stream.read(buffer, 10, 14);
		assertEquals(29, stream.getOffset());
		assertEquals(1, stream.available());
		assertEquals(14, read);
		assertEquals(DATA.substring(15, 15 + read), new String(buffer, 10, read));
		read = stream.read(buffer);
		assertEquals(30, stream.getOffset());
		assertEquals(0, stream.available());
		assertEquals(1, read);
		assertEquals((byte) DATA.charAt(29), buffer[0]);
		read = stream.read(buffer);
		assertEquals(30, stream.getOffset());
		assertEquals(0, stream.available());
		assertEquals(-1, read);
		stream.close();
	}

	@Test
	public void testMarkAndReset() throws IOException {
		ByteArrayInputStream underlying = new ByteArrayInputStream(DATA.getBytes());
		OpenLazyInputStream stream = new OpenLazyInputStream(underlying, 7);
		assertEquals(30, stream.available());
		stream.skip(13);
		assertEquals(17, stream.available());
		stream.mark(0);
		assertEquals(13, stream.getMark());
		assertEquals('3', stream.read());
		assertEquals('4', stream.read());
		assertEquals(15, stream.getOffset());
		assertEquals(15, stream.available());
		stream.reset();
		assertEquals(17, stream.available());
		assertEquals(13, stream.getOffset());
		stream.reset();
		assertEquals(17, stream.available());
		assertEquals(13, stream.getOffset());
		stream.rewind();
		assertEquals(0, stream.getOffset());
		stream.close();
	}

	@Test
	public void testContentHasEOF() throws IOException {
		byte[] changedData = DATA.getBytes();
		changedData[0] = (byte) 0xFF;
		ByteArrayInputStream underlying = new ByteArrayInputStream(changedData);
		OpenLazyInputStream stream = new OpenLazyInputStream(underlying, 7);
		int c = stream.read();
		assertNotEquals(-1, c);
		assertEquals(0xFF, c);
		stream.close();
	}

	@Test
	public void testVariedContent() throws IOException {
		byte[] contents = new byte[VARIOUS_INTS.length];
		for (int i = 0; i < contents.length; i++) {
			contents[i] = (byte) VARIOUS_INTS[i];
		}
		ByteArrayInputStream underlying = new ByteArrayInputStream(contents);
		OpenLazyInputStream stream = new OpenLazyInputStream(underlying, 7);
		for (int i = 0; i < VARIOUS_INTS.length; i++) {
			assertEquals(VARIOUS_INTS[i], stream.read(), i + "");
		}
		stream.close();
	}

	@Test
	public void testEnsureAvailable_BufferSizeDoesNotOverflow() throws IOException {
		InputStream infinitelyEmpty = new InputStream() {

			@Override
			public int read() throws IOException {
				return 0;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return len;
			}
		};

		try (OpenLazyInputStream objectUnderTest = new OpenLazyInputStream(infinitelyEmpty, 10)) {
			// HACK: needed to avoid filling up the RAM with 0's when calling "skip"
			objectUnderTest.setBufferSize(Integer.MAX_VALUE);
			objectUnderTest.setOffset(Integer.MAX_VALUE);

			objectUnderTest.skip(1);

			assertTrue(objectUnderTest.getBufferSize() > Integer.MAX_VALUE, "The buffer size suffered an Overflow");
		}

	}
}
