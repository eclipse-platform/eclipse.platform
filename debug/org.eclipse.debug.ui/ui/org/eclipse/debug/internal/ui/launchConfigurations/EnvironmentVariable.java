/*******************************************************************************
 * Copyright (c) 2000, 2005 Keith Seitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Keith Seitz (keiths@redhat.com) - initial implementation
 *     IBM Corporation - integration and code cleanup
 *     Red Hat - Bug 548344
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

/**
 * A key/value set whose data is passed into Runtime.exec(...)
 */
public class EnvironmentVariable
{
	// The name of the environment variable
	private String name;

	// The value of the environment variable
	private String value;

	public EnvironmentVariable(String name, String value)
	{
		this.name = name;
		this.value = value;
	}

	/**
	 * Returns this variable's name, which serves as the key in the key/value
	 * pair this variable represents
	 *
	 * @return this variable's name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns this variables value.
	 *
	 * @return this variable's value
	 */
	public String getValue()
	{
		return value;
	}


	/**
	 * Sets this variable's value
	 * @param value
	 */
	public void setValue(String value)
	{
		this.value = value;
	}

	/**
	 * Sets this variable's name
	 *
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getName();
	}


	@Override
	public boolean equals(Object obj) {
		boolean equal = false;
		if (obj instanceof EnvironmentVariable) {
			EnvironmentVariable var = (EnvironmentVariable)obj;
			equal = var.getName().equals(name);
		}
		return equal;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
