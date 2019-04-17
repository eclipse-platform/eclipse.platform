/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *     William Mitsuda (wmitsuda@gmail.com) - Bug 153879 [Wizards] configurable size of cvs commit comment history
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.ui.repo;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class CommentHistoryContentHandler extends DefaultHandler {

	private StringBuffer buffer;
	private Vector comments;
	public CommentHistoryContentHandler() {
	}

	@Override
	public void characters(char[] chars, int startIndex, int length) throws SAXException {
		if (buffer == null) return;
		buffer.append(chars, startIndex, length);
	}

	@Override
	public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts)
			throws SAXException {
		
		String elementName = getElementName(namespaceURI, localName, qName);
		if (elementName.equals(RepositoryManager.ELEMENT_COMMIT_COMMENT)) {
			buffer = new StringBuffer();
			return;
		} 
		if (elementName.equals(RepositoryManager.ELEMENT_COMMIT_HISTORY)) {
			comments = new Vector(RepositoryManager.DEFAULT_MAX_COMMENTS);
			return;
		}
	}
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
		String elementName = getElementName(namespaceURI, localName, qName);
		if (elementName.equals(RepositoryManager.ELEMENT_COMMIT_COMMENT)) {
			comments.add(buffer.toString());
			buffer = null;
			return;
		} 
		if (elementName.equals(RepositoryManager.ELEMENT_COMMIT_HISTORY)) {
			RepositoryManager.previousComments = new String[comments.size()];
			comments.copyInto(RepositoryManager.previousComments);
			return;
		} 
	}
	
	/*
	 * Couldn't figure out from the SAX API exactly when localName vs. qName is used.
	 * However, the XML for project sets doesn't use namespaces so either of the two names
	 * is fine. Therefore, use whichever one is provided.
	 */
	private String getElementName(String namespaceURI, String localName, String qName) {
		if (localName != null && localName.length() > 0) {
			return localName;
		} else {
			return qName;
		}
	}
}
