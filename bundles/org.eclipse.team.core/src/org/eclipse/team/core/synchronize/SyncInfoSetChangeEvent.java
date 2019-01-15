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
package org.eclipse.team.core.synchronize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.ITeamStatus;

/**
 * This event keeps track of the changes in a {@link SyncInfoSet}
 *
 * @see SyncInfoSet
 * @since 3.5
 */
public class SyncInfoSetChangeEvent implements ISyncInfoSetChangeEvent {

	private SyncInfoSet set;

	// List that accumulate changes
	// SyncInfo
	private Map<IResource, SyncInfo> changedResources = new HashMap<>();
	private Set<IResource> removedResources = new HashSet<>();
	private Map<IResource, SyncInfo> addedResources = new HashMap<>();

	private boolean reset = false;

	private List<ITeamStatus> errors = new ArrayList<>();

	public SyncInfoSetChangeEvent(SyncInfoSet set) {
		super();
		this.set = set;
	}

	public void added(SyncInfo info) {
		if (removedResources.contains(info.getLocal())) {
			// A removal followed by an addition is treated as a change
			removedResources.remove(info.getLocal());
			changed(info);
		} else {
			addedResources.put(info.getLocal(), info);
		}
	}

	public void removed(IResource resource) {
		if (changedResources.containsKey(resource)) {
			// No use in reporting the change since it has subsequently been removed
			changedResources.remove(resource);
		} else if (addedResources.containsKey(resource)) {
			// An addition followed by a removal can be dropped
			addedResources.remove(resource);
			return;
		}
		removedResources.add(resource);
	}

	public void changed(SyncInfo info) {
		IResource resource = info.getLocal();
		if (addedResources.containsKey(resource)) {
			// An addition followed by a change is an addition
			addedResources.put(resource, info);
			return;
		}
		changedResources.put(resource, info);
	}

	@Override
	public SyncInfo[] getAddedResources() {
		return addedResources.values().toArray(new SyncInfo[addedResources.size()]);
	}

	@Override
	public SyncInfo[] getChangedResources() {
		return changedResources.values().toArray(new SyncInfo[changedResources.size()]);
	}

	@Override
	public IResource[] getRemovedResources() {
		return removedResources.toArray(new IResource[removedResources.size()]);
	}

	@Override
	public SyncInfoSet getSet() {
		return set;
	}

	public void reset() {
		reset = true;
	}

	public boolean isReset() {
		return reset;
	}

	public boolean isEmpty() {
		return changedResources.isEmpty() && removedResources.isEmpty() && addedResources.isEmpty() && errors.isEmpty();
	}

	public void errorOccurred(ITeamStatus status) {
		errors.add(status);
	}

	public ITeamStatus[] getErrors() {
		return errors.toArray(new ITeamStatus[errors.size()]);
	}
}
