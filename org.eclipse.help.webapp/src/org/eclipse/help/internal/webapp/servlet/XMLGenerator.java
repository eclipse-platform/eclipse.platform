/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.webapp.servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.internal.base.util.TString;

/**
 * Helper class to generate xml files.
 */
public class XMLGenerator {
	private File outFile = null;

	private PrintWriter out = null;

	public int pad = 0;

	// XML escaped characters mapping
	private static final String invalidXML[] = { "&", ">", "<", "\"", "\'" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private static final String escapedXML[] = {
			"&amp;", "&gt;", "&lt;", "&quot;", "&apos;" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	/**
	 * Constructor.
	 */
	public XMLGenerator(Writer writer) {
		if (writer instanceof PrintWriter printWriter)
			this.out = printWriter;
		else
			this.out = new PrintWriter(writer);
	}

	/**
	 * Constructor.
	 */
	public XMLGenerator(File outFile) {
		super();
		this.outFile = outFile;
		try {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outFile), StandardCharsets.UTF_8)),
					false /* no aotoFlush */
			);
			println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		} catch (IOException ioe) {
			ILog.of(getClass()).error("Error accessing file: " + outFile.getAbsolutePath() + "", ioe); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	// returns a String that is a valid XML string
	// by XML escaping special characters
	public static String xmlEscape(String cdata) {
		for (int i = 0; i < invalidXML.length; i++)
			cdata = TString.change(cdata, invalidXML[i], escapedXML[i]);
		return cdata;
	}

	public void close() {
		out.flush();
		out.close();
		if (out.checkError()) {
			if (outFile != null) {
				ILog.of(getClass()).error("Errors occurred generating file: " + outFile.getAbsolutePath() + "", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		out = null;
	}

	public void print(Object o) {
		if (out != null)
			out.print(o);
	}

	public void println(Object o) {
		print(o);
		print("\n"); //$NON-NLS-1$
	}

	public void printPad() {
		for (int i = 0; i < pad; i++)
			print(" "); //$NON-NLS-1$
	}
}
