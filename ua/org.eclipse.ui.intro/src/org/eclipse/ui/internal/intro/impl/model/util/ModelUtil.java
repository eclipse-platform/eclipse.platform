/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model.util;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroExtensionContent;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Util class for model. Has methods for resolving model attributes, and methods
 * for manipulating XHTML DOM.
 */
public class ModelUtil {

	private static String TAG_BODY = "body"; //$NON-NLS-1$
	private static String TAG_HEAD = "head"; //$NON-NLS-1$
	private static String TAG_BASE = "base"; //$NON-NLS-1$
	public static String TAG_DIV = "div"; //$NON-NLS-1$
	public static String TAG_HEAD_LINK = "link"; //$NON-NLS-1$
	private static String TAG_PARAM = "param"; //$NON-NLS-1$
	private static String ATT_SRC = "src"; //$NON-NLS-1$
	private static String ATT_HREF = "href"; //$NON-NLS-1$
	private static String ATT_CITE = "cite"; //$NON-NLS-1$
	private static String ATT_LONGDESC = "longdesc"; //$NON-NLS-1$
	private static String ATT_DATA = "data"; //$NON-NLS-1$
	private static String ATT_CODEBASE = "codebase"; //$NON-NLS-1$
	private static String ATT_VALUE = "value"; //$NON-NLS-1$
	private static String ATT_VALUE_TYPE = "valuetype"; //$NON-NLS-1$
	private static String ATT_REL = "rel"; //$NON-NLS-1$
	private static String ATT_TYPE = "type"; //$NON-NLS-1$



	/*
	 * ********* Model util methods ************************************
	 */

	/**
	 * Checks to see if the passed string is a valid URL (has a protocol), if
	 * yes, returns it as is. If no, treats it as a resource relative to the
	 * declaring plugin. Return the plugin relative location, fully qualified.
	 * Returns null if the passed string itself is null.
	 *
	 * @param resource
	 * @param pluginDesc
	 * @return returns the URL as is if it had a protocol.
	 */
	public static String resolveURL(String url, String pluginId) {
		Bundle bundle = null;
		if (pluginId != null)
			// if pluginId is not null, use it.
			bundle = Platform.getBundle(pluginId);
		return resolveURL("", url, bundle); //$NON-NLS-1$
	}



	/**
	 * Checks to see if the passed string is a valid URL (has a protocol), if
	 * yes, returns it as is. If no, treats it as a resource relative to the
	 * declaring plugin. Return the plugin relative location, fully qualified.
	 * Returns null if the passed string itself is null.
	 *
	 * @param resource
	 * @param pluginDesc
	 * @return returns the URL as is if it had a protocol.
	 */
	public static String resolveURL(String url, IConfigurationElement element) {
		Bundle bundle = BundleUtil.getBundleFromConfigurationElement(element);
		return resolveURL("", url, bundle); //$NON-NLS-1$
	}



	/**
	 * @see resolveURL(String url, IConfigurationElement element)
	 */
	public static String resolveURL(String base, String url, Bundle bundle) {
		// quick exit
		if (url == null)
			return null;
		IntroURLParser parser = new IntroURLParser(url);
		if (parser.hasProtocol())
			return url;
		// make plugin relative url. Only now we need the bundle.
		return BundleUtil.getResolvedResourceLocation(base, url, bundle);
	}

	/**
	 * Ensures that a file:// URL exists for the bundle root. This will
	 * cause jarred bundles to be extracted into a cache directory.
	 */
	public static void ensureFileURLsExist(Bundle bundle, String contentFile) {
		try {
			FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
		} catch (Exception e) {
			if (contentFile != null)
				Log.error("Failed to extract Intro content folder for: " //$NON-NLS-1$
						+ contentFile, e);
		}
	}


	/**
	 * Returns the path to the parent folder containing the passed content xml
	 * file. It is assumed that the path is a local url representing a content
	 * file.
	 */
	public static String getParentFolderToString(String contentFilePath) {
		IPath path = getParentFolderPath(contentFilePath);
		return path.toString();
	}


	/*
	 *
	 * ******** XHTML DOM util methods *********************************
	 */

	/**
	 * Returns the path to the parent folder containing the passed content xml
	 * file. It is assumed that the path is a local url representing a content
	 * file.
	 */
	public static String getParentFolderOSString(String contentFilePath) {
		IPath path = getParentFolderPath(contentFilePath);
		return path.toOSString();
	}

	/**
	 * Returns the parent folder of the given path.
	 */
	public static IPath getParentFolderPath(String contentFilePath) {
		IPath path = IPath.fromOSString(contentFilePath);
		path = path.removeLastSegments(1).addTrailingSeparator();
		return path;
	}




