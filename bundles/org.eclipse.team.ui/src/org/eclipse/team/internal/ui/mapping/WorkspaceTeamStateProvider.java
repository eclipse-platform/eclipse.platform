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
package org.eclipse.team.internal.ui.mapping;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.internal.core.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.*;
import org.eclipse.team.ui.synchronize.TeamStateProvider;

/**
 * A decorated state provider that delegates to the provider for the repository
 * provider type that is associated with the projects that an element maps to
 * using the ResourceMapping API.
 * 
 */
public class WorkspaceTeamStateProvider extends TeamStateProvider
		implements ITeamStateChangeListener, IRepositoryProviderListener,
		IResourceChangeListener {

	private Map providers = new HashMap();

	public WorkspaceTeamStateProvider() {
		RepositoryProviderManager.getInstance().addListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (int i = 0; i < allProjects.length; i++) {
			IProject project = allProjects[i];
			handleProject(project);
		}
	}
	
	public void dispose() {
		RepositoryProviderManager.getInstance().removeListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.DecoratedStateProvider#isDecorationEnabled(java.lang.Object)
	 */
	public final boolean isDecorationEnabled(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.isDecorationEnabled(element);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.DecoratedStateProvider#isDecorated(java.lang.Object)
	 */
	public boolean hasDecoratedState(Object element) throws CoreException {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			provider.hasDecoratedState(element);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.DecoratedStateProvider#getDecoratedStateMask(java.lang.Object)
	 */
	public final int getDecoratedStateMask(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getDecoratedStateMask(element);
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamStateProvider#getDecoratedProperties(java.lang.Object)
	 */
	public String[] getDecoratedProperties(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getDecoratedProperties(element);
		return new String[0];
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.DecoratedStateProvider#getState(java.lang.Object, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ITeamStateDescription getStateDescription(Object element, int stateMask,
			String[] properties, IProgressMonitor monitor) throws CoreException {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getStateDescription(element, stateMask, properties, monitor);
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamStateProvider#getResourceMappingContext(java.lang.Object)
	 */
	public ResourceMappingContext getResourceMappingContext(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getResourceMappingContext(element);
		return ResourceMappingContext.LOCAL_CONTEXT;
	}
	
	private ITeamStateProvider getDecoratedStateProvider(Object element) {
		RepositoryProviderType type = getProviderType(element);
		if (type != null)
			return Utils.getAdapter(type, ITeamStateProvider.class);
		return null;
	}

	private ITeamStateProvider getDecoratedStateProviderForId(String id) {
		RepositoryProviderType type = getProviderTypeForId(id);
		if (type != null)
			return Adapters.adapt(type, ITeamStateProvider.class);
		return null;
	}

	private RepositoryProviderType getProviderType(Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			String providerId = getProviderId(mapping.getProjects());
			if (providerId != null)
				return getProviderTypeForId(providerId);
		}
		return null;
	}

	private String getProviderId(IProject[] projects) {
		String id = null;
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			String nextId = getProviderId(project);
			if (id == null)
				id = nextId;
			else if (nextId != null && !id.equals(nextId))
				return null;
		}
		return id;
	}

	private String getProviderId(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider != null)
			return provider.getID();
		return null;
	}

	private RepositoryProviderType getProviderTypeForId(String providerId) {
		return RepositoryProviderType.getProviderType(providerId);
	}

	private void handleProject(IProject project) {
		if (RepositoryProvider.isShared(project)) {
			try {
				String currentId = project
						.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY);
				if (currentId != null) {
					listenerForStateChangesForId(currentId);
				}
			} catch (CoreException e) {
				TeamPlugin.log(e);
			}
		}
	}
	
	private void listenerForStateChangesForId(String id) {
		if (!providers.containsKey(id)) {
			ITeamStateProvider provider = getDecoratedStateProviderForId(id);
			if (provider != null) {
				providers.put(id, provider);
				provider.addDecoratedStateChangeListener(this);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateChangeListener#decoratedStateChanged(org.eclipse.team.ui.mapping.IDecoratedStateChangeEvent)
	 */
	public void teamStateChanged(ITeamStateChangeEvent event) {
		fireStateChangeEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.IRepositoryProviderListener#providerUnmapped(org.eclipse.core.resources.IProject)
	 */
	public void providerUnmapped(IProject project) {
		// We don't need to worry about this
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.IRepositoryProviderListener#providerMapped(org.eclipse.team.core.RepositoryProvider)
	 */
	public void providerMapped(RepositoryProvider provider) {
		String id = provider.getID();
		listenerForStateChangesForId(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		IResourceDelta[] projectDeltas = delta
				.getAffectedChildren(IResourceDelta.ADDED
						| IResourceDelta.CHANGED);
		for (int i = 0; i < projectDeltas.length; i++) {
			IResourceDelta projectDelta = projectDeltas[i];
			IResource resource = projectDelta.getResource();
			if ((projectDelta.getFlags() & IResourceDelta.OPEN) != 0
					&& resource.getType() == IResource.PROJECT) {
				IProject project = (IProject) resource;
				if (project.isAccessible()) {
					handleProject(project);
				}
			}
		}
	}
}
