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
package org.eclipse.team.internal.ui.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipant;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

public class ResourceModelScopeParticipant implements
		ISynchronizationScopeParticipant, IResourceChangeListener, IPropertyChangeListener {
	private final ModelProvider provider;
	private final ISynchronizationScope scope;

	public ResourceModelScopeParticipant(ModelProvider provider, ISynchronizationScope scope) {
		this.provider = provider;
		this.scope = scope;
		if (hasWorkspaceMapping()) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		}
		if (hasWorkingSetMappings()) {
			PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(this);
		}
	}

	private boolean hasWorkingSetMappings() {
		ResourceMapping[] mappings = scope.getMappings(provider.getDescriptor().getId());
		for (ResourceMapping mapping : mappings) {
			Object modelObject = mapping.getModelObject();
			if (modelObject instanceof IWorkingSet) {
				return true;
			}
		}
		return false;
	}

	private boolean hasWorkspaceMapping() {
		ResourceMapping[] mappings = scope.getMappings(provider.getDescriptor().getId());
		for (ResourceMapping mapping : mappings) {
			Object modelObject = mapping.getModelObject();
			if (modelObject instanceof IResource) {
				IResource resource = (IResource) modelObject;
				if (resource.getType() == IResource.ROOT) {
					return true;
				}
			} else if (modelObject instanceof ModelProvider) {
				ModelProvider provider = (ModelProvider) modelObject;
				if (provider.getId().equals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public ResourceMapping[] handleContextChange(
			ISynchronizationScope scope, IResource[] resources,
			IProject[] projects) {
		Set<ResourceMapping> result = new HashSet<>();
		for (IProject project : projects) {
			collectMappings(project, result);
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	private void collectMappings(IProject project, Set<ResourceMapping> result) {
		ResourceMapping[] mappings = scope.getMappings(provider.getDescriptor().getId());
		for (ResourceMapping mapping : mappings) {
			boolean refresh = false;
			Object modelObject = mapping.getModelObject();
			if (modelObject instanceof IWorkingSet) {
				IWorkingSet set = (IWorkingSet)modelObject;
				IAdaptable[] elements = set.getElements();
				for (IAdaptable adaptable : elements) {
					ResourceMapping m = Adapters.adapt(adaptable, ResourceMapping.class);
					if (m != null) {
						IProject[] p = m.getProjects();
						for (IProject mp : p) {
							if (mp.equals(project)) {
								refresh = true;
								break;
							}
						}
					}
					if (refresh)
						break;
				}
			} else if (modelObject instanceof IResource) {
				IResource resource = (IResource) modelObject;
				if (resource.getType() == IResource.ROOT) {
					refresh = true;
				}
			} else if (modelObject instanceof ModelProvider) {
				ModelProvider mp = (ModelProvider) modelObject;
				try {
					ResourceMapping[] list = mp.getMappings(project, ResourceMappingContext.LOCAL_CONTEXT, null);
					if (list.length > 0) {
						refresh = true;
					}
				} catch (CoreException e) {
					TeamUIPlugin.log(e);
				}
			}
			if (refresh) {
				result.add(mapping);
			}
		}
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (PlatformUI.isWorkbenchRunning())
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(this);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		// Only interested in project additions and removals
		Set<ResourceMapping> result = new HashSet<>();
		IResourceDelta[] children = event.getDelta().getAffectedChildren();
		for (IResourceDelta delta : children) {
			IResource resource = delta.getResource();
			if (resource.getType() == IResource.PROJECT
					&& ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.REMOVED)) != 0
					|| (delta.getFlags() & IResourceDelta.OPEN) != 0)) {
				if (isInContext(resource))
					collectMappings((IProject)resource, result);
			}
		}
		if (!result.isEmpty())
			fireChange(result.toArray(new ResourceMapping[result.size()]));


	}

	private boolean isInContext(IResource resource) {
		IProject[] projects = scope.getProjects();
		for (IProject project : projects) {
			if (project.equals(resource.getProject())) {
				return true;
			}
		}
		return false;
	}

	private void fireChange(ResourceMapping[] mappings) {
		scope.refresh(mappings);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty() == IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE) {
			IWorkingSet newSet = (IWorkingSet) event.getNewValue();
			ResourceMapping[] mappings = scope.getMappings(provider.getDescriptor().getId());
			for (ResourceMapping mapping : mappings) {
				if (newSet == mapping.getModelObject()) {
					fireChange(new ResourceMapping[] { mapping });
				}
			}
		} else if(event.getProperty() == IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE) {
			// TODO: Need to update the participant description somehow
			//firePropertyChangedEvent(new PropertyChangeEvent(this, NAME, null, event.getNewValue()));
		}
	}

}
