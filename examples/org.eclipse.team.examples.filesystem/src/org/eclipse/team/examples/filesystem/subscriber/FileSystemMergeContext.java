/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.subscriber;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.MergeStatus;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;

/**
 * A merge context for merging file system changes.
 */
public class FileSystemMergeContext extends SubscriberMergeContext {

	/**
	 * Create the file system merge context for the given scope manager.
	 * @param manager the scope manager
	 */
	public FileSystemMergeContext(ISynchronizationScopeManager manager) {
		super(FileSystemSubscriber.getInstance(), manager);
		initialize();
	}

	@Override
	protected void makeInSync(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		IResource resource = ResourceDiffTree.getResourceFor(diff);
		FileSystemSubscriber.getInstance().makeInSync(resource);
	}

	@Override
	public void markAsMerged(IDiff diff, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		// TODO if inSyncHint is true, we should test to see if the contents match
		IResource resource = ResourceDiffTree.getResourceFor(diff);
		FileSystemSubscriber.getInstance().markAsMerged(resource, monitor);
	}

	@Override
	public void reject(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		markAsMerged(diff, false, monitor);
	}

	@Override
	public ISchedulingRule getMergeRule(IDiff node) {
		return ResourceDiffTree.getResourceFor(node).getProject();
	}

	@Override
	public IStatus merge(IDiff diff, boolean ignoreLocalChanges, IProgressMonitor monitor) throws CoreException {
		// Only attempt the merge for non-conflicts. The reason we do this
		// is because the file system provider doesn't really have the proper base
		// so merging conflicts doesn't work properly
		if (!ignoreLocalChanges) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (diff instanceof IThreeWayDiff && resource instanceof IFile) {
				IThreeWayDiff twd = (IThreeWayDiff) diff;
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING) {
					return new MergeStatus(FileSystemPlugin.ID, "Cannot merge conflicting files", new IFile[] { (IFile)resource });
				}
			}
		}
		return super.merge(diff, ignoreLocalChanges, monitor);
	}

}
