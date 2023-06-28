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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.variables.IStringVariable;
import org.eclipse.core.variables.IValueVariable;

/**
 * Common implementation of context and value variables
 */
public abstract class StringVariable implements IStringVariable {

	/**
	 * Variable name
	 */
	private String fName;

	/**
	 * Variable description, or <code>null</code>
	 */
	private String fDescription;

	/**
	 * Configuration element associated with this variable, or <code>null</code>
	 */
	private IConfigurationElement fConfigurationElement;

	/**
	 * Constructs a new variable with the given name, description and configuration element.
	 *
	 * @param name variable name
	 * @param description variable description, or <code>null</code>
	 * @param configurationElement configuration element or <code>null</code>
	 */
	public StringVariable(String name, String description, IConfigurationElement configurationElement) {
		fName = name;
		fDescription = description;
		fConfigurationElement = configurationElement;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getDescription() {
		return fDescription;
	}

	/**
	 * Returns the configuration element associated with this variable, or <code>null</code>
	 * if none.
	 *
	 * @return configuration element or <code>null</code>
	 */
	protected IConfigurationElement getConfigurationElement() {
		return fConfigurationElement;
	}

	/**
	 * @see IValueVariable#setDescription(String)
	 * @param description the new description to set for the variable
	 */
	public void setDescription(String description) {
		fDescription = description;
	}

}
