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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.internal.core.InputStreamMonitor;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.TestsPlugin;
import org.junit.Test;

/**
 * Tests the {@link InputStreamMonitor}.
 */
public class InputStreamMonitorTests extends AbstractDebugTest {

	/**
	 * Simple test for input stream monitor. Write some bytes before starting
	 * the monitor, some after and check if they are correctly transfered.
	 */
	@Test
	@SuppressWarnings("resource")
	public void testInputStreamMonitor() throws Exception {
		PipedInputStream sysin = new PipedInputStream();
		InputStreamMonitor monitor = new InputStreamMonitor(new PipedOutputStream(sysin));

		byte[] content = new byte[100];
		for (int i = 0; i < content.length; i++) {
			content[i] = (byte) (i % 255);
		}
		try {
			int half = content.length / 2;
			monitor.write(content, 0, half);
			monitor.startMonitoring();
			monitor.write(content, half, content.length - half);
			Thread.sleep(30);

			byte[] readBack = new byte[content.length];
			int read = sysin.read(readBack);
			assertEquals("Monitor wrote to few bytes.", read, content.length);
			assertEquals("Monitor wrote to much bytes.", 0, sysin.available());
			assertArrayEquals("Monitor wrote wrong content.", content, readBack);
		} finally {
			monitor.close();
		}
	}

	/**
	 * Test that passing <code>null</code> as charset does not raise exceptions.
	 */
	@Test
	@SuppressWarnings("resource")
	public void testNullCharset() throws Exception {
		PipedInputStream sysin = new PipedInputStream();
		InputStreamMonitor monitor = new InputStreamMonitor(new PipedOutputStream(sysin), (Charset) null);
		String text = "o\u00F6O\u00EFiI\u00D6\u00D8\u00F8";
		try {
			monitor.startMonitoring();
			monitor.write(text);
			Thread.sleep(30);

			byte[] readBack = new byte[1000];
			int len = sysin.read(readBack);
			assertEquals("Monitor wrote wrong content.", text, new String(readBack, 0, len));
		} finally {
			monitor.close();
		}
	}

	/**
	 * Test different combinations of stream closing.
	 */
	@Test
	@SuppressWarnings("resource")
	public void testClose() throws Exception {
		String threadName = "MAGICtestClose";
		Supplier<Long> getInputStreamMonitorThreads = () -> {
			Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
			long numMonitorThreads = allThreads.stream().filter(t -> t.getName().contains(threadName)).count();
			return numMonitorThreads;
		};
		long alreadyLeakedThreads = getInputStreamMonitorThreads.get();
		if (alreadyLeakedThreads > 0) {
			ILog.of(TestsPlugin.class).warn("Test started with " + alreadyLeakedThreads + " leaked monitor threads.");
		}

		{
			ClosableTestOutputStream testStream = new ClosableTestOutputStream();
			InputStreamMonitor monitor = new InputStreamMonitor(testStream);
			assertEquals("Stream closed to early.", 0, testStream.numClosed);
			monitor.closeInputStream();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, 100);
			assertEquals("Stream not closed.", 1, testStream.numClosed);
		}
		{
			ClosableTestOutputStream testStream = new ClosableTestOutputStream();
			InputStreamMonitor monitor = new InputStreamMonitor(testStream);
			monitor.startMonitoring(threadName);
			assertEquals("Stream closed to early.", 0, testStream.numClosed);
			monitor.close();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, 200);
			assertEquals("Stream not closed.", 1, testStream.numClosed);
		}
		{
			ClosableTestOutputStream testStream = new ClosableTestOutputStream();
			InputStreamMonitor monitor = new InputStreamMonitor(testStream);
			monitor.startMonitoring(threadName);
			assertEquals("Stream closed to early.", 0, testStream.numClosed);
			monitor.closeInputStream();
			monitor.close();
			monitor.close();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, 100);
			assertEquals("Stream not closed or to often.", 1, testStream.numClosed);
		}

		TestUtil.waitWhile(() -> getInputStreamMonitorThreads.get() > 0, 500);
		assertEquals("Leaked monitor threads.", 0, (long) getInputStreamMonitorThreads.get());
	}

	/**
	 * Extension of output stream to log calls to {@link #close()}.
	 */
	public static class ClosableTestOutputStream extends OutputStream {
		public volatile int numClosed = 0;

		@Override
		public void close() throws IOException {
			numClosed++;
		}

		@Override
		public void write(int b) throws IOException {
		}
	}
}
