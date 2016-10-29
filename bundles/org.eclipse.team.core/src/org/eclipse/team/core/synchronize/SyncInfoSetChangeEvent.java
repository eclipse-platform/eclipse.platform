/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.synchronize;

import java.util.*;

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
	private Map changedResources = new HashMap();
	private Set removedResources = new HashSet();
	private Map addedResources = new HashMap();

	private boolean reset = false;

	private List errors = new ArrayList();

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

	public SyncInfo[] getAddedResources() {
		return (SyncInfo[]) addedResources.values().toArray(new SyncInfo[addedResources.size()]);
	}

	public SyncInfo[] getChangedResources() {
		return (SyncInfo[]) changedResources.values().toArray(new SyncInfo[changedResources.size()]);
	}

	public IResource[] getRemovedResources() {
		return (IResource[]) removedResources.toArray(new IResource[removedResources.size()]);
	}

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
		return (ITeamStatus[]) errors.toArray(new ITeamStatus[errors.size()]);
	}
}
