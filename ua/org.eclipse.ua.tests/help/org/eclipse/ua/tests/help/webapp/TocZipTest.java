/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.server.WebappManager;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for reading from toc.zip
 */
public class TocZipTest {

	@Before
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
	}

	@Test
	public void testDocInZipOnly() throws IOException {
		final String path= "/org.eclipse.ua.tests/data/help/manual/dz1.html";
		String contents= readPage(path);
		assertTrue(contents.indexOf("dz1 from doc.zip") > -1);
	}

	/**
	 * Verify that loose files override those in doc.zip
	 * @throws IOException
	 */
	@Test
	public void testDocInZipAndBundle() throws IOException {
		final String path = "/org.eclipse.ua.tests/data/help/manual/dz2.html";
		 String contents  = readPage(path);
		 assertFalse(contents.indexOf("dz2 from doc.zip") > -1);
		 assertTrue(contents.indexOf("dz2 from bundle") > -1);
	}

	private String readPage(final String path) throws MalformedURLException,
			IOException {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/topic" + path);
		try (InputStream is = url.openStream();
				BufferedInputStream buffered = new BufferedInputStream(is);
				ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			int result = buffered.read();
			while (result != -1) {
				os.write(result);
				result = buffered.read();
			}
			return os.toString();
		}
	}

}
