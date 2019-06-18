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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ModelProject extends ModelObject {

	public static boolean isModProject(IProject project) throws CoreException {
		if (!project.isOpen())
			return false;
		IProjectDescription description = project.getDescription();
		return description.hasNature(ModelNature.NATURE_ID);
	}

	public static void makeModProject(IProject project, IProgressMonitor monitor)
			throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natureIds = description.getNatureIds();
		List<String> result = new ArrayList<>();
		Collections.addAll(result, natureIds);
		result.add(ModelNature.NATURE_ID);
		description.setNatureIds(result.toArray(new String[result
				.size()]));
		project.setDescription(description, monitor);
	}

	public ModelProject(IProject project) {
		super(project);
	}

	protected IContainer getContainer() {
		return (IContainer) getResource();
	}

	@Override
	public ModelObject[] getChildren() throws CoreException {
		IResource[] members = getContainer().members();
		List<ModelObject> result = new ArrayList<>();
		for (IResource resource : members) {
			if (ModelFile.isModFile(resource)) {
				result.add(new ModelFile((IFile) resource));
			} else if (resource instanceof IProject
					&& ModelProject.isModProject((IProject) resource)) {
				result.add(new ModelProject((IProject) resource));
			}
		}
		return result.toArray(new ModelObject[result.size()]);
	}

}
