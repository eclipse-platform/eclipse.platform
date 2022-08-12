/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources.mapping;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;

/**
 * A description of the changes found in a delta
 */
public class ChangeDescription {

	private List<IResource> addedRoots = new ArrayList<>();
	private List<IResource> changedFiles = new ArrayList<>();
	private List<IResource> closedProjects = new ArrayList<>();
	private List<IResource> copiedRoots = new ArrayList<>();
	private List<IResource> movedRoots = new ArrayList<>();
	private List<IResource> removedRoots = new ArrayList<>();

	private IResource createSourceResource(IResourceDelta delta) {
		IPath sourcePath = delta.getMovedFromPath();
		IResource resource = delta.getResource();
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		switch (resource.getType()) {
			case IResource.PROJECT :
				return wsRoot.getProject(sourcePath.segment(0));
			case IResource.FOLDER :
				return wsRoot.getFolder(sourcePath);
			case IResource.FILE :
				return wsRoot.getFile(sourcePath);
		}
		return null;
	}

	private void ensureResourceCovered(IResource resource, List<IResource> list) {
		IPath path = resource.getFullPath();
		for (IResource root : list) {
			if (root.getFullPath().isPrefixOf(path)) {
				return;
			}
		}
		list.add(resource);
	}

	public IResource[] getRootResources() {
		Set<IResource> result = new HashSet<>();
		result.addAll(addedRoots);
		result.addAll(changedFiles);
		result.addAll(closedProjects);
		result.addAll(copiedRoots);
		result.addAll(movedRoots);
		result.addAll(removedRoots);
		return result.toArray(new IResource[result.size()]);
	}

	private void handleAdded(IResourceDelta delta) {
		if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
			handleMove(delta);
		} else if ((delta.getFlags() & IResourceDelta.COPIED_FROM) != 0) {
			handleCopy(delta);
		} else {
			ensureResourceCovered(delta.getResource(), addedRoots);
		}
	}

	private void handleChange(IResourceDelta delta) {
		if ((delta.getFlags() & IResourceDelta.REPLACED) != 0) {
			// A replace was added in place of a removed resource
			handleAdded(delta);
		} else if (delta.getResource().getType() == IResource.FILE) {
			ensureResourceCovered(delta.getResource(), changedFiles);
		}
	}

	private void handleCopy(IResourceDelta delta) {
		if ((delta.getFlags() & IResourceDelta.COPIED_FROM) != 0) {
			IResource source = createSourceResource(delta);
			ensureResourceCovered(source, copiedRoots);
		}
	}

	private void handleMove(IResourceDelta delta) {
		if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
			movedRoots.add(delta.getResource());
		} else if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
			IResource source = createSourceResource(delta);
			ensureResourceCovered(source, movedRoots);
		}
	}

	private void handleRemoved(IResourceDelta delta) {
		if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
			closedProjects.add(delta.getResource());
		} else if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
			handleMove(delta);
		} else {
			ensureResourceCovered(delta.getResource(), removedRoots);
		}
	}

	/**
	 * Record the change and return whether any child changes should be visited.
	 * @param delta the change
	 * @return whether any child changes should be visited
	 */
	public boolean recordChange(IResourceDelta delta) {
		switch (delta.getKind()) {
			case IResourceDelta.ADDED :
				handleAdded(delta);
				return true; // Need to traverse children to look  for moves or other changes under added roots
			case IResourceDelta.REMOVED :
				handleRemoved(delta);
				// No need to look for further changes under a remove (such as moves).
				// Changes will be discovered in corresponding destination delta
				return false;
			case IResourceDelta.CHANGED :
				handleChange(delta);
				return true;
		}
		return true;
	}

}
