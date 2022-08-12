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
package org.eclipse.team.ui.synchronize;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.registry.TeamDecoratorDescription;
import org.eclipse.team.internal.ui.registry.TeamDecoratorManager;
import org.eclipse.team.ui.mapping.ITeamStateChangeEvent;
import org.eclipse.team.ui.mapping.ITeamStateChangeListener;
import org.eclipse.team.ui.mapping.ITeamStateProvider;
import org.eclipse.team.ui.mapping.SynchronizationStateTester;
import org.eclipse.ui.PlatformUI;

/**
 * A team state provider is used by the {@link SynchronizationStateTester} to obtain
 * the team state for model elements. A team state provider is
 * associated with a {@link RepositoryProviderType} using the adaptable mechanism. A default
 * team state provider that uses the subscriber of the type is provided.
 * <p>
 * Clients may subclass this class.
 *
 * @see IAdapterManager
 * @see RepositoryProviderType
 * @since 3.2
 */
public abstract class TeamStateProvider implements ITeamStateProvider {

	private ListenerList<ITeamStateChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

	/**
	 * Determine if the decorator for the element is enabled by consulting the
	 * <code>teamDecorator</code> extension point to determine the decorator
	 * id associated with the resources the element maps to. Subclasses may
	 * override.
	 *
	 * @see org.eclipse.team.ui.mapping.ITeamStateProvider#isDecorationEnabled(java.lang.Object)
	 */
	@Override
	public boolean isDecorationEnabled(Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			IProject[] projects = mapping.getProjects();
			return internalIsDecorationEnabled(projects);
		}
		return false;
	}

	/**
	 * Determine the decorated state for the element by consulting the
	 * <code>teamDecorator</code> extension point to get the decorated state
	 * mask associated with the resources the element maps to. Subclasses may
	 * override.
	 *
	 * @see org.eclipse.team.ui.mapping.ITeamStateProvider#getDecoratedStateMask(java.lang.Object)
	 */
	@Override
	public int getDecoratedStateMask(Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			IProject[] projects = mapping.getProjects();
			return internalGetDecoratedStateMask(projects);
		}
		return 0;
	}

	@Override
	public void addDecoratedStateChangeListener(ITeamStateChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeDecoratedStateChangeListener(ITeamStateChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Fire the change event to all listeners.
	 * @param event the change event
	 */
	protected final void fireStateChangeEvent(final ITeamStateChangeEvent event) {
		Object[] allListeners = listeners.getListeners();
		for (Object l : allListeners) {
			final ITeamStateChangeListener listener = (ITeamStateChangeListener) l;
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.teamStateChanged(event);
				}
				@Override
				public void handleException(Throwable exception) {
					// Logged by the runner
				}
			});
		}
	}

	private int internalGetDecoratedStateMask(IProject[] projects) {
		int stateMask = 0;
		String[] providerIds = getProviderIds(projects);
		for (String providerId : providerIds) {
			stateMask |= internalGetDecoratedStateMask(providerId);
		}
		return stateMask;
	}

	private int internalGetDecoratedStateMask(String providerId) {
		TeamDecoratorDescription decoratorDescription = TeamDecoratorManager
				.getInstance().getDecoratorDescription(providerId);
		if (decoratorDescription != null)
			return decoratorDescription.getDecoratedDirectionFlags();
		return 0;
	}

	private String[] getProviderIds(IProject[] projects) {
		Set<String> providerIds = new HashSet<>();
		for (IProject project : projects) {
			String id = getProviderId(project);
			if (id != null)
				providerIds.add(id);
		}
		return providerIds.toArray(new String[providerIds.size()]);
	}

	private String getProviderId(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider != null)
			return provider.getID();
		return null;
	}

	private boolean internalIsDecorationEnabled(IProject[] projects) {
		String[] providerIds = getProviderIds(projects);
		for (String providerId : providerIds) {
			if (internalIsDecorationEnabled(providerId)) {
				return true;
			}
		}
		return false;
	}

	private boolean internalIsDecorationEnabled(String providerId) {
		String decoratorId = getDecoratorId(providerId);
		if (decoratorId != null) {
			return PlatformUI.getWorkbench().getDecoratorManager().getEnabled(
					decoratorId);
		}
		return false;
	}

	private String getDecoratorId(String providerId) {
		TeamDecoratorDescription decoratorDescription = TeamDecoratorManager
				.getInstance().getDecoratorDescription(providerId);
		if (decoratorDescription != null)
			return decoratorDescription.getDecoratorId();
		return null;
	}

}
