/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au) - bug 154907
 *******************************************************************************/
package org.eclipse.ant.internal.launching.debug.model;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * A property in an Ant build.
 */
public class AntProperty extends AntDebugElement implements IVariable, Comparable<AntProperty> {

	private final String fName;
	private final AntValue fValue;
	private String fLabel;

	/**
	 * Constructs a variable associated with the debug target with the given name and value.
	 *
	 * @param target
	 *            the debug target
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 */
	public AntProperty(AntDebugTarget target, String name, String value) {
		super(target);
		fName = name;
		fValue = new AntValue(target, value);
	}

	@Override
	public IValue getValue() {
		return fValue;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getReferenceTypeName() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean hasValueChanged() {
		return false;
	}

	@Override
	public void setValue(String expression) {
		// do nothing
	}

	@Override
	public void setValue(IValue value) {
		// do nothing
	}

	@Override
	public boolean supportsValueModification() {
		return false;
	}

	@Override
	public boolean verifyValue(String expression) {
		return false;
	}

	@Override
	public boolean verifyValue(IValue value) {
		return false;
	}

	/**
	 * @return the text used to render this property
	 */
	public String getText() {
		if (fLabel == null) {
			StringBuilder buffer = new StringBuilder(getName());
			buffer.append("= "); //$NON-NLS-1$
			buffer.append(fValue.getValueString());
			fLabel = buffer.toString();
		}
		return fLabel;
	}

	@Override
	public int compareTo(AntProperty other) {
		return fName.compareToIgnoreCase(other.getName());
	}
}
