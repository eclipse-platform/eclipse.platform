/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

public class ModelProviderResourceMapping extends ResourceMapping {

	ModelProvider provider;

	public ModelProviderResourceMapping(ModelProvider provider) {
		this.provider = provider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.ResourceMapping#getModelObject()
	 */
	public Object getModelObject() {
		return provider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.ResourceMapping#getModelProviderId()
	 */
	public String getModelProviderId() {
		// Use the resource model provider id. Model providers
		// can override this by adapting their specific model provider class
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.ResourceMapping#getProjects()
	 */
	public IProject[] getProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		try {
			IResource[] resources = provider.getDescriptor().getMatchingResources(projects);
			Set result = new HashSet();
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if (resource.isAccessible())
					result.add(resource.getProject());
			}
			return (IProject[]) result.toArray(new IProject[result.size()]);
		} catch (CoreException e) {
			TeamPlugin.log(e);
		}
		return projects;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.ResourceMapping#getTraversals(org.eclipse.core.resources.mapping.ResourceMappingContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(null, 100);
			ResourceMapping[] mappings = provider.getMappings(getProviderResources(context), context, Policy.subMonitorFor(monitor, 50));
			return provider.getTraversals(mappings, context, Policy.subMonitorFor(monitor, 50));
		} finally {
			monitor.done();
		}
	}

	private IResource[] getProviderResources(ResourceMappingContext context) {
		try {
			if (context instanceof RemoteResourceMappingContext) {
				RemoteResourceMappingContext rrmc = (RemoteResourceMappingContext) context;
				return provider.getDescriptor().getMatchingResources(rrmc.getProjects());
			}
		} catch (CoreException e) {
			TeamPlugin.log(e);
		}
		return getProjects();
	}

	public boolean contains(ResourceMapping mapping) {
		return (mapping.getModelProviderId().equals(getModelProviderId()));
	}

}
