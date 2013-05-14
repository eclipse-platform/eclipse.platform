/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 181919 LineReader creating unneeded garbage
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

public class LineReader {

	/*
	 * Reads the contents from the given file and returns them as a List of
	 * lines.
	 */
	public static List load(IStorage file, boolean create) {
		List lines = null;
		if (!create && file != null && exists(file)) {
			// read current contents
			String charset = Utilities.getCharset(file);
			InputStream is = null;
			try {
				is = file.getContents();

				Reader streamReader = null;
				try {
					streamReader = new InputStreamReader(is, charset);
				} catch (UnsupportedEncodingException x) {
					// use default encoding
					streamReader = new InputStreamReader(is);
				}

				BufferedReader reader = new BufferedReader(streamReader);
				lines = readLines(reader);
			} catch (CoreException ex) {
				// TODO
				CompareUIPlugin.log(ex);
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (IOException ex) {
						// silently ignored
					}
			}
		}

		if (lines == null)
			lines = new ArrayList();
		return lines;
	}

	private static boolean exists(IStorage file) {
		if (file instanceof IFile) {
			return ((IFile) file).exists();
		}
		return true;
	}

	public static List readLines(BufferedReader reader) {
		List lines;
		LineReader lr = new LineReader(reader);
		if (!Platform.WS_CARBON.equals(Platform.getWS()))
			// Don't treat single CRs as line feeds to be consistent with command line patch
			lr.ignoreSingleCR(); 
		lines = lr.readLines();
		return lines;
	}

	/*
	 * Concatenates all strings found in the given List.
	 */
	public static String createString(boolean preserveLineDelimeters, List lines) {
		StringBuffer sb = new StringBuffer();
		Iterator iter = lines.iterator();
		if (preserveLineDelimeters) {
			while (iter.hasNext())
				sb.append((String) iter.next());
		} else {
			String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
			while (iter.hasNext()) {
				String line = (String) iter.next();
				int l = length(line);
				if (l < line.length()) { // line has delimiter
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
	 * Returns the length (excluding a line delimiter CR, LF, CR/LF) of the
	 * given string.
	 */
	/* package */static int length(String s) {
		int l = s.length();
		if (l > 0) {
			char c = s.charAt(l - 1);
			if (c == '\r')
				return l - 1;
			if (c == '\n') {
				if (l > 1 && s.charAt(l - 2) == '\r')
					return l - 2;
				return l - 1;
			}
		}
		return l;
	}

	private boolean fHaveChar = false;
	private int fLastChar;
	private boolean fSawEOF = false;
	private BufferedReader fReader;
	private boolean fIgnoreSingleCR = false;
	private StringBuffer fBuffer = new StringBuffer();

	public LineReader(BufferedReader reader) {
		fReader = reader;
		Assert.isNotNull(reader);
	}

	public void ignoreSingleCR() {
		fIgnoreSingleCR = true;
	}

	/**
	 * Reads a line of text. A line is considered to be terminated by any one of
	 * a line feed ('\n'), a carriage return ('\r'), or a carriage return
	 * followed immediately by a line-feed.
	 * 
	 * @return A string containing the contents of the line including the
	 *         line-termination characters, or <code>null</code> if the end of
	 *         the stream has been reached
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	/* package */String readLine() throws IOException {
		try {
			while (!fSawEOF) {
				int c = readChar();
				if (c == -1) {
					fSawEOF = true;
					break;
				}
				fBuffer.append((char) c);
				if (c == '\n')
					break;
				if (c == '\r') {
					c = readChar();
					if (c == -1) {
						fSawEOF = true;
						break; // EOF
					}
					if (c != '\n') {
						if (fIgnoreSingleCR) {
							fBuffer.append((char) c);
							continue;
						}
						fHaveChar = true;
						fLastChar = c;
					} else
						fBuffer.append((char) c);
					break;
				}
			}

			if (fBuffer.length() != 0) {
				return fBuffer.toString();
			}
			return null;
		} finally {
			fBuffer.setLength(0);
		}
	}

	/* package */void close() {
		try {
			fReader.close();
		} catch (IOException ex) {
			// silently ignored
		}
	}

	public List readLines() {
		try {
			List lines = new ArrayList();
			String line;
			while ((line = readLine()) != null)
				lines.add(line);
			return lines;
		} catch (IOException ex) {
			// NeedWork
			// System.out.println("error while reading file: " + fileName + "("
			// + ex + ")");
		} finally {
			close();
		}
		return null;
	}

	/*
	 * Returns the number of characters in the given string without counting a
	 * trailing line separator.
	 */
	/* package */int lineContentLength(String line) {
		if (line == null)
			return 0;
		int length = line.length();
		for (int i = length - 1; i >= 0; i--) {
			char c = line.charAt(i);
			if (c == '\n' || c == '\r')
				length--;
			else
				break;
		}
		return length;
	}

	// ---- private

	private int readChar() throws IOException {
		if (fHaveChar) {
			fHaveChar = false;
			return fLastChar;
		}
		return fReader.read();
	}
}
