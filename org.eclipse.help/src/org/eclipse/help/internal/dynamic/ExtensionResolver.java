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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.extension.ContentExtension;
import org.eclipse.help.internal.extension.ContentExtensionManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/*
 * Resolves the content to use for extensions. For anchors, these are the
 * nodes that will be inserted at the anchor. For replaces, these are the
 * nodes that will replace the original nodes.
 */
public class ExtensionResolver {

	private static final String ELEMENT_BODY = "body"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	private DocumentProcessor processor;
	private DocumentReader reader;
	private String locale;
	private ContentExtensionManager manager;

	/*
	 * Creates the resolver. The processor is needed to process the extension
	 * content, and locale because we're pulling in content from other documents.
	 */
	public ExtensionResolver(DocumentProcessor processor, DocumentReader reader, String locale) {
		this.processor = processor;
		this.reader = reader;
		this.locale = locale;
	}

	/*
	 * Resolves the given path into nodes to be inserted.
	 */
	public Node[] resolveExtension(String path, int type) {
		if (manager == null) {
			manager = HelpPlugin.getContentExtensionManager();
		}
		ContentExtension[] extensions = manager.getExtensions(path, type, locale);
		List<Node> list = new ArrayList<>();
		for (int i=0;i<extensions.length;++i) {
			String content = extensions[i].getContent();
			try {
				Node[] nodes = getContent(content);
				Collections.addAll(list, nodes);
			}
			catch (Throwable t) {
				// ignore invalid extensions
			}
		}
		return list.toArray(new Node[list.size()]);
	}

	/*
	 * Resolves the given content path (the content to insert/replace with) into
	 * nodes.
	 */
	private Node[] getContent(String content) throws IOException, SAXException, ParserConfigurationException {
		String bundleId = null;
		String relativePath = null;
		String nodeId = null;

		int bundleStart = 0;
		// legacy; can omit leading slash
		if (content.charAt(0) == '/') {
			bundleStart = 1;
		}
		int bundleEnd = content.indexOf('/', bundleStart + 1);
		if (bundleEnd > bundleStart) {
			bundleId = content.substring(bundleStart, bundleEnd);
			int pathStart = bundleEnd + 1;
			int pathEnd = content.indexOf('#', pathStart + 1);
			if (pathEnd == -1) {
				// legacy; slash can be used instead of '#'
				int lastSlash = content.lastIndexOf('/');
				int secondLastSlash = content.lastIndexOf('/', lastSlash - 1);
				if (secondLastSlash != -1 && lastSlash > secondLastSlash) {
					String secondLastToken = content.substring(secondLastSlash + 1, lastSlash);
					if (secondLastToken.indexOf('.') != -1) {
						pathEnd = lastSlash;
					}
					else {
						pathEnd = content.length();
					}
				}
				else {
					pathEnd = content.length();
				}
			}
			relativePath = content.substring(pathStart, pathEnd);
			if (pathEnd < content.length()) {
				nodeId = content.substring(pathEnd + 1);
			}
		}

		if (bundleId != null && relativePath != null) {
			return getContent(bundleId, relativePath, nodeId);
		}
		return null;
	}

	/*
	 * Resolves the given parsed content fragments into nodes.
	 */
	private Node[] getContent(String bundleId, String relativePath, String nodeId) throws IOException, SAXException, ParserConfigurationException {
		String href = '/' + bundleId + '/' + relativePath;
		try (InputStream in = HelpSystem.getHelpContent(href, locale)) {
			if (nodeId != null) {
				Element element = findElement(in, nodeId);
				processor.process(new UAElement(element), href);
				return new Node[] { element };
			}
			Element body = findBody(in);
			List<Node> children = new ArrayList<>();
			Node node = body.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					processor.process(new UAElement((Element)node), href);
				}
				children.add(node);
				node = node.getNextSibling();
			}
			return children.toArray(new Node[children.size()]);
		}
	}

	/*
	 * Finds and returns the element with the given elementId from the XML input
	 * stream, or null if not found.
	 */
	private Element findElement(InputStream in, String elementId) throws IOException, SAXException, ParserConfigurationException {
		return findElement(reader.read(in).getElement(), elementId);
	}

	/*
	 * Finds and returns the element with the given elementId from the under the
	 * given element, or null if not found.
	 */
	private Element findElement(Element element, String elementId) {
		String id = element.getAttribute(ATTRIBUTE_ID);
		if (id != null && id.equals(elementId)) {
			return element;
		}
		Node node = element.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				element = findElement((Element)node, elementId);
				if (element != null) {
					return element;
				}
			}
			node = node.getNextSibling();
		}
		return null;
	}

	/*
	 * Finds and returns the body node in the given XML input.
	 */
	private Element findBody(InputStream in) throws IOException, SAXException, ParserConfigurationException {
		return findBody(reader.read(in).getElement());
	}

	/*
	 * Finds and returns the body node under the given node.
	 */
	private Element findBody(Element element) {
		if (ELEMENT_BODY.equals(element.getNodeName())) {
			return element;
		}
		Node node = element.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element body = findBody((Element)node);
				if (body != null) {
					return body;
				}
			}
			node = node.getNextSibling();
		}
		return null;
	}
}
