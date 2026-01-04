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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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
	private final Map<IPath, List<IMarkerDelta>> changes = new ConcurrentHashMap<>();

	public MarkersChangeListener() {
		reset();
	}

	/**
	 * Asserts whether the changes for the given resource (or null for the workspace)
	 * are exactly the added, removed and changed markers given. The arrays may be null.
	 */
	public void assertChanges(IResource resource, IMarker[] added, IMarker[] removed, IMarker[] changed) {
		IPath path = resource == null ? IPath.ROOT : resource.getFullPath();
		Supplier<List<IMarkerDelta>> changesRetriever = () -> changes.getOrDefault(path, Collections.emptyList());
		int numChanges = (added == null ? 0 : added.length) + (removed == null ? 0 : removed.length) + (changed == null ? 0 : changed.length);
		TestUtil.waitForCondition(() -> changesRetriever.get().size() == numChanges, 5000);
		assertThat(numChanges).as("number of markers for resource %s", path).isEqualTo(changesRetriever.get().size());

		for (IMarkerDelta delta : changesRetriever.get()) {
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				assertThat(added).as("check added markers contain resource %s", path).contains(delta.getMarker());
				break;
			case IResourceDelta.REMOVED:
				assertThat(removed).as("check removed markers contain resource %s", path).contains(delta.getMarker());
				break;
			case IResourceDelta.CHANGED:
				assertThat(changed).as("check changed markers contain resource %s", path).contains(delta.getMarker());
				break;
			default:
				throw new IllegalArgumentException("delta with unsupported kind: " + delta);
			}
		}
	}

	/**
	 * Asserts the number of resources (or the workspace) which have had marker
	 * changes since last reset.
	 */
	public void assertNumberOfAffectedResources(int expectedNumberOfResource) {
		TestUtil.waitForCondition(() -> changes.size() == expectedNumberOfResource, 5000);
		assertThat(changes).hasSize(expectedNumberOfResource);
	}

	public void reset() {
		changes.clear();
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
			List<IMarkerDelta> v = changes.computeIfAbsent(path, p -> Collections.synchronizedList(new ArrayList<>()));
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
