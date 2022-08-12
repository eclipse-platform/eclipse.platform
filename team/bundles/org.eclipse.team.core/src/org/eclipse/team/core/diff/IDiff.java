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
import org.eclipse.team.core.diff.provider.Diff;

/**
 * A diff describes differences between two or more model objects.
 *
 * @see ITwoWayDiff
 * @see IThreeWayDiff
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Instead, clients can subclass {@link Diff}.
 */
public interface IDiff {

	/*
	 * ====================================================================
	 * Constants defining diff kinds:
	 * ====================================================================
	 */

	/**
	 * Diff kind constant (bit mask) indicating that the resource has not been changed in
	 * any way.
	 *
	 * @see IDiff#getKind()
	 */
	public static final int NO_CHANGE = 0;

	/**
	 * Diff kind constant (bit mask) indicating that the resource has been
	 * added to its parent. That is, one that appears in the "after" state, not
	 * in the "before" one.
	 *
	 * @see IDiff#getKind()
	 */
	public static final int ADD = 0x1;

	/**
	 * Diff kind constant (bit mask) indicating that the resource has been
	 * removed from its parent. That is, one that appears in the "before" state,
	 * not in the "after" one.
	 *
	 * @see IDiff#getKind()
	 */
	public static final int REMOVE = 0x2;

	/**
	 * Diff kind constant (bit mask) indicating that the resource has been
	 * changed. That is, one that appears in both the "before" and "after"
	 * states.
	 *
	 * @see IDiff#getKind()
	 */
	public static final int CHANGE = 0x4;

	/**
	 * Returns the full, absolute path of the object to which the diff applies
	 * with respect to the model root.
	 * <p>
	 * Note: the returned path never has a trailing separator.
	 * </p>
	 *
	 * @return the full, absolute path of this diff
	 */
	public IPath getPath();

	/**
	 * Returns the kind of this diff. Normally, one of
	 * <code>ADDED</code>, <code>REMOVED</code>, <code>CHANGED</code>.
	 *
	 * @return the kind of this diff
	 * @see IDiff#ADD
	 * @see IDiff#REMOVE
	 * @see IDiff#CHANGE
	 */
	public int getKind();

	/**
	 * Return a string that describes the difference represented by
	 * this node.
	 * @return a string that describes the difference represented by
	 * this node
	 */
	public String toDiffString();
}
