/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Adapted from classes in org.eclipse.ui.ide.undo and org.eclipse.ui.internal.ide.undo
 *******************************************************************************/
package org.eclipse.core.resources.undo.snapshot;

import java.net.URI;
import org.eclipse.core.resources.IResourceFilterDescription;

/**
 * IContainerSnapshot is a lightweight description that describes a container to
 * be created.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.20
 */
public interface IContainerSnapshot extends IResourceSnapshot {

	/**
	 * Get a list of snapshots of members of this container
	 *
	 * @return a list of snapshots
	 */
	public IResourceSnapshot[] getMembers();

	/**
	 * Add the specified resource description as a member of this resource
	 * description
	 *
	 * @param member the resource description considered a member of this container.
	 */
	public void addMember(IResourceSnapshot member);

	/**
	 * Set the location to which this container is linked.
	 *
	 * @param linkLocation the location URI, or <code>null</code> if there is no
	 *                     link
	 */
	public void setLocation(URI linkLocation);

	/**
	 * Set the filters to which should be created on this container.
	 *
	 * @param filterList the filters
	 */
	public void setFilters(IResourceFilterDescription[] filterList);
}
