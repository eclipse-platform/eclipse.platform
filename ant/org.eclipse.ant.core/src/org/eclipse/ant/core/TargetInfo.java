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
 * Represents information about a target within an Ant build file.
 * Clients may not instantiate or subclass this class.
 * @since 2.1
 */
public class TargetInfo {

	private String name = null;
	private String description = null;
	private ProjectInfo project = null;
	private String[] dependencies = null;
	private boolean isDefault = false;

	/**
	 * Create a target information
	 * 
	 * @param name target name
	 * @param description a brief explanation of the target's purpose
	 * 		or <code>null</code> if not specified
	 * @param project enclosing project
	 * @param dependencies names of prerequisite projects 
	 * @param isDefault whether this is the build file default target
	 */
	/*package*/
	TargetInfo(ProjectInfo project, String name, String description, String[] dependencies, boolean isDefault) {
		this.name = name == null ? "" : name; //$NON-NLS-1$
		this.description = description;
		this.project = project;
		this.dependencies = dependencies;
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
	 * Returns the ProjectInfo of the enclosing project.
	 * 
	 * @return the project info for the enclosing project
	 */
	public ProjectInfo getProject() {
		return project;
	}
	
	/**
	 * Return the names of the targets that this target depends on.
	 * 
	 * @return the dependent names
	 */
	public String[] getDependencies() {
		return dependencies;
	}

	/**
	 * Returns whether this is the build file default target.
	 * 
	 * @return whether this is the build file default target
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof TargetInfo)) {
			return false;
		}
		TargetInfo other= (TargetInfo)obj;
		return getName().equals(other.getName());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getName().hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}
}
