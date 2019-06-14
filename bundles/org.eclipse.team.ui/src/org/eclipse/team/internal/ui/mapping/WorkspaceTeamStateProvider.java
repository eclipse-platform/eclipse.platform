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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.internal.core.IRepositoryProviderListener;
import org.eclipse.team.internal.core.RepositoryProviderManager;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.ITeamStateChangeEvent;
import org.eclipse.team.ui.mapping.ITeamStateChangeListener;
import org.eclipse.team.ui.mapping.ITeamStateDescription;
import org.eclipse.team.ui.mapping.ITeamStateProvider;
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

	private Map<String, ITeamStateProvider> providers = new HashMap<>();

	public WorkspaceTeamStateProvider() {
		RepositoryProviderManager.getInstance().addListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : allProjects) {
			handleProject(project);
		}
	}

	public void dispose() {
		RepositoryProviderManager.getInstance().removeListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	@Override
	public final boolean isDecorationEnabled(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.isDecorationEnabled(element);
		return false;
	}

	@Override
	public boolean hasDecoratedState(Object element) throws CoreException {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			provider.hasDecoratedState(element);
		return false;
	}

	@Override
	public final int getDecoratedStateMask(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getDecoratedStateMask(element);
		return 0;
	}

	@Override
	public String[] getDecoratedProperties(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getDecoratedProperties(element);
		return new String[0];
	}

	@Override
	public ITeamStateDescription getStateDescription(Object element, int stateMask,
			String[] properties, IProgressMonitor monitor) throws CoreException {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getStateDescription(element, stateMask, properties, monitor);
		return null;
	}

	@Override
	public ResourceMappingContext getResourceMappingContext(Object element) {
		ITeamStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getResourceMappingContext(element);
		return ResourceMappingContext.LOCAL_CONTEXT;
	}

	private ITeamStateProvider getDecoratedStateProvider(Object element) {
		RepositoryProviderType type = getProviderType(element);
		if (type != null)
			return Adapters.adapt(type, ITeamStateProvider.class);
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
		for (IProject project : projects) {
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

	@Override
	public void teamStateChanged(ITeamStateChangeEvent event) {
		fireStateChangeEvent(event);
	}

	@Override
	public void providerUnmapped(IProject project) {
		// We don't need to worry about this
	}

	@Override
	public void providerMapped(RepositoryProvider provider) {
		String id = provider.getID();
		listenerForStateChangesForId(id);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		IResourceDelta[] projectDeltas = delta
				.getAffectedChildren(IResourceDelta.ADDED
						| IResourceDelta.CHANGED);
		for (IResourceDelta projectDelta : projectDeltas) {
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
