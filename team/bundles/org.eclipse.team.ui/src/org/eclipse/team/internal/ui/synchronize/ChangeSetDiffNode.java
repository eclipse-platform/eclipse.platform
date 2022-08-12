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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * Node that represents a Change set in a synchronize page.
 */
public class ChangeSetDiffNode extends SynchronizeModelElement {

	private final ChangeSet set;

	public ChangeSetDiffNode(IDiffContainer parent, ChangeSet set) {
		super(parent);
		this.set = set;
	}

	@Override
	public IResource getResource() {
		return null;
	}

	public ChangeSet getSet() {
		return set;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_CHANGE_SET);
	}

	@Override
	public String getName() {
		return set.getName();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChangeSetDiffNode) {
			return((ChangeSetDiffNode)object).getSet() == set;
		}
		return super.equals(object);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(ChangeSet.class)) {
			return (T) set;
		}
		return super.getAdapter(adapter);
	}
}
