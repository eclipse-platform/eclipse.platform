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
package org.eclipse.team.internal.ccvs.ui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * A simple XML writer.
 */
public class XMLWriter extends PrintWriter {
	protected int tab;

	/* constants */
	protected static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$

public XMLWriter(OutputStream output) {
	super(new OutputStreamWriter(output, StandardCharsets.UTF_8));
	tab = 0;
	println(XML_VERSION);
}
public void endTag(String name) {
	tab--;
	printTag('/' + name, null);
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
private void printTag(String name, HashMap parameters) {
	printTag(name, parameters, true, true);
}
private void printTag(String name, HashMap parameters, boolean tab, boolean newLine) {
	printTag(name, parameters, tab, newLine, false);
}
private void printTag(String name, HashMap parameters, boolean tab, boolean newLine, boolean end) {
	StringBuilder sb = new StringBuilder();
	sb.append("<"); //$NON-NLS-1$
	sb.append(name);
	if (parameters != null)
		for (Enumeration e = Collections.enumeration(parameters.keySet()); e.hasMoreElements();) {
			sb.append(" "); //$NON-NLS-1$
			String key = (String) e.nextElement();
			sb.append(key);
			sb.append("=\""); //$NON-NLS-1$
			sb.append(getEscaped(String.valueOf(parameters.get(key))));
			sb.append("\""); //$NON-NLS-1$
		}
	if (end)
		sb.append('/');
	sb.append(">"); //$NON-NLS-1$
	if (tab)
		printTabulation();
	if (newLine)
		println(sb.toString());
	else
		print(sb.toString());
}
public void startTag(String name, HashMap parameters) {
	startTag(name, parameters, true);
}
public void startTag(String name, HashMap parameters, boolean newLine) {
	printTag(name, parameters, true, newLine);
	tab++;
}
public void startAndEndTag(String name, HashMap parameters, boolean newLine) {
	printTag(name, parameters, true, true, true);
}
private static void appendEscapedChar(StringBuffer buffer, char c) {
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
	StringBuffer result = new StringBuffer(s.length() + 10);
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
