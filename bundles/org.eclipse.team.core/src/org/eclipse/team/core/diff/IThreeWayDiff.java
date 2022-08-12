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

import org.eclipse.team.core.diff.provider.ThreeWayDiff;

/**
 * A three-way delta that describe the synchronization state between two
 * contributors and an ancestor. For simplicity, we refer to one of the
 * contributors as the local and the other as the remote. A three-way delta is
 * represented as a combination of two two-way deltas, one between the ancestor
 * and local and the other between the ancestor and remote. For a three-way
 * delta, clients can assume that the before state of both the local and remote
 * changes are the same.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients that need to create deltas should instead use
 *              {@link ThreeWayDiff}.
 */
public interface IThreeWayDiff extends IDiff {

	/*====================================================================
	 * Constants defining synchronization direction:
	 *====================================================================*/

	/**
	 * Constant (bit mask) indicating that there is a local change.
	 *
	 * @see IThreeWayDiff#getDirection()
	 */
	public static final int OUTGOING = 0x100;

	/**
	 * Constant (bit mask) indicating that there is a local change.
	 *
	 * @see IThreeWayDiff#getDirection()
	 */
	public static final int INCOMING = 0x200;

	/**
	 * Constant (bit mask) indicating that there is both a local change
	 * and a remote change.
	 * This flag is equivalent
	 * to <code>OUTGOING | INCOMING</code>.
	 *
	 * @see IThreeWayDiff#getDirection()
	 */
	public static final int CONFLICTING = OUTGOING | INCOMING;

	/**
	 * Bit mask for extracting the synchronization direction.
	 */
	public static final int DIRECTION_MASK = CONFLICTING;

	/**
	 * Return the local change associated with this delta.
	 * If there is no local change, either a delta with kind
	 * {@link IDiff#NO_CHANGE} is returned or <code>null</code>
	 * may be returned.
	 * @return the local change associated with this delta or <code>null</code>
	 */
	public ITwoWayDiff getLocalChange();

	/**
	 * Return the remote change associated with this delta.
	 * If there is no remote change, either a delta with kind
	 * {@link IDiff#NO_CHANGE} is returned or <code>null</code>
	 * may be returned.
	 * @return the remote change associated with this delta or <code>null</code>
	 */
	public ITwoWayDiff getRemoteChange();

	/**
	 * Return the direction of this three-way delta.
	 * @return the direction of this three-way delta
	 * @see IThreeWayDiff#INCOMING
	 * @see IThreeWayDiff#OUTGOING
	 * @see IThreeWayDiff#CONFLICTING
	 */
	public int getDirection();

}
