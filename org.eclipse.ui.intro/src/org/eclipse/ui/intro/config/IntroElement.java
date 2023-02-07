/***************************************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 **************************************************************************************************/

package org.eclipse.ui.intro.config;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Used to provide children of the computed gruops while constructing intro content. Clients provide
 * instances of this class from <code>IntroConfigurer</code> to dynamically complete the intro
 * content. Attribute and element names, as well as content model must match the intro
 * schema.
 *
 * @since 3.2
 */
public class IntroElement {

	private String name;
	private String value;
	private Hashtable<String, String> atts = new Hashtable<>();
	private List<IntroElement> children;

	/**
	 * Creates a new intro element with the provided name.
	 *
	 * @param name
	 *            the name of the new intro element
	 */
	public IntroElement(String name) {
		this.name = name;
	}

	/**
	 * Sets the value of the named attribute.
	 *
	 * @param name
	 *            attribute name
	 * @param value
	 *            attribute value
	 */
	public void setAttribute(String name, String value) {
		atts.put(name, value);
	}

	/**
	 * Returns the value of the attribute with a given name.
	 *
	 * @param name
	 *            the attribute name
	 * @return value of the attribute with a given name or <code>null</code> if not set.
	 */
	public String getAttribute(String name) {
		return atts.get(name);
	}

	/**
	 * Returns the names of all the attributes defined in this element.
	 *
	 * @return an enumeration of all the element names
	 */

	public Enumeration<String> getAttributes() {
		return atts.keys();
	}

	/**
	 * Returns the name of the element.
	 *
	 * @return name of the element
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the value of the element.
	 *
	 * @return value of the element or <code>null</code> if not set.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the value of the element.
	 *
	 * @param value
	 *            the value of this element
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Adds a child to this element.
	 *
	 * @param child
	 *            the new child of this element
	 */
	public void addChild(IntroElement child) {
		if (children == null)
			children = new ArrayList<>();
		children.add(child);
	}

	/**
	 * Returns the children of this element.
	 *
	 * @return an array of child elements or an empty array of there are no children.
	 */
	public IntroElement[] getChildren() {
		if (children == null)
			return new IntroElement[0];
		return children.toArray(new IntroElement[children.size()]);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntroElement) {
			if (obj == this) {
				return true;
			}
			String id1 = atts.get("id"); //$NON-NLS-1$
			String id2 = ((IntroElement)obj).atts.get("id"); //$NON-NLS-1$
			if (id1 == null && id2 == null) {
				return super.equals(obj);
			}
			if (id1 != null && id2 != null) {
				return id1.equals(id2);
			}
		}
		return false;
	}
}