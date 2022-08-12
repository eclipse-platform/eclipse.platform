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
package org.eclipse.team.core.history;

import org.eclipse.team.core.history.provider.FileHistory;

/**
 * Provides a complete set of IFileRevisions that make up this IFileHistory.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients can instead subclass {@link FileHistory}.
 */
public interface IFileHistory {

	/**
	 * Returns the complete set of file revisions for this file (this includes all predecessors
	 * and all descendents)
	 * @return an array containing all of the file revisions for this particular file or
	 * an empty array if this file has no revisions
	 */
	public abstract IFileRevision[] getFileRevisions();

	/**
	 * Returns the file revision that corresponds to the passed in content identifier.
	 * @param id the content identifier that uniquely identifies a file revision.
	 * @return the file revision corresponding to the passed in content id or null if no file revisions
	 * match the given id
	 */
	public abstract IFileRevision getFileRevision(String id);

	/**
	 * Returns the direct predecessors of the given revision.
	 * If the file revision was the result of a single modification,
	 * then only a single predecessor will be returned. However,
	 * if the revision was the result of a merge of multiple
	 * revisions, then all revisions that were merge may be returned
	 * depending on whether the history provider tracks merges.
	 * @param revision a file revision revision
	 * @return the direct predecessors of the given file revision or
	 * an empty array if there are no predecessors
	 */
	public abstract IFileRevision[] getContributors(IFileRevision revision);

	/**
	 * Returns all of the direct descendents of the given in file revision.
	 * Multiple revisions may be returned if the given revision is a branch
	 * point or was merged into another revision. Some history providers may not
	 * track branches or merges, in which case a singel descendant may be returned.
	 *
	 * @param revision the file revision that acts as the root
	 * @return an array containing all of the the descendents or an empty array if the revision has
	 * no direct descendents
	 */
	public abstract IFileRevision[] getTargets(IFileRevision revision);
}
