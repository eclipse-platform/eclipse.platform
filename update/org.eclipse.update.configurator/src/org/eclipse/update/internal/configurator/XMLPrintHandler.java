/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

package org.eclipse.update.internal.configurator;

import java.io.*;

import org.w3c.dom.*;


public class XMLPrintHandler {
	//	used to print XML file
	public static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\""; //$NON-NLS-1$
	public static final String XML_HEAD_END_TAG = "?>"; //$NON-NLS-1$
	public static final String XML_DBL_QUOTES = "\""; //$NON-NLS-1$
	public static final String XML_SPACE = " "; //$NON-NLS-1$
	public static final String XML_BEGIN_TAG = "<"; //$NON-NLS-1$
	public static final String XML_END_TAG = ">"; //$NON-NLS-1$
	public static final String XML_EQUAL = "="; //$NON-NLS-1$
	public static final String XML_SLASH = "/"; //$NON-NLS-1$

	public static void printBeginElement(Writer xmlWriter, String elementString) throws IOException{
		StringBuilder temp = new StringBuilder(XML_BEGIN_TAG);
		temp.append(elementString).append(XML_END_TAG).append("\n"); //$NON-NLS-1$
		xmlWriter.write(temp.toString());

	}

	public static void printEndElement(Writer xmlWriter, String elementString) throws IOException{
		StringBuilder temp = new StringBuilder(XML_BEGIN_TAG);
		temp.append(XML_SLASH).append(elementString).append(XML_END_TAG).append("\n"); //$NON-NLS-1$
		xmlWriter.write(temp.toString());

	}

	public static void printHead(Writer xmlWriter, String encoding) throws IOException {
		StringBuilder temp = new StringBuilder(XML_HEAD);
		temp.append(encoding).append(XML_DBL_QUOTES).append(XML_HEAD_END_TAG).append("\n"); //$NON-NLS-1$
		xmlWriter.write(temp.toString());
	}

	public static String wrapAttributeForPrint(String attribute, String value) throws IOException {
		StringBuilder temp = new StringBuilder(XML_SPACE);
		temp.append(attribute).append(XML_EQUAL).append(XML_DBL_QUOTES)
				.append(encode(value).toString()).append(XML_DBL_QUOTES);
		return temp.toString();

	}

	public static void printNode(Writer xmlWriter, Node node,String encoding)  throws Exception{
		if (node == null) {
			return;
		}

		switch (node.getNodeType()) {
		case Node.DOCUMENT_NODE: {
			printHead(xmlWriter,encoding);
			printNode(xmlWriter, ((Document) node).getDocumentElement(),encoding);
			break;
		}
		case Node.ELEMENT_NODE: {
			//get the attribute list for this node.
			StringBuilder tempElementString = new StringBuilder(node.getNodeName());
			NamedNodeMap attributeList = node.getAttributes();
			if ( attributeList != null ) {
				for(int i= 0; i <attributeList.getLength();i++){
					Node attribute = attributeList.item(i);
					tempElementString.append(wrapAttributeForPrint(attribute.getNodeName(),attribute.getNodeValue()));
				}
			}
			printBeginElement(xmlWriter,tempElementString.toString());

			// do this recursively for the child nodes.
			NodeList childNodes = node.getChildNodes();
			if (childNodes != null) {
				int length = childNodes.getLength();
				for (int i = 0; i < length; i++) {
					printNode(xmlWriter, childNodes.item(i),encoding);
				}
			}

			printEndElement(xmlWriter,node.getNodeName());
			break;
		}

		case Node.TEXT_NODE: {
			xmlWriter.write(encode(node.getNodeValue()).toString());
			break;
		}
		default: {
			throw new UnsupportedOperationException(Messages.XMLPrintHandler_unsupportedNodeType);

		}
		}

	}

	public static StringBuffer encode(String value) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf;
	}
}
