/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;
import org.xml.sax.SAXException;

/*
 * Resolves includes by parsing the target document, extracting the node and
 * replacing the original include with it.
 */
public class IncludeResolver {

	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	private DocumentProcessor processor;
	private DocumentReader reader;
	private String locale;

	public IncludeResolver(DocumentProcessor processor, DocumentReader reader, String locale) {
		this.processor = processor;
		this.reader = reader;
		this.locale = locale;
	}

	/*
	 * Resolves the include target to a processed Element.
	 */
	public UAElement resolve(String bundleId, String relativePath, String elementId) throws IOException, SAXException, ParserConfigurationException {
		String href = '/' + bundleId + '/' + relativePath;
		try (InputStream in = HelpSystem.getHelpContent(href, locale)) {
			UAElement element = findElement(in, elementId);
			processor.process(element, href);
			return element;
		}
	}

	/*
	 * Finds the specified element from the given XML input stream.
	 */
	private UAElement findElement(InputStream in, String elementId) throws IOException, SAXException, ParserConfigurationException {
		UAElement element = reader.read(in);
		return findElement(element, elementId);
	}

	/*
	 * Finds the specified element under the given subtree.
	 */
	private UAElement findElement(UAElement element, String elementId) {
		String id = element.getAttribute(ATTRIBUTE_ID);
		if (id != null && id.equals(elementId)) {
			return element;
		}
		IUAElement[] children = element.getChildren();
		for (int i=0;i<children.length;++i) {
			UAElement result = findElement((UAElement)children[i], elementId);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
}
