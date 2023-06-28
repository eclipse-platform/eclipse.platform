/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.core.internal.variables;

import org.eclipse.core.variables.IValueVariable;

/**
 * Implementation of a value variable.
 */
public class ValueVariable extends StringVariable implements IValueVariable {

	/**
	 * Variable value or <code>null</code> if none
	 */
	private String fValue;

	/**
	 * Whether this variable is read only.  If true, users cannot change the value.
	 */
	private boolean fReadOnly;

	/**
	 * Constructs a new value variable with the given name, description, read only
	 * property and string value.  Value can be null.
	 *
	 * @param name variable name
	 * @param description variable description or <code>null</code>
	 * @param readOnly whether the variable should be a read only variable
	 * @param value the initial value of the variable or <code>null</code>
	 */
	public ValueVariable(String name, String description, boolean readOnly, String value) {
		super(name, description, null);
		fReadOnly = readOnly;
		fValue = value;
	}

	@Override
	public void setValue(String value) {
		if (!isReadOnly()){
			fValue = value;
			StringVariableManager.getDefault().notifyChanged(this);
		}
	}

	@Override
	public String getValue() {
		return fValue;
	}

	@Override
	public boolean isReadOnly() {
		return fReadOnly;
	}

	@Override
	public boolean isContributed() {
		return false;
	}

}
