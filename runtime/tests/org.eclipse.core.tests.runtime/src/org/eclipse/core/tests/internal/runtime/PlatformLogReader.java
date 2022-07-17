/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.core.tests.internal.runtime;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.eclipse.core.internal.runtime.Messages;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * Reads a structured log from disk and reconstructs status and exception objects.
 * General strategy: log entries that are malformed in any way are skipped, and an extra
 * status is returned mentioned that there were problems.
 */
@SuppressWarnings("restriction")
public class PlatformLogReader {
	private ArrayList<IStatus> list = null;
	private String currentLine = "";
	private BufferedReader reader;

	// constants copied from the PlatformLogWriter (since they are private
	// to that class and this class should be used only in test suites)
	private static final String KEYWORD_SESSION = "!SESSION";
	private static final String KEYWORD_ENTRY = "!ENTRY";
	private static final String KEYWORD_SUBENTRY = "!SUBENTRY";
	private static final String KEYWORD_MESSAGE = "!MESSAGE";
	private static final String KEYWORD_STACK = "!STACK";

	private static final int NULL = -2;
	private static final int SESSION = 1;
	private static final int ENTRY = 2;
	private static final int SUBENTRY = 4;
	private static final int MESSAGE = 8;
	private static final int STACK = 16;
	private static final int UNKNOWN = 32;

	protected void log(Exception ex) {
		String msg = NLS.bind(Messages.meta_exceptionParsingLog, ex.getMessage());
		list.add(new Status(IStatus.WARNING, Platform.PI_RUNTIME, Platform.PARSE_PROBLEM, msg, ex));
	}

	protected Throwable readException(String message) throws IOException {
		if (currentLine == null || getLineType() != STACK) {
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(currentLine);
		tokenizer.nextToken();
		// FIXME: handle the code to read status objects which were
		// nested inside of CoreExceptions in the log.
		Integer.parseInt(tokenizer.nextToken());
		currentLine = reader.readLine();
		return new FakeException(null, readText());
	}

	/**
	 * Reads the given log file and returns the contained status objects.
	 * If the log file could not be read, a status object indicating this fact
	 * is returned.
	 */
	public synchronized IStatus[] readLogFile(String path) {
		list = new ArrayList<>();
		InputStream input = null;
		try {
			input = new FileInputStream(path);
			reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));//$NON-NLS-1$
			currentLine = reader.readLine();
			while (currentLine != null) {
				switch (getLineType()) {
					case ENTRY :
						IStatus status = readEntry();
						if (status != null) {
							list.add(status);
						}
						break;
					case SESSION :
						readSession();
						break;
					case SUBENTRY :
					case MESSAGE :
					case STACK :
					case UNKNOWN :
						currentLine = reader.readLine();
						break;
				}
			}
		} catch (IOException e) {
			log(e);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				log(e);
			}
		}
		return list.toArray(new IStatus[list.size()]);
	}

	protected int getLineType() {
		if (currentLine == null) {
			return NULL;
		}
		StringTokenizer tokenizer = new StringTokenizer(currentLine);
		if (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals(KEYWORD_SESSION)) {
				return SESSION;
			}
			if (token.equals(KEYWORD_ENTRY)) {
				return ENTRY;
			}
			if (token.equals(KEYWORD_SUBENTRY)) {
				return SUBENTRY;
			}
			if (token.equals(KEYWORD_MESSAGE)) {
				return MESSAGE;
			}
			if (token.equals(KEYWORD_STACK)) {
				return STACK;
			}
		}
		return UNKNOWN;
	}

	/**
	 * A reconsituted exception that only contains a stack trace and a message.
	 */
	static class FakeException extends Throwable {
		/**
		 * All serializable objects should have a stable serialVersionUID
		 */
		private static final long serialVersionUID = 1L;
		private String message;
		private String stackTrace;

		FakeException(String msg, String stack) {
			this.message = msg;
			this.stackTrace = stack;
		}

		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public void printStackTrace() {
			printStackTrace(System.out);
		}

		@Override
		public void printStackTrace(PrintWriter writer) {
			writer.println(stackTrace);
		}

		@Override
		public void printStackTrace(PrintStream stream) {
			stream.println(stackTrace);
		}
	}

	protected IStatus readEntry() throws IOException {
		if (currentLine == null || getLineType() != ENTRY) {
			return null;
		}
		StringTokenizer tokens = new StringTokenizer(currentLine);
		// skip over the ENTRY keyword
		tokens.nextToken();
		String pluginID = tokens.nextToken();
		int severity = Integer.parseInt(tokens.nextToken());
		int code = Integer.parseInt(tokens.nextToken());
		// ignore the rest of the line since its the date
		currentLine = reader.readLine();
		String message = readMessage();
		Throwable exception = readException(message);
		if (currentLine == null || getLineType() != SUBENTRY) {
			return new Status(severity, pluginID, code, message, exception);
		}
		MultiStatus parent = new MultiStatus(pluginID, code, message, exception);
		readSubEntries(parent);
		return parent;
	}

	protected void readSubEntries(MultiStatus parent) throws IOException {
		while (getLineType() == SUBENTRY) {
			StringTokenizer tokens = new StringTokenizer(currentLine);
			// skip over the subentry keyword
			tokens.nextToken();
			int currentDepth = Integer.parseInt(tokens.nextToken());
			String pluginID = tokens.nextToken();
			int severity = Integer.parseInt(tokens.nextToken());
			int code = Integer.parseInt(tokens.nextToken());
			// ignore the rest of the line since its the date
			currentLine = reader.readLine();
			String message = readMessage();
			Throwable exception = readException(message);

			IStatus current = new Status(severity, pluginID, code, message, exception);
			if (currentLine == null || getLineType() != SUBENTRY) {
				parent.add(current);
				return;
			}

			tokens = new StringTokenizer(currentLine);
			tokens.nextToken();
			int depth = Integer.parseInt(tokens.nextToken());
			if (currentDepth == depth) {
				// next sub-entry is a sibling
				parent.add(current);
			} else if (currentDepth == (depth - 1)) {
				// next sub-entry is a child
				current = new MultiStatus(pluginID, code, message, exception);
				readSubEntries((MultiStatus) current);
				parent.add(current);
			} else {
				parent.add(current);
				return;
			}
		}
	}

	protected int readDepth() {
		StringTokenizer tokens = new StringTokenizer(currentLine);
		// skip the keyword
		tokens.nextToken();
		return Integer.parseInt(tokens.nextToken());
	}

	protected String readMessage() throws IOException {
		if (currentLine == null || getLineType() != MESSAGE) {
			return "";
		}
		StringBuilder buffer = new StringBuilder();
		buffer.append(currentLine.substring(KEYWORD_MESSAGE.length() + 1));
		currentLine = reader.readLine();
		buffer.append(readText());
		return buffer.toString();
	}

	protected String readSession() throws IOException {
		if (currentLine == null || getLineType() != SESSION) {
			return "";
		}
		currentLine = reader.readLine();
		return readText();
	}

	protected String readText() throws IOException {
		StringBuilder buffer = new StringBuilder();
		if (currentLine == null || getLineType() != UNKNOWN) {
			return "";
		}
		buffer.append(currentLine);
		boolean done = false;
		while (!done) {
			currentLine = reader.readLine();
			if (currentLine == null) {
				done = true;
				continue;
			}
			if (getLineType() == UNKNOWN) {
				// preserves line terminators between lines
				buffer.append('\n');
				buffer.append(currentLine);
			} else {
				done = true;
			}
		}
		return buffer.toString();
	}
}
