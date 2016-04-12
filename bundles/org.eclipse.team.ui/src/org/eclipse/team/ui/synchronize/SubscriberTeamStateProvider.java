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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.*;

/**
 * A team state provider that makes use of a {@link Subscriber} to determine the synchronization
 * state. Repository provider types that have a subscriber will get one of these free through the adaptable mechanism.
 * If a repository provider type does not have a subscriber, or it a repository provider type wishes to se a custom
 * provider, they must adapt their {@link RepositoryProviderType} class to an appropriate {@link ITeamStateProvider}.
 * <p>
 * Clients may subclass this class.
 *
 * @since 3.2
 */
public class SubscriberTeamStateProvider extends TeamStateProvider implements ISubscriberChangeListener {

	private Subscriber subscriber;

	/**
	 * Create a provider that determines the synchronization state
	 * from the subscriber. This method registers this provider as a listener
	 * on the subscriber in order to know when to fire state change events.
	 * @param subscriber the subscriber for this provider
	 */
	public SubscriberTeamStateProvider(Subscriber subscriber) {
		this.subscriber = subscriber;
		subscriber.addListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.DecoratedStateProvider#isDecorated(java.lang.Object)
	 */
	@Override
	public boolean hasDecoratedState(Object element) throws CoreException {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			ResourceTraversal[] traversals = mapping.getTraversals(
					ResourceMappingContext.LOCAL_CONTEXT, null);
			for (int i = 0; i < traversals.length; i++) {
				ResourceTraversal traversal = traversals[i];
				IResource[] resources = traversal.getResources();
				for (int j = 0; j < resources.length; j++) {
					IResource resource = resources[j];
					if (getSubscriber().isSupervised(resource))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Obtain the synchronization state of the element. If the model
	 * provider for the element adapts to an
	 * ISynchronizationCompareAdapter, then the adapter is used to determine the
	 * synchronization state. Others, the state is obtained from the subscriber
	 * using {@link Subscriber#getState(ResourceMapping, int, IProgressMonitor)}
	 *
	 * @param element the element
	 * @param stateMask the state mask that indicates which state flags are desired
	 * @param monitor a progress monitor
	 * @return the synchronization state of the element
	 * @throws CoreException
	 */
	protected final int getSynchronizationState(Object element, int stateMask,
			IProgressMonitor monitor) throws CoreException {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			return getSynchronizationState(mapping, stateMask, monitor);
		}
		return IDiff.NO_CHANGE;
	}

	private int getSynchronizationState(ResourceMapping mapping, int stateMask, IProgressMonitor monitor) throws CoreException {
		ISynchronizationCompareAdapter compareAdapter = (ISynchronizationCompareAdapter)Utils.getAdapter(mapping.getModelProvider(), ISynchronizationCompareAdapter.class);
		try {
			if (compareAdapter != null) {
				int state = compareAdapter.getSynchronizationState(this, mapping, stateMask, monitor);
				if (state != -1)
					return state;
			}
			return getSubscriber().getState(mapping, stateMask, monitor);
		} catch (CoreException e) {
			IProject[] projects = mapping.getProjects();
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				// Only through the exception if the project for the mapping
				// is accessible
				if (project.isAccessible()) {
					throw e;
				}
			}
		}
		return IDiff.NO_CHANGE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamStateProvider#getStateDescription(java.lang.Object, int, java.lang.String[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public ITeamStateDescription getStateDescription(Object element, int stateMask,
			String[] properties, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		if (stateMask == USE_DECORATED_STATE_MASK)
			stateMask = getDecoratedStateMask(element);
		return new TeamStateDescription(getSynchronizationState(element, stateMask, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamStateProvider#getResourceMappingContext(java.lang.Object)
	 */
	@Override
	public ResourceMappingContext getResourceMappingContext(Object element) {
		return new SubscriberResourceMappingContext(subscriber, false);
	}

	/**
	 * Return the subscriber associated with this tester.
	 *
	 * @return the subscriber associated with this tester.
	 */
	protected final Subscriber getSubscriber() {
		return subscriber;
	}

	/**
	 * Called when the provider is no longer needed. This method stops listening
	 * to the subscriber. Subclasses may extend this method but must call this
	 * method if they do.
	 */
	public void dispose() {
		subscriber.removeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISubscriberChangeListener#subscriberResourceChanged(org.eclipse.team.core.subscribers.ISubscriberChangeEvent[])
	 */
	@Override
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
		fireStateChangeEvent(new TeamStateChangeEvent(deltas));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamStateProvider#getDecoratedProperties(java.lang.Object)
	 */
	@Override
	public String[] getDecoratedProperties(Object element) {
		return new String[0];
	}
}
