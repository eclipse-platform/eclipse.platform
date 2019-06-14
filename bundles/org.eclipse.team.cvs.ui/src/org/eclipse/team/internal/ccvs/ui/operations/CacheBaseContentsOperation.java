/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfoFilter.ContentComparisonSyncInfoFilter;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation that ensures that the contents for base
 * of each local resource is cached.
 */
public class CacheBaseContentsOperation extends CacheTreeContentsOperation {


	public CacheBaseContentsOperation(IWorkbenchPart part, ResourceMapping[] mappings, IResourceDiffTree tree, boolean includeOutgoing) {
		super(part, mappings, tree);
	}

	@Override
	protected IFileRevision getRemoteFileState(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff == null)
			diff = (IResourceDiff)twd.getLocalChange();
		IFileRevision base = diff.getBeforeState();
		return base;
	}

	@Override
	protected boolean isEnabledForDirection(int direction) {
		return true;
	}

	@Override
	protected ICVSRemoteResource buildTree(CVSTeamProvider provider) throws TeamException {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().buildBaseTree(provider.getProject(), true, new NullProgressMonitor());
	}
	
	@Override
	protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		IResource[] localChanges = getFilesWithLocalChanges(resources, recurse);
		super.execute(provider, resources, recurse, monitor);
		// Now that the contents are cached, reset the timestamps for any false local changes
		if (localChanges.length > 0) {
			performCleanTimestamps(localChanges[0].getProject(), localChanges, monitor);
		}
	}
	
	private IResource[] getFilesWithLocalChanges(IResource[] resources, boolean recurse) {
		ArrayList<IResource> result = new ArrayList<>();
		for (IResource resource : resources) {
			IDiff[] nodes = getTree().getDiffs(resource, recurse ? IResource.DEPTH_INFINITE: IResource.DEPTH_ONE);
			for (IDiff node : nodes) {
				if (isFileWithLocalChange(node)) {
					result.add(getTree().getResource(node));
				}
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	private boolean isFileWithLocalChange(IDiff node) {
		if (node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			return (twd.getDirection() == IThreeWayDiff.OUTGOING || twd.getDirection() == IThreeWayDiff.CONFLICTING)
				&& getTree().getResource(node).getType() == IResource.FILE;
		}
		return false;
	}
	
	private void performCleanTimestamps(IProject project, final IResource[] resources, IProgressMonitor monitor) throws CVSException {
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project);
		final ContentComparisonSyncInfoFilter comparator = new SyncInfoFilter.ContentComparisonSyncInfoFilter(false);
		folder.run(monitor1 -> {
			monitor1.beginTask(null, resources.length * 100);
			for (IResource resource : resources) {
				if (resource.exists() && resource.getType() == IResource.FILE) {
					IResourceVariant remoteResource = (IResourceVariant) CVSWorkspaceRoot
							.getRemoteResourceFor(resource);
					if (remoteResource != null && comparator.compareContents((IFile) resource, remoteResource,
							Policy.subMonitorFor(monitor1, 100))) {
						ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile) resource);
						cvsFile.checkedIn(null, false /* not a commit */);
					}
				}
			}
			monitor1.done();
		}, Policy.subMonitorFor(monitor, 100));
	}

}
