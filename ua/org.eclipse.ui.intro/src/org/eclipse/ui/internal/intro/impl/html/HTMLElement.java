/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.html;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * This class represents an HTML element. An HTML element has a name, a
 * collection of attributes, and content
 */
public class HTMLElement {

	// the name of the element
	private String elementName;

	// a collection of attributes that belong to this element (possibly empty)
	private Map<String, String> elementAttributes;

	// a collection of other HTMLElements or Strings contained inside this
	// element
	private Vector<Object> elementContent;

	public HTMLElement(String name) {
		this.elementName = name;
		this.elementAttributes = new HashMap<>();
		this.elementContent = new Vector<>();
	}

	public HTMLElement(String name, Map<String, String> attributes, Vector<Object> content) {
		this.elementName = name;
		this.elementAttributes = attributes;
		this.elementContent = content;
	}

	/**
	 * Add an attribute with the given name and value to this HTMLElement
	 *
	 * @param attributeName
	 * @param attributeValue
	 */
	public void addAttribute(String attributeName, String attributeValue) {
		if(attributeName != null && attributeValue != null)
			getElementAttributes().put(attributeName, attributeValue);
	}

	/**
	 * Add content to this element. The content should be in the form of
	 * another HTMLElement, or a String
	 */
	public void addContent(Object content) {
		getElementContent().add(content);
	}

	/**
	 * Get the attributes associated with this element
	 *
	 * @return Returns the elementAttributes.
	 */
	public Map<String, String> getElementAttributes() {
		if (elementAttributes == null)
			elementAttributes = new HashMap<>();

		return elementAttributes;
	}

	/**
	 * Set the attributes associated with this element
	 *
	 * @param elementAttributes
	 *            The elementAttributes to set.
	 */
	public void setElementAttributes(Map<String, String> elementAttributes) {
		this.elementAttributes = elementAttributes;
	}

	/**
	 * Get this element's content
	 *
	 * @return Returns the elementContent.
	 */
	public Vector<Object> getElementContent() {
		if (elementContent == null)
			elementContent = new Vector<>();

		return elementContent;
	}

	/**
	 * Set this element's content
	 *
	 * @param elementContent
	 *            The elementContent to set.
	 */
	public void setElementContent(Vector<Object> elementContent) {
		this.elementContent = elementContent;
	}

	/**
	 * Get the name of this element
	 *
	 * @return Returns the elementName.
	 */
	public String getElementName() {
		return elementName;
	}

	/**
	 * Set the name of this element
	 *
	 * @param elementName
	 *            The elementName to set.
	 */
	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	@Override
	public String toString() {
		StringBuilder element = new StringBuilder();

		// add the start tag and attributes
		element.append(
			HTMLUtil.createHTMLStartTag(
				getElementName(),
				getElementAttributes(),
				false));

		// include the element's content
		for (Iterator it = getElementContent().iterator(); it.hasNext();) {
			Object content = it.next();
			element.append(content);
		}

		// include an end tag
		element.append(HTMLUtil.createHTMLEndTag(getElementName(), false));
		return element.toString();
	}
}
