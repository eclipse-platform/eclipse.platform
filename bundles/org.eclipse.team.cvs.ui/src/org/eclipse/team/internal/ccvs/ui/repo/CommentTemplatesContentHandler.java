/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

package org.eclipse.team.internal.ccvs.ui.repo;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class CommentTemplatesContentHandler extends DefaultHandler {

	private StringBuffer buffer;
	private Vector comments;
	public CommentTemplatesContentHandler() {
	}

	@Override
	public void characters(char[] chars, int startIndex, int length) {
		if (buffer == null) return;
		buffer.append(chars, startIndex, length);
	}

	@Override
	public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts) {
		
		String elementName = getElementName(localName, qName);
		if (elementName.equals(RepositoryManager.ELEMENT_COMMIT_COMMENT)) {
			buffer = new StringBuffer();
			return;
		} 
		if (elementName.equals(RepositoryManager.ELEMENT_COMMENT_TEMPLATES)) {
			comments = new Vector();
			return;
		}
	}
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
		String elementName = getElementName(localName, qName);
		if (elementName.equals(RepositoryManager.ELEMENT_COMMIT_COMMENT)) {
			comments.add(buffer.toString());
			buffer = null;
			return;
		} 
		if (elementName.equals(RepositoryManager.ELEMENT_COMMENT_TEMPLATES)) {
			RepositoryManager.commentTemplates = new String[comments.size()];
			comments.copyInto(RepositoryManager.commentTemplates);
			return;
		} 
	}
	
	/*
	 * Couldn't figure out from the SAX API exactly when localName vs. qName is used.
	 * However, the XML for project sets doesn't use namespaces so either of the two names
	 * is fine. Therefore, use whichever one is provided.
	 */
	private String getElementName(String localName, String qName) {
		if (localName != null && localName.length() > 0) {
			return localName;
		}
		return qName;
	}
}
