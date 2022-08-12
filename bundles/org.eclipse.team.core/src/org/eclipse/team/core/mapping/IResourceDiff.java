/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.core.mapping;

import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.history.IFileRevision;

/**
 * A resource diff represents the changes between two resources. The diff can be
 * used to describe the change between an ancestor and remote, an ancestor and
 * local or between the local and a remote for two-way comparisons.
 *
 * @see IDiffTree
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients that need to create deltas should instead use or
 *              subclass
 *              {@link org.eclipse.team.core.mapping.provider.ResourceDiff}
 */
public interface IResourceDiff extends ITwoWayDiff {

	/**
	 * Change constant (bit mask) indicating that the resource was opened or closed.
	 * For example, if the current state of the resource is open then
	 * it was previously closed.
	 *
	 * @see ITwoWayDiff#getFlags()
	 * @see org.eclipse.core.resources.IResourceDelta#OPEN
	 */
	public static final int OPEN = 0x10000;

	/**
	 * Change constant (bit mask) indicating that a project's description has changed.
	 *
	 * @see ITwoWayDiff#getFlags()
	 * @see org.eclipse.core.resources.IResourceDelta#DESCRIPTION
	 */
	public static final int DESCRIPTION = 0x20000;

	/**
	 * Return the local resource to which this diff applies.
	 * @return the local resource to which this diff applies
	 */
	public IResource getResource();

	/**
	 * Return a handle to the file state representing the "before" state
	 * of the file used to calculate this diff. A <code>null</code> is
	 * returned if the resource is not a file or if the file does not exist in
	 * the before state. If a file state is returned, clients should still
	 * check the {@link IFileState#exists()} method to see if the file
	 * existed in the before state.
	 *
	 * @return a handle to the file state representing the "before" state
	 *         used to calculate this diff
	 */
	public IFileRevision getBeforeState();

	/**
	 * Return a handle to the file state representing the "after" state
	 * of the file used to calculate this diff. A <code>null</code> is
	 * returned if the resource is not a file or if the file does not exist in
	 * the after state. If a file state is returned, clients should still
	 * check the {@link IFileState#exists()} method to see if the file
	 * existed in the after state.
	 *
	 * @return a handle to the file state representing the "before" state
	 *         used to calculate this diff
	 */
	public IFileRevision getAfterState();
}
