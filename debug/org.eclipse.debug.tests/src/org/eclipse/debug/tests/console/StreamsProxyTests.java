/*******************************************************************************
 * Copyright (c) 2019 Paul Pazderski and others.
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.eclipse.debug.internal.core.StreamsProxy;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.junit.Test;

/**
 * Tests the {@link StreamsProxy}.
 */
public class StreamsProxyTests extends AbstractDebugTest {

	/**
	 * Test console receiving UTF-8 output from process where two-byte UTF-8
	 * characters start at even offsets.
	 */
	@Test
	public void testReceiveUTF8Even() throws Exception {
		// 4500 characters results in 9000 byte of output which should be more
		// than most common buffer sizes.
		receiveUTF8Test("", 4500);
	}

	/**
	 * Test console receiving UTF-8 output from process where two-byte UTF-8
	 * characters start at odd offsets.
	 */
	@Test
	public void testReceiveUTF8Odd() throws Exception {
		// 4500 characters results in 9000 byte of output which should be more
		// than most common buffer sizes.
		receiveUTF8Test("+", 4500);
	}

	/**
	 * Shared code for the UTF-8 tests.
	 * <p>
	 * Receive some UTF-8 content from a (mockup) process output stream.
	 * </p>
	 *
	 * @param prefix an arbitrary prefix inserted before the two byte UTF-8
	 *            characters. Used to move the other characters to specific
	 *            offsets e.g. a prefix of one byte will produce output where
	 *            every two byte character starts at an odd offset.
	 * @param numTwoByteCharacters number of two byte UTF-8 characters to output
	 */
	private void receiveUTF8Test(String prefix, int numTwoByteCharacters) throws Exception {
		final String s = prefix + String.join("", Collections.nCopies(numTwoByteCharacters, "\u00F8"));
		final ByteArrayInputStream stdout = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
		final Process mockProcess = new MockProcess(stdout, null, 0);
		final StreamsProxy streamProxy = new StreamsProxy(mockProcess, StandardCharsets.UTF_8, "");
		streamProxy.close();
		final String readContent = streamProxy.getOutputStreamMonitor().getContents();
		assertEquals("Process output got corrupted.", s, readContent);
	}
}
