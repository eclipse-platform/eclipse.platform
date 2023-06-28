/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.debug.core.IBinaryStreamListener;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IBinaryStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.internal.core.OutputStreamMonitor;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link OutputStreamMonitor}.
 */
public class OutputStreamMonitorTests extends AbstractDebugTest {

	/** Stream to simulate an application writing to system out. */
	PipedOutputStream sysout = new PipedOutputStream();
	/** The {@link OutputStreamMonitor} used for the test runs. */
	TestOutputStreamMonitor monitor;
	/** The bytes received through listener. */
	ByteArrayOutputStream notifiedBytes = new ByteArrayOutputStream();
	/** The strings received through listener. */
	StringBuilder notifiedChars = new StringBuilder();

	IBinaryStreamListener fBinaryListener = new IBinaryStreamListener() {
		@Override
		public void streamAppended(byte[] data, IBinaryStreamMonitor mon) {
			if (monitor == mon) {
				try {
					notifiedBytes.write(data);
				} catch (IOException e) {
				}
			}
		}
	};
	IStreamListener fStreamListener = new IStreamListener() {
		@Override
		public void streamAppended(String text, IStreamMonitor mon) {
			if (monitor == mon) {
				notifiedChars.append(text);
			}
		}
	};

	@Override
	@Before
	@SuppressWarnings("resource")
	public void setUp() throws IOException {
		monitor = new TestOutputStreamMonitor(new PipedInputStream(sysout), StandardCharsets.UTF_8);
	}

	/**
	 * Simple test for output stream monitor. Test buffering and listeners.
	 */
	@Test
	public void testBufferedOutputStreamMonitor() throws Exception {
		String input = "o\u00F6O";
		byte[] byteInput = input.getBytes(StandardCharsets.UTF_8);
		try {
			monitor.addBinaryListener(fBinaryListener);
			monitor.addListener(fStreamListener);

			sysout.write(byteInput, 0, 2);
			sysout.flush();
			monitor.startMonitoring();
			TestUtil.waitWhile(() -> notifiedBytes.size() < 2, 1000);
			String contents = monitor.getContents();
			assertEquals("Monitor read wrong content.", input.substring(0, 1), contents);
			assertEquals("Notified and buffered content differ.", contents, notifiedChars.toString());
			assertEquals("Failed to access buffered content twice.", contents, monitor.getContents());
			byte[] data = monitor.getData();
			byte[] expected = new byte[2];
			System.arraycopy(byteInput, 0, expected, 0, 2);
			assertArrayEquals("Monitor read wrong binary content.", expected, data);
			assertArrayEquals("Notified and buffered binary content differ.", data, notifiedBytes.toByteArray());
			assertArrayEquals("Failed to access buffered binary content twice.", data, monitor.getData());

			monitor.flushContents();
			sysout.write(byteInput, 2, byteInput.length - 2);
			sysout.flush();
			TestUtil.waitWhile(() -> notifiedBytes.size() < byteInput.length, 1000);
			contents = monitor.getContents();
			assertEquals("Monitor buffered wrong content.", input.substring(1), contents);
			assertEquals("Failed to access buffered content twice.", contents, monitor.getContents());
			assertEquals("Wrong content through listener.", input, notifiedChars.toString());
			data = monitor.getData();
			expected = new byte[byteInput.length - 2];
			System.arraycopy(byteInput, 2, expected, 0, expected.length);
			assertArrayEquals("Monitor read wrong binary content.", expected, data);
			assertArrayEquals("Failed to access buffered binary content twice.", data, monitor.getData());
			assertArrayEquals("Wrong binary content through listener.", byteInput, notifiedBytes.toByteArray());
		} finally {
			sysout.close();
			monitor.close();
		}
	}

	/**
	 * Simple test for output stream monitor. Test listeners without buffering.
	 */
	@Test
	public void testUnbufferedOutputStreamMonitor() throws Exception {
		String input = "o\u00F6O";
		byte[] byteInput = input.getBytes(StandardCharsets.UTF_8);
		try {
			monitor.addBinaryListener(fBinaryListener);
			monitor.addListener(fStreamListener);

			sysout.write(byteInput, 0, 2);
			sysout.flush();
			monitor.setBuffered(false);
			monitor.startMonitoring();
			TestUtil.waitWhile(() -> notifiedBytes.size() < 2, 1000);
			assertEquals("Monitor read wrong content.", input.substring(0, 1), notifiedChars.toString());
			byte[] expected = new byte[2];
			System.arraycopy(byteInput, 0, expected, 0, 2);
			assertArrayEquals("Monitor read wrong binary content.", expected, notifiedBytes.toByteArray());

			monitor.flushContents();
			sysout.write(byteInput, 2, byteInput.length - 2);
			sysout.flush();
			TestUtil.waitWhile(() -> notifiedBytes.size() < byteInput.length, 1000);
			assertEquals("Wrong content through listener.", input, notifiedChars.toString());
			expected = new byte[byteInput.length - 2];
			System.arraycopy(byteInput, 2, expected, 0, expected.length);
			assertArrayEquals("Wrong binary content through listener.", byteInput, notifiedBytes.toByteArray());
		} finally {
			sysout.close();
			monitor.close();
		}
	}

	/**
	 * Test that passing <code>null</code> as charset does not raise exceptions.
	 */
	@Test
	@SuppressWarnings("resource")
	public void testNullCharset() throws Exception {
		String input = "o\u00F6O\u00EFiI\u00D6\u00D8\u00F8";

		sysout.close();
		sysout = new PipedOutputStream();
		monitor.close();
		monitor = new TestOutputStreamMonitor(new PipedInputStream(sysout), null);
		try {
			monitor.addListener(fStreamListener);
			monitor.startMonitoring();
			try (PrintStream out = new PrintStream(sysout)) {
				out.print(input);
			}
			sysout.flush();

			TestUtil.waitWhile(() -> notifiedChars.length() < input.length(), 500);
			assertEquals("Monitor read wrong content.", input, notifiedChars.toString());
		} finally {
			sysout.close();
			monitor.close();
		}
	}

	/**
	 * {@link OutputStreamMonitor} with public {@link #startMonitoring()} for
	 * testing.
	 */
	private static class TestOutputStreamMonitor extends OutputStreamMonitor {

		public TestOutputStreamMonitor(InputStream stream, Charset charset) {
			super(stream, charset);
		}

		public void startMonitoring() {
			super.startMonitoring("");
		}

		@Override
		public void close() {
			super.close();
		}
	}
}
