/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.team.internal.core.streams.CRLFtoLFInputStream;
import org.eclipse.team.internal.core.streams.LFtoCRLFInputStream;

public class StreamTests extends TestCase {

	public StreamTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(StreamTests.class);
	}

	public void testCRLFtoLFInputStream() throws IOException {
		testCRLFtoLFTranslation("", "");
		testCRLFtoLFTranslation("a", "a");
		testCRLFtoLFTranslation("abc", "abc");
		testCRLFtoLFTranslation("\n", "\n");
		testCRLFtoLFTranslation("\r", "\r");
		testCRLFtoLFTranslation("\r\n", "\n");
		testCRLFtoLFTranslation("x\r\r\n\rx", "x\r\n\rx");
		testCRLFtoLFTranslation("The \r\n quick brown \n fox \r\n\n\r\r\n jumped \n\n over \r\n the \n lazy dog.\r\n",
			"The \n quick brown \n fox \n\n\r\n jumped \n\n over \n the \n lazy dog.\n");
	}

	private void testCRLFtoLFTranslation(String pre, String post) throws IOException {
		ByteArrayInputStream bin = new ByteArrayInputStream(pre.getBytes());
		InputStream in = new CRLFtoLFInputStream(bin);
		InputStream inExpected = new ByteArrayInputStream(post.getBytes());
		assertStreamEquals(inExpected, in);
	}

	public void testLFtoCRLFInputStream() throws IOException {
		testLFtoCRLFTranslation("", "");
		testLFtoCRLFTranslation("a", "a");
		testLFtoCRLFTranslation("abc", "abc");
		testLFtoCRLFTranslation("\n", "\r\n");
		testLFtoCRLFTranslation("\r", "\r");
		testLFtoCRLFTranslation("\r\n", "\r\r\n");
		testLFtoCRLFTranslation("x\r\r\n\rx", "x\r\r\r\n\rx");
		testLFtoCRLFTranslation("The \r\n quick brown \n fox \r\n\n\r\r\n jumped \n\n over \r\n the \n lazy dog.\r\n",
			"The \r\r\n quick brown \r\n fox \r\r\n\r\n\r\r\r\n jumped \r\n\r\n over \r\r\n the \r\n lazy dog.\r\r\n");
	}

	private void testLFtoCRLFTranslation(String pre, String post) throws IOException {
		ByteArrayInputStream bin = new ByteArrayInputStream(pre.getBytes());
		InputStream in = new LFtoCRLFInputStream(bin);
		InputStream inExpected = new ByteArrayInputStream(post.getBytes());
		assertStreamEquals(inExpected, in);
	}

	private void assertStreamEquals(InputStream in1, InputStream in2) throws IOException {
		try {
			for (;;) {
				int byte1 = in1.read();
				int byte2 = in2.read();
				assertEquals("Streams not equal", byte1, byte2);
				if (byte1 == -1) break;
			}
		} finally {
			in1.close();
			in2.close();
		}
	}
}
