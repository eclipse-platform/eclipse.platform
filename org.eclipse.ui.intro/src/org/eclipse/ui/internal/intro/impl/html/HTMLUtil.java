/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Convenience class for generating HTML elements.
 */
public final class HTMLUtil {
	/**
	 * Creates an HTML opening element of the form
	 * <pre>{@code
	 * <elementName elementAttributes>
	 * }</pre>
	 *
	 * @param elementName
	 *            the name of the element to create
	 * @param elementAttributes
	 *            a map of attribute names and values to be inserted into the
	 *            element start tag
	 * @param insertLineBreak
	 *            true to insert a line break after the start tag is closed,
	 *            false otherwise
	 * @return
	 */
	public static StringBuilder createHTMLStartTag(String elementName,
			Map elementAttributes, boolean insertLineBreak) {
		StringBuilder element = new StringBuilder();
		if (elementName != null) {
			// open the start tag
			element.append(openHTMLStartTag(elementName));
			// add the attributes, if there are any
			if (elementAttributes != null && !(elementAttributes.isEmpty()))
				element.append(IIntroHTMLConstants.SPACE).append(
						createAttributeList(elementAttributes));
			// close the start tag
			element.append(closeHTMLTag(insertLineBreak));
		}
		return element;
	}

	/**
	 * Creates an HTML start tag of the form
	 * <pre>{@code
	 * <elementName>
	 * }</pre>
	 *
	 * @param elementName
	 *            the name of the element to create
	 * @param insertLineBreak
	 *            true to insert a new line after the start tag
	 * @return
	 */
	public static StringBuilder createHTMLStartTag(String elementName,
			boolean insertLineBreak) {
		return createHTMLStartTag(elementName, null, insertLineBreak);
	}

	/**
	 * Creates an HTML start tag of the form
	 * <pre>{@code
	 * <elementName>
	 * }</pre>
	 * and inserts a line break after the start tag
	 *
	 * @param elementName
	 *            the name of the element to create
	 * @return
	 */
	public static StringBuilder createHTMLStartTag(String elementName) {
		return createHTMLStartTag(elementName, null, true);
	}

	/**
	 * Creates an HTML closing element of the form
	 * <pre>{@code
	 * </elementName>
	 * }</pre>
	 *
	 * @param elementName
	 *            the name of the closing element to create
	 * @param addNewLine
	 *            true to add a new line at the end
	 * @return
	 */
	public static StringBuilder createHTMLEndTag(String elementName,
			boolean addNewLine) {
		StringBuilder closingElement = new StringBuilder();
		if (elementName != null)
			closingElement.append(IIntroHTMLConstants.LT).append(
					IIntroHTMLConstants.FORWARD_SLASH).append(elementName)
					.append(closeHTMLTag(addNewLine));
		return closingElement;
	}

	/**
	 * Given a map of attribute names and values, this method will create a
	 * StringBuilder of the attributes in the form: <code>attrName="attrValue"</code>. These
	 * attributes can appear in the start tag of an HTML element.
	 *
	 * @param attributes
	 *            the attributes to be converted into a String list
	 * @return
	 */
	public static String createAttributeList(Map attributes) {
		if (attributes == null)
			return null;
		StringBuilder attributeList = new StringBuilder();
		Set attrNames = attributes.keySet();
		for (Iterator it = attrNames.iterator(); it.hasNext();) {
			Object name = it.next();
			Object value = attributes.get(name);
			if ((name instanceof String) && (value instanceof String)) {
				attributeList.append(createAttribute((String) name,
						(String) value));
				if (it.hasNext()) {
					attributeList.append(IIntroHTMLConstants.SPACE);
				}
			}
		}
		return attributeList.toString();
	}

	/**
	 * Creates an HTML attribute of the form <code>attrName="attrValue"</code>
	 *
	 * @param attrName
	 *            the name of the attribute
	 * @param attrValue
	 *            the value of the attribute
	 * @return
	 */
	public static StringBuilder createAttribute(String attrName, String attrValue) {
		StringBuilder attribute = new StringBuilder();
		if (attrName != null && attrValue != null) {
			attribute.append(attrName).append(IIntroHTMLConstants.EQUALS)
					.append(IIntroHTMLConstants.QUOTE).append(attrValue)
					.append(IIntroHTMLConstants.QUOTE);
		}
		return attribute;
	}

	public static StringBuilder openHTMLStartTag(String elementName) {
		return new StringBuilder().append(IIntroHTMLConstants.LT).append(
				elementName);
	}

	public static StringBuilder closeHTMLTag() {
		return closeHTMLTag(true);
	}

	public static StringBuilder closeHTMLTag(boolean newLine) {
		StringBuilder closing = new StringBuilder()
				.append(IIntroHTMLConstants.GT);
		if (newLine)
			closing.append(IIntroHTMLConstants.NEW_LINE);
		return closing;
	}

	/**
	 * Determine if the contents of two character arrays are equal
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equalCharArrayContent(char[] a, char[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}
}
