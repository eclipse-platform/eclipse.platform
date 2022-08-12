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
package org.eclipse.team.internal.core.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffTree;

/**
 * Implementation of {@link IDiffChangeEvent}
 */
public class DiffChangeEvent implements IDiffChangeEvent {

	private final IDiffTree tree;

	// List that accumulate changes
	// SyncInfo
	private Map<IPath, IDiff> changedResources = new HashMap<>();
	private Set<IPath> removedResources = new HashSet<>();
	private Map<IPath, IDiff> addedResources = new HashMap<>();

	private boolean reset = false;

	private List<IStatus> errors = new ArrayList<>();

	/**
	 * Create a diff change event
	 * @param tree the originating tree
	 */
	public DiffChangeEvent(IDiffTree tree) {
		this.tree = tree;
	}

	@Override
	public IDiffTree getTree() {
		return tree;
	}

	@Override
	public IDiff[] getAdditions() {
		return addedResources.values().toArray(new IDiff[addedResources.size()]);
	}

	@Override
	public IPath[] getRemovals() {
		return removedResources.toArray(new IPath[removedResources.size()]);
	}

	@Override
	public IDiff[] getChanges() {
		return changedResources.values().toArray(new IDiff[changedResources.size()]);
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

	@Override
	public IStatus[] getErrors() {
		return errors.toArray(new IStatus[errors.size()]);
	}

}
