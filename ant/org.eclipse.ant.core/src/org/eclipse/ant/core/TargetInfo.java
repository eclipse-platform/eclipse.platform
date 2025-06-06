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
package org.eclipse.ant.core;

import org.eclipse.ant.internal.core.IAntCoreConstants;

/**
 * Represents information about a target within an Ant build file. Clients may not instantiate or subclass this class.
 *
 * @since 2.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
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
	 * @param name
	 *            target name
	 * @param description
	 *            a brief explanation of the target's purpose or <code>null</code> if not specified
	 * @param project
	 *            enclosing project
	 * @param dependencies
	 *            names of prerequisite projects
	 * @param isDefault
	 *            whether this is the build file default target
	 * @since 3.3
	 */
	public TargetInfo(ProjectInfo project, String name, String description, String[] dependencies, boolean isDefault) {
		this.name = name == null ? IAntCoreConstants.EMPTY_STRING : name;
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
	 * Returns the target description or <code>null</code> if no description is provided.
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

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TargetInfo other)) {
			return false;
		}
		return getName().equals(other.getName());
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public String toString() {
		return getName();
	}
}
