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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import org.eclipse.core.internal.content.LazyReader;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LazyReader}.
 */
public class LazyReaderTest {

	/**
	 * Opens up protected methods from LazyInputStream.
	 */
	private static class OpenLazyReader extends LazyReader {

		public OpenLazyReader(Reader in, int blockCapacity) {
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
		protected void setBufferSize(long bufferSize) {
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

	@Test
	public void testReadSingleChar() throws IOException {
		CharArrayReader underlying = new CharArrayReader(DATA.toCharArray());
		OpenLazyReader stream = new OpenLazyReader(underlying, 7);
		assertEquals('0', stream.read());
		assertEquals('1', stream.read());
		stream.skip(10);
		assertEquals('2', stream.read());
		assertEquals(13, stream.getOffset());
		stream.close();
	}

	@Test
	public void testReadBlock() throws IOException {
		CharArrayReader underlying = new CharArrayReader(DATA.toCharArray());
		OpenLazyReader stream = new OpenLazyReader(underlying, 7);
		stream.skip(4);
		char[] buffer = new char[7];
		int read = stream.read(buffer);
		assertThat(buffer).hasSize(read);
		assertEquals(DATA.substring(4, 4 + buffer.length), new String(buffer));
		assertEquals(11, stream.getOffset());
		read = stream.read(buffer, 3, 4);
		assertEquals(4, read);
		assertEquals(DATA.substring(11, 11 + read), new String(buffer, 3, read));
		assertEquals(15, stream.getOffset());
		stream.mark(0);
		buffer = new char[100];
		read = stream.read(buffer);
		assertEquals(DATA.length() - 15, read);
		assertEquals(DATA.substring(15, 15 + read), new String(buffer, 0, read));
		assertFalse(stream.ready());
		stream.reset();
		assertEquals(15, stream.getOffset());
		read = stream.read(buffer, 10, 14);
		assertEquals(29, stream.getOffset());
		assertTrue(stream.ready());
		assertEquals(14, read);
		assertEquals(DATA.substring(15, 15 + read), new String(buffer, 10, read));
		read = stream.read(buffer);
		assertEquals(30, stream.getOffset());
		assertFalse(stream.ready());
		assertEquals(1, read);
		assertEquals((byte) DATA.charAt(29), buffer[0]);
		read = stream.read(buffer);
		assertEquals(30, stream.getOffset());
		assertFalse(stream.ready());
		assertEquals(-1, read);
		stream.close();
	}

	@Test
	public void testMarkAndReset() throws IOException {
		CharArrayReader underlying = new CharArrayReader(DATA.toCharArray());
		OpenLazyReader stream = new OpenLazyReader(underlying, 7);
		assertTrue(stream.ready());
		stream.skip(13);
		assertTrue(stream.ready());
		stream.mark(0);
		assertEquals(13, stream.getMark());
		assertEquals('3', stream.read());
		assertEquals('4', stream.read());
		assertEquals(15, stream.getOffset());
		assertTrue(stream.ready());
		stream.reset();
		assertTrue(stream.ready());
		assertEquals(13, stream.getOffset());
		assertEquals(17, stream.skip(1000));
		assertFalse(stream.ready());
		stream.reset();
		assertTrue(stream.ready());
		assertEquals(13, stream.getOffset());
		stream.reset();
		assertTrue(stream.ready());
		assertEquals(13, stream.getOffset());
		stream.rewind();
		assertEquals(0, stream.getOffset());
		stream.close();
	}

	@Test
	public void testEnsureAvailable_BufferSizeDoesNotOverflow() throws IOException {
		Reader infinitelyEmpty = new Reader() {

			@Override
			public int read() throws IOException {
				return 0;
			}

			@Override
			public int read(char[] b, int off, int len) throws IOException {
				return len;
			}

			@Override
			public void close() throws IOException {
				// nothing to close
			}
		};

		try (OpenLazyReader objectUnderTest = new OpenLazyReader(infinitelyEmpty, 10)) {
			// HACK: needed to avoid filling up the RAM with 0's when calling "skip"
			objectUnderTest.setBufferSize(Integer.MAX_VALUE);
			objectUnderTest.setOffset(Integer.MAX_VALUE);

			objectUnderTest.skip(1);

			assertTrue(objectUnderTest.getBufferSize() > Integer.MAX_VALUE, "The buffer size suffered an Overflow");
		}
	}
}
