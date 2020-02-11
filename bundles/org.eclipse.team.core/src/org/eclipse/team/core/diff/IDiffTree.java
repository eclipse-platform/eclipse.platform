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
package org.eclipse.team.core.diff;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A diff tree provides access to a tree of {@link IDiff} instances. For
 * efficiency reasons, the tree only provides diffs for paths that represent a
 * change. Paths that do not contain a diff represent but are returned from the
 * tree will contain child paths in the set.
 *
 * @see org.eclipse.team.core.diff.provider.DiffTree
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients should use
 *              {@link org.eclipse.team.core.diff.provider.DiffTree} instead.
 */
public interface IDiffTree {

	/**
	 * Property constant used to indicate that a particular path may be involved in an operation.
	 */
	public static final int P_BUSY_HINT = 1;

	/**
	 * Property constant used to indicate that a particular path has descendants that are conflicts.
	 */
	public static final int P_HAS_DESCENDANT_CONFLICTS = 2;

	/**
	 * Add a listener to the tree. The listener will be informed of any changes
	 * in the tree. Registering a listener that is already registered will have
	 * no effects.
	 *
	 * @param listener the listener to be added
	 */
	public void addDiffChangeListener(IDiffChangeListener listener);

	/**
	 * Remove the listener from the tree. Removing a listener that is not
	 * registered has no effect.
	 *
	 * @param listener the listener to be removed
	 */
	public void removeDiffChangeListener(IDiffChangeListener listener);

	/**
	 * Accepts the given visitor. The only kinds of deltas visited are
	 * <code>ADDED</code>, <code>REMOVED</code>, and <code>CHANGED</code>.
	 * The visitor's <code>visit</code> method is called with the given delta
	 * if applicable. If the visitor returns <code>true</code>, any of the
	 * delta's children in this tree are also visited.
	 *
	 * @param path the path to start the visit in the tree
	 * @param visitor the visitor
	 * @param depth the depth to visit
	 * @see IDiffVisitor#visit(IDiff)
	 */
	public void accept(IPath path, IDiffVisitor visitor, int depth);

	/**
	 * Returns the delta identified by the given path,
	 * or <code>null</code> if there is no delta at that path. The supplied path
	 * may be absolute or relative; in either case, it is interpreted as
	 * relative to the workspace. Trailing separators are ignored.
	 * <p>
	 * This method only returns a delta if there is a change at the given
	 * path. To know if there are deltas in descendent paths, clients
	 * should class {@link #getChildren(IPath) }.
	 *
	 * @param path the path of the desired delta
	 * @return the delta, or <code>null</code> if no such
	 *         delta exists
	 */
	public IDiff getDiff(IPath path);

	/**
	 * Returns the child paths of the given path that either point to
	 * a sync delta or have a descendant path that points to a sync delta.
	 * Returns an empty array if there are no sync deltas that are descendents
	 * of the given path.
	 *
	 * @return the child paths of the given path that either point to
	 * a sync delta or have a descendant path that points to a sync delta
	 */
	public IPath[] getChildren(IPath parent);

	/**
	 * Return the number of diffs contained in the tree.
	 * @return the number of diffs contained in the tree
	 */
	public int size();

	/**
	 * Return whether the set is empty.
	 * @return whether the set is empty
	 */
	public boolean isEmpty();

	/**
	 * Return the number of out-of-sync elements in the given set whose
	 * synchronization state matches the given mask. A state of 0 assumes a count of
	 * all changes. A mask of 0 assumes a direct match of the given state.
	 * <p>
	 * For example, this will return the number of outgoing changes in the set:
	 * </p>
	 *
	 * <pre>
	 * long outgoing = countFor(IThreeWayDiff.OUTGOING, IThreeWayDiff.DIRECTION_MASK);
	 * </pre>
	 *
	 * @param state the sync state
	 * @param mask  the sync state mask
	 * @return the number of matching resources in the set.
	 */
	public long countFor(int state, int mask);

	/**
	 * Set the given diff nodes and all their parents to busy
	 * @param diffs the busy diffs
	 * @param monitor a progress monitor or <code>null</code> if progress indication
	 * is not required
	 */
	public void setBusy(IDiff[] diffs, IProgressMonitor monitor);

	/**
	 * Return the value of the property for the given path.
	 * @param path the path
	 * @param property the property
	 * @return the value of the property
	 */
	public boolean getProperty(IPath path, int property);


	/**
	 * Clear all busy properties in this tree.
	 * @param monitor a progress monitor or <code>null</code> if progress indication
	 * is not required
	 */
	public void clearBusy(IProgressMonitor monitor);

	/**
	 * Return whether the this diff tree contains any diffs that match the given filter
	 * at of below the given path.
	 * @param path the path
	 * @param filter the diff node filter
	 * @return whether the given diff tree contains any deltas that match the given filter
	 */
	public boolean hasMatchingDiffs(IPath path, final FastDiffFilter filter);

}
