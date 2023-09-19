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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * IMarkerSnapshot is a lightweight snapshot of a marker for the purposes of
 * undoing.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.20
 */
public interface IMarkerSnapshot {

	/**
	 * Create a marker from the marker description.
	 *
	 * @return the created marker
	 * @throws CoreException
	 */
	public IMarker createMarker() throws CoreException;

	/**
	 * Update an existing marker using the attributes in the marker description.
	 *
	 * @param marker the marker to be updated
	 * @throws CoreException
	 */
	public void updateMarker(IMarker marker) throws CoreException;

	/**
	 * Return the resource associated with this marker.
	 *
	 * @return the resource associated with this marker
	 */
	public IResource getResource();

	/**
	 * Return the marker type associated with this marker.
	 *
	 * @return the string marker type of this marker
	 */
	public String getType();
}
