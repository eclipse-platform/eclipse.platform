/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import org.eclipse.compare.internal.core.patch.LineReader;
import org.eclipse.core.runtime.IPath;
import org.junit.Test;

public class LineReaderTest  {

	@Test
	public void testReadEmpty() {
		LineReader lr= new LineReader(getReader("empty.txt")); //$NON-NLS-1$
		List<String> inLines= lr.readLines();
		assertEquals(0, inLines.size());
	}

	@Test
	public void testReadNormal() {
		LineReader lr= new LineReader(getReader("normal.txt")); //$NON-NLS-1$
		List<String> inLines= lr.readLines();
		assertEquals(3, inLines.size());
		assertEquals("[1]\n", convertLineDelimeters(inLines.get(0))); //$NON-NLS-1$
		assertEquals("[2]\n", convertLineDelimeters(inLines.get(1))); //$NON-NLS-1$
		assertEquals("[3]\n", convertLineDelimeters(inLines.get(2))); //$NON-NLS-1$
	}

	private String convertLineDelimeters(Object object) {
		String line = (String)object;
		if (line.endsWith("\r\n"))
			return line.substring(0, line.length() - 2) + "\n";
		return line;
	}

	@Test
	public void testReadUnterminatedLastLine() {
		LineReader lr= new LineReader(getReader("unterminated.txt")); //$NON-NLS-1$
		List<String> inLines= lr.readLines();
		assertEquals(3, inLines.size());
		assertEquals("[1]\n", convertLineDelimeters(inLines.get(0))); //$NON-NLS-1$
		assertEquals("[2]\n", convertLineDelimeters(inLines.get(1))); //$NON-NLS-1$
		assertEquals("[3]", inLines.get(2)); //$NON-NLS-1$
	}

	private BufferedReader getReader(String name) {
		IPath path = IPath.fromOSString("linereaderdata/" + name);
		URL url;
		try {
			url = new URL(CompareTestPlugin.getDefault().getBundle().getEntry("/"), path.toString());
			InputStream resourceAsStream = url.openStream();
			InputStreamReader reader2 = new InputStreamReader(resourceAsStream);
			return new BufferedReader(reader2);
		} catch ( IOException e) {
			fail(e.getMessage());
		}
		return null;
	}
}
