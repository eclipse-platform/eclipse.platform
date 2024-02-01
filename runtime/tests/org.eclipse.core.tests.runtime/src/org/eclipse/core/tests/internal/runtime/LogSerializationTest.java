/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class LogSerializationTest {

	static class TestException extends Exception {
		private static final long serialVersionUID = 1L;

		TestException() {
			super();
		}
	}

	private File logFile = null;

	private void assertStatusEqual(String msg, IStatus[] expected, IStatus[] actual) {
		if (expected == null) {
			assertNull(msg + " expected null but got: " + Arrays.toString(actual), actual);
			return;
		}
		if (actual == null) {
			assertNull(msg + " expected " + Arrays.toString(expected) + " but got null", expected);
		}
		assertThat(actual).as(msg + " number of statuses").hasSameSizeAs(expected);
		for (int i = 0, imax = expected.length; i < imax; i++) {
			assertStatusEquals(msg + " differ at status " + i, expected[i], actual[i]);
		}
	}

	private void assertStatusEquals(String msg, IStatus expected, IStatus actual) {
		assertEquals(msg + " severity", expected.getSeverity(), actual.getSeverity());
		assertEquals(msg + " plugin-id", expected.getPlugin(), actual.getPlugin());
		assertEquals(msg + " code", expected.getCode(), actual.getCode());
		assertEquals(msg + " message", expected.getMessage(), actual.getMessage());
		assertExceptionEquals(msg + " exception", expected.getException(), actual.getException());
		assertStatusEqual(msg + " children", expected.getChildren(), actual.getChildren());
	}

	private void assertExceptionEquals(String msg, Throwable expected, Throwable actual) {
		if (expected == null) {
			assertNull(msg + " expected null but got: " + actual, actual);
			return;
		}
		if (actual == null) {
			assertNull(msg + " expected " + expected + " but got null", expected);
		}
		assertEquals(msg + " stack trace", encodeStackTrace(expected), encodeStackTrace(actual));
		assertEquals(msg + " message", expected.getMessage(), actual.getMessage());
	}

	protected String encodeStackTrace(Throwable t) {
		StringWriter sWriter = new StringWriter();
		PrintWriter pWriter = new PrintWriter(sWriter);
		pWriter.println();
		t.printStackTrace(pWriter);
		pWriter.flush();
		return canonicalizeStackTrace(sWriter.toString());
	}

	/**
	 * Returns the given stack trace in a canonical format in order to make stack
	 * trace comparisons easier. The canonical format is: each line is ended by a
	 * <code>'\n'</code> character, each line (except the first one) starts with a
	 * <code>'\t'</code> character, there are no other occurrences of space
	 * characters other than ' ', and there are no consecutive occurrences of new-
	 * line or space characters.
	 */
	protected String canonicalizeStackTrace(String stackTrace) {
		final char NEW_LINE = '\n';
		final char TAB = '\t';
		final char SPACE = ' ';
		final String LINE_SEPARATORS = "\r\n\f";
		final String SPACES = "\t ";
		StringBuilder sb = new StringBuilder(stackTrace.trim());
		sb.append(NEW_LINE);
		char lastChar = 0;
		for (int i = 0; i < sb.length();) {
			// only \n is used as line separator, with no consecutive occurrences
			if (LINE_SEPARATORS.indexOf(sb.charAt(i)) != -1) {
				if (LINE_SEPARATORS.indexOf(lastChar) != -1) {
					sb.deleteCharAt(i);
					continue;
				}
				sb.setCharAt(i, NEW_LINE);
			} else if (lastChar == NEW_LINE) {
				// each line (except the first one) starts with a tab
				sb.insert(i, TAB);
			} else if (SPACES.indexOf(sb.charAt(i)) != -1) {
				// only ' ' is used as space, with no consecutive occurrences
				if (SPACES.indexOf(lastChar) != -1) {
					sb.deleteCharAt(i);
					continue;
				}
				sb.setCharAt(i, SPACE);
			}
			lastChar = sb.charAt(i);
			i++;
		}
		return sb.toString();
	}

	protected IStatus[] getInterestingMultiStatuses() {
		IStatus[] interesting = getInterestingStatuses();
		int len = interesting.length;
		IStatus[][] interestingChildren = new IStatus[len][];
		for (int i = 0; i < len; i++) {
			IStatus[] subArray = new IStatus[len];
			System.arraycopy(interesting, 0, subArray, 0, len);
			interestingChildren[i] = subArray;
		}
		int childOff = 0;
		return new IStatus[] {new MultiStatus("plugin-id", 1, interestingChildren[childOff++ % len], "message", null), new MultiStatus("org.foo.bar", 5, interestingChildren[childOff++ % len], "message", new NullPointerException()), new MultiStatus("plugin-id", 8, interestingChildren[childOff++ % len], "message", null), new MultiStatus("plugin-id", 0, interestingChildren[childOff++ % len], "message", new IllegalStateException()), new MultiStatus("plugin-id", 65756, interestingChildren[childOff++ % len], "message", null), new MultiStatus(".", 1, interestingChildren[childOff++ % len], "message", null), new MultiStatus("org.foo.blaz", 1, interestingChildren[childOff++ % len], "", null), new MultiStatus("plugin-id", 1, interestingChildren[childOff++ % len], "%$(% 98%(%(*^", null),
				new MultiStatus("plugin-id", 1, "message", null), new MultiStatus("..", 87326, "", null),};
	}

	protected IStatus[] getInterestingStatuses() {
		return new IStatus[] {new Status(IStatus.WARNING, "(#(*$%#", 1, "../\\\\\'\'\"", new TestException()), //
				new Status(IStatus.WARNING, "org.foo", 1, "This is the message", null), //
				new Status(IStatus.ERROR, "org.foo", 1, "This is the message", new TestException()), //
				new Status(IStatus.OK, ".", 1, "This is the message", new TestException()), //
				new Status(IStatus.INFO, "org.asdfhsfhsdf976dsf6sd0f6s", 1, "#*&^$(*&#@^$)(#&)(", null),};
	}

	protected void doTest(String msg, IStatus[] oldStats) {
		writeLog(oldStats);
		IStatus[] newStats = readLog();
		assertStatusEqual(msg, oldStats, newStats);
	}

	protected void doTest(String msg, IStatus status) {
		doTest(msg, new IStatus[] {status});
	}

	protected IStatus[] readLog() {
		PlatformLogReader reader = new PlatformLogReader();
		return reader.readLogFile(logFile.getAbsolutePath());
	}

	@Before
	public void setUp() throws Exception {
		//setup the log file
		if (logFile == null) {
			logFile = Platform.getLogFileLocation().toFile();
		}
	}

	@After
	public void tearDown() throws Exception {
		logFile.delete();
	}

	@Test
	public void testDeepMultiStatus() {
		MultiStatus multi = new MultiStatus("id", 1, getInterestingMultiStatuses(), "ok", null);
		for (int i = 0; i < 5; i++) {
			multi = new MultiStatus("id", 1, new IStatus[] {multi}, "ok", null);
			doTest("1." + i, multi);
		}
	}

	@Test
	public void testMultiMultiStatusSerialize() {
		IStatus[] interesting = getInterestingMultiStatuses();
		int len = interesting.length;
		for (int i = 1; i < len; i++) {
			IStatus[] subArray = new IStatus[len];
			System.arraycopy(interesting, 0, subArray, 0, len);
			doTest("1." + i, subArray);
		}
	}

	@Test
	public void testMultiSerialize() {
		IStatus[] interesting = getInterestingStatuses();
		int len = interesting.length;
		for (int i = 1; i < len; i++) {
			IStatus[] subArray = new IStatus[len];
			System.arraycopy(interesting, 0, subArray, 0, len);
			doTest("1." + i, subArray);
		}
	}

	@Test
	public void testMultiStatus() {
		IStatus[] interesting = getInterestingMultiStatuses();
		for (int i = 0; i < interesting.length; i++) {
			doTest("1." + i, interesting[i]);
		}
	}

	@Test
	public void testSimpleSerialize() {
		IStatus[] interesting = getInterestingStatuses();
		for (int i = 0; i < interesting.length; i++) {
			doTest("1." + i, interesting[i]);
		}
	}

	protected void writeLog(IStatus status) {
		writeLog(new IStatus[] {status});
	}

	protected void writeLog(IStatus[] statuses) {
		if (logFile.exists()) {
			logFile.delete();
		}
		for (IStatus status : statuses) {
			RuntimeLog.log(status);
		}
	}
}
