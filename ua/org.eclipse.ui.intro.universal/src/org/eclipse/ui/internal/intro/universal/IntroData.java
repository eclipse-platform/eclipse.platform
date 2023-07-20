/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.universal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.intro.impl.model.loader.IntroContentParser;
import org.eclipse.ui.internal.intro.universal.util.BundleUtil;
import org.eclipse.ui.internal.intro.universal.util.Log;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class IntroData {
	private String productId;
	private Map<String, PageData> pages = new HashMap<>();
	private boolean active;

	public IntroData(String productId, String fileNameOrData, boolean active) {
		this.productId = productId;
		this.active = active;
		if (fileNameOrData!=null)
			initialize(fileNameOrData);
	}

	public String getProductId() {
		return productId;
	}

	public PageData getPage(String pageId) {
		return pages.get(pageId);
	}

	public boolean isActive() {
		return active;
	}

	private void initialize(String fileNameOrData) {
		Document doc = parse(fileNameOrData);
		if (doc == null)
			return;
		Element root = doc.getDocumentElement();
		NodeList pages = root.getChildNodes();
		for (int i = 0; i < pages.getLength(); i++) {
			Node node = pages.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("page")) { //$NON-NLS-1$
				loadPage((Element) node);
			}
		}
	}

	private void loadPage(Element page) {
		PageData pd = new PageData(page);
		pages.put(pd.getId(), pd);
	}

	public void addImplicitContent() {
		IConfigurationElement [] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.intro.configExtension"); //$NON-NLS-1$
		for (int i=0; i<elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("configExtension")) { //$NON-NLS-1$
				String cid = element.getAttribute("configId"); //$NON-NLS-1$
				if (cid!=null && cid.equals("org.eclipse.ui.intro.universalConfig")) { //$NON-NLS-1$
					addCandidate(element);
				}
			}
		}
	}

	private void addCandidate(IConfigurationElement element) {
		String fileName = element.getAttribute("content"); //$NON-NLS-1$
		if (fileName==null)
			return;
		String bundleId = element.getDeclaringExtension().getContributor().getName();
		Bundle bundle = Platform.getBundle(bundleId);
		if (bundle==null)
			return;
		String content = BundleUtil.getResolvedResourceLocation("", fileName, //$NON-NLS-1$
					bundle);
		IntroContentParser parser = new IntroContentParser(content);
		Document dom = parser.getDocument();
		// dom can be null if the content file cannot be found
		if (dom==null)
			return;
		Element root = dom.getDocumentElement();
		Element extension = null;
		NodeList children = root.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType()==Node.ELEMENT_NODE) {
				Element el = (Element)child;
				if (el.getNodeName().equalsIgnoreCase("extensionContent")) { //$NON-NLS-1$
					extension = el;
					break;
				}
			}
		}
		if (extension==null)
			return;
		String id = extension.getAttribute("id"); //$NON-NLS-1$
		String name = extension.getAttribute("name"); //$NON-NLS-1$
		String path = extension.getAttribute("path"); //$NON-NLS-1$
		if (id==null || path==null)
			return;
		int at = path.lastIndexOf("/@"); //$NON-NLS-1$
		if (at == -1)
			return;
		if (path.charAt(path.length()-1)!='@')
			return;
		String pageId = path.substring(0, at);
		PageData pd = pages.get(pageId);
		if (pd==null) {
			pd = new PageData(pageId);
			pages.put(pageId, pd);
		}
		pd.addImplicitExtension(id, name);
	}

	private Document parse(String fileNameOrData) {
		Document document = null;
		try {
			@SuppressWarnings("restriction")
			DocumentBuilderFactory docFactory = org.eclipse.core.internal.runtime.XmlProcessorFactory.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
			// if this is not set, Document.getElementsByTagNameNS() will fail.
			docFactory.setNamespaceAware(true);
			docFactory.setExpandEntityReferences(false);
			DocumentBuilder parser = docFactory.newDocumentBuilder();

			if (fileNameOrData.charAt(0)=='<') {
				//This is actual content, not the file name
				StringReader reader = new StringReader(fileNameOrData);
				document = parser.parse(new InputSource(reader));
			}
			else
				document = parser.parse(fileNameOrData);
			return document;

		} catch (SAXParseException spe) {
			StringBuilder buffer = new StringBuilder("IntroData error in line "); //$NON-NLS-1$
			buffer.append(spe.getLineNumber());
			buffer.append(", uri "); //$NON-NLS-1$
			buffer.append(spe.getSystemId());
			buffer.append("\n"); //$NON-NLS-1$
			buffer.append(spe.getMessage());

			// Use the contained exception.
			Exception x = spe;
			if (spe.getException() != null)
				x = spe.getException();
			Log.error(buffer.toString(), x);

		} catch (SAXException sxe) {
			Exception x = sxe;
			if (sxe.getException() != null)
				x = sxe.getException();
			Log.error(x.getMessage(), x);

		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			Log.error(pce.getMessage(), pce);

		} catch (IOException ioe) {
			Log.error(ioe.getMessage(), ioe);
		}
		return null;
	}

	public void write(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"utf-8\" ?>"); //$NON-NLS-1$
		writer.println("<extensions>"); //$NON-NLS-1$
		for (PageData pd : pages.values()) {
			pd.write(writer, "   "); //$NON-NLS-1$
		}
		writer.println("</extensions>"); //$NON-NLS-1$
	}
}
