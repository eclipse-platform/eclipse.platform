/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.core;

/**
 * Represents information about a target within an Ant build file.
 */
public class TargetInfo {
	
	private String name = null;
	private String description = null;
	private boolean isDefault = false;
	
/**
 * Create a target information
 * 
 * @param name target name
 * @param description a brief explanation of the target's purpose
 * 		or <code>null</code> if not specified
 * @param isDefault whether this is the build file default target
 */
/*package*/ TargetInfo(String name, String description, boolean isDefault) {
	this.name = name == null ? "" : name; //$NON-NLS-1$
	this.description = description;
	this.isDefault = isDefault;
}

/**
 * Returns the target name.
 * 
 * @return the target name
 */
public String getName() {
	return name;
}

/**
 * Returns the target description or <code>null</code> if no
 * description is provided.
 * 
 * @return the target description or <code>null</code> if none
 */
public String getDescription() {
	return description;
}

/**
 * Returns whether this is the build file default target.
 * 
 * @return whether this is the build file default target
 */
public boolean isDefault() {
	return isDefault;
}
}