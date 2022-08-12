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

import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoTree;

/**
 * A checked-in change set represents a group of resource
 * changes that were previously checked into a repository
 * as a single logical change.
 * <p>
 * A previously checked-in set of changes may not apply directly
 * to the local versions of the resources involved. However,
 * a <code>SyncInfo</code> is still used to represent each change.
 * The base and remote slots of the <code>SyncInfo</code> identify
 * the state before and after the resources were checked-in.
 * @since 3.1
 */
public abstract class CheckedInChangeSet extends ChangeSet {

	private final SyncInfoTree set = new SyncInfoTree();

	public abstract String getAuthor();

	public abstract Date getDate();

	/**
	 * Return the SyncInfoSet that contains the resources that belong to this change set.
	 * @return  the SyncInfoSet that contains the resources that belong to this change set
	 */
	public SyncInfoTree getSyncInfoSet() {
		return set;
	}

	/**
	 * Return the resources that are contained in this set.
	 * @return the resources that are contained in this set
	 */
	@Override
	public IResource[] getResources() {
		return set.getResources();
	}

	/**
	 * Return whether the set contains any files.
	 * @return whether the set contains any files
	 */
	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	/**
	 * Return true if the given file is included in this set.
	 * @param local a local file
	 * @return true if the given file is included in this set
	 */
	@Override
	public boolean contains(IResource local) {
		return set.getSyncInfo(local) != null;
	}

	/**
	 * Add the resource to this set if it is modified
	 * w.r.t. the subscriber.
	 * @param info
	 */
	public void add(SyncInfo info) {
		if (isValidChange(info)) {
			set.add(info);
		}
	}

	/**
	 * Return whether the given sync info is a valid change
	 * and can be included in this set. This method is used
	 * by the <code>add</code> method to filter set additions.
	 * @param info a sync info
	 * @return whether the sync info is a valid member of this set
	 */
	protected boolean isValidChange(SyncInfo info) {
		return (info != null);
	}

	/**
	 * Add the resources to this set if they are modified
	 * w.r.t. the subscriber.
	 * @param infos the resources to be added.
	 */
	public void add(SyncInfo[] infos) {
		try {
			set.beginInput();
			for (SyncInfo info : infos) {
				add(info);
			}
		} finally {
			set.endInput(null);
		}
	}

	/**
	 * Remove the resource from the set.
	 * @param resource the resource to be removed
	 */
	@Override
	public void remove(IResource resource) {
		if (contains(resource)) {
			set.remove(resource);
		}
	}

	/**
	 * Remove the resources from the set.
	 * @param resources the resources to be removed
	 */
	@Override
	public void remove(IResource[] resources) {
		for (IResource resource : resources) {
			remove(resource);
		}
	}

	/**
	 * Remove the resource and it's descendants to the given depth.
	 * @param resource the resource to be removed
	 * @param depth the depth of the removal (one of
	 * <code>IResource.DEPTH_ZERO, IResource.DEPTH_ONE, IResource.DEPTH_INFINITE)</code>
	 */
	@Override
	public void rootRemoved(IResource resource, int depth) {
		SyncInfo[] infos = set.getSyncInfos(resource, depth);
		if (infos.length > 0) {
			IResource[] resources = new IResource[infos.length];
			for (int i = 0; i < resources.length; i++) {
				resources[i] = infos[i].getLocal();
			}
			set.removeAll(resources);
		}
	}

	@Override
	public boolean containsChildren(IResource resource, int depth) {
		return set.getSyncInfos(resource, depth).length > 0;
	}
}
