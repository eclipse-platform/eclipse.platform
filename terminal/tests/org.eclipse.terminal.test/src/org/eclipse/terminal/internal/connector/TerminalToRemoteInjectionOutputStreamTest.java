/*******************************************************************************
 * Copyright (c) 2008, 2026 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.internal.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class TerminalToRemoteInjectionOutputStreamTest {
	final static Charset ENCODING = StandardCharsets.UTF_8;

	/**
	 * This class escapes strings coming on the original
	 * terminal..
	 *
	 */
	class CleverInterceptor extends TerminalToRemoteInjectionOutputStream.Interceptor {

		@Override
		public void close() throws IOException {
		}

		@Override
		public void write(int b) throws IOException {
			fOriginal.write('[');
			fOriginal.write(b);
			fOriginal.write(']');
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			fOriginal.write('[');
			fOriginal.write(b, off, len);
			fOriginal.write(']');
		}

	}

	class NullInterceptor extends TerminalToRemoteInjectionOutputStream.Interceptor {
	}

	@Test
	public void testClose() throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		try (TerminalToRemoteInjectionOutputStream s = new TerminalToRemoteInjectionOutputStream(bs)) {
			s.write("begin:".getBytes(ENCODING));
			assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
			OutputStream os1 = s.grabOutput();
			os1.write('x');
			s.write('A');
			os1.write('y');
			s.write('B');
			os1.close();

			s.write('-');
			OutputStream os = s.grabOutput();
			// make sure the closed output does not inject anything
			assertThrows(IOException.class, () -> os1.write('k'));
			os.write('X');
			s.write('a');
			os.write('Y');
			// make sure the closed output does not inject anything
			assertThrows(IOException.class, () -> os1.write('l'));
			s.write('b');
			os.close();
			assertEquals("begin:xyAB-XYab", new String(bs.toByteArray(), ENCODING));
		}
	}

	@Test
	public void testFlush() {
	}

	@Test
	public void testWriteInt() throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		TerminalToRemoteInjectionOutputStream s = new TerminalToRemoteInjectionOutputStream(bs);
		s.write("begin:".getBytes(ENCODING));
		assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
		OutputStream os = s.grabOutput();
		os.write('x');
		s.write('A');
		os.write('y');
		s.write('B');
		s.close();
		assertEquals("begin:xyAB", new String(bs.toByteArray(), ENCODING));

	}

	@Test
	public void testWriteByteArray() {
	}

	@Test
	public void testWriteByteArrayIntInt() {
	}

	@Test
	public void testGrabOutput() throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		try (TerminalToRemoteInjectionOutputStream s = new TerminalToRemoteInjectionOutputStream(bs)) {
			s.write("begin:".getBytes(ENCODING));
			assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
			OutputStream os1 = s.grabOutput();
			assertThrows(IOException.class, () -> s.grabOutput(), "should fail until the first output is closed");
			os1.close();
			OutputStream os2 = s.grabOutput();
			assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
			os2.write("Test".getBytes(ENCODING));
			assertEquals("begin:Test", new String(bs.toByteArray(), ENCODING));
			s.write(" west".getBytes(ENCODING));
			assertEquals("begin:Test", new String(bs.toByteArray(), ENCODING));
			os2.write(" the".getBytes(ENCODING));
			assertEquals("begin:Test the", new String(bs.toByteArray(), ENCODING));
			os2.close();
			assertEquals("begin:Test the west", new String(bs.toByteArray(), ENCODING));
			s.write('!');
			assertEquals("begin:Test the west!", new String(bs.toByteArray(), ENCODING));
		}

	}

	@Test
	public void testGrabOutputWithCleverInterceptor() throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		try (TerminalToRemoteInjectionOutputStream s = new TerminalToRemoteInjectionOutputStream(bs)) {
			s.write("begin:".getBytes(ENCODING));
			assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
			// the injector escapes the output coming from the main stream
			OutputStream os = s.grabOutput(new CleverInterceptor());
			assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
			os.write("Test".getBytes(ENCODING));
			assertEquals("begin:Test", new String(bs.toByteArray(), ENCODING));
			s.write(" west".getBytes(ENCODING));
			assertEquals("begin:Test[ west]", new String(bs.toByteArray(), ENCODING));
			os.write(" the".getBytes(ENCODING));
			assertEquals("begin:Test[ west] the", new String(bs.toByteArray(), ENCODING));
			s.write('x');
			assertEquals("begin:Test[ west] the[x]", new String(bs.toByteArray(), ENCODING));
			os.close();
			assertEquals("begin:Test[ west] the[x]", new String(bs.toByteArray(), ENCODING));
			s.write('!');
			assertEquals("begin:Test[ west] the[x]!", new String(bs.toByteArray(), ENCODING));
		}
	}

	@Test
	public void testGrabOutputWithNullInterceptor() throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		try (TerminalToRemoteInjectionOutputStream s = new TerminalToRemoteInjectionOutputStream(bs)) {
			s.write("begin:".getBytes(ENCODING));
			assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
			// bytes written to the main stream are ignored while the injector
			// is active
			OutputStream os = s.grabOutput(new NullInterceptor());
			assertEquals("begin:", new String(bs.toByteArray(), ENCODING));
			os.write("Test".getBytes(ENCODING));
			assertEquals("begin:Test", new String(bs.toByteArray(), ENCODING));
			s.write(" west".getBytes(ENCODING));
			assertEquals("begin:Test", new String(bs.toByteArray(), ENCODING));
			os.write(" the".getBytes(ENCODING));
			assertEquals("begin:Test the", new String(bs.toByteArray(), ENCODING));
			s.write('x');
			assertEquals("begin:Test the", new String(bs.toByteArray(), ENCODING));
			os.close();
			assertEquals("begin:Test the", new String(bs.toByteArray(), ENCODING));
			s.write('!');
			assertEquals("begin:Test the!", new String(bs.toByteArray(), ENCODING));
		}

	}

}
