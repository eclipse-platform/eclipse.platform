/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;

public abstract class ModelObject extends PlatformObject {

	public static ModelObject create(IResource resource) {
		switch (resource.getType()) {
		case IResource.ROOT:
			return new ModelWorkspace();
		case IResource.PROJECT:
			return new ModelProject((IProject)resource);
		case IResource.FOLDER:
			return new ModelFolder((IFolder)resource);
		case IResource.FILE:
			if (ModelObjectDefinitionFile.isModFile(resource)) {
				return new ModelObjectDefinitionFile((IFile)resource);
			}
		}
		return null;
	}

	/**
	 * Return the name of the model object.
	 * @return the name of the model object
	 */
	public abstract String getName();

	/**
	 * Return the path of this object in the model namespace.
	 * @return the path of this object in the model namespace
	 */
	public abstract String getPath();

	/**
	 * Return the children of this object.
	 * @return the children of this object
	 */
	public abstract ModelObject[] getChildren() throws CoreException;

	/**
	 * Return the parent of this object.
	 * @return the parent of this object
	 */
	public abstract ModelObject getParent();

	/**
	 * Delete the model object
	 */
	public abstract void delete() throws CoreException;

	/**
	 * Return the project that contains this model object.
	 * @return the project that contains this model object
	 */
	public abstract ModelProject getProject();
}
