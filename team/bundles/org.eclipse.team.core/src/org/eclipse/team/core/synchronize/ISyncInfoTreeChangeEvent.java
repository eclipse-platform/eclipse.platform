/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.core.synchronize;

import org.eclipse.core.resources.IResource;

/**
 * This is a change event that provides access to changes in subtrees
 * that contain the out-of-sync resources. It is the event type
 * provided by {@link SyncInfoTree} when it notifies listeners
 * of changes.
 * @since 3.0
 */
public interface ISyncInfoTreeChangeEvent extends ISyncInfoSetChangeEvent {

	/**
	 * Returns the highest parent resources of all newly added elements available in this event
	 * by calling <code>getAddedResources()</code>. In other words, it returns the set of all
	 * parent containers that did not previously have descendants in the sync set but are direct
	 * children of containers that did previously have descendants in the set.
	 * <p>
	 * These roots are provided in order
	 * to allow listeners to optimize the reconciliation of hierarchical views of
	 * the <code>SyncInfoSet</code> contents.
	 *
	 * @return parents of all newly added elements  or an empty list if this event
	 * doesn't contain added resources.
	 */
	public IResource[] getAddedSubtreeRoots();

	/**
	 * Returns the highest parent resources of all newly removed elements available in this event
	 * by calling <code>getRemovedResources()</code>. In other words, it returns the set of all
	 * parent containers that previously had descendants in the sync set but are direct
	 * children of containers that still have descendants in the set.
	 * <p>
	 * These roots are provided in order
	 * to allow listeners to optimize the reconciliation of hierarchical views of
	 * the <code>SyncInfoSet</code> contents.
	 *
	 * @return parents of all newly removed elements.  or an empty list if this event
	 * doesn't contain added resources.
	 */
	public IResource[] getRemovedSubtreeRoots();
}
