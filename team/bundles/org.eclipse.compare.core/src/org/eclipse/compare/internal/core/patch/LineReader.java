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
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 181919 LineReader creating unneeded garbage
 *******************************************************************************/
package org.eclipse.compare.internal.core.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import org.eclipse.compare.internal.core.Messages;
import org.eclipse.compare.patch.ReaderCreator;
import org.eclipse.core.runtime.*;

public class LineReader {
	/**
	 * Reads the contents and returns them as a List of lines.
	 */
	public static List<String> load(ReaderCreator content, boolean create) {
		List<String> lines = null;
		if (!create && content != null && content.canCreateReader()) {
			// read current contents
			try (BufferedReader bufferedReader = new BufferedReader(content.createReader())) {
				lines = readLines(bufferedReader);
			} catch (CoreException ex) {
				ILog.of(LineReader.class).error(Messages.Activator_1, ex);
			} catch (IOException closeException) {
				// silently ignored
			}
		}

		if (lines == null)
			lines = new ArrayList<>();
		return lines;
	}

	public static List<String> readLines(BufferedReader reader) {
		List<String> lines;
		LineReader lr= new LineReader(reader);
		lr.ignoreSingleCR(); // Don't treat single CRs as line feeds to be consistent with command line patch
		lines= lr.readLines();
		return lines;
	}

	/*
	 * Concatenates all strings found in the given List.
	 */
	public static String createString(boolean preserveLineDelimeters, List<String> lines) {
		StringBuilder sb= new StringBuilder();
		Iterator<String> iter= lines.iterator();
		if (preserveLineDelimeters) {
			while (iter.hasNext())
				sb.append(iter.next());
		} else {
			String lineSeparator= System.getProperty("line.separator"); //$NON-NLS-1$
			while (iter.hasNext()) {
				String line= iter.next();
				int l= length(line);
				if (l < line.length()) {	// line has delimiter
					sb.append(line.substring(0, l));
					sb.append(lineSeparator);
				} else {
					sb.append(line);
				}
			}
		}
		return sb.toString();
	}

	/*
	 * Returns the length (excluding a line delimiter CR, LF, CR/LF)
	 * of the given string.
	 */
	static int length(String s) {
		int l= s.length();
		if (l > 0) {
			char c= s.charAt(l-1);
			if (c == '\r')
				return l-1;
			if (c == '\n') {
				if (l > 1 && s.charAt(l-2) == '\r')
					return l-2;
				return l-1;
			}
		}
		return l;
	}

	private boolean fHaveChar= false;
	private int fLastChar;
	private boolean fSawEOF= false;
	private BufferedReader fReader;
	private boolean fIgnoreSingleCR= false;
	private StringBuilder fBuffer= new StringBuilder();

	public LineReader(BufferedReader reader) {
		this.fReader= reader;
		Assert.isNotNull(reader);
	}

	public void ignoreSingleCR() {
		this.fIgnoreSingleCR= true;
	}

	/**
	 * Reads a line of text. A line is considered to be terminated by any one
	 * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
	 * followed immediately by a line-feed.
	 * @return A string containing the contents of the line including
	 *	the line-termination characters, or <code>null</code> if the end of the
	 *	stream has been reached
	 * @exception IOException If an I/O error occurs
	 */
	String readLine() throws IOException {
		try {
			while (!this.fSawEOF) {
				int c= readChar();
				if (c == -1) {
					this.fSawEOF= true;
					break;
				}
				this.fBuffer.append((char)c);
				if (c == '\n')
					break;
				if (c == '\r') {
					c= readChar();
					if (c == -1) {
						this.fSawEOF= true;
						break;	// EOF
					}
					if (c != '\n') {
						if (this.fIgnoreSingleCR) {
							this.fBuffer.append((char)c);
							continue;
						}
						this.fHaveChar= true;
						this.fLastChar= c;
					} else
						this.fBuffer.append((char)c);
					break;
				}
			}

			if (this.fBuffer.length() != 0) {
				return this.fBuffer.toString();
			}
			return null;
		} finally {
			this.fBuffer.setLength(0);
		}
	}

	void close() {
		try {
			this.fReader.close();
		} catch (IOException ex) {
			// silently ignored
		}
	}

	public List<String> readLines() {
		try {
			List<String> lines= new ArrayList<>();
			String line;
			while ((line= readLine()) != null)
				lines.add(line);
			return lines;
		} catch (IOException ex) {
			// NeedWork
			//System.out.println("error while reading file: " + fileName + "(" + ex + ")");
		} finally {
			close();
		}
		return null;
	}

	/*
	 * Returns the number of characters in the given string without
	 * counting a trailing line separator.
	 */
	int lineContentLength(String line) {
		if (line == null)
			return 0;
		int length= line.length();
		for (int i= length-1; i >= 0; i--) {
			char c= line.charAt(i);
			if (c =='\n' || c == '\r')
				length--;
			else
				break;
		}
		return length;
	}

	//---- private

	private int readChar() throws IOException {
		if (this.fHaveChar) {
			this.fHaveChar= false;
			return this.fLastChar;
		}
		return this.fReader.read();
	}
}
