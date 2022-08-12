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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.mapping;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ModelStatus;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * A description of the changes found in a delta
 */
public class ChangeDescription {

	public static final String ADDED = "Added {0}";
	public static final String CHANGED = "Changed {0}";
	public static final String CLOSED = "Closed {0}";
	public static final String COPIED = "Copied {0}";
	public static final String MOVED = "Moved {0}";
	public static final String REMOVED = "Removed {0}";

	private List<IResource> addedRoots = new ArrayList<>();
	private List<IResource> changedRoots = new ArrayList<>();
	private List<IResource> closedProjects = new ArrayList<>();
	private List<IResource> copiedRoots = new ArrayList<>();
	private List<Status> errors = new ArrayList<>();
	private List<IResource> movedRoots = new ArrayList<>();
	private List<IResource> removedRoots = new ArrayList<>();

	public static String getMessageFor(String messageTemplate, IResource resource) {
		return NLS.bind(messageTemplate, resource.getFullPath().toString());
	}

	private void accumulateStatus(IResource[] resources, List<ModelStatus> result, String message) {
		for (IResource resource : resources) {
			result.add(new ModelStatus(IStatus.WARNING, "org.eclipse.core.tests.resources", TestModelProvider.ID, getMessageFor(message, resource)));
		}
	}

	public void addError(CoreException e) {
		errors.add(new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", 0, "An error occurred", e));
	}

	public IStatus asStatus() {
		if (errors.isEmpty()) {
			List<ModelStatus> result = new ArrayList<>();
			accumulateStatus(addedRoots.toArray(new IResource[addedRoots.size()]), result, ADDED);
			accumulateStatus(removedRoots.toArray(new IResource[removedRoots.size()]), result, REMOVED);
			accumulateStatus(movedRoots.toArray(new IResource[movedRoots.size()]), result, MOVED);
			accumulateStatus(copiedRoots.toArray(new IResource[copiedRoots.size()]), result, COPIED);
			accumulateStatus(changedRoots.toArray(new IResource[changedRoots.size()]), result, CHANGED);
			accumulateStatus(closedProjects.toArray(new IResource[closedProjects.size()]), result, CLOSED);
			if (!result.isEmpty()) {
				if (result.size() == 1) {
					return result.get(0);
				}
				return new MultiStatus("org.eclipse.core.tests.resources", 0, result.toArray(new IStatus[result.size()]), "Changes were validated", null);
			}
			return Status.OK_STATUS;
		} else if (errors.size() == 1) {
			return errors.get(0);
		}
		return new MultiStatus("org.eclipse.core.tests.resources", 0, errors.toArray(new IStatus[errors.size()]), "Errors occurred", null);
	}

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
			ensureResourceCovered(delta.getResource(), changedRoots);
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
			ensureResourceCovered(delta.getResource(), movedRoots);
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
