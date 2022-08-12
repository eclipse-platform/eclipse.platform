/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A support class for the marker tests.
 */
public class MarkersChangeListener implements IResourceChangeListener {
	protected HashMap<IPath, List<IMarkerDelta>> changes;

	public MarkersChangeListener() {
		reset();
	}

	/**
	 * Returns whether the changes for the given resource (or null for the workspace)
	 * are exactly the added, removed and changed markers given. The arrays may be null.
	 */
	public boolean checkChanges(IResource resource, IMarker[] added, IMarker[] removed, IMarker[] changed) {
		IPath path = resource == null ? Path.ROOT : resource.getFullPath();
		List<IMarkerDelta> v = changes.get(path);
		if (v == null) {
			v = new Vector<>();
		}
		int numChanges = (added == null ? 0 : added.length) + (removed == null ? 0 : removed.length) + (changed == null ? 0 : changed.length);
		if (numChanges != v.size()) {
			return false;
		}
		for (IMarkerDelta delta : v) {
			switch (delta.getKind()) {
				case IResourceDelta.ADDED :
					if (!contains(added, delta.getMarker())) {
						return false;
					}
					break;
				case IResourceDelta.REMOVED :
					if (!contains(removed, delta.getMarker())) {
						return false;
					}
					break;
				case IResourceDelta.CHANGED :
					if (!contains(changed, delta.getMarker())) {
						return false;
					}
					break;
				default :
					throw new Error();
			}
		}
		return true;
	}

	/**
	 * Returns whether the given marker is contained in the given list of markers.
	 */
	protected boolean contains(IMarker[] markers, IMarker marker) {
		if (markers != null) {
			for (IMarker marker2 : markers) {
				if (marker2.equals(marker)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the number of resources (or the workspace) which have had marker changes since last reset.
	 */
	public int numAffectedResources() {
		return changes.size();
	}

	public void reset() {
		changes = new HashMap<>();
	}

	/**
	 * Notification from the workspace.  Extract the marker changes.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		resourceChanged(event.getDelta());
	}

	/**
	 * Recurse over the delta, extracting marker changes.
	 */
	protected void resourceChanged(IResourceDelta delta) {
		if (delta == null) {
			return;
		}
		if ((delta.getFlags() & IResourceDelta.MARKERS) != 0) {
			IPath path = delta.getFullPath();
			List<IMarkerDelta> v = changes.get(path);
			if (v == null) {
				v = new Vector<>();
				changes.put(path, v);
			}
			IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
			for (IMarkerDelta markerDelta : markerDeltas) {
				v.add(markerDelta);
			}
		}
		IResourceDelta[] children = delta.getAffectedChildren();
		for (IResourceDelta element : children) {
			resourceChanged(element);
		}
	}
}
