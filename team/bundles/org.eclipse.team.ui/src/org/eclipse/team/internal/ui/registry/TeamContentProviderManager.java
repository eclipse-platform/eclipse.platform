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
package org.eclipse.team.internal.ui.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;

/**
 * Manages the team content provider extension point
 */
public class TeamContentProviderManager implements ITeamContentProviderManager {

	public static final String PT_TEAM_CONTENT_PROVIDERS = "teamContentProviders"; //$NON-NLS-1$

	private static ITeamContentProviderManager instance;

	Map<String, ITeamContentProviderDescriptor> descriptors;

	private ListenerList<IPropertyChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

	public static ITeamContentProviderManager getInstance() {
		if (instance == null)
			instance = new TeamContentProviderManager();
		return instance;
	}

	@Override
	public ITeamContentProviderDescriptor[] getDescriptors() {
		lazyInitialize();
		return descriptors.values().toArray(new ITeamContentProviderDescriptor[descriptors.size()]);
	}

	@Override
	public String[] getContentProviderIds(ISynchronizationScope scope) {
		List<String> result = new ArrayList<>();
		ITeamContentProviderDescriptor[] descriptors = getDescriptors();
		for (ITeamContentProviderDescriptor descriptor : descriptors) {
			if (descriptor.isEnabled() && scope.getMappings(descriptor.getModelProviderId()).length > 0)
				result.add(descriptor.getContentExtensionId());
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	public ITeamContentProviderDescriptor getDescriptor(String modelProviderId) {
		lazyInitialize();
		return descriptors.get(modelProviderId);
	}

	protected void lazyInitialize() {
		if (descriptors != null)
			return;
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(TeamUIPlugin.ID, PT_TEAM_CONTENT_PROVIDERS);
		IExtension[] extensions = point.getExtensions();
		descriptors = new HashMap<>(extensions.length * 2);
		for (IExtension extension : extensions) {
			ITeamContentProviderDescriptor desc = null;
			try {
				desc = new TeamContentProviderDescriptor(extension);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
			if (desc != null)
				descriptors.put(desc.getModelProviderId(), desc);
		}
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	private void firePropertyChange(final PropertyChangeEvent event) {
		Object[] allListeners = listeners.getListeners();
		for (Object l : allListeners) {
			final IPropertyChangeListener listener = (IPropertyChangeListener) l;
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.propertyChange(event);
				}
				@Override
				public void handleException(Throwable exception) {
					// handler by runner
				}
			});
		}
	}

	public void enablementChanged(ITeamContentProviderDescriptor[] oldEnabled, ITeamContentProviderDescriptor[] newEnabled) {
		firePropertyChange(new PropertyChangeEvent(this, PROP_ENABLED_MODEL_PROVIDERS, oldEnabled, newEnabled));
	}

	@Override
	public void setEnabledDescriptors(ITeamContentProviderDescriptor[] descriptors) {
		List<ITeamContentProviderDescriptor> previouslyEnabled = new ArrayList<>();
		for (ITeamContentProviderDescriptor element : this.descriptors.values()) {
			TeamContentProviderDescriptor descriptor = (TeamContentProviderDescriptor) element;
			if (descriptor.isEnabled()) {
				previouslyEnabled.add(descriptor);
				descriptor.setEnabled(false);
			}
		}
		for (ITeamContentProviderDescriptor d : descriptors) {
			TeamContentProviderDescriptor descriptor = (TeamContentProviderDescriptor) d;
			descriptor.setEnabled(true);
		}
		enablementChanged(previouslyEnabled.toArray(new ITeamContentProviderDescriptor[previouslyEnabled.size()]),
				descriptors);
	}
}
