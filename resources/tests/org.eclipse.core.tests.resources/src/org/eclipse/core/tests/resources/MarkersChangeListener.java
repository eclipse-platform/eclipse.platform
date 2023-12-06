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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;

/**
 * A support class for the marker tests.
 */
public class MarkersChangeListener implements IResourceChangeListener {
	protected HashMap<IPath, List<IMarkerDelta>> changes;

	public MarkersChangeListener() {
		reset();
	}

	/**
	 * Asserts whether the changes for the given resource (or null for the workspace)
	 * are exactly the added, removed and changed markers given. The arrays may be null.
	 */
	public void assertChanges(IResource resource, IMarker[] added, IMarker[] removed, IMarker[] changed) {
		IPath path = resource == null ? IPath.ROOT : resource.getFullPath();
		List<IMarkerDelta> v = changes.get(path);
		if (v == null) {
			v = new Vector<>();
		}
		int numChanges = (added == null ? 0 : added.length) + (removed == null ? 0 : removed.length) + (changed == null ? 0 : changed.length);
		assertThat("wrong number of markers for resource " + path, numChanges, is(v.size()));

		for (IMarkerDelta delta : v) {
			switch (delta.getKind()) {
				case IResourceDelta.ADDED :
					assertThat("added marker is missing resource " + path, added, hasItemInArray(delta.getMarker()));
					break;
				case IResourceDelta.REMOVED :
					assertThat("removed marker is missing resource " + path, removed,
							hasItemInArray(delta.getMarker()));
					break;
				case IResourceDelta.CHANGED :
					assertThat("changed marker is missing resource " + path, changed,
							hasItemInArray(delta.getMarker()));
					break;
				default :
					throw new IllegalArgumentException("delta with unsupported kind: " + delta);
			}
		}
	}

	/**
	 * Asserts the number of resources (or the workspace) which have had marker
	 * changes since last reset.
	 */
	public void assertNumberOfAffectedResources(int expectedNumberOfResource) {
		assertThat(changes, aMapWithSize(expectedNumberOfResource));
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