	public static void insertBase(Document dom, String baseURL) {
		// there should only be one head and one base element dom.
		NodeList headList = dom.getElementsByTagName(TAG_HEAD);
		Element head = (Element) headList.item(0);
		NodeList baseList = head.getElementsByTagName(TAG_BASE);
		if (baseList.getLength() == 0) {
			// insert a base element, since one is not defined already.
			Element base = dom.createElement(TAG_BASE);
			base.setAttribute(ATT_HREF, baseURL);
			head.insertBefore(base, head.getFirstChild());
		}
	}


	public static Element getBase(Document dom) {
		// there should only be one head and one base element dom.
		NodeList headList = dom.getElementsByTagName(TAG_HEAD);
		Element head = (Element) headList.item(0);
		NodeList baseList = head.getElementsByTagName(TAG_BASE);
		if (baseList.getLength() == 0)
			// no base defined, signal failure.
			return null;

		return (Element) baseList.item(baseList.getLength() - 1);

	}


	// <link rel="stylesheet" href="shared.css" type="text/css" />
	public static void insertStyle(Document dom, String cssUrl) {
		// there should only be one head and one base element dom.
		NodeList headList = dom.getElementsByTagName(TAG_HEAD);
		Element head = null;
		// Element base = getBase(dom);
		NodeList styleList = null;
		// there can be more than one style. DO not add style if it exists.
		if (headList.getLength() >= 1) {
			head = (Element) headList.item(0);
			styleList = head.getElementsByTagName(TAG_HEAD_LINK);
			for (int i = 0; i < styleList.getLength(); i++) {
				Element style = (Element) styleList.item(0);
				String styleString = style.getAttribute(ATT_HREF);
				if (styleString.equals(cssUrl))
					return;
			}
		}

		// insert the style, since it is not defined.
		Element styleToAdd = dom.createElement(TAG_HEAD_LINK);
		styleToAdd.setAttribute(ATT_HREF, cssUrl);
		styleToAdd.setAttribute(ATT_REL, "stylesheet"); //$NON-NLS-1$
		styleToAdd.setAttribute(ATT_TYPE, "text/css"); //$NON-NLS-1$
		if (styleList != null && styleList.getLength() >= 1)
			styleList.item(0).getParentNode().insertBefore(styleToAdd,
				styleList.item(0));
		else
			head.appendChild(styleToAdd);

	}

	/**
	 * Returns a reference to the body of the DOM.
	 *
	 * @param dom
	 * @return
	 */
	public static Element getBodyElement(Document dom) {
		// there should only be one body element dom.
		NodeList bodyList = dom.getElementsByTagName(TAG_BODY);
		Element body = (Element) bodyList.item(0);
		return body;
	}



