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

	private Set<IResource> changes = new HashSet<>();
	private Set<IResource> addedRoots = new HashSet<>();
	private Set<IResource> removedRoots = new HashSet<>();

	public TeamStateChangeEvent() {
		super();
	}

	/**
	 * Convenience constructor for creating an event from a subscriber change.
	 * @param deltas the set of subscriber changes
	 */
	public TeamStateChangeEvent(ISubscriberChangeEvent[] deltas) {
		for (ISubscriberChangeEvent event : deltas) {
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

	@Override
	public IResource[] getAddedRoots() {
		return addedRoots.toArray(new IResource[addedRoots.size()]);
	}

	@Override
	public IResource[] getRemovedRoots() {
		return removedRoots.toArray(new IResource[removedRoots.size()]);
	}

	@Override
	public IResource[] getChangedResources() {
		return changes.toArray(new IResource[changes.size()]);
	}

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
