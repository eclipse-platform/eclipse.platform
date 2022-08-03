/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

public class ModelProviderResourceMapping extends ResourceMapping {

	ModelProvider provider;

	public ModelProviderResourceMapping(ModelProvider provider) {
		this.provider = provider;
	}

	@Override
	public Object getModelObject() {
		return provider;
	}

	@Override
	public String getModelProviderId() {
		// Use the resource model provider id. Model providers
		// can override this by adapting their specific model provider class
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
	}

	@Override
	public IProject[] getProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		try {
			IResource[] resources = provider.getDescriptor().getMatchingResources(projects);
			Set<IProject> result = new HashSet<>();
			for (IResource resource : resources) {
				if (resource.isAccessible())
					result.add(resource.getProject());
			}
			return result.toArray(new IProject[result.size()]);
		} catch (CoreException e) {
			TeamPlugin.log(e);
		}
		return projects;
	}

	@Override
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

	@Override
	public boolean contains(ResourceMapping mapping) {
		return (mapping.getModelProviderId().equals(getModelProviderId()));
	}

}
