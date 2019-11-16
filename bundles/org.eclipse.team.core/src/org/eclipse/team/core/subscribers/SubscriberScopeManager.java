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
package org.eclipse.team.core.subscribers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipant;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipantFactory;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;

/**
 * A {@link ISynchronizationScopeManager} that uses a {@link Subscriber} to provide
 * a {@link RemoteResourceMappingContext} and to notify participants when the
 * remote state of resources change.
 * @since 3.2
 */
public class SubscriberScopeManager extends SynchronizationScopeManager implements ISubscriberChangeListener {

	private final Subscriber subscriber;
	private Map<ModelProvider, ISynchronizationScopeParticipant> participants = new HashMap<>();

	/**
	 * Create a manager for the given subscriber and input.
	 * @param name a human readable name for the scope
	 * @param inputMappings the input mappings
	 * @param subscriber the subscriber
	 * @param consultModels whether models should be consulted when calculating the scope
	 */
	public SubscriberScopeManager(String name, ResourceMapping[] inputMappings, Subscriber subscriber, boolean consultModels) {
		this(name, inputMappings, subscriber, SubscriberResourceMappingContext.createContext(subscriber), consultModels);
	}

	/**
	 * Create a manager for the given subscriber and input.
	 * @param name a human readable name for the scope
	 * @param inputMappings the input mappings
	 * @param subscriber the subscriber
	 * @param context a remote resource mapping context for the subscriber
	 * @param consultModels whether models should be consulted when calculating the scope
	 */
	public SubscriberScopeManager(String name, ResourceMapping[] inputMappings, Subscriber subscriber, RemoteResourceMappingContext context, boolean consultModels) {
		super(name, inputMappings, context, consultModels);
		this.subscriber = subscriber;
	}

	/**
	 * Return the subscriber for this manager.
	 * @return the subscriber for this manager
	 */
	protected Subscriber getSubscriber() {
		return subscriber;
	}

	@Override
	public void dispose() {
		for (ISynchronizationScopeParticipant participant : participants.values()) {
			participant.dispose();
		}
		super.dispose();
	}

	@Override
	public void initialize(IProgressMonitor monitor) throws CoreException {
		ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor1 -> {
			SubscriberScopeManager.super.initialize(monitor1);
			hookupParticipants();
			getSubscriber().addListener(SubscriberScopeManager.this);
		}, getSchedulingRule(), IResource.NONE, monitor);
	}

	@Override
	public ResourceTraversal[] refresh(final ResourceMapping[] mappings, IProgressMonitor monitor) throws CoreException {
		final List<ResourceTraversal[]> result = new ArrayList<>(1);
		ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor1 -> {
			result.add(SubscriberScopeManager.super.refresh(mappings, monitor1));
			hookupParticipants();
		}, getSchedulingRule(), IResource.NONE, monitor);
		if (result.isEmpty())
			return new ResourceTraversal[0];
		return result.get(0);
	}

	/*
	 * Hook up the participants for the participating models.
	 * This is done to ensure that future local and remote changes to
	 * resources will update the resources contained in the scope
	 * appropriately
	 */
	/* private */ void hookupParticipants() {
		for (ModelProvider provider : getScope().getModelProviders()) {
			if (!participants.containsKey(provider)) {
				ISynchronizationScopeParticipant p = createParticipant(provider);
				if (p != null) {
					participants.put(provider, p);
				}
			}
		}
	}

	/*
	 * Obtain a participant through the factory which is obtained using IAdaptable
	 */
	private ISynchronizationScopeParticipant createParticipant(ModelProvider provider) {
		Object factoryObject = provider.getAdapter(ISynchronizationScopeParticipantFactory.class);
		if (factoryObject instanceof ISynchronizationScopeParticipantFactory) {
			ISynchronizationScopeParticipantFactory factory = (ISynchronizationScopeParticipantFactory) factoryObject;
			return factory.createParticipant(provider, this.getScope());
		}
		return null;
	}

	@Override
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
		List<IResource> changedResources = new ArrayList<>();
		List<IProject> changedProjects = new ArrayList<>();
		for (ISubscriberChangeEvent event : deltas) {
			if ((event.getFlags() & (ISubscriberChangeEvent.ROOT_ADDED | ISubscriberChangeEvent.ROOT_REMOVED)) != 0) {
				changedProjects.add(event.getResource().getProject());
			}
			if ((event.getFlags() & ISubscriberChangeEvent.SYNC_CHANGED) != 0) {
				changedResources.add(event.getResource());
			}
		}
		fireChange(changedResources.toArray(new IResource[changedResources.size()]), changedProjects.toArray(new IProject[changedProjects.size()]));
	}

	private void fireChange(final IResource[] resources, final IProject[] projects) {
		final Set<ResourceMapping> result = new HashSet<>();
		for (final ISynchronizationScopeParticipant participant : participants.values()) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					for (ResourceMapping mapping : participant
							.handleContextChange(SubscriberScopeManager.this.getScope(), resources, projects)) {
						result.add(mapping);
					}
				}
				@Override
				public void handleException(Throwable exception) {
					// Handled by platform
				}
			});
		}
		if (!result.isEmpty()) {
			refresh(result.toArray(new ResourceMapping[result.size()]));
		}
	}

}
