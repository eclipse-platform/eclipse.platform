/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.mapping.*;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;

/**
 * A {@link ISynchronizationScopeManager} that uses a {@link Subscriber} to provide
 * a {@link RemoteResourceMappingContext} and to notify participants when the
 * remote state of resources change.
 * @since 3.2
 */
public class SubscriberScopeManager extends SynchronizationScopeManager implements ISubscriberChangeListener {

	private final Subscriber subscriber;
	private Map participants = new HashMap();

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

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IResourceMappingScopeManager#dispose()
	 */
	public void dispose() {
		for (Iterator iter = participants.values().iterator(); iter.hasNext();) {
			ISynchronizationScopeParticipant p = (ISynchronizationScopeParticipant) iter.next();
			p.dispose();
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.provider.ResourceMappingScopeManager#initialize(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void initialize(IProgressMonitor monitor) throws CoreException {
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				SubscriberScopeManager.super.initialize(monitor);
				hookupParticipants();
				getSubscriber().addListener(SubscriberScopeManager.this);
			}
		}, getSchedulingRule(), IResource.NONE, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.provider.ResourceMappingScopeManager#refresh(org.eclipse.core.resources.mapping.ResourceMapping[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ResourceTraversal[] refresh(final ResourceMapping[] mappings, IProgressMonitor monitor) throws CoreException {
		final List result = new ArrayList(1);
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				result.add(SubscriberScopeManager.super.refresh(mappings, monitor));
				hookupParticipants();
			}
		}, getSchedulingRule(), IResource.NONE, monitor);
		if (result.isEmpty())
			return new ResourceTraversal[0];
		return (ResourceTraversal[])result.get(0);
	}

	/*
	 * Hook up the participants for the participating models.
	 * This is done to ensure that future local and remote changes to
	 * resources will update the resources contained in the scope
	 * appropriately
	 */
	/* private */ void hookupParticipants() {
		ModelProvider[] providers = getScope().getModelProviders();
		for (int i = 0; i < providers.length; i++) {
			ModelProvider provider = providers[i];
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISubscriberChangeListener#subscriberResourceChanged(org.eclipse.team.core.subscribers.ISubscriberChangeEvent[])
	 */
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
		List changedResources = new ArrayList();
		List changedProjects = new ArrayList();
		for (int i = 0; i < deltas.length; i++) {
			ISubscriberChangeEvent event = deltas[i];
			if ((event.getFlags() & (ISubscriberChangeEvent.ROOT_ADDED | ISubscriberChangeEvent.ROOT_REMOVED)) != 0) {
				changedProjects.add(event.getResource().getProject());
			}
			if ((event.getFlags() & ISubscriberChangeEvent.SYNC_CHANGED) != 0) {
				changedResources.add(event.getResource());
			}
		}
		fireChange((IResource[]) changedResources.toArray(new IResource[changedResources.size()]), (IProject[]) changedProjects.toArray(new IProject[changedProjects.size()]));
	}

	private void fireChange(final IResource[] resources, final IProject[] projects) {
		final Set result = new HashSet();
		ISynchronizationScopeParticipant[] handlers = (ISynchronizationScopeParticipant[]) participants.values().toArray(new ISynchronizationScopeParticipant[participants.size()]);
		for (int i = 0; i < handlers.length; i++) {
			final ISynchronizationScopeParticipant participant = handlers[i];
			SafeRunner.run(new ISafeRunnable() {
				public void run() throws Exception {
					ResourceMapping[] mappings = participant.handleContextChange(SubscriberScopeManager.this.getScope(), resources, projects);
					for (int j = 0; j < mappings.length; j++) {
						ResourceMapping mapping = mappings[j];
						result.add(mapping);
					}
				}
				public void handleException(Throwable exception) {
					// Handled by platform
				}
			});
		}
		if (!result.isEmpty()) {
			refresh((ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]));
		}
	}

}
