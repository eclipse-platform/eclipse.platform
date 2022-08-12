/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.subscribers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoSetChangeEvent;

public class SyncInfoTreeChangeEvent extends SyncInfoSetChangeEvent implements ISyncInfoTreeChangeEvent {

	private Set<IResource> removedSubtrees = new HashSet<>();
	private Set<IResource> addedSubtrees = new HashSet<>();

	public SyncInfoTreeChangeEvent(SyncInfoSet set) {
		super(set);
	}

	public void removedSubtreeRoot(IResource root) {
		if (addedSubtrees.contains(root)) {
			// The root was added and removed which is a no-op
			addedSubtrees.remove(root);
		} else if (isDescendantOfAddedRoot(root)) {
			// Nothing needs to be done since no listeners ever knew about the root
		} else {
			// check if the root is a child of an existing root
			// (in which case it need not be added).
			// Also, remove any exisiting roots that are children
			// of the new root
			for (Iterator<IResource> iter = removedSubtrees.iterator(); iter.hasNext();) {
				IResource element = iter.next();
				// check if the root is already in the list
				if (root.equals(element)) return;
				if (isParent(root, element)) {
					// the root invalidates the current element
					iter.remove();
				} else if (isParent(element, root)) {
					// the root is a child of an existing element
					return;
				}
			}
			removedSubtrees.add(root);
		}
	}

	private boolean isParent(IResource root, IResource element) {
		return root.getFullPath().isPrefixOf(element.getFullPath());
	}

	public void addedSubtreeRoot(IResource parent) {
		if (removedSubtrees.contains(parent)) {
			// The root was re-added. Just removing the removedRoot
			// may not give the proper event.
			// Since we can't be sure, just force a reset.
			reset();
		} else {
			// only add the root if their isn't a higher root in the list already
			if (!isDescendantOfAddedRoot(parent)) {
				addedSubtrees.add(parent);
			}
		}
	}

	private boolean isDescendantOfAddedRoot(IResource resource) {
		for (IResource root : addedSubtrees) {
			if (isParent(root, resource)) {
				// There is a higher added root already in the list
				return true;
			}
		}
		return false;
	}

	@Override
	public IResource[] getAddedSubtreeRoots() {
		return addedSubtrees.toArray(new IResource[addedSubtrees.size()]);
	}

	@Override
	public IResource[] getRemovedSubtreeRoots() {
		return removedSubtrees.toArray(new IResource[removedSubtrees.size()]);
	}

	@Override
	public boolean isEmpty() {
		return super.isEmpty() && removedSubtrees.isEmpty() && addedSubtrees.isEmpty();
	}
}
