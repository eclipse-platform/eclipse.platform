/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.HunkResult;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;

public class ApplyPatchSubscriberMergeContext extends SubscriberMergeContext {

	protected ApplyPatchSubscriberMergeContext(Subscriber subscriber,
			ISynchronizationScopeManager manager) {
		super(subscriber, manager);
	}

	public static ApplyPatchSubscriberMergeContext createContext(
			Subscriber subscriber, ISynchronizationScopeManager manager) {
		ApplyPatchSubscriberMergeContext mergeContext = new ApplyPatchSubscriberMergeContext(
				subscriber, manager);
		// Initialize using the ApplyPatchSubscriber to populate the diff tree.
		mergeContext.initialize();
		return mergeContext;
	}

	@Override
	protected void makeInSync(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		markAsMerged(diff, true, monitor);
	}

	@Override
	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		IResource resource = getDiffTree().getResource(node);
		WorkspacePatcher patcher = ((ApplyPatchSubscriber) getSubscriber()).getPatcher();
		Object object = PatchModelProvider.getPatchObject(resource, patcher);
		if (object instanceof FilePatch2) {
			HunkResult[] hunkResults = patcher.getDiffResult((FilePatch2) object).getHunkResults();
			for (HunkResult hunkResult : hunkResults) {
				if (inSyncHint) {
					// clean Merge > disable hunks that have merged
					if (hunkResult.isOK()) {
						patcher.setEnabled(hunkResult.getHunk(), false);
					}
				} else {
					// Mark as Merged > mark *all* hunks from the file as manually merged
					patcher.setManuallyMerged(hunkResult.getHunk(), true);
				}
			}
		} else {
			patcher.setEnabled(object, false);
			// TODO: mark as merged
		}
		// fire a team resource change event
		((ApplyPatchSubscriber)getSubscriber()).merged(new IResource[] { resource});
		// don't need to worry about the node no more... it is in sync now
		// see ApplyPatchSubscriber.ApplyPatchSyncInfo.calculateKind()
	}

	@Override
	protected IStatus performThreeWayMerge(IThreeWayDiff diff,
			IProgressMonitor monitor) throws CoreException {
		IStatus status = super.performThreeWayMerge(diff, monitor);
		if (status.isOK()) {
			// Merge with conflicts > all hunks from the diff have been marked
			// as manually merged...
			IResource resource = getDiffTree().getResource(diff);
			WorkspacePatcher patcher = ((ApplyPatchSubscriber) getSubscriber()).getPatcher();
			Object object = PatchModelProvider.getPatchObject(resource, patcher);
			if (object instanceof FilePatch2) {
				HunkResult[] hunkResults = patcher.getDiffResult((FilePatch2) object).getHunkResults();
				for (HunkResult hunkResult : hunkResults) {
					// ... unmark them and exclude those properly merged
					if (patcher.isManuallyMerged(hunkResult.getHunk())) {
						patcher.setManuallyMerged(hunkResult.getHunk(), false);
						if (hunkResult.isOK()) {
							patcher.setEnabled(hunkResult.getHunk(), false);
						}
					}
				}
			}
		}
		return status;
	}

	@Override
	public void reject(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		// do nothing
	}
}
