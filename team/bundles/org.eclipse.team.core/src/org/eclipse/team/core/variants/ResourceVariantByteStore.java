/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.core.variants;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.team.core.TeamException;

/**
 * The purpose of a <code>ResourceVariantByteStore</code> is to support the caching of
 * the synchronization bytes for the resource variants that represent
 * a resource line-up of interest such as a version, baseline or branch. The
 * cache stores bytes in order to minimize the memory footprint of the tree. It is the
 * responsibility of the client of this API to cache enough bytes to meaningfully identify
 * a resource variant (and possibly create an {@link IResourceVariant} handle from them).
 * <p>
 * The bytes for a resource variant are accessed using the local <code>IResource</code> handle
 * that corresponds to the resource variant (using the <code>getBytes</code> method).
 * The potential children of a resource variant are also accessed
 * by using the local handle that corresponds to the resource variant
 * (using the <code>members</code> method).
 *
 * @since 3.0
 */
public abstract class ResourceVariantByteStore {

	/**
	 * Dispose of any cached sync bytes when this cache is no longer needed.
	 */
	public abstract void dispose();

	/**
	 * Return the bytes for the variant corresponding the given local resource.
	 * A return value of <code>null</code> means that no bytes have been stored
	 * for the resource variant. It is up to the client to determine whether
	 * this means that the resource variant does not exist or that it has not been
	 * fetched or otherwise determined yet.
	 * @param resource the local resource
	 * @return the bytes that represent the resource's variant
	 * @throws TeamException if an error occurs
	 */
	public abstract byte[] getBytes(IResource resource) throws TeamException;

	/**
	 * Set the bytes for the variant corresponding the given local resource.
	 * The bytes should never be <code>null</code>. If it is known that the remote
	 * does not exist, <code>deleteBytes(IResource)</code> should be used instead.
	 * If the sync bytes for the remote are stale and should be removed,
	 * <code>flushBytes(IResouce, int)</code> should be called.
	 * @param resource the local resource
	 * @param bytes the bytes that represent the resource's variant
	 * @return <code>true</code> if the bytes changed
	 * @throws TeamException if an error occurs
	 */
	public abstract boolean setBytes(IResource resource, byte[] bytes) throws TeamException;

	/**
	 * Remove the bytes from the tree for the resource variants corresponding to the
	 * given local resource and its descendants to the given depth.
	 * After the bytes are removed, <code>getBytes(resource)</code> will
	 * return <code>null</code> for the affected resources.
	 * @param resource the local resource
	 * @param depth the depth of the operation (one of <code>IResource.DEPTH_ZERO</code>,
	 * <code>IResource.DEPTH_ONE</code>, or <code>IResource.DEPTH_INFINITE</code>)
	 * @return <code>true</code> if there were bytes present which were removed
	 * @throws TeamException if an error occurs
	 */
	public abstract boolean flushBytes(IResource resource, int depth) throws TeamException;

	/**
	 * Method called to indicate that it is known that there is no variant
	 * associated with the local resource. Subclasses may handle this information in
	 * different ways. The <code>flush(IResource, int)</code> method should be used
	 * in the cases where a client wishes to remove bytes for other reason.
	 *
	 * @param resource the local resource
	 * @return <code>true</code> if this changes the bytes for the variant
	 * @throws TeamException if an error occurs
	 */
	public abstract boolean deleteBytes(IResource resource) throws TeamException;

	/**
	 * Return the children of the given resource that have resource variants in this
	 * tree.
	 *
	 * @param resource the parent resource
	 * @return the members who have resource variants in this tree.
	 * @throws TeamException if an error occurs
	 */
	public abstract IResource[] members(IResource resource) throws TeamException;

	/**
	 * Helper method to compare two byte arrays for equality
	 * @param syncBytes1 the first byte array or <code>null</code>
	 * @param syncBytes2 the second byte array or <code>null</code>
	 * @return whether the two arrays are equal (i.e. same content)
	 */
	protected boolean equals(byte[] syncBytes1, byte[] syncBytes2) {
		if (syncBytes1 == null) {
			return syncBytes2 == null;
		} else if (syncBytes2 == null) {
			return false;
		}
		if (syncBytes1.length != syncBytes2.length) return false;
		for (int i = 0; i < syncBytes1.length; i++) {
			if (syncBytes1[i] != syncBytes2[i]) return false;
		}
		return true;
	}

	/**
	 * Run the given action which may contain multiple modifications
	 * to the byte store. By default, the action is run. Subclasses
	 * may override to obtain scheduling rules or batch deltas (if
	 * the byte store modifies workspace resources).
	 * @param root the root resource for all modifications
	 * @param runnable the action to perform
	 * @param monitor a progress monitor.
	 * @exception TeamException if the operation failed.
	 * @exception OperationCanceledException if the operation is canceled.
	 */
	public void run(IResource root, IWorkspaceRunnable runnable, IProgressMonitor monitor) throws TeamException {
		try {
			runnable.run(monitor);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}
}
