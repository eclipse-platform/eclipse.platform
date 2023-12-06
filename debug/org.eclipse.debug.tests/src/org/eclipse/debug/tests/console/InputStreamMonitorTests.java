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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.internal.core.InputStreamMonitor;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.TestsPlugin;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link InputStreamMonitor}.
 */
public class InputStreamMonitorTests extends AbstractDebugTest {
	private static int CONDITION_TIMEOUT_IN_MILLIS = 1_000;

	/**
	 * Simple test for input stream monitor. Write some bytes before starting
	 * the monitor, some after and check if they are correctly transfered.
	 */
	@Test
	public void testInputStreamMonitor() throws Exception {
		InputStreamMonitor monitor = null;
		try(PipedInputStream sysin = new PipedInputStream();
				OutputStream outputFromSysin = new PipedOutputStream(sysin);
		) {
			monitor = new InputStreamMonitor(outputFromSysin);
			byte[] content = new byte[100];
			for (int i = 0; i < content.length; i++) {
				content[i] = (byte) (i % 255);
			}
			int half = content.length / 2;
			monitor.write(content, 0, half);
			monitor.startMonitoring();
			monitor.write(content, half, content.length - half);
			waitForElementsInStream(sysin, content.length);

			byte[] readBack = new byte[content.length];
			int read = sysin.read(readBack);
			assertEquals("Monitor wrote to few bytes.", read, content.length);
			assertThat("Monitor wrote to much bytes.", sysin.available(), is(0));
			assertThat("Monitor wrote wrong content.", readBack, is(content));
		} finally {
			if (monitor != null) {
				monitor.close();
			}
		}
	}

	private void waitForElementsInStream(PipedInputStream sysin, int numberOfElements) throws Exception {
		TestUtil.waitWhile(() -> {
			try {
				return sysin.available() < numberOfElements;
			} catch (IOException e) {
				// Ignore and check again
			}
			return true;
		}, CONDITION_TIMEOUT_IN_MILLIS);
		assertThat(sysin.available(), is(numberOfElements));
	}

	/**
	 * Test that passing <code>null</code> as charset does not raise exceptions.
	 */
	@Test
	public void testNullCharset() throws Exception {
		InputStreamMonitor monitor = null;
		try (PipedInputStream sysin = new PipedInputStream(); OutputStream outputFromSysin = new PipedOutputStream(sysin);) {
			monitor = new InputStreamMonitor(outputFromSysin);
			String text = "o\u00F6O\u00EFiI\u00D6\u00D8\u00F8";
			monitor.startMonitoring();
			monitor.write(text);
			waitForElementsInStream(sysin, text.getBytes().length);

			byte[] readBack = new byte[1000];
			int len = sysin.read(readBack);
			assertThat("Monitor wrote wrong content.", text, is(new String(readBack, 0, len)));
		} finally {
			if (monitor != null) {
				monitor.close();
			}
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
			assertThat("Stream closed to early.", testStream.numClosed, is(0));
			monitor.closeInputStream();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, CONDITION_TIMEOUT_IN_MILLIS);
			assertThat("Stream not closed.", testStream.numClosed, is(1));
		}
		{
			ClosableTestOutputStream testStream = new ClosableTestOutputStream();
			InputStreamMonitor monitor = new InputStreamMonitor(testStream);
			monitor.startMonitoring(threadName);
			assertThat("Stream closed to early.", testStream.numClosed, is(0));
			monitor.close();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, CONDITION_TIMEOUT_IN_MILLIS);
			assertThat("Stream not closed.", testStream.numClosed, is(1));
		}
		{
			ClosableTestOutputStream testStream = new ClosableTestOutputStream();
			InputStreamMonitor monitor = new InputStreamMonitor(testStream);
			monitor.startMonitoring(threadName);
			assertThat("Stream closed to early.", testStream.numClosed, is(0));
			monitor.closeInputStream();
			monitor.close();
			monitor.close();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, CONDITION_TIMEOUT_IN_MILLIS);
			assertThat("Stream not closed or to often.", testStream.numClosed, is(1));
		}

		TestUtil.waitWhile(() -> getInputStreamMonitorThreads.get() > 0, CONDITION_TIMEOUT_IN_MILLIS);
		assertThat("Leaked monitor threads.", getInputStreamMonitorThreads.get(), is(0L));
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
