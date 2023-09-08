/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.core.internal.resources.undo.snapshot;

import java.util.Map;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.undo.snapshot.IMarkerSnapshot;
import org.eclipse.core.runtime.CoreException;

/**
 * MarkerSnapshot is a lightweight snapshot of a marker that can be used to
 * describe a marker to be created or updated.
 *
 * This class is not intended to be instantiated or used by clients.
 *
 * @since 3.20
 *
 */
public class MarkerSnapshot implements IMarkerSnapshot {
	String type;

	Map<String, Object> attributes;

	IResource resource;

	/**
	 *
	 * Create a marker snapshot from the specified marker.
	 *
	 * @param marker the marker to be described
	 * @throws CoreException
	 */
	public MarkerSnapshot(IMarker marker) throws CoreException {
		this.type = marker.getType();
		this.attributes = marker.getAttributes();
		this.resource = marker.getResource();

	}

	/**
	 * Create a marker snapshot from the specified marker type, attributes, and
	 * resource.
	 *
	 * @param type       the type of marker to be created.
	 * @param attributes the attributes to be assigned to the marker
	 * @param resource   the resource on which the marker should be created
	 */
	public MarkerSnapshot(String type, Map<String, Object> attributes, IResource resource) {
		this.type = type;
		this.attributes = attributes;
		this.resource = resource;
	}

	/**
	 * Create a marker from the marker description.
	 *
	 * @return the created marker
	 * @throws CoreException
	 */
	@Override
	public IMarker createMarker() throws CoreException {
		IMarker marker = resource.createMarker(type);
		marker.setAttributes(attributes);
		return marker;
	}

	/**
	 * Update an existing marker using the attributes in the marker description.
	 *
	 * @param marker
	 *            the marker to be updated
	 * @throws CoreException
	 */
	@Override
	public void updateMarker(IMarker marker) throws CoreException {
		marker.setAttributes(attributes);
	}

	/**
	 * Return the resource associated with this marker.
	 *
	 * @return the resource associated with this marker
	 */
	@Override
	public IResource getResource() {
		return resource;
	}

	/**
	 * Return the marker type associated with this marker.
	 *
	 * @return the string marker type of this marker
	 */
	@Override
	public String getType() {
		return type;
	}
}