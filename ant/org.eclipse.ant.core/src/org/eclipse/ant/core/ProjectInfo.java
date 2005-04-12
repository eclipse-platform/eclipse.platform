/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.core;


/**
 * Represents information about a project within an Ant build file.
 * Clients may not instantiate or subclass this class.
 * @since 2.1
 */

public class ProjectInfo {

	private String name = null;
	private String description = null;

	/**
	 * Create a project information
	 * 
	 * @param name project name
	 * @param description a brief explanation of the project's purpose 		or
	 * <code>null</code> if not specified
	 */
	/*package*/
	ProjectInfo(String name, String description) {
		this.name = name == null ? "" : name; //$NON-NLS-1$
		this.description = description;
	}

	/**
	 * Returns the project name.
	 * 
	 * @return the project name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the project description or <code>null</code> if no
	 * description is provided.
	 * 
	 * @return the project description or <code>null</code> if none
	 */
	public String getDescription() {
		return description;
	}
}
