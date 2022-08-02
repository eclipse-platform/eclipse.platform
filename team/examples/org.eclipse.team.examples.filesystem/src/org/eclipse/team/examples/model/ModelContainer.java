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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public abstract class ModelContainer extends ModelResource {

	protected ModelContainer(IContainer container) {
		super(container);
	}

	protected IContainer getContainer() {
		return (IContainer)getResource();
	}

	@Override
	public ModelObject[] getChildren() throws CoreException {
		IResource[] members = getContainer().members();
		List<ModelObject> result = new ArrayList<>();
		for (IResource resource : members) {
			if (resource instanceof IFolder) {
				result.add(new ModelFolder((IFolder) resource));
			} else if (ModelObjectDefinitionFile.isModFile(resource)) {
				result.add(new ModelObjectDefinitionFile((IFile)resource));
			} else if (resource instanceof IProject && ModelProject.isModProject((IProject) resource)) {
				result.add(new ModelProject((IProject) resource));
			}
		}
		return result.toArray(new ModelObject[result.size()]);
	}

}
