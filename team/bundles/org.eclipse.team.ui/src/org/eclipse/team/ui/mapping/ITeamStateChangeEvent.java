/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.ui.mapping;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.synchronize.TeamStateChangeEvent;

/**
 * A description of the team state changes that have occurred. This event
 * indicates the resources for which the team state may have changed.
 * However, it may be the case that the state did not actually change. Clients
 * that wish to determine if the state ha changed must cache the previous state
 * and re-obtain the state when they receive this event. Also, the event may
 * not include team state changes that resulted from local changes. Clients should listen
 * for resource changes as well.
 * <p>
 * This interface is not intended to be implemented by clients. Clients should
 * instead use {@link TeamStateChangeEvent}.
 *
 * @see ITeamStateChangeListener
 * @see ITeamStateProvider
 * @see TeamStateChangeEvent
 *
 * @since 3.2
 */
public interface ITeamStateChangeEvent {

	/**
	 * Return the set of resources that were previously undecorated
	 * but are now decorated.
	 * @return the set of resources that were previously undecorated
	 * but are now decorated.
	 */
	public IResource[] getAddedRoots();

	/**
	 * Return the set of resources that were previously decorated
	 * but are now undecorated.
	 * @return the set of resources that were previously decorated
	 * but are now undecorated.
	 */
	public IResource[] getRemovedRoots();

	/**
	 * Return the set of resources whose decorated state has changed.
	 * @return the set of resources whose decorated state has changed.
	 */
	public IResource[] getChangedResources();

	/**
	 * Return whether the resource has any state changes. This returns
	 * <code>true</code> if the resource is included in the set
	 * of changes returned by {@link #getChangedResources()} or
	 * if it is a descendant of a root that is present in a set
	 * returned by {@link #getAddedRoots()} or {@link #getRemovedRoots()}.
	 *
	 * @param resource the resource
	 * @return whether the resource has any state changes
	 */
	public boolean hasChange(IResource resource);

}
