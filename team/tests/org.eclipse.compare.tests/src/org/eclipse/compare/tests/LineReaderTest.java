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

import static org.assertj.core.api.Assertions.assertThat;

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
	public void testReadEmpty() throws IOException {
		try (BufferedReader reader = getReader("empty.txt")) {
			LineReader lr = new LineReader(reader);
			List<String> inLines = lr.readLines();
			assertThat(inLines).isEmpty();
		}
	}

	@Test
	public void testReadNormal() throws IOException {
		try (BufferedReader reader = getReader("normal.txt")) {
			LineReader lr = new LineReader(reader);
		List<String> inLines= lr.readLines();
		assertThat(inLines).hasSize(3).satisfiesExactlyInAnyOrder(
				first -> assertThat(convertLineDelimeters(first)).isEqualTo("[1]\n"),
				second -> assertThat(convertLineDelimeters(second)).isEqualTo("[2]\n"),
				third -> assertThat(convertLineDelimeters(third)).isEqualTo("[3]\n"));
		}
	}

	private String convertLineDelimeters(Object object) {
		String line = (String)object;
		if (line.endsWith("\r\n"))
			return line.substring(0, line.length() - 2) + "\n";
		return line;
	}

	@Test
	public void testReadUnterminatedLastLine() throws IOException {
		try (BufferedReader reader = getReader("unterminated.txt")) {
			LineReader lr = new LineReader(reader);
		List<String> inLines= lr.readLines();
		assertThat(inLines).hasSize(3).satisfiesExactlyInAnyOrder(
				first -> assertThat(convertLineDelimeters(first)).isEqualTo("[1]\n"),
				second -> assertThat(convertLineDelimeters(second)).isEqualTo("[2]\n"),
				third -> assertThat(third).isEqualTo("[3]"));
		}
	}

	private BufferedReader getReader(String name) throws IOException {
		IPath path = IPath.fromOSString("linereaderdata/" + name);
		URL url = new URL(CompareTestPlugin.getDefault().getBundle().getEntry("/"), path.toString());
		InputStream resourceAsStream = url.openStream();
		InputStreamReader reader2 = new InputStreamReader(resourceAsStream);
		return new BufferedReader(reader2);
	}
}
