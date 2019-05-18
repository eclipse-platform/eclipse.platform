/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.core.history;

import java.net.URI;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.provider.FileRevision;

/**
 * Represents an individual revision of a file.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients can instead subclass {@link FileRevision}.
 */
public interface IFileRevision {

	/**
	 * Returns the storage for this file revision.
	 * If the returned storage is an instance of
	 * <code>IFile</code> clients can assume that this
	 * file state represents the current state of
	 * the returned <code>IFile</code>.
	 * @param monitor a progress monitor
	 * @return IStorage containing file storage
	 * @throws CoreException if an error occurs
	 */
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns the name of the file to which this state is associated
	 * @return String containing the name of the file
	 */
	public String getName();

	/**
	 * Returns the URI of the file to which this state is associated
	 * or <code>null</code> if the file does not have a URI.
	 * @return URI of the file to which this state is associated
	 */
	public URI getURI();

	/**
	 * Returns the time stamp of this revision as a long or <code>-1</code>
	 * if the timestamp is unknown.
	 *
	 * @return a long that represents the time of this revision as the number of milliseconds
	 * since the base time
	 *
	 * @see java.lang.System#currentTimeMillis()
	 */
	public long getTimestamp();

	/**
	 * Returns whether the file represented by this state exists.
	 * @return whether the file represented by this state exists
	 */
	public boolean exists();

	/**
	 * Returns the <em>unique</em> identifier for this file revision
	 * or <code>null</code> if one is not available. If <code>null</code>
	 * is returned, clients can use the timestamp to differentiate
	 * revisions.
	 * @return the <em>unique</em> identifier for this file revision
	 * or <code>null</code>
	 */
	public String getContentIdentifier();

	/**
	 *  Returns the author of this revision or <code>null</code> if
	 *  this information is not available.
	 *
	 *  @return the author of this revision or <code>null</code>
	 */
	public String getAuthor();

	/**
	 * Returns the comment for this file revision or <code>null</code> if
	 * this information is not available.
	 *
	 * @return the comment for this file revision or <code>null</code>
	 */
	public String getComment();

	/**
	 * Returns the branches names of file revision.
	 *
	 * @return an array of ITag's if branch names exist for this revision
	 * or an empty ITag array if no names exist
	 * @since 3.6
	 */
	public ITag[] getBranches();

	/**
	 * Returns the set of tags available for this file revision.
	 *
	 * @return an array of ITag's if ITags exist for this revision or an empty ITag array
	 * if no tags exist
	 */
	public ITag[] getTags();

	/**
	 * Returns whether this particular file revision has at least one supported property
	 * missing. If the revision is missing some queries
	 * clients can use {@link #withAllProperties(IProgressMonitor)}.
	 *
	 * @return whether this particular file revision has at least one supported property
	 * missing
	 */
	public boolean isPropertyMissing();

	/**
	 * Returns an {@link IFileRevision} with all supported properties present.
	 * @param monitor a monitor
	 * @return a complete version of this file revision or null
	 * @throws CoreException if an error occurs
	 */
	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException;
}
