/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.resources.mapping;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A simple resource mapping for converting IResource to ResourceMapping.
 * It uses the resource as the model object and traverses deeply.
 *
 * @since 3.1
 */
public class SimpleResourceMapping extends ResourceMapping {
	private final IResource resource;

	public SimpleResourceMapping(IResource resource) {
		this.resource = resource;
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		if (mapping.getModelProviderId().equals(this.getModelProviderId())) {
			Object object = mapping.getModelObject();
			if (object instanceof IResource) {
				IResource other = (IResource) object;
				return resource.getFullPath().isPrefixOf(other.getFullPath());
			}
			if (object instanceof ShallowContainer) {
				ShallowContainer sc = (ShallowContainer) object;
				IResource other = sc.getResource();
				return resource.getFullPath().isPrefixOf(other.getFullPath());
			}
		}
		return false;
	}

	@Override
	public Object getModelObject() {
		return resource;
	}

	@Override
	public String getModelProviderId() {
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
	}

	@Override
	public IProject[] getProjects() {
		if (resource.getType() == IResource.ROOT)
			return ((IWorkspaceRoot) resource).getProjects();
		return new IProject[] {resource.getProject()};
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) {
		if (resource.getType() == IResource.ROOT) {
			return new ResourceTraversal[] {new ResourceTraversal(((IWorkspaceRoot) resource).getProjects(), IResource.DEPTH_INFINITE, IResource.NONE)};
		}
		return new ResourceTraversal[] {new ResourceTraversal(new IResource[] {resource}, IResource.DEPTH_INFINITE, IResource.NONE)};
	}
}
