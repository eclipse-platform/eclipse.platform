/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

//
/**
 * Visits a unified tree, and collects local sync information in
 * a multi-status.  At the end of the visit, the resource tree will NOT
 * be synchronized with the file system, but all discrepancies between
 * the two will be recorded in the returned status.
 */
public class CollectSyncStatusVisitor extends RefreshLocalVisitor {
	protected List<Resource> affectedResources;
	/**
	 * Determines how to treat cases where the resource is missing from
	 * the local file system.  When performing a deletion with force=false,
	 * we don't care about files that are out of sync because they do not
	 * exist in the file system.
	 */
	private boolean ignoreLocalDeletions = false;
	protected MultiStatus status;

	/**
	 * Creates a new visitor, whose sync status will have the given title.
	 */
	public CollectSyncStatusVisitor(String multiStatusTitle, IProgressMonitor monitor) {
		super(monitor);
		status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IStatus.INFO, multiStatusTitle, null);
	}

	protected void changed(Resource target) {
		String message = NLS.bind(Messages.localstore_resourceIsOutOfSync, target.getFullPath());
		status.add(new ResourceStatus(IResourceStatus.OUT_OF_SYNC_LOCAL, target.getFullPath(), message));
		if (affectedResources == null)
			affectedResources = new ArrayList<>(20);
		affectedResources.add(target);
		resourceChanged = true;
	}

	@Override
	protected void createResource(UnifiedTreeNode node, Resource target) {
		changed(target);
	}

	@Override
	protected void deleteResource(UnifiedTreeNode node, Resource target) {
		if (!ignoreLocalDeletions)
			changed(target);
	}

	@Override
	protected void fileToFolder(UnifiedTreeNode node, Resource target) {
		changed(target);
	}

	@Override
	protected void folderToFile(UnifiedTreeNode node, Resource target) {
		changed(target);
	}

	/**
	 * Returns the list of resources that were not synchronized with
	 * the local file system, or <code>null</code> if all resources
	 * are synchronized.
	 */
	public List<Resource> getAffectedResources() {
		return affectedResources;
	}

	/**
	 * Returns the sync status that has been collected as a result of this visit.
	 */
	public MultiStatus getSyncStatus() {
		return status;
	}

	@Override
	protected void makeLocal(UnifiedTreeNode node, Resource target) {
		changed(target);
	}

	@Override
	protected void refresh(Container parent) {
		changed(parent);
	}

	@Override
	protected void resourceChanged(UnifiedTreeNode node, Resource target) {
		changed(target);
	}

	/**
	 * Instructs this visitor to ignore changes due to local deletions
	 * in the file system.
	 */
	public void setIgnoreLocalDeletions(boolean value) {
		this.ignoreLocalDeletions = value;
	}
}
