/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import java.util.*;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.diff.*;

/**
 * Implementation of {@link IDiffChangeEvent}
 */
public class DiffChangeEvent implements IDiffChangeEvent {

	private final IDiffTree tree;

	// List that accumulate changes
	// SyncInfo
	private Map changedResources = new HashMap();
	private Set removedResources = new HashSet();
	private Map addedResources = new HashMap();

	private boolean reset = false;

	private List errors = new ArrayList();

	/**
	 * Create a diff change event
	 * @param tree the originating tree
	 */
	public DiffChangeEvent(IDiffTree tree) {
		this.tree = tree;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.delta.ISyncDeltaChangeEvent#getTree()
	 */
	public IDiffTree getTree() {
		return tree;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.delta.ISyncDeltaChangeEvent#getAdditions()
	 */
	public IDiff[] getAdditions() {
		return (IDiff[]) addedResources.values().toArray(new IDiff[addedResources.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.delta.ISyncDeltaChangeEvent#getRemovals()
	 */
	public IPath[] getRemovals() {
		return (IPath[]) removedResources.toArray(new IPath[removedResources.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.delta.ISyncDeltaChangeEvent#getChanges()
	 */
	public IDiff[] getChanges() {
		return (IDiff[]) changedResources.values().toArray(new IDiff[changedResources.size()]);
	}

	public void added(IDiff delta) {
		if (removedResources.contains(delta.getPath())) {
			// A removal followed by an addition is treated as a change
			removedResources.remove(delta.getPath());
			changed(delta);
		} else {
			addedResources.put(delta.getPath(), delta);
		}
	}

	public void removed(IPath path, IDiff delta) {
		if (changedResources.containsKey(path)) {
			// No use in reporting the change since it has subsequently been removed
			changedResources.remove(path);
		} else if (addedResources.containsKey(path)) {
			// An addition followed by a removal can be dropped
			addedResources.remove(path);
			return;
		}
		removedResources.add(path);
	}

	public void changed(IDiff delta) {
		if (addedResources.containsKey(delta.getPath())) {
			// An addition followed by a change is an addition
			addedResources.put(delta.getPath(), delta);
			return;
		}
		changedResources.put(delta.getPath(), delta);
	}

	public void reset() {
		reset = true;
	}

	public boolean isReset() {
		return reset;
	}

	public boolean isEmpty() {
		return changedResources.isEmpty() && removedResources.isEmpty() && addedResources.isEmpty();
	}

	public void errorOccurred(IStatus status) {
		errors .add(status);
	}

	public IStatus[] getErrors() {
		return (IStatus[]) errors.toArray(new IStatus[errors.size()]);
	}

}
