/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.mappings.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;

public abstract class ModelObject extends PlatformObject {

	private final IResource resource;

	public static ModelObject create(IResource resource) {
		switch (resource.getType()) {
		case IResource.PROJECT:
			return new ModelProject((IProject) resource);
		case IResource.FILE:
			if (ModelFile.isModFile(resource)) {
				return new ModelFile((IFile) resource);
			}
		}
		return null;
	}

	protected ModelObject(IResource resource) {
		this.resource = resource;
	}

	public abstract ModelObject[] getChildren() throws CoreException;

	public IResource getResource() {
		return resource;
	}

	public String getName() {
		return getResource().getName();
	}

	public String getPath() {
		return getResource().getFullPath().makeRelative().toString();
	}

	public ModelObject getParent() {
		return ModelObject.create(getResource().getParent());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModelObject) {
			ModelObject mr = (ModelObject) obj;
			return getResource().equals(mr.getResource());
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return getResource().hashCode();
	}

	public void delete() throws CoreException {
		getResource().delete(false, null);
	}

	public ModelProject getProject() {
		return (ModelProject) ModelObject.create(getResource().getProject());
	}
}
