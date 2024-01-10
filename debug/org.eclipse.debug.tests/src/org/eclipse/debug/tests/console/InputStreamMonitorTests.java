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

import static org.assertj.core.api.Assertions.assertThat;

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
			byte[] originalContent = new byte[100];
			for (int i = 0; i < originalContent.length; i++) {
				originalContent[i] = (byte) (i % 255);
			}
			int half = originalContent.length / 2;
			monitor.write(originalContent, 0, half);
			monitor.startMonitoring();
			monitor.write(originalContent, half, originalContent.length - half);
			waitForElementsInStream(sysin, originalContent.length);

			byte[] contentWrittenByMonitor = new byte[originalContent.length];
			sysin.read(contentWrittenByMonitor);
			int additionalBytesWritten = sysin.available();
			assertThat(additionalBytesWritten).isZero();
			assertThat(contentWrittenByMonitor).isEqualTo(originalContent);
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
		assertThat(sysin.available()).isEqualTo(numberOfElements);
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
			String readContent = new String(readBack, 0, len);
			assertThat(readContent).isEqualTo(text);
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
			assertThat(testStream.numClosed).withFailMessage("stream closed too early").isZero();
			monitor.closeInputStream();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, CONDITION_TIMEOUT_IN_MILLIS);
			assertThat(testStream.numClosed).withFailMessage("stream not closed").isNotZero();
		}
		{
			ClosableTestOutputStream testStream = new ClosableTestOutputStream();
			InputStreamMonitor monitor = new InputStreamMonitor(testStream);
			monitor.startMonitoring(threadName);
			assertThat(testStream.numClosed).withFailMessage("stream closed too early").isZero();
			monitor.close();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, CONDITION_TIMEOUT_IN_MILLIS);
			assertThat(testStream.numClosed).withFailMessage("stream not closed").isNotZero();
		}
		{
			ClosableTestOutputStream testStream = new ClosableTestOutputStream();
			InputStreamMonitor monitor = new InputStreamMonitor(testStream);
			monitor.startMonitoring(threadName);
			assertThat(testStream.numClosed).withFailMessage("stream closed too early").isZero();
			monitor.closeInputStream();
			monitor.close();
			monitor.close();
			TestUtil.waitWhile(() -> testStream.numClosed == 0, CONDITION_TIMEOUT_IN_MILLIS);
			assertThat(testStream.numClosed).as("stream should be closed once").isEqualTo(1);
		}

		TestUtil.waitWhile(() -> getInputStreamMonitorThreads.get() > 0, CONDITION_TIMEOUT_IN_MILLIS);
		assertThat(getInputStreamMonitorThreads.get()).as("leaked monitor threads").isZero();
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
