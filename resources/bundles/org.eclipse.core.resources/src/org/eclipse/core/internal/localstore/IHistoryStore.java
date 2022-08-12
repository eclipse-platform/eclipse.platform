/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.core.internal.localstore;

import java.io.InputStream;
import java.util.Set;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.IManager;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;

/**
 * The history store is an association of paths to file states.
 * Typically the path is the full path of a resource in the workspace.
 * <p>
 * History store policies are stored in the org.eclipse.core.resources'
 * plug-in preferences.
 * </p>
 *
 * @since 3.1
 */
public interface IHistoryStore extends IManager {

	/**
	 * Add an entry to the history store, represented by the given key. Return the
	 * file state for the newly created entry or<code>null</code> if it couldn't
	 * be created.
	 * <p>
	 * Note: Depending on the history store implementation, some of the history
	 * store policies can be applied during this method call to determine whether
	 * or not the entry should be added to the store.
	 * </p>
	 * @param key full workspace path to resource being logged
	 * @param localFile local file system file handle
	 * @param fileInfo The IFileInfo for the entry
	 * @return the file state or <code>null</code>
	 *
	 * TODO: should this method take a progress monitor?
	 *
	 * TODO: look at #getFileFor(). Is there a case where we wouldn't want to
	 * copy over the file attributes to the local history? If we did that here then
	 * we wouldn't have to have that other API.
	 */
	IFileState addState(IPath key, IFileStore localFile, IFileInfo fileInfo, boolean moveContents);

	/**
	 * Returns the paths of all files with entries in this history store at or below
	 * the given workspace resource path to the given depth. Returns an
	 * empty set if there are none.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 * @param path full workspace path to resource
	 * @param depth depth limit: one of <code>DEPTH_ZERO</code>, <code>DEPTH_ONE</code>
	 *    or <code>DEPTH_INFINITE</code>
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return the set of paths for files that have at least one history entry
	 *   (element type: <code>IPath</code>)
	 */
	Set<IPath> allFiles(IPath path, int depth, IProgressMonitor monitor);

	/**
	 * Clean this store applying the current policies.
	 * <p>
	 * Note: The history store policies are stored as part of
	 * the org.eclipse.core.resource plug-in's preferences and
	 * include such settings as: maximum file size, maximum number
	 * of states per file, file expiration date, etc.
	 * </p>
	 * <p>
	 * Note: Depending on the implementation of the history store,
	 * if all the history store policies are applying when the entries
	 * are first added to the store then this method might be a no-op.
	 * </p>
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	void clean(IProgressMonitor monitor);

	/**
	 * Closes the history store for the given resource.
	 */
	void closeHistoryStore(IResource resource);

	/**
	 * Copies the history store information from the source path given destination path.
	 * Note that destination may already have some history store information. Also note
	 * that this is a DEPTH_INFINITY operation. That is, history will be copied for partial
	 * matches of the source path.
	 *
	 * @param source the resource containing the original copy of the history store information
	 * @param destination the target resource where to copy the history
	 * @param moving whether the history is being copied due to a resource move
	 *
	 * TODO: should this method take a progress monitor?
	 */
	void copyHistory(IResource source, IResource destination, boolean moving);

	/**
	 * Verifies existence of specified resource in the history store. Returns
	 * <code>true</code> if the file state exists and <code>false</code>
	 * otherwise.
	 * <p>
	 * Note: This method cannot take a progress monitor since it is surfaced
	 * to the real API via IFileState#exists() which doesn't take a progress
	 * monitor.
	 * </p>
	 * @param target the file state to be verified
	 * @return <code>true</code>  if file state exists,
	 * 	and <code>false</code> otherwise
	 */
	boolean exists(IFileState target);

	/**
	 * Returns an input stream containing the file contents of the specified state.
	 * The user is responsible for closing the returned stream.
	 * <p>
	 * Note: This method cannot take a progress monitor since it is
	 * surfaced through to the real API via IFileState#getContents which
	 * doesn't take one.
	 * </p>
	 * @param target File state for which an input stream is requested
	 * @return the stream for requested file state
	 */
	InputStream getContents(IFileState target) throws CoreException;

	/**
	 * Returns an array of all states available for the specified resource path or
	 * an empty array if none.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 * @param path the resource path
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return the list of file states
	 */
	IFileState[] getStates(IPath path, IProgressMonitor monitor);

	/**
	 * Remove all of the file states for the given resource path and
	 * all its children. If the workspace root path is the given argument,
	 * then all history for this store is removed.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 * @param path the resource path whose history is to be removed
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	void remove(IPath path, IProgressMonitor monitor);

	/**
	 * Go through the history store and remove all of the unreferenced states.
	 *
	 * As of 3.0, this method is used for testing purposes only. Otherwise the history
	 * store is garbage collected during the #clean method.
	 */
	void removeGarbage();
}
