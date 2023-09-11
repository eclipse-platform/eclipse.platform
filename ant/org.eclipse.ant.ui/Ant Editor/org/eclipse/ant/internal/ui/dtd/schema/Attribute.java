/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.schema;

import org.eclipse.ant.internal.ui.dtd.IAttribute;
import org.eclipse.ant.internal.ui.dtd.IElement;

/**
 * Attr contains information about a single attribute.
 * 
 * @author Bob Foster
 */
public class Attribute extends Atom implements IAttribute {
	private String fType;
	private String[] fEnum;
	private final IElement fElement;
	private String fDefault;
	private boolean fFixed;
	private boolean fRequired;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Attribute qname.
	 * @param element
	 *            Parent element.
	 */
	public Attribute(String name, IElement element) {
		super(ATTRIBUTE, name);
		fElement = element;
	}

	@Override
	public String getType() {
		return fType;
	}

	@Override
	public String[] getEnum() {
		return fEnum;
	}

	@Override
	public IElement getElement() {
		return fElement;
	}

	@Override
	public String getDefault() {
		return fDefault;
	}

	@Override
	public boolean isFixed() {
		return fFixed;
	}

	@Override
	public boolean isRequired() {
		return fRequired;
	}

	public void setType(String type) {
		fType = type;
	}

	/**
	 * Sets the default value.
	 * 
	 * @param defaultValue
	 *            Value
	 */
	public void setDefault(String defaultValue) {
		fDefault = defaultValue;
	}

	/**
	 * Sets the enumeration.
	 * 
	 * @param enumeration
	 *            The enumeration to set
	 */
	public void setEnum(String[] enumeration) {
		fEnum = enumeration;
	}

	/**
	 * Sets the fixed.
	 * 
	 * @param fixed
	 *            The fixed to set
	 */
	public void setFixed(boolean fixed) {
		fFixed = fixed;
	}

	/**
	 * Sets the required.
	 * 
	 * @param required
	 *            The required to set
	 */
	public void setRequired(boolean required) {
		fRequired = required;
	}
}
