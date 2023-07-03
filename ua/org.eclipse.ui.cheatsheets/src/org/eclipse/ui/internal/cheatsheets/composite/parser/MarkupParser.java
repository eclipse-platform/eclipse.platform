/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.parser;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MarkupParser {

	public static String parseAndTrimTextMarkup(Node parentNode) {
		return parseMarkup(parentNode).trim();
	}

	private static String parseMarkup(Node parentNode) {
		NodeList children = parentNode.getChildNodes();
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < children.getLength(); i++) {
			Node childNode = children.item(i);
			if (childNode.getNodeType() == Node.TEXT_NODE) {
				text.append(escapeText(childNode.getNodeValue()));
			} else if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				text.append('<');
				text.append(childNode.getNodeName());
				// Add the attributes
				NamedNodeMap attributes = childNode.getAttributes();
				if (attributes != null) {
					for (int x = 0; x < attributes.getLength(); x++) {
						Node attribute = attributes.item(x);
						String attributeName = attribute.getNodeName();
						if (attributeName == null)
							continue;
						text.append(' ');
						text.append(attributeName);
						text.append(" = \""); //$NON-NLS-1$
						text.append(attribute.getNodeValue());
						text.append('"');
					}
				}
				text.append('>');
				text.append(parseMarkup(childNode));
				text.append("</"); //$NON-NLS-1$
				text.append(childNode.getNodeName());
				text.append('>');
			}
		}
		return text.toString();
	}

	public static String escapeText(String input) {
		StringBuilder result = new StringBuilder(input.length() + 10);
		for (int i = 0; i < input.length(); ++i)
			appendEscapedChar(result, input.charAt(i));
		return result.toString();
	}

	private static void appendEscapedChar(StringBuilder buffer, char c) {
		String replacement = getReplacement(c);
		if (replacement != null) {
			buffer.append(replacement);
		} else {
			buffer.append(c);
		}
	}

	private static String getReplacement(char c) {
		// Encode characters which need to be escaped for use in form text
		// Replace tabs with spaces
		switch (c) {
			case '<' :
				return "&lt;"; //$NON-NLS-1$
			case '>' :
				return "&gt;"; //$NON-NLS-1$
			case '&' :
				return "&amp;"; //$NON-NLS-1$
			case '\t' :
				return " "; //$NON-NLS-1$
		}
		return null;
	}

	/*
	 * Add paragraph tags if not already present
	 */
	public static String createParagraph(String text, String imageTag) {
		String result = ""; //$NON-NLS-1$
		String trimmed = text.trim();
		boolean addParagraphTags = trimmed.length() < 3 || trimmed.charAt(0)!='<' ||
		  (trimmed.charAt(1)!='p' && trimmed.charAt(1) != 'l');
		if (addParagraphTags) {
			result +=  "<p>"; //$NON-NLS-1$
		}

		if (imageTag != null) {
			result += "<img href=\""; //$NON-NLS-1$
			result += imageTag;
			result += "\"/> "; //$NON-NLS-1$
		}

		result += trimmed;

		if (addParagraphTags) {
			result += "</p>"; //$NON-NLS-1$
		}
		return result;
	}

}
