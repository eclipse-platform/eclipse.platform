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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * WorkingSet filter for a SyncSet.
 */
public class SyncInfoWorkingSetFilter extends FastSyncInfoFilter {

	private IResource[] resources;

	@Override
	public boolean select(SyncInfo info) {
		// if there's no set, the resource is included
		if (isEmpty()) return true;
		return isIncluded(info.getLocal());
	}

	/*
	 * Answer true if the given resource is included in the working set
	 */
	private boolean isIncluded(IResource resource) {
		// otherwise, if their is a parent of the resource in the set,
		// it is included
		for (IResource setResource : resources) {
			if (isParent(setResource, resource)) {
				return true;
			}
		}
		return false;
	}

	private boolean isParent(IResource parent, IResource child) {
		return (parent.getFullPath().isPrefixOf(child.getFullPath()));
	}

	public IResource[] getRoots(Subscriber subscriber) {
		IResource[] roots = subscriber.roots();
		if (isEmpty()) return roots;

		// filter the roots by the selected working set
		Set<IResource> result = new HashSet<>();
		for (IResource resource : roots) {
			result.addAll(Arrays.asList(getIntersectionWithSet(subscriber, resource)));
		}
		return result.toArray(new IResource[result.size()]);
	}

	/*
	 * Answer the intersection between the given resource and it's children
	 * and the receiver's working set.
	 */
	private IResource[] getIntersectionWithSet(Subscriber subscriber, IResource resource) {
		List<IResource> result = new ArrayList<>();
		for (IResource setResource : resources) {
			if (setResource != null) {
				if (isParent(resource, setResource)) {
					try {
						if (subscriber.isSupervised(setResource)) {
							result.add(setResource);
						}
					} catch (TeamException e) {
						// Log the exception and add the resource to the list
						TeamPlugin.log(e);
						result.add(setResource);
					}
				} else if (isParent(setResource, resource)) {
					result.add(resource);
				}
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	public void setWorkingSet(IResource[] resources) {
		this.resources = resources;
	}

	public IResource[] getWorkingSet() {
		return this.resources;
	}

	private boolean isEmpty() {
		return resources == null || resources.length == 0;
	}
}
