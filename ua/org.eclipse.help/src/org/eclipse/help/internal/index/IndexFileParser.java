/*******************************************************************************
 * Copyright (c) 2005, 2016 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *     IBM Corporation - Use IndexDocumentReader
 *     IBM Corporation - [Bug 297921
 *******************************************************************************/
package org.eclipse.help.internal.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.dynamic.DocumentReader;
import org.xml.sax.SAXException;

public class IndexFileParser {

	private DocumentReader reader;

	public IndexContribution parse(IndexFile indexFile) throws IOException, SAXException, ParserConfigurationException {
		if (reader == null) {
			reader = new IndexDocumentReader();
		}
		try (InputStream in = indexFile.getInputStream()) {
			if (in != null) {
				Index index = (Index) reader.read(in);
				IndexContribution contrib = new IndexContribution();
				contrib.setId('/' + indexFile.getPluginId() + '/' + indexFile.getFile());
				contrib.setIndex(index);
				contrib.setLocale(indexFile.getLocale());
				return contrib;
			} else {
				throw new FileNotFoundException();
			}
		}
	}
}
