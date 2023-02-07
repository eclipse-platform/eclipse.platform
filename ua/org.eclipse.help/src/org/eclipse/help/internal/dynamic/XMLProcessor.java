/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.dynamic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.help.internal.UAElement;
import org.xml.sax.SAXException;

/*
 * Processes XML input streams by converting to a DOM, calling all the handlers,
 * then converting back into an XML input stream.
 */
public class XMLProcessor {

	private DocumentProcessor processor;
	private DocumentReader reader;
	private DocumentWriter writer;

	/*
	 * Creates the processor, which will use the given handlers.
	 */
	public XMLProcessor(ProcessorHandler[] handlers) {
		this.processor = new DocumentProcessor(handlers);
	}

	/*
	 * Processes the given input stream with the supplied document id,
	 * and returns a new processed input stream.
	 */
	public InputStream process(InputStream in, String id, String charset) throws IOException, SAXException, ParserConfigurationException, TransformerException, TransformerConfigurationException {
		if (reader == null) {
			reader = new DocumentReader();
		}
		UAElement element = reader.read(in, charset);
		processor.process(element, id);
		if (writer == null) {
			writer = new DocumentWriter();
		}
		return new ByteArrayInputStream(writer.writeBytes(element, true));
	}
}
