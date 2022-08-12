/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple XML writer.
 */
public class XMLWriter extends PrintWriter {
	protected int tab;
	protected String lineSeparator;

	/* constants */
	protected static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$

	public XMLWriter(OutputStream output, String separator) {
		super(new OutputStreamWriter(output, StandardCharsets.UTF_8));
		tab = 0;
		lineSeparator = separator;
		println(XML_VERSION);
	}

	public void endTag(String name) {
		tab--;
		printTag('/' + name, null);
	}

	@Override
	public void println(String x) {
		super.print(x);
		super.print(lineSeparator);
	}

	public void printSimpleTag(String name, Object value) {
		if (value != null) {
			printTag(name, null, true, false);
			print(getEscaped(String.valueOf(value)));
			printTag('/' + name, null, false, true);
		}
	}

	public void printTabulation() {
		for (int i = 0; i < tab; i++)
			super.print('\t');
	}

	public void printTag(String name, HashMap<String, Object> parameters) {
		printTag(name, parameters, true, true);
	}

	public void printTag(String name, HashMap<String, Object> parameters, boolean shouldTab, boolean newLine) {
		StringBuilder sb = new StringBuilder();
		sb.append("<"); //$NON-NLS-1$
		sb.append(name);
		if (parameters != null)
			for (Map.Entry<String, Object> entry : parameters.entrySet()) {
				sb.append(" "); //$NON-NLS-1$
				String key = entry.getKey();
				sb.append(key);
				sb.append("=\""); //$NON-NLS-1$
				sb.append(getEscaped(String.valueOf(entry.getValue())));
				sb.append("\""); //$NON-NLS-1$
			}
		sb.append(">"); //$NON-NLS-1$
		if (shouldTab)
			printTabulation();
		if (newLine)
			println(sb.toString());
		else
			print(sb.toString());
	}

	public void startTag(String name, HashMap<String, Object> parameters) {
		startTag(name, parameters, true);
	}

	public void startTag(String name, HashMap<String, Object> parameters, boolean newLine) {
		printTag(name, parameters, true, newLine);
		tab++;
	}

	private static void appendEscapedChar(StringBuilder buffer, char c) {
		String replacement = getReplacement(c);
		if (replacement != null) {
			buffer.append('&');
			buffer.append(replacement);
			buffer.append(';');
		} else {
			buffer.append(c);
		}
	}

	public static String getEscaped(String s) {
		StringBuilder result = new StringBuilder(s.length() + 10);
		for (int i = 0; i < s.length(); ++i)
			appendEscapedChar(result, s.charAt(i));
		return result.toString();
	}

	private static String getReplacement(char c) {
		// Encode special XML characters into the equivalent character references.
		// These five are defined by default for all XML documents.
		switch (c) {
			case '<' :
				return "lt"; //$NON-NLS-1$
			case '>' :
				return "gt"; //$NON-NLS-1$
			case '"' :
				return "quot"; //$NON-NLS-1$
			case '\'' :
				return "apos"; //$NON-NLS-1$
			case '&' :
				return "amp"; //$NON-NLS-1$
		}
		return null;
	}
}
