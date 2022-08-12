/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A resource mapping for a shallow container.
 */
public class ShallowResourceMapping extends ResourceMapping {

	private final ShallowContainer container;

	public ShallowResourceMapping(ShallowContainer container) {
		this.container = container;
	}

	@Override
	public Object getModelObject() {
		return container;
	}

	@Override
	public String getModelProviderId() {
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
	}

	@Override
	public IProject[] getProjects() {
		return new IProject[] {container.getResource().getProject()};
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) {
		return new ResourceTraversal[] {new ResourceTraversal(new IResource[] {container.getResource()}, IResource.DEPTH_ONE, IResource.NONE)};
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		if (mapping.getModelProviderId().equals(this.getModelProviderId())) {
			Object object = mapping.getModelObject();
			IResource resource = container.getResource();
			// A shallow mapping only contains direct file children or equal shallow containers
			if (object instanceof ShallowContainer) {
				ShallowContainer sc = (ShallowContainer) object;
				return sc.getResource().equals(resource);
			}
			if (object instanceof IResource) {
				IResource other = (IResource) object;
				return other.getType() == IResource.FILE && resource.getFullPath().equals(other.getFullPath().removeLastSegments(1));
			}
		}
		return false;
	}

}
