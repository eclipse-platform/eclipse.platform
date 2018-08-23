/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;

/**
 * Defines the status codes used in the status of exceptions and errors relating to Team.
 *
 * @since 3.0
 */
public interface ITeamStatus extends IStatus {

	/**
	 * An error occurred trying to obtain the <code>SyncInfo</code> for a single resource.
	 * The error will be cleared when the set is reset or possibly when a sync info is added to
	 * the set for the resource for which the error occurred.
	 */
	public static final int RESOURCE_SYNC_INFO_ERROR = 1;

	/**
	 * An error occurred that may affect several resources in a <code>SyncInfoSet</code>.
	 * The error will be cleared when the set is reset.
	 */
	public static final int SYNC_INFO_SET_ERROR = 2;

	/**
	 * The collection of sync info for a sync info set has been canceled so the
	 * contents do not reflect the state of the system.
	 * The error will be cleared when the set is reset.
	 */
	public static final int SYNC_INFO_SET_CANCELLATION = 3;

	/** Status code constant (value 279) indicating that the
	 * file in the file system is marked as read-only and could
	 * not be made writable.
	 * @since 3.3
	 */
	public static final int READ_ONLY_LOCAL = 279;

	/**
	 * Return the resource associated with this status.
	 * @return Returns the resource.
	 */
	public IResource getResource();
}
