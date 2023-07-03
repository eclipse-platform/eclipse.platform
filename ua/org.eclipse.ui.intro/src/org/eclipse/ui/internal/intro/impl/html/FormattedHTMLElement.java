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
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
/**
 * A FormattedHTMLElement is an HTMLElement that is indented and can have its
 * content on a separate line from its start and end tags
 */
public class FormattedHTMLElement extends HTMLElement {
	// an integer representing how many tabs to insert when printing out this
	// element and its content
	private int indentLevel;
	// indicates whether to print this element on a single line or on multiple
	// lines
	private boolean spanMultipleLines;
	// indicates whether an end tag is required for this element. default is
	// true, since most elements require an end tag
	private boolean endTagRequired = true;
	public FormattedHTMLElement(String name, int indentLevel,
			boolean spanMultipleLines) {
		super(name);
		this.indentLevel = indentLevel;
		this.spanMultipleLines = spanMultipleLines;
		// void tags do not have close tags
		boolean isVoidTag = IIntroHTMLConstants.ELEMENT_META.equalsIgnoreCase(name)
				|| IIntroHTMLConstants.ELEMENT_BASE.equalsIgnoreCase(name)
				|| IIntroHTMLConstants.ELEMENT_LINK.equalsIgnoreCase(name);
		endTagRequired = !isVoidTag;
	}
	public FormattedHTMLElement(String name, int indentLevel,
			boolean spanMultipleLines, boolean endTagRequired) {
		super(name);
		this.indentLevel = indentLevel;
		this.spanMultipleLines = spanMultipleLines;
		this.endTagRequired = endTagRequired;
	}

	public FormattedHTMLElement(String name, Map<String, String> attributes, Vector<Object> content,
			int indentLevel, boolean spanMultipleLines) {
		super(name, attributes, content);
		this.indentLevel = indentLevel;
		this.spanMultipleLines = spanMultipleLines;
		endTagRequired = true;
	}
	/**
	 * Set whether the end tag is required for this element
	 *
	 * @param required
	 *            true if end tag required, false otherwise
	 */
	public void setEndTagRequired(boolean required) {
		this.endTagRequired = required;
	}
	/**
	 * Set the indent level that should be applied to this element when printed
	 *
	 * @param indentLevel
	 *            The indentLevel to set.
	 */
	public void setIndentLevel(int indentLevel) {
		this.indentLevel = indentLevel;
	}
	/**
	 * Set whether or not this element should be printed over multiple lines,
	 * or on a single line
	 *
	 * @param spanMultipleLines
	 *            true if the element should be printed over multiple lines,
	 *            false if it should be printed on a single line
	 */
	public void setSpanMultipleLines(boolean spanMultipleLines) {
		this.spanMultipleLines = spanMultipleLines;
	}
	/**
	 * Create a string of tabs to insert before the element is printed
	 *
	 * @param indentLevel
	 *            the number of tabs to insert
	 * @return
	 */
	private StringBuilder getIndent(int indentLevel) {
		// figure out the tab width
		StringBuilder indent = new StringBuilder();
		for (int i = 0; i < indentLevel; i++) {
			indent.append(IIntroHTMLConstants.SMALL_TAB);
		}
		return indent;
	}
	@Override
	public String toString() {
		StringBuilder element = new StringBuilder();
		// insert the indent
		element.append(getIndent(indentLevel));
		// add the start tag and attributes
		element.append(HTMLUtil.createHTMLStartTag(getElementName(),
				getElementAttributes(), spanMultipleLines));
		// if there is no content and an end tag is not required just
		// return the element as is
		if (getElementContent().isEmpty() && !endTagRequired) {
			return element.toString();
		}
		// include the element's content, if there is any
		for (Iterator it = getElementContent().iterator(); it.hasNext();) {
			Object content = it.next();
			element.append(content);
		}
		// indent the end tag if we're on a new line
		if (indentLevel > 0 && spanMultipleLines)
			element.append(getIndent(indentLevel));
		// include an end tag
		element.append(HTMLUtil.createHTMLEndTag(getElementName(), true));
		return element.toString();
	}
}
