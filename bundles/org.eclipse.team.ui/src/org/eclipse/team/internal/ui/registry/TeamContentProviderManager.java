/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.registry;

import java.util.*;

import org.eclipse.core.runtime.*;
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

	Map descriptors;

	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

	public static ITeamContentProviderManager getInstance() {
		if (instance == null)
			instance = new TeamContentProviderManager();
		return instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.registry.ITeamContentProviderManager#getDescriptors()
	 */
	@Override
	public ITeamContentProviderDescriptor[] getDescriptors() {
		lazyInitialize();
		return (ITeamContentProviderDescriptor[]) descriptors.values().toArray(new ITeamContentProviderDescriptor[descriptors.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamContentProviderManager#getContentProviderIds(org.eclipse.team.core.mapping.ISynchronizationScope)
	 */
	@Override
	public String[] getContentProviderIds(ISynchronizationScope scope) {
		List result = new ArrayList();
		ITeamContentProviderDescriptor[] descriptors = getDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			ITeamContentProviderDescriptor descriptor = descriptors[i];
			if (descriptor.isEnabled() && scope.getMappings(descriptor.getModelProviderId()).length > 0)
				result.add(descriptor.getContentExtensionId());
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.registry.ITeamContentProviderManager#getDescriptor(java.lang.String)
	 */
	@Override
	public ITeamContentProviderDescriptor getDescriptor(String modelProviderId) {
		lazyInitialize();
		return (ITeamContentProviderDescriptor)descriptors.get(modelProviderId);
	}

	protected void lazyInitialize() {
		if (descriptors != null)
			return;
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(TeamUIPlugin.ID, PT_TEAM_CONTENT_PROVIDERS);
		IExtension[] extensions = point.getExtensions();
		descriptors = new HashMap(extensions.length * 2);
		for (int i = 0, imax = extensions.length; i < imax; i++) {
			ITeamContentProviderDescriptor desc = null;
			try {
				desc = new TeamContentProviderDescriptor(extensions[i]);
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
		for (int i = 0; i < allListeners.length; i++) {
			final IPropertyChangeListener listener = (IPropertyChangeListener)allListeners[i];
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
		List previouslyEnabled = new ArrayList();
		for (Iterator iter = this.descriptors.values().iterator(); iter.hasNext();) {
			TeamContentProviderDescriptor descriptor = (TeamContentProviderDescriptor) iter.next();
			if (descriptor.isEnabled()) {
				previouslyEnabled.add(descriptor);
				descriptor.setEnabled(false);
			}
		}
		for (int i = 0; i < descriptors.length; i++) {
			TeamContentProviderDescriptor descriptor = (TeamContentProviderDescriptor)descriptors[i];
			descriptor.setEnabled(true);
		}
		enablementChanged(
				(ITeamContentProviderDescriptor[]) previouslyEnabled.toArray(new ITeamContentProviderDescriptor[previouslyEnabled.size()]),
				descriptors);
	}
}
