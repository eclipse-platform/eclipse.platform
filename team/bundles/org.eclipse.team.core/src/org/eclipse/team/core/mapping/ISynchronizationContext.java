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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.team.core.ICache;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;

/**
 * Allows a model provider to build a view of their model that includes
 * synchronization information with a remote location (usually a repository).
 * <p>
 * The scope of the context is defined when the context is created. The creator
 * of the scope may affect changes on the scope which will result in property
 * change events from the scope and may result in change events from the diff
 * tree. Clients should note that it is possible that a change in the scope will
 * result in new resources with differences being covered by the scope but not
 * result in a change event from the diff tree. This can occur because the set
 * may already have contained a diff for the resource with the understanding
 * that the client would have ignored it. Consequently, clients should listen to
 * both sources in order to guarantee that they update any dependent state
 * appropriately.
 * <p>
 * The diff tree associated with this context may be updated asynchronously in
 * response to calls to any method of this context (e.g. refresh methods) that
 * may result in changes in the synchronization state of resources. It may also
 * get updated as a result of changes triggered from other sources. Hence, the
 * callback from the diff tree to report changes may occur in the same thread as
 * the method call or asynchronously in a separate thread, regardless of who
 * triggered the refresh. Clients of this method (and any other asynchronous
 * method on this context) may determine if all changes have been collected
 * using {@link IJobManager#find(Object)} using this context as the
 * <code>family</code> argument in order to determine if there are any jobs
 * running that are populating the diff tree. Clients may also call
 * {@link IJobManager#join(Object, IProgressMonitor)} if they wish to wait until
 * all background handlers related to this context are finished.
 * </p>
 *
 * @see SynchronizationContext
 * @see MergeContext
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              They should subclass {@link SynchronizationContext} or one of
 *              its subclasses instead.
 */
public interface ISynchronizationContext {

	/**
	 * Synchronization type constant that indicates that
	 * context is a two-way synchronization.
	 */
	public final static int TWO_WAY = 2;

	/**
	 * Synchronization type constant that indicates that
	 * context is a three-way synchronization.
	 */
	public final static int THREE_WAY = 3;

	/**
	 * Return the input that defined the scope of this synchronization context.
	 * The input determines the set of resources to which the context applies.
	 * Changes in the input may result in changes to the sync-info available in
	 * the tree of this context.
	 *
	 * @return the input that defined the scope of this synchronization context.
	 */
	ISynchronizationScope getScope();

	/**
	 * Return a tree that contains {@link IDiff} entries for resources that
	 * are out-of-sync. The tree will contain entries for any out-of-sync
	 * resources that are within the scope of this context. The tree may include
	 * entries for additional resources, which should be ignored by the client.
	 * Clients can test for inclusion using the method
	 * {@link ISynchronizationScope#contains(IResource)}.
	 * <p>
	 * The returned {@link IResourceDiffTree} will be homogeneous and contain either
	 * {@link IResourceDiff} or {@link IThreeWayDiff} instances. Any
	 * {@link IThreeWayDiff} contained in the returned tree will contain
	 * {@link IResourceDiff} instances as the local and remote changes. This
	 * interface also has several helper methods for handling entries contained in
	 * the returned diff tree.
	 *
	 * @return a tree that contains an entry for any
	 *         resources that are out-of-sync.
	 * @see IResourceDiffTree#getDiffs(ResourceTraversal[])
	 * @see IResourceDiffTree#getResource(IDiff)
	 */
	public IResourceDiffTree getDiffTree();

	/**
	 * Return the synchronization type. A type of <code>TWO_WAY</code>
	 * indicates that the synchronization information associated with the
	 * context will also be two-way {@link IDiff} instances (i.e. there is
	 * only a remote but no base involved in the comparison used to determine
	 * the synchronization state of resources. A type of <code>THREE_WAY</code>
	 * indicates that the synchronization information will be three-way
	 * {@link IThreeWayDiff} instances.
	 *
	 * @return the type of synchronization information available in the context
	 *
	 * @see IDiff
	 * @see IThreeWayDiff
	 */
	public int getType();

	/**
	 * Return the cache associated with this synchronization context.
	 * The cache is maintained for the lifetime of this context and is
	 * disposed when the the context is disposed. It can be used by
	 * clients to cache model state related to the context so that it can
	 * be maintained for the life of the operation to which the context
	 * applies.
	 * @return the cache associated with this synchronization context
	 */
	public ICache getCache();

	/**
	 * Dispose of the synchronization context and the cache of the context. This
	 * method should be invoked by clients when the context is no longer needed.
	 */
	public void dispose();

	/**
	 * Refresh the context in order to update the diff tree returned by
	 * {@link #getDiffTree()} to include the latest synchronization state for
	 * the resources. Any changes will be reported through the change listeners
	 * registered with the diff tree of this context.
	 * <p>
	 * Changes to the diff tree may be triggered by a call to this method or by a
	 * refresh triggered by some other source. Hence, the callback from the diff tree
	 * to report changes may occur in the same thread as the refresh or
	 * <a href="#async">asynchronously</a> in a separate thread, regardless of who triggered
	 * the refresh.
	 *
	 * @see #getDiffTree()
	 * @see IDiffTree#addDiffChangeListener(IDiffChangeListener)
	 *
	 * @param traversals
	 *            the resource traversals which indicate which resources are to
	 *            be refreshed
	 * @param flags
	 *            additional refresh behavior. For instance, if
	 *            <code>RemoteResourceMappingContext.FILE_CONTENTS_REQUIRED</code>
	 *            is one of the flags, this indicates that the client will be
	 *            accessing the contents of the files covered by the traversals.
	 *            <code>NONE</code> should be used when no additional behavior
	 *            is required
	 * @param monitor
	 *            a progress monitor, or <code>null</code> if progress
	 *            reporting is not desired
	 * @throws CoreException
	 *             if the refresh fails. Reasons include:
	 *             <ul>
	 *             <li>The server could not be contacted for some reason (e.g.
	 *             the context in which the operation is being called must be
	 *             short running). The status code will be
	 *             SERVER_CONTACT_PROHIBITED. </li>
	 *             </ul>
	 */
	public void refresh(ResourceTraversal[] traversals, int flags,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Refresh the portion of the context related to the given resource
	 * mappings. The provided mappings must be within the scope of this context.
	 * Refreshing mappings may result in additional resources being added to the
	 * scope of this context. If new resources are included in the scope, a
	 * property change event will be fired from the scope. If the
	 * synchronization state of any of the resources covered by the mapping
	 * change, a change event will be fired from the diff tree of this context.
	 * <p>
	 * Changes to the diff tree may be triggered by a call to this method or by
	 * a refresh triggered by some other source. Hence, the callback from the
	 * diff tree to report changes may occur in the same thread as the refresh
	 * or <a href="#async">asynchronously</a> in a separate thread, regardless
	 * of who triggered the refresh.
	 *
	 * @param mappings
	 *            the mappings to be refreshed
	 * @param monitor
	 *            a progress monitor
	 * @throws CoreException
	 *             if errors occur
	 */
	public void refresh(ResourceMapping[] mappings, IProgressMonitor monitor)
			throws CoreException;

}
