/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.subscribers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.core.mapping.AbstractResourceMappingScope;

/**
 * A synchronization scope for a set of resources.
 */
public class RootResourceSynchronizationScope extends AbstractResourceMappingScope {

	private IResource[] roots;

	public RootResourceSynchronizationScope(IResource[] roots) {
		this.roots = roots;
	}

	@Override
	public ResourceTraversal[] getTraversals() {
		return new ResourceTraversal[] {new ResourceTraversal(roots, IResource.DEPTH_INFINITE, IResource.NONE)};
	}

	/**
	 * Set the traversal of this scope to a single traversal
	 * of infinite depth on the given resources.
	 * @param roots the new roots of the scope
	 */
	public void setRoots(IResource[] roots) {
		this.roots = roots;
		fireTraversalsChangedEvent(getTraversals(), getMappings());
	}

	@Override
	public ResourceMapping[] getInputMappings() {
		return getMappings();
	}

	@Override
	public ISynchronizationScope asInputScope() {
		return this;
	}

	@Override
	public ResourceMapping[] getMappings() {
		List<ResourceMapping> result = new ArrayList<>();
		for (IResource resource : roots) {
			Object o = resource.getAdapter(ResourceMapping.class);
			if (o instanceof ResourceMapping) {
				result.add((ResourceMapping) o);
			}
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			return new ResourceTraversal[] {new ResourceTraversal(new IResource[] { resource }, IResource.DEPTH_INFINITE, IResource.NONE)};
		}
		return null;
	}

	@Override
	public boolean hasAdditionalMappings() {
		return false;
	}

	@Override
	public boolean hasAdditonalResources() {
		return false;
	}

	@Override
	public IProject[] getProjects() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

	@Override
	public ResourceMappingContext getContext() {
		return ResourceMappingContext.LOCAL_CONTEXT;
	}

	@Override
	public void refresh(ResourceMapping[] mappings) {
		// Not supported
	}

}
