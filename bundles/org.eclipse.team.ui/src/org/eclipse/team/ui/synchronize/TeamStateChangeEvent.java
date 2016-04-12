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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.ui.mapping.ITeamStateChangeEvent;

/**
 * An implementation of {@link ITeamStateChangeEvent}.
 * <p>
 * This class is not intended to be subclassed by clients.
 *
 * @since 3.2
 */
public class TeamStateChangeEvent implements ITeamStateChangeEvent {

	private Set changes = new HashSet();
	private Set addedRoots = new HashSet();
	private Set removedRoots = new HashSet();

	public TeamStateChangeEvent() {
		super();
	}

	/**
	 * Convenience constructor for creating an event from a subscriber change.
	 * @param deltas the set of subscriber changes
	 */
	public TeamStateChangeEvent(ISubscriberChangeEvent[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			ISubscriberChangeEvent event = deltas[i];
			IResource resource = event.getResource();
			if ((event.getFlags() & ISubscriberChangeEvent.ROOT_ADDED) != 0)
				rootAdded(resource);
			if ((event.getFlags() & ISubscriberChangeEvent.ROOT_REMOVED) != 0)
				rootRemoved(resource);
			if ((event.getFlags() & ISubscriberChangeEvent.SYNC_CHANGED) != 0)
				changed(resource);
			// Indicate that the ancestors may have changed as well
			while (resource.getType() != IResource.PROJECT) {
				resource = resource.getParent();
				changed(resource);
			}
		}
	}

	/**
	 * The given resource has changed state.
	 * @param resource the resource whose state has changed
	 */
	public void changed(IResource resource) {
		changes.add(resource);
	}

	/**
	 * The given root resource has been removed.
	 * @param resource the resource
	 */
	public void rootRemoved(IResource resource) {
		removedRoots.add(resource);
	}

	/**
	 * The given root resource has been added.
	 * @param resource the resource
	 */
	public void rootAdded(IResource resource) {
		addedRoots.add(resource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateChangeEvent#getAddedRoots()
	 */
	@Override
	public IResource[] getAddedRoots() {
		return (IResource[]) addedRoots.toArray(new IResource[addedRoots.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateChangeEvent#getRemovedRoots()
	 */
	@Override
	public IResource[] getRemovedRoots() {
		return (IResource[]) removedRoots.toArray(new IResource[removedRoots.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateChangeEvent#getChangedResources()
	 */
	@Override
	public IResource[] getChangedResources() {
		return (IResource[]) changes.toArray(new IResource[changes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateChangeEvent#hasChange(org.eclipse.core.resources.IResource)
	 */
	@Override
	public boolean hasChange(IResource resource) {
		if (changes.contains(resource))
			return true;
		if (isChildOfChangedRoot(resource)) {
			return true;
		}
		return false;
	}

	private boolean isChildOfChangedRoot(IResource resource) {
		if (resource == null || resource.getType() == IResource.ROOT)
			return false;
		if (addedRoots.contains(resource) || removedRoots.contains(resource))
			return true;
		return isChildOfChangedRoot(resource.getParent());
	}

}