	public static Element createElement(Document dom, String elementName,
			Properties attributes) {

		// make sure to create element with any namespace uri to enable finding
		// it again using Dom.getElementsByTagNameNS()
		Element element = dom.createElementNS("", elementName); //$NON-NLS-1$
		if (attributes != null) {
			Enumeration e = attributes.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				element.setAttribute(key, attributes.getProperty(key));
			}
		}
		return element;
	}

	public static Element createAndAppendChild(Element parentElement,
			String elementName, Properties attributes) {

		Element element = createElement(parentElement.getOwnerDocument(),
			elementName, attributes);
		parentElement.appendChild(element);
		return element;
	}



	/**
	 * Returns an Element array of all first level descendant Elements with a
	 * given tag name, in the order in which they are encountered in the DOM.
	 * Unlike the JAXP apis, which returns preorder traversal of this Element
	 * tree, this method filters out children deeper than first level child
	 * nodes.
	 */
	public static Element[] getElementsByTagName(Element parent, String tagName) {
		NodeList allChildElements = parent.getElementsByTagName(tagName);
		Vector<Element> vector = new Vector<>();
		for (int i = 0; i < allChildElements.getLength(); i++) {
			// we know that the nodelist is of elements.
			Element aElement = (Element) allChildElements.item(i);
			if (aElement.getParentNode().equals(parent))
				// first level child element. add it.
				vector.add(aElement);
		}
		Element[] filteredElements = new Element[vector.size()];
		vector.copyInto(filteredElements);
		return filteredElements;
	}

	/**
	 * Same as getElementsByTagName(Element parent, String tagName) but the
	 * parent element is assumed to be the root of the document.
	 *
	 * @see getElementsByTagName(Element parent, String tagName)
	 */
	public static Element[] getElementsByTagName(Document dom, String tagName) {
		NodeList allChildElements = dom.getElementsByTagName(tagName);
		Vector<Element> vector = new Vector<>();
		for (int i = 0; i < allChildElements.getLength(); i++) {
			// we know that the nodelist is of elements.
			Element aElement = (Element) allChildElements.item(i);
			if (aElement.getParentNode().equals(dom.getDocumentElement()))
				// first level child element. add it. Cant use getParent
				// here.
				vector.add(aElement);
		}
		Element[] filteredElements = new Element[vector.size()];
		vector.copyInto(filteredElements);
		return filteredElements;
	}


	/*
	 * Util method similar to DOM getElementById() method, but it works without
	 * an id attribute being specified. Deep searches all children in this
	 * container's DOM for the first child with the given id. The element
	 * retrieved must have the passed local name. Note that in an XHTML file
	 * (aka DOM) elements should have a unique id within the scope of a
	 * document. We use local name because this allows for finding intro
	 * anchors, includes and dynamic content element regardless of whether or
	 * not an xmlns was used in the xml.
	 */
	public static Element getElementById(Document dom, String id,
			String localElementName) {

		NodeList children = dom.getElementsByTagNameNS("*", localElementName); //$NON-NLS-1$
		for (int i = 0; i < children.getLength(); i++) {
			Element element = (Element) children.item(i);
			if (element.getAttribute("id").equals(id)) //$NON-NLS-1$
				return element;
		}
		// non found.
		return null;

	}

	public static Element getElementById(Document dom, String id) {
		return getElementById(dom, id, "*"); //$NON-NLS-1$
	}

	public static void updateResourceAttributes(Element element,
			AbstractIntroPage page) {
		updateResourceAttributes(element, page.getBase(), page.getBundle());
	}


	public static void updateResourceAttributes(Element element,
			IntroExtensionContent extensionContent) {
		updateResourceAttributes(element, extensionContent.getBase(),
			extensionContent.getBundle());
	}

	/**
	 * Updates all the resource attributes of the passed element to point to a
	 * local resolved url.
	 *
	 * @param element
	 * @param extensionContent
	 */
	private static void updateResourceAttributes(Element element, String base,
			Bundle bundle) {
		// doUpdateResourceAttributes(element, base, bundle);
		NodeList children = element.getElementsByTagName("*"); //$NON-NLS-1$
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element) children.item(i);
			doUpdateResourceAttributes(child, base, bundle);
		}
	}

	private static void doUpdateResourceAttributes(Element element,
			String base, Bundle bundle) {
		qualifyAttribute(element, ATT_SRC, base, bundle);
		qualifyAttribute(element, ATT_HREF, base, bundle);
		qualifyAttribute(element, ATT_CITE, base, bundle);
		qualifyAttribute(element, ATT_LONGDESC, base, bundle);
		qualifyAttribute(element, ATT_CODEBASE, base, bundle);
		qualifyAttribute(element, ATT_DATA, base, bundle);
		qualifyValueAttribute(element, base, bundle);
	}

	private static void qualifyAttribute(Element element, String attributeName,
			String base, Bundle bundle) {
		if (element.hasAttribute(attributeName)) {
			String attributeValue = element.getAttribute(attributeName);
			if (new IntroURLParser(attributeValue).hasProtocol())
				return;

			// resolve the resource against the nl mechanism.
			String attributePath = BundleUtil.getResolvedResourceLocation(base,
				attributeValue, bundle);
			element.setAttribute(attributeName, attributePath);
		}
	}

	private static void qualifyValueAttribute(Element element, String base,
			Bundle bundle) {
		if (element.hasAttribute(ATT_VALUE)
				&& element.hasAttribute(ATT_VALUE_TYPE)
				&& element.getAttribute(ATT_VALUE_TYPE).equals("ref") //$NON-NLS-1$
				&& element.getLocalName().equals(TAG_PARAM)) {
			String value = element.getAttribute(ATT_VALUE);
			if (new IntroURLParser(value).hasProtocol())
				return;
			// resolve the resource against the nl mechanism.
			String attributePath = BundleUtil.getResolvedResourceLocation(base,
				value, bundle);
			element.setAttribute(ATT_VALUE, attributePath);
		}
	}


	/**
	 * Returns an array version of the passed NodeList. Used to work around DOM
	 * design issues.
	 */
	public static Node[] getArray(NodeList nodeList) {
		Node[] nodes = new Node[nodeList.getLength()];
		for (int i = 0; i < nodeList.getLength(); i++)
			nodes[i] = nodeList.item(i);
		return nodes;
	}


	/**
	 * Remove all instances of the element from the DOM.
	 *
	 */
	public static void removeAllElements(Document dom, String elementLocalName) {
		// get all elements in DOM and remove them.
		NodeList elements = dom.getElementsByTagNameNS("*", //$NON-NLS-1$
			elementLocalName);
		// get the array version of the nodelist to work around DOM api design.
		Node[] elementsArray = ModelUtil.getArray(elements);
		for (int i = 0; i < elementsArray.length; i++) {
			Node element = elementsArray[i];
			element.getParentNode().removeChild(element);
		}

	}



}
