/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Utility class used to warp an set of resources in a resource mapper.
 * The resulting mapper will return the workspace root as the model
 * object.
 * 
 * TODO: The ability to wrap multiple resources in a single mapping
 * should be provided by the resources plugin.
 * 
 * @since 3.1
 */
public final class WorkspaceResourceMapper extends ResourceMapping {
	
	private final IResource resource;
	private final int depth;
	
	/**
	 * Convert the provided resources to one or more resource mappers
	 * that traverse the elements deeply. The model element of the resource
	 * mappers will be the workspace root.
	 * @param resources the resources
	 * @return a resource mappers that traverses the resources
	 */
	public static ResourceMapping[] asResourceMappers(final IResource[] resources, int depth) {
		List<WorkspaceResourceMapper> result = new ArrayList<>();
		for (IResource resource : resources) {
			result.add(new WorkspaceResourceMapper(resource, depth));
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}
	
	public WorkspaceResourceMapper(IResource resource, int depth) {
		this.resource = resource;
		this.depth = depth;
	}
	@Override
	public Object getModelObject() {
		return resource;
	}
	@Override
	public IProject[] getProjects() {
		return new IProject[] { resource.getProject() };
	}
	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		return asTraversal(resource, depth, context);
	}
	private ResourceTraversal[] asTraversal(IResource resource, final int depth, ResourceMappingContext context) {
		return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { resource }, depth, IResource.NONE)} ;
	}
	@Override
	public boolean contains(ResourceMapping mapping) {
		return false;
	}

	@Override
	public String getModelProviderId() {
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
	}
}
