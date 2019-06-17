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
package org.eclipse.team.tests.ccvs.core.mappings.model.mapping;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.tests.ccvs.core.mappings.model.ModelFile;
import org.eclipse.team.tests.ccvs.core.mappings.model.ModelObject;
import org.eclipse.team.tests.ccvs.core.mappings.model.ModelProject;

public class ModelResourceMapping extends ResourceMapping {

	public static String projectName = "";
	private final ModelObject object;

	public static ResourceMapping create(ModelObject object) {
		if (object instanceof ModelProject) {
			return new ModelResourceMapping(object);
		}
		if (object instanceof ModelFile) {
			return new ModelResourceMapping(object);
		}
		return null;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		return new ResourceTraversal[] { new ResourceTraversal(
				new IResource[] { ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName).getFile("file1.txt") },
				IResource.DEPTH_ZERO, IResource.NONE) };
	}

	protected ModelResourceMapping(ModelObject object) {
		this.object = object;
	}

	@Override
	public Object getModelObject() {
		return object;
	}

	@Override
	public String getModelProviderId() {
		return CustomModelProvider.ID;
	}

	@Override
	public IProject[] getProjects() {
		return new IProject[] { (IProject) object.getProject().getResource() };
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		if (mapping instanceof ModelResourceMapping) {
			ModelObject object = (ModelObject) mapping.getModelObject();
			IResource resource = object.getResource();
			return getResource().getFullPath().isPrefixOf(
					resource.getFullPath());
		}
		return false;
	}

	private IResource getResource() {
		return ((ModelProject) getModelObject()).getResource();
	}

}
