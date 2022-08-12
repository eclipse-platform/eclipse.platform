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
 *     Matt McCutchen - fix for bug 174492
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.util.Iterator;
import java.util.List;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

public class DeleteVisitor implements IUnifiedTreeVisitor, ICoreConstants {
	protected boolean force;
	protected boolean keepHistory;
	protected IProgressMonitor monitor;
	protected List<Resource> skipList;
	protected MultiStatus status;

	/**
	 * The number of tickets available on the progress monitor
	 */
	private int ticks;

	public DeleteVisitor(List<Resource> skipList, int flags, IProgressMonitor monitor, int ticks) {
		this.skipList = skipList;
		this.ticks = ticks;
		this.force = (flags & IResource.FORCE) != 0;
		this.keepHistory = (flags & IResource.KEEP_HISTORY) != 0;
		this.monitor = monitor;
		status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_DELETE_LOCAL, Messages.localstore_deleteProblem, null);
	}

	/**
	 * Deletes a file from both the workspace resource tree and the file system.
	 */
	protected void delete(UnifiedTreeNode node, boolean shouldKeepHistory) {
		Resource target = (Resource) node.getResource();
		try {
			final boolean deleteLocalFile = !target.isLinked() && node.existsInFileSystem();
			IFileStore localFile = deleteLocalFile ? node.getStore() : null;
			if (deleteLocalFile && shouldKeepHistory)
				recursiveKeepHistory(target.getLocalManager().getHistoryStore(), node);
			node.removeChildrenFromTree();
			//delete from disk
			int work = ticks < 0 ? 0 : ticks;
			ticks -= work;
			if (deleteLocalFile)
				localFile.delete(EFS.NONE, Policy.subMonitorFor(monitor, work));
			else
				monitor.worked(work);
			//delete from tree
			if (node.existsInWorkspace())
				target.deleteResource(true, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
			//	delete might have been partly successful, so refresh to ensure in sync
			try {
				target.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e1) {
				//ignore secondary failure - we are just trying to cleanup from first failure
			}
		}
	}

	/**
	 * Only consider path in equality in order to handle gender changes
	 */
	protected boolean equals(IResource one, IResource another) {
		return one.getFullPath().equals(another.getFullPath());
	}

	public MultiStatus getStatus() {
		return status;
	}

	protected boolean isAncestor(IResource one, IResource another) {
		return one.getFullPath().isPrefixOf(another.getFullPath()) && !equals(one, another);
	}

	protected boolean isAncestorOfResourceToSkip(IResource resource) {
		if (skipList == null)
			return false;
		for (IResource target : skipList) {
			if (isAncestor(resource, target))
				return true;
		}
		return false;
	}

	private void recursiveKeepHistory(IHistoryStore store, UnifiedTreeNode node) {
		final IResource target = node.getResource();
		//we don't delete linked content, so no need to keep history
		if (target.isLinked() || target.isVirtual() || node.isSymbolicLink())
			return;
		if (node.isFolder()) {
			monitor.subTask(NLS.bind(Messages.localstore_deleting, target.getFullPath()));
			for (Iterator<UnifiedTreeNode> children = node.getChildren(); children.hasNext();)
				recursiveKeepHistory(store, children.next());
		} else {
			IFileInfo info = node.fileInfo;
			if (info == null)
				info = new FileInfo(node.getLocalName());
			if (FileSystemResourceManager.storeHistory(node.getResource())) {
				store.addState(target.getFullPath(), node.getStore(), info, true);
			}
		}
		monitor.worked(1);
		ticks--;
	}

	protected void removeFromSkipList(IResource resource) {
		if (skipList != null)
			skipList.remove(resource);
	}

	protected boolean shouldSkip(IResource resource) {
		if (skipList == null)
			return false;
		for (Resource element : skipList)
			if (equals(resource, element))
				return true;
		return false;
	}

	@Override
	public boolean visit(UnifiedTreeNode node) {
		Policy.checkCanceled(monitor);
		Resource target = (Resource) node.getResource();
		if (shouldSkip(target)) {
			removeFromSkipList(target);
			int skipTicks = target.countResources(IResource.DEPTH_INFINITE, false);
			monitor.worked(skipTicks);
			ticks -= skipTicks;
			return false;
		}
		if (isAncestorOfResourceToSkip(target))
			return true;
		delete(node, keepHistory);
		return false;
	}
}
