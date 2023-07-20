/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.debug.core.DebugPlugin;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An XML memento. Copied from workspace plug-in.
 */
public final class XMLMemento {
	private Document factory;

	private Element element;

	/**
	 * Creates a <code>Document</code> from the <code>Reader</code>
	 * and returns a memento on the first <code>Element</code> for reading
	 * the document.
	 * <p>
	 * Same as calling createReadRoot(reader, null)
	 * </p>
	 *
	 * @param reader the <code>Reader</code> used to create the memento's document
	 * @return a memento on the first <code>Element</code> for reading the document
	 * @throws Exception if IO problems, invalid format, or no element.
	 */
	public static XMLMemento createReadRoot(Reader reader)
			throws Exception {
		return createReadRoot(reader, null);
	}

	/**
	 * Creates a <code>Document</code> from the <code>Reader</code>
	 * and returns a memento on the first <code>Element</code> for reading
	 * the document.
	 *
	 * @param reader the <code>Reader</code> used to create the memento's document
	 * @param baseDir the directory used to resolve relative file names
	 * 		in the XML document. This directory must exist and include the
	 * 		trailing separator. The directory format, including the separators,
	 * 		must be valid for the platform. Can be <code>null</code> if not
	 * 		needed.
	 * @return a memento on the first <code>Element</code> for reading the document
	 * @throws Exception if IO problems, invalid format, or no element.
	 */
	public static XMLMemento createReadRoot(Reader reader, String baseDir)
			throws Exception {
		String errorMessage = null;
		Exception exception = null;
		try {
			InputSource source = new InputSource(reader);
			if (baseDir != null) {
				source.setSystemId(baseDir);
			}
			@SuppressWarnings("restriction")
			Document document = org.eclipse.core.internal.runtime.XmlProcessorFactory.parseWithErrorOnDOCTYPE(source);
			NodeList list = document.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node instanceof Element) {
					return new XMLMemento(document, (Element) node);
				}
			}
		} catch (ParserConfigurationException e) {
			exception = e;
			// errorMessage = WorkbenchMessages.XMLMemento_parserConfigError;
		} catch (IOException e) {
			exception = e;
			// errorMessage = WorkbenchMessages.XMLMemento_ioError;
		} catch (SAXException e) {
			exception = e;
			// errorMessage = WorkbenchMessages.XMLMemento_formatError;
		}

		String problemText = null;
		if (exception != null) {
			problemText = exception.getMessage();
		}
		if (problemText == null || problemText.length() == 0) {
			problemText = errorMessage != null ? errorMessage
					:"ERROR"; //;WorkbenchMessages.XMLMemento_noElement; //$NON-NLS-1$
		}
		throw new Exception(problemText, exception);
	}

	/**
	 * Returns a root memento for writing a document.
	 *
	 * @param type the element node type to create on the document
	 * @return the root memento for writing a document
	 */
	public static XMLMemento createWriteRoot(String type) {
		Document document;
		try {
			@SuppressWarnings("restriction")
			DocumentBuilder b = org.eclipse.core.internal.runtime.XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
			document = b.newDocument();
			Element element = document.createElement(type);
			document.appendChild(element);
			return new XMLMemento(document, element);
		} catch (ParserConfigurationException e) {
//            throw new Error(e);
			throw new Error(e.getMessage());
		}
	}

	/**
	 * Creates a memento for the specified document and element.
	 * <p>
	 * Clients should use <code>createReadRoot</code> and
	 * <code>createWriteRoot</code> to create the initial
	 * memento on a document.
	 * </p>
	 *
	 * @param document the document for the memento
	 * @param element the element node for the memento
	 */
	public XMLMemento(Document document, Element element) {
		super();
		this.factory = document;
		this.element = element;
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public XMLMemento createChild(String type) {
		Element child = factory.createElement(type);
		element.appendChild(child);
		return new XMLMemento(factory, child);
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public XMLMemento createChild(String type, String id) {
		Element child = factory.createElement(type);
		child.setAttribute(IMementoConstants.TAG_ID, id == null ? "" : id); //$NON-NLS-1$
		element.appendChild(child);
		return new XMLMemento(factory, child);
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public XMLMemento copyChild(XMLMemento child) {
		Element childElement = child.element;
		Element newElement = (Element) factory.importNode(childElement, true);
		element.appendChild(newElement);
		return new XMLMemento(factory, newElement);
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public XMLMemento getChild(String type) {

		// Get the nodes.
		NodeList nodes = element.getChildNodes();
		int size = nodes.getLength();
		if (size == 0) {
			return null;
		}

		// Find the first node which is a child of this node.
		for (int nX = 0; nX < size; nX++) {
			Node node = nodes.item(nX);
			if (node instanceof Element) {
				Element element1 = (Element) node;
				if (element1.getNodeName().equals(type)) {
					return new XMLMemento(factory, element1);
				}
			}
		}

		// A child was not found.
		return null;
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public XMLMemento[] getChildren(String type) {

		// Get the nodes.
		NodeList nodes = element.getChildNodes();
		int size = nodes.getLength();
		if (size == 0) {
			return new XMLMemento[0];
		}

		// Extract each node with given type.
		ArrayList<Element> list = new ArrayList<>(size);
		for (int nX = 0; nX < size; nX++) {
			Node node = nodes.item(nX);
			if (node instanceof Element) {
				Element element1 = (Element) node;
				if (element1.getNodeName().equals(type)) {
					list.add(element1);
				}
			}
		}

		// Create a memento for each node.
		size = list.size();
		XMLMemento[] results = new XMLMemento[size];
		for (int x = 0; x < size; x++) {
			results[x] = new XMLMemento(factory, list.get(x));
		}
		return results;
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public Float getFloat(String key) {
		Attr attr = element.getAttributeNode(key);
		if (attr == null) {
			return null;
		}
		String strValue = attr.getValue();
		try {
			return Float.valueOf(strValue);
		} catch (NumberFormatException e) {
			DebugPlugin.logMessage("Memento problem - Invalid float for key: " //$NON-NLS-1$
					+ key + " value: " + strValue, e); //$NON-NLS-1$
			return null;
		}
	}

	public String getType() {
		return element.getNodeName();
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public String getID() {
		return element.getAttribute(IMementoConstants.TAG_ID);
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public Integer getInteger(String key) {
		Attr attr = element.getAttributeNode(key);
		if (attr == null) {
			return null;
		}
		String strValue = attr.getValue();
		try {
			return Integer.valueOf(strValue);
		} catch (NumberFormatException e) {
			DebugPlugin
					.logMessage("Memento problem - invalid integer for key: " + key //$NON-NLS-1$
							+ " value: " + strValue, e); //$NON-NLS-1$
			return null;
		}
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public String getString(String key) {
		Attr attr = element.getAttributeNode(key);
		if (attr == null) {
			return null;
		}
		return attr.getValue();
	}

	public Boolean getBoolean(String key) {
		Attr attr = element.getAttributeNode(key);
		if (attr == null) {
			return null;
		}
		return Boolean.valueOf(attr.getValue());
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public String getTextData() {
		Text textNode = getTextNode();
		if (textNode != null) {
			return textNode.getData();
		}
		return null;
	}

	public String[] getAttributeKeys() {
		NamedNodeMap map = element.getAttributes();
		int size = map.getLength();
		String[] attributes = new String[size];
		for (int i = 0; i < size; i++) {
			Node node = map.item(i);
			attributes[i] = node.getNodeName();
		}
		return attributes;
	}

	/**
	 * Returns the Text node of the memento. Each memento is allowed only
	 * one Text node.
	 *
	 * @return the Text node of the memento, or <code>null</code> if
	 * the memento has no Text node.
	 */
	private Text getTextNode() {
		// Get the nodes.
		NodeList nodes = element.getChildNodes();
		int size = nodes.getLength();
		if (size == 0) {
			return null;
		}
		for (int nX = 0; nX < size; nX++) {
			Node node = nodes.item(nX);
			if (node instanceof Text) {
				return (Text) node;
			}
		}
		// a Text node was not found
		return null;
	}

	/**
	 * Places the element's attributes into the document.
	 * @param element1 the element
	 * @param copyText true if the first text node should be copied
	 */
	private void putElement(Element element1, boolean copyText) {
		NamedNodeMap nodeMap = element1.getAttributes();
		int size = nodeMap.getLength();
		for (int i = 0; i < size; i++) {
			Attr attr = (Attr) nodeMap.item(i);
			putString(attr.getName(), attr.getValue());
		}

		NodeList nodes = element1.getChildNodes();
		size = nodes.getLength();
		// Copy first text node (fixes bug 113659).
		// Note that text data will be added as the first child (see putTextData)
		boolean needToCopyText = copyText;
		for (int i = 0; i < size; i++) {
			Node node = nodes.item(i);
			if (node instanceof Element) {
				XMLMemento child = createChild(node.getNodeName());
				child.putElement((Element) node, true);
			} else if (node instanceof Text && needToCopyText) {
				putTextData(((Text) node).getData());
				needToCopyText = false;
			}
		}
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public void putFloat(String key, float f) {
		element.setAttribute(key, String.valueOf(f));
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public void putInteger(String key, int n) {
		element.setAttribute(key, String.valueOf(n));
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public void putMemento(XMLMemento memento) {
		// Do not copy the element's top level text node (this would overwrite the existing text).
		// Text nodes of children are copied.
		putElement(memento.element, false);
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public void putString(String key, String value) {
		if (value == null) {
			return;
		}
		element.setAttribute(key, value);
	}

	/**
	 * @param key the attribute key
	 * @param value the boolean value for the key
	 * @since 3.4
	 */
	public void putBoolean(String key, boolean value) {
		element.setAttribute(key, value ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Method declared in XMLMemento.
	 */
	public void putTextData(String data) {
		Text textNode = getTextNode();
		if (textNode == null) {
			textNode = factory.createTextNode(data);
			// Always add the text node as the first child (fixes bug 93718)
			element.insertBefore(textNode, element.getFirstChild());
		} else {
			textNode.setData(data);
		}
	}

	/**
	 * Saves this memento's document current values to the
	 * specified writer.
	 *
	 * @param writer the writer used to save the memento's document
	 * @throws IOException if there is a problem serializing the document to the stream.
	 */
	public void save(Writer writer) throws IOException {
		try (DOMWriter out = new DOMWriter(writer)) {
			out.print(element);
		}
	}

	/**
	 * A simple XML writer.  Using this instead of the javax.xml.transform classes allows
	 * compilation against JCL Foundation (bug 80053).
	 */
	private static final class DOMWriter extends PrintWriter {

//    	private int tab;

		/* constants */
		private static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$

		/**
		 * Creates a new DOM writer on the given output writer.
		 *
		 * @param output the output writer
		 */
		public DOMWriter(Writer output) {
			super(output);
//    		tab = 0;
			println(XML_VERSION);
		}

		/**
		 * Prints the given element.
		 *
		 * @param element the element to print
		 */
		public void print(Element element) {
			// Ensure extra whitespace is not emitted next to a Text node,
			// as that will result in a situation where the restored text data is not the
			// same as the saved text data.
			boolean hasChildren = element.hasChildNodes();
			startTag(element, hasChildren);
			if (hasChildren) {
//	        	tab++;
				boolean prevWasText = false;
				NodeList children = element.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					Node node = children.item(i);
					if (node instanceof Element) {
						if (!prevWasText) {
							println();
							printTabulation();
						}
						print((Element) children.item(i));
						prevWasText = false;
					}
					else if (node instanceof Text) {
						print(getEscaped(node.getNodeValue()));
						prevWasText = true;
					}
				}
//	    		tab--;
				if (!prevWasText) {
					println();
					printTabulation();
				}
				endTag(element);
			}
		}

		private void printTabulation() {
			// Indenting is disabled, as it can affect the result of getTextData().
			// In 3.0, elements were separated by a newline but not indented.
			// This causes getTextData() to return "\n" even if no text data had explicitly been set.
			// The code here emulates that behaviour.

//    		for (int i = 0; i < tab; i++)
//    			super.print("\t"); //$NON-NLS-1$
		}

		private void startTag(Element element, boolean hasChildren) {
			StringBuilder sb = new StringBuilder();
			sb.append("<"); //$NON-NLS-1$
			sb.append(element.getTagName());
			NamedNodeMap attributes = element.getAttributes();
			for (int i = 0;  i < attributes.getLength(); i++) {
				Attr attribute = (Attr)attributes.item(i);
				sb.append(" "); //$NON-NLS-1$
				sb.append(attribute.getName());
				sb.append("=\""); //$NON-NLS-1$
				sb.append(getEscaped(String.valueOf(attribute.getValue())));
				sb.append("\""); //$NON-NLS-1$
			}
			sb.append(hasChildren ? ">" : "/>"); //$NON-NLS-1$ //$NON-NLS-2$
			print(sb.toString());
		}

		private void endTag(Element element) {
			StringBuilder sb = new StringBuilder();
			sb.append("</"); //$NON-NLS-1$
			sb.append(element.getNodeName());
			sb.append(">"); //$NON-NLS-1$
			print(sb.toString());
		}

		private static void appendEscapedChar(StringBuilder buffer, char c) {
			String replacement = getReplacement(c);
			if (replacement != null) {
				buffer.append('&');
				buffer.append(replacement);
				buffer.append(';');
			} else if (c==9 || c==10 || c==13 || c>=32){
				buffer.append(c);
			}
		}

		private static String getEscaped(String s) {
			StringBuilder result = new StringBuilder(s.length() + 10);
			for (int i = 0; i < s.length(); ++i) {
				appendEscapedChar(result, s.charAt(i));
			}
			return result.toString();
		}

		private static String getReplacement(char c) {
			// Encode special XML characters into the equivalent character references.
			// The first five are defined by default for all XML documents.
			// The next three (#xD, #xA, #x9) are encoded to avoid them
			// being converted to spaces on deserialization
			// (fixes bug 93720)
			switch (c) {
				case '<' :
					return "lt"; //$NON-NLS-1$
				case '>' :
					return "gt"; //$NON-NLS-1$
				case '"' :
					return "quot"; //$NON-NLS-1$
				case '\'' :
					return "apos"; //$NON-NLS-1$
				case '&' :
					return "amp"; //$NON-NLS-1$
				case '\r':
					return "#x0D"; //$NON-NLS-1$
				case '\n':
					return "#x0A"; //$NON-NLS-1$
				case '\u0009':
					return "#x09"; //$NON-NLS-1$
				default:
					break;
			}
			return null;
		}
	}
}
