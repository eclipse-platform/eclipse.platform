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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.IBinaryStreamListener;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IBinaryStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.internal.core.OutputStreamMonitor;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;

/**
 * Tests the {@link OutputStreamMonitor}.
 */
public class OutputStreamMonitorTests extends AbstractDebugTest {
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	/** The {@link OutputStreamMonitor} used for the test runs. */
	TestOutputStreamMonitor monitor;
	/** Stream to simulate an application writing to system out. */
	PipedOutputStream sysout;
	PipedInputStream inputFromSysout;

	private class BinaryListener implements IBinaryStreamListener {
		/** The bytes received through listener. */
		private volatile ByteArrayOutputStream recordedBytes = new ByteArrayOutputStream();

		private final List<IOException> exceptions = Collections.synchronizedList(new ArrayList<>());

		@Override
		public void streamAppended(byte[] data, IBinaryStreamMonitor mon) {
			if (monitor == mon) {
				try {
					recordedBytes.write(data);
				} catch (IOException e) {
					exceptions.add(e);
				}
			}
		}

		public void waitForBytes(int numberOfBytes) throws Exception {
			TestUtil.waitWhile(() -> recordedBytes.size() < numberOfBytes, TIMEOUT.toMillis());
		}

		public byte[] getRecordedBytes() {
			return recordedBytes.toByteArray();
		}

		public void assertNoExceptions() {
			assertThat(exceptions, is(empty()));
		}
	}

	private class StreamListener implements IStreamListener {
		/** The strings received through listener. */
		private volatile StringBuilder recordedChars = new StringBuilder();

		@Override
		public void streamAppended(String text, IStreamMonitor mon) {
			if (monitor == mon) {
				recordedChars.append(text);
			}
		}

		public void waitForBytes(int numberOfChars) throws Exception {
			TestUtil.waitWhile(() -> recordedChars.length() < numberOfChars, TIMEOUT.toMillis());
		}

		public String getRecordedChars() {
			return recordedChars.toString();
		}
	}

	@Before
	public void setupStreams() throws IOException {
		sysout = new PipedOutputStream();
		inputFromSysout = new PipedInputStream(sysout);
		monitor = new TestOutputStreamMonitor(inputFromSysout, StandardCharsets.UTF_8);
	}

	@After
	public void closeStreams() throws IOException {
		inputFromSysout.close();
		sysout.close();
		monitor.close();
	}

	/**
	 * Simple test for output stream monitor. Test buffering and listeners.
	 */
	@Test
	public void testBufferedOutputStreamMonitor() throws Exception {
		String input = "o\u00F6O";
		byte[] byteInput = input.getBytes(StandardCharsets.UTF_8);

		BinaryListener binaryListener = new BinaryListener();
		StreamListener streamListener = new StreamListener();
		monitor.addBinaryListener(binaryListener);
		monitor.addListener(streamListener);

		sysout.write(byteInput, 0, 2);
		sysout.flush();
		monitor.startMonitoring();
		binaryListener.waitForBytes(2);
		streamListener.waitForBytes(1);
		String contents = monitor.getContents();
		assertThat("Monitor read wrong content.", contents, is(input.substring(0, 1)));
		assertThat("Notified and buffered content differ.", streamListener.getRecordedChars(), is(contents));
		assertThat("Failed to access buffered content twice.", monitor.getContents(), is(contents));
		byte[] data = monitor.getData();
		byte[] expected = new byte[2];
		System.arraycopy(byteInput, 0, expected, 0, 2);
		assertThat("Monitor read wrong binary content.", data, is(expected));
		assertThat("Notified and buffered binary content differ.", binaryListener.getRecordedBytes(), is(data));
		assertThat("Failed to access buffered binary content twice.", monitor.getData(), is(data));

		monitor.flushContents();
		sysout.write(byteInput, 2, byteInput.length - 2);
		sysout.flush();
		binaryListener.waitForBytes(byteInput.length);
		streamListener.waitForBytes(new String(byteInput).length());
		contents = monitor.getContents();
		assertThat("Monitor buffered wrong content.", contents, is(input.substring(1)));
		assertThat("Failed to access buffered content twice.", monitor.getContents(), is(contents));
		assertThat("Wrong content through listener.", streamListener.getRecordedChars(), is(input));
		data = monitor.getData();
		expected = new byte[byteInput.length - 2];
		System.arraycopy(byteInput, 2, expected, 0, expected.length);
		assertThat("Monitor read wrong binary content.", data, is(expected));
		assertThat("Failed to access buffered binary content twice.", monitor.getData(), is(data));
		assertThat("Wrong binary content through listener.", binaryListener.getRecordedBytes(), is(byteInput));

		binaryListener.assertNoExceptions();
	}

	/**
	 * Simple test for output stream monitor. Test listeners without buffering.
	 */
	@Test
	public void testUnbufferedOutputStreamMonitor() throws Exception {
		String input = "o\u00F6O";
		byte[] byteInput = input.getBytes(StandardCharsets.UTF_8);

		BinaryListener binaryListener = new BinaryListener();
		StreamListener streamListener = new StreamListener();
		monitor.addBinaryListener(binaryListener);
		monitor.addListener(streamListener);

		sysout.write(byteInput, 0, 2);
		sysout.flush();
		monitor.setBuffered(false);
		monitor.startMonitoring();
		binaryListener.waitForBytes(2);
		streamListener.waitForBytes(1);
		assertThat("Monitor read wrong content.", streamListener.getRecordedChars(), is(input.substring(0, 1)));
		byte[] expected = new byte[2];
		System.arraycopy(byteInput, 0, expected, 0, 2);
		assertThat("Monitor read wrong binary content.", binaryListener.getRecordedBytes(), is(expected));

		monitor.flushContents();
		sysout.write(byteInput, 2, byteInput.length - 2);
		sysout.flush();
		binaryListener.waitForBytes(byteInput.length);
		streamListener.waitForBytes(new String(byteInput).length());
		assertThat("Wrong content through listener.", streamListener.getRecordedChars(), is(input));
		expected = new byte[byteInput.length - 2];
		System.arraycopy(byteInput, 2, expected, 0, expected.length);
		assertThat("Wrong binary content through listener.", binaryListener.getRecordedBytes(), is(byteInput));

		binaryListener.assertNoExceptions();
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

		StreamListener streamListener = new StreamListener();
		monitor.addListener(streamListener);
		monitor.startMonitoring();
		try (PrintStream out = new PrintStream(sysout)) {
			out.print(input);
		}
		sysout.flush();

		streamListener.waitForBytes(input.length());
		assertThat("Monitor read wrong content.", streamListener.getRecordedChars(), is(input));
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
