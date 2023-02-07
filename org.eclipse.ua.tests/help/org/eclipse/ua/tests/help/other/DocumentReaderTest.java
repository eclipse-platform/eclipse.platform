/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.util.ResourceLocator;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

public class DocumentReaderTest {
	private final static int RUNNING = 0;
	private final static int SUCCESS = 1;
	private final static int FAILURE = 2;

	private void readFile(DocumentReader docReader, String file) throws IOException, SAXException, ParserConfigurationException {
		String pluginId = FrameworkUtil.getBundle(getClass()).getSymbolicName();
		String locale = "en";
		try (InputStream in = ResourceLocator.openFromPlugin(pluginId, file, locale)) {
			docReader.read(in);
		}
	}

	@Test
	public void testDocumentReader() throws IOException, SAXException, ParserConfigurationException {
		DocumentReader docReader = new DocumentReader();
		ResourceReader[] resReaders = new ResourceReader[3];
		resReaders[0] = new ResourceReader(docReader,"data/help/index/assembler/a.xml");
		resReaders[1] = new ResourceReader(docReader,"data/help/index/assembler/b.xml");
		resReaders[2] = new ResourceReader(docReader,"data/help/index/assembler/c.xml");
		for (int i = 0; i < 3; i++) {
			resReaders[i].start();
		}
		int count = 0;
		for (int i = 0 ; i < 3; i++) {
			while (resReaders[i].status == RUNNING && count < 50) {
				try {
					Thread.sleep(100);
					count++;
				} catch (InterruptedException e) {
				}
			}
		}
		assertTrue(resReaders[0].status == SUCCESS);
		assertTrue(resReaders[1].status == SUCCESS);
		assertTrue(resReaders[2].status == SUCCESS);
	}

	private class ResourceReader extends Thread {

		private DocumentReader reader;
		private String file;
		ResourceReader(DocumentReader reader, String file) {
			this.reader = reader;
			this.file = file;
		}
		public int status = RUNNING;

		@Override
		public void run() {
			try {
				for (int i = 0; i < 10; i++) {
					readFile(reader, file);
				}
				status = SUCCESS;
			} catch (Exception e) {
				e.printStackTrace();
				status = FAILURE;
			}
		}
	}

}
