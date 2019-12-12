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
package org.eclipse.team.core.variants;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;

/**
 * A handle that provides access to locally cached resource variants that
 * represent a resource line-up such as a project version or branch.
 *
 * @see AbstractResourceVariantTree
 * @see ResourceVariantTree
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 *              However, clients may subclass
 *              {@link AbstractResourceVariantTree} or
 *              {@link ResourceVariantTree}.
 */
public interface IResourceVariantTree {

	/**
	 * Returns the list of root resources for which this tree may have resource
	 * variants.
	 * @return the list of root resources.
	 */
	public abstract IResource[] roots();

	/**
	 * Returns the members of the local resource that have resource variants in this tree.
	 * The members may or may not exist locally. The resource variants corresponding to the
	 * members can be retrieved using <code>getResourceVariant(IResource)</code>.
	 * @param resource the local resource
	 * @return the members of the local resource for which this tree contains resource variants
	 * @throws TeamException if an error occurs
	 */
	public abstract IResource[] members(IResource resource) throws TeamException;

	/**
	 * Return the resource variant corresponding to the local resource. Return
	 * <code>null</code> if there is no variant for the resource.
	 * @param resource the local resource
	 * @return the resource's variant in this tree
	 * @throws TeamException if an error occurs
	 */
	public abstract IResourceVariant getResourceVariant(IResource resource) throws TeamException;

	/**
	 * Return whether the local resource has a variant in this tree.
	 * @param resource the local resource
	 * @return <code>true</code> if the tree contains a variant for the resource
	 * @throws TeamException if an error occurs
	 */
	public boolean hasResourceVariant(IResource resource) throws TeamException;

	/**
	 * Refreshes the resource variant tree for the specified resources and possibly
	 * their descendants, depending on the depth.
	 * @param resources the resources whose variants should be refreshed
	 * @param depth the depth of the refresh (one of <code>IResource.DEPTH_ZERO</code>,
	 * <code>IResource.DEPTH_ONE</code>, or <code>IResource.DEPTH_INFINITE</code>)
	 * @param monitor a progress monitor
	 * @return the array of resources whose corresponding variants have changed
	 * as a result of the refresh
	 * @throws TeamException if an error occurs
	 */
	public IResource[] refresh(
			IResource[] resources,
			int depth,
			IProgressMonitor monitor) throws TeamException;

	/**
	 * Flush any variants in the tree for the given resource to the depth
	 * specified.
	 * @param resource the resource
	 * @param depth the flush depth (one of <code>IResource.DEPTH_ZERO</code>,
	 * <code>IResource.DEPTH_ONE</code>, or <code>IResource.DEPTH_INFINITE</code>)
	 * @throws TeamException if an error occurs
	 */
	public void flushVariants(IResource resource, int depth) throws TeamException;
}
