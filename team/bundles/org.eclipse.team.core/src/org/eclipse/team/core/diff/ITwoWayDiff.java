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

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.diff.provider.TwoWayDiff;

/**
 * A two-way diff represents the changes between two states of the same object,
 * referred to as the "before" state and the "after" state. It is modeled after
 * the {@link IResourceDelta} but is simplified.
 *
 * @see IDiffTree
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients that need to create two-way diffs should instead use or
 *              subclass {@link TwoWayDiff}
 */
public interface ITwoWayDiff extends IDiff {

	/*====================================================================
	 * Constants which describe resource changes:
	 *====================================================================*/

	/**
	 * Change constant (bit mask) indicating that the content of the object has changed.
	 *
	 * @see ITwoWayDiff#getFlags()
	 */
	public static final int CONTENT = 0x100;

	/**
	 * Change constant (bit mask) indicating that the object was moved from another location.
	 * The location in the "before" state can be retrieved using <code>getFromPath()</code>.
	 *
	 * @see ITwoWayDiff#getFlags()
	 */
	public static final int MOVE_FROM = 0x200;

	/**
	 * Change constant (bit mask) indicating that the object was moved to another location.
	 * The location in the new state can be retrieved using <code>getToPath()</code>.
	 *
	 * @see ITwoWayDiff#getFlags()
	 */
	public static final int MOVE_TO = 0x400;

	/**
	 * Change constant (bit mask) indicating that the object was copied from another location.
	 * The location in the "before" state can be retrieved using <code>getFromPath()</code>.
	 *
	 * @see ITwoWayDiff#getFlags()
	 */
	public static final int COPY_FROM = 0x800;

	/**
	 * Change constant (bit mask) indicating that the object has been
	 * replaced by another at the same location (i.e., the object has
	 * been deleted and then added).
	 *
	 * @see ITwoWayDiff#getFlags()
	 */
	public static final int REPLACE = 0x1000;

	/**
	 * Returns flags which describe in more detail how a object has been affected.
	 * <p>
	 * The following codes (bit masks) are used when kind is <code>CHANGE</code>,
	 * and also when the object is involved in a move:
	 * </p>
	 * <ul>
	 * <li><code>CONTENT</code> - The bytes contained by the resource have been
	 * altered.</li>
	 * <li><code>REPLACE</code> - The object was deleted (either by a delete or
	 * move), and was subsequently re-created (either by a create, move, or
	 * copy).</li>
	 * </ul>
	 * The following code is only used if kind is <code>REMOVE</code> (or
	 * <code>CHANGE</code> in conjunction with <code>REPLACE</code>):
	 * <ul>
	 * <li><code>MOVE_TO</code> - The object has moved. <code>getToPath</code> will
	 * return the path of where it was moved to.</li>
	 * </ul>
	 * The following code is only used if kind is <code>ADD</code> (or
	 * <code>CHANGE</code> in conjunction with <code>REPLACE</code>):
	 * <ul>
	 * <li><code>MOVE_FROM</code> - The object has moved. <code>getFromPath</code>
	 * will return the path of where it was moved from.</li>
	 * <li><code>COPY_FROM</code> - The object has copied. <code>getFromPath</code>
	 * will return the path of where it was copied from.</li>
	 * </ul>
	 * <p>
	 * A simple move operation would result in the following diff information. If a
	 * object is moved from A to B (with no other changes to A or B), then A will
	 * have kind <code>REMOVE</code>, with flag <code>MOVE_TO</code>, and
	 * <code>getToPath</code> on A will return the path for B. B will have kind
	 * <code>ADD</code>, with flag <code>MOVE_FROM</code>, and
	 * <code>getFromPath</code> on B will return the path for A. B's other flags
	 * will describe any other changes to the resource, as compared to its previous
	 * location at A.
	 * </p>
	 * <p>
	 * Note that the move flags only describe the changes to a single object; they
	 * don't necessarily imply anything about the parent or children of the object.
	 * If the children were moved as a consequence of a subtree move operation, they
	 * will have corresponding move flags as well.
	 * </p>
	 *
	 * @return the flags
	 * @see ITwoWayDiff#CONTENT
	 * @see ITwoWayDiff#MOVE_TO
	 * @see ITwoWayDiff#MOVE_FROM
	 * @see ITwoWayDiff#COPY_FROM
	 * @see ITwoWayDiff#REPLACE
	 * @see #getKind()
	 * @see #getFromPath()
	 * @see #getToPath()
	 */
	public int getFlags();

	/**
	 * Returns the full path (in the "before" state) from which this resource
	 * (in the "after" state) was moved.  This value is only valid
	 * if the <code>MOVE_FROM</code> change flag is set; otherwise,
	 * <code>null</code> is returned.
	 * <p>
	 * Note: the returned path never has a trailing separator.
	 *
	 * @return a path, or <code>null</code>
	 * @see #getToPath()
	 * @see #getPath()
	 * @see #getFlags()
	 */
	public IPath getFromPath();

	/**
	 * Returns the full path (in the "after" state) to which this resource
	 * (in the "before" state) was moved.  This value is only valid if the
	 * <code>MOVE_TO</code> change flag is set; otherwise,
	 * <code>null</code> is returned.
	 * <p>
	 * Note: the returned path never has a trailing separator.
	 *
	 * @return a path, or <code>null</code>
	 * @see #getFromPath()
	 * @see #getPath()
	 * @see #getFlags()
	 */
	public IPath getToPath();

}
