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
 * An event generated when a {@link SyncInfoSet} collection is changed. The event contains
 * a description of the changes which include added, changed and removed resources.
 * In some cases, (e.g. when the change is too complicated to be efficiently described
 * using the mechanisms provided by this interface) the event will be a reset. In these
 * cases, the client should ignore any other contents of the event and recalculate
 * from scratch any state that is derived from the <code>SyncInfoSet</code> from
 * which the event originated.
 * <p>
 * The mix of return types, <code>SyncInfo</code> and <code>IResource</code>is required as a result of an optimization
 * included in {@link SyncInfoSet} collections that doesn't maintain <code>SyncInfo</code> objects
 * for in-sync resources.
 *  </p>
 * @see SyncInfoSet#addSyncSetChangedListener(ISyncInfoSetChangeListener)
 * @see ISyncInfoSetChangeListener
 * @since 3.0
 */
public interface ISyncInfoSetChangeEvent {

	/**
	 * Returns newly added out-of-sync <code>SyncInfo</code> elements.
	 *
	 * @return newly added <code>SyncInfo</code> elements or an empty list if this event
	 * doesn't contain added resources.
	 */
	public SyncInfo[] getAddedResources();

	/**
	 * Returns changed <code>SyncInfo</code> elements. The returned elements
	 * are still out-of-sync.
	 *
	 * @return changed <code>SyncInfo</code> elements or an empty list if this event
	 * doesn't contain changes resources.
	 */
	public SyncInfo[] getChangedResources();

	/**
	 * Returns the removed <code>IResource</code> elements for which the set no longer
	 * contains on out-of-sync <code>SyncInfo</code>. The returned elements
	 * are all in-sync resources.
	 *
	 * @return removed <code>SyncInfo</code> elements or an empty list if this event
	 * doesn't contain removed resources.
	 */
	public IResource[] getRemovedResources();

	/**
	 * Returns the {@link SyncInfoSet} that generated these events.
	 *
	 * @return the {@link SyncInfoSet} that generated these events.
	 */
	public SyncInfoSet getSet();

}
