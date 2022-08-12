/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.runtime.Assert;

/**
 *
 */
public class VariableDescription implements Comparable<VariableDescription> {
	private String name;
	private String value;

	public VariableDescription() {
		this.name = ""; //$NON-NLS-1$
		this.value = ""; //$NON-NLS-1$
	}

	public VariableDescription(String name, String value) {
		super();
		Assert.isNotNull(name);
		Assert.isNotNull(value);
		this.name = name;
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o.getClass() != this.getClass())
			return false;
		VariableDescription other = (VariableDescription) o;
		return name.equals(other.name) && value == other.value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return name.hashCode() + value.hashCode();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Compare string descriptions in a way that sorts them topologically by
	 * name.
	 */
	@Override
	public int compareTo(VariableDescription that) {
		return name.compareTo(that.name);
	}
}
