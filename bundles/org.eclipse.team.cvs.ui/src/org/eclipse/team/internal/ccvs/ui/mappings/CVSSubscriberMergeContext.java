/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.*;
import org.eclipse.team.core.mapping.provider.MergeStatus;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;

public abstract class CVSSubscriberMergeContext extends SubscriberMergeContext {

	private static final IStorageMerger MERGER = new DelegatingStorageMerger() {
		protected IStorageMerger createDelegateMerger(IStorage target) throws CoreException {
			IStorageMerger storageMerger = super.createDelegateMerger(target);
			if (storageMerger == null) {
				if (target instanceof IFile) {
					IFile file = (IFile) target;
					if (isText(file))
						storageMerger = createTextMerger();
				}
			}
			return storageMerger;
		}

		protected int getType(IStorage target) {
			if (target instanceof IFile) {
				IFile file = (IFile) target;
				if (isText(file))
					return Team.TEXT;
				return Team.BINARY;
			}
			return super.getType(target);
		}
		
		private boolean isText(IFile file) {
			try {
				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
				byte[] syncBytes = cvsFile.getSyncBytes();
				if (syncBytes != null)
					return !ResourceSyncInfo.isBinary(syncBytes);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
			return false;
		}
	};
	
	protected CVSSubscriberMergeContext(Subscriber subscriber, ISynchronizationScopeManager manager) {
		super(subscriber, manager);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.MergeContext#run(org.eclipse.core.resources.IWorkspaceRunnable, org.eclipse.core.runtime.jobs.ISchedulingRule, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IWorkspaceRunnable runnable, final ISchedulingRule rule, int flags, IProgressMonitor monitor) throws CoreException {
		super.run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				EclipseSynchronizer.getInstance().run(rule, new ICVSRunnable(){
					public void run(IProgressMonitor monitor) throws CVSException {
						try {
							runnable.run(monitor);
						} catch (CoreException e) {
							throw CVSException.wrapException(e);
						}
					}
				}, monitor);
			}
		
		}, rule, flags, monitor);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.MergeContext#getMergeRule(org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule getMergeRule(IDiff node) {
		// Return the project since that is what the EclipseSynchronize needs
		return getDiffTree().getResource(node).getProject();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.provider.MergeContext#makeInSync(org.eclipse.team.core.diff.IDiff, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void makeInSync(IDiff diff, IProgressMonitor monitor) throws CoreException {
		markAsMerged(diff, true, monitor);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IMergeContext#reject(org.eclipse.team.core.diff.IDiff, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void reject(IDiff diff, IProgressMonitor monitor) throws CoreException {
		markAsMerged(diff, false, monitor);
	}
	
	public IStatus merge(final IDiff[] diffs, final boolean ignoreLocalChanges, IProgressMonitor monitor) throws CoreException {
		final IStatus[] result = new IStatus[] { Status.OK_STATUS };
		if (diffs.length > 0)
			run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					result[0] = internalMerge(diffs, ignoreLocalChanges, monitor);
				}
			}, getMergeRule(diffs), IWorkspace.AVOID_UPDATE, monitor);
		return result[0];
	}

	private IStatus internalMerge(final IDiff[] diffs, final boolean ignoreLocalChanges, IProgressMonitor monitor) throws CoreException {
		
		// The list of diffs that add or change the local file
		List fileChanges = new ArrayList();
		// The list of folders diffs
		List folderDiffs = new ArrayList();
		// The list of diffs that will result in the deletion of
		// the local file
		List fileDeletions = new ArrayList();
		
		for (int i = 0; i < diffs.length; i++) {
			IDiff diff = diffs[i];
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource.getType() == IResource.FILE) {
				if (isIncomingDeletion(diff, ignoreLocalChanges)) {
					fileDeletions.add(diff);
				} else {
					fileChanges.add(diff);
				}
			} else {
				folderDiffs.add(diff);
			}
		}
		
		if (fileDeletions.isEmpty() && fileChanges.isEmpty() && folderDiffs.isEmpty())
			return Status.OK_STATUS;
		
		// We do deletions first so that case changes can occur on platforms that are no case sensitive
		int ticks = (fileDeletions.size() + fileChanges.size()) * 100;
		try {
			monitor.beginTask(null, ticks);
			List result = new ArrayList();
			if (!fileDeletions.isEmpty()) {
				IStatus status = CVSSubscriberMergeContext.super.merge(
						(IDiff[]) fileDeletions.toArray(new IDiff[fileDeletions.size()]), 
						ignoreLocalChanges, 
						Policy.subMonitorFor(monitor, 100 * fileDeletions.size()));
				if (!status.isOK()) {
					if (status.isMultiStatus()) {
						result.addAll(Arrays.asList(status.getChildren()));
					} else {
						result.add(status);
					}
				}
			}
			if (!fileChanges.isEmpty()) {
				IStatus status = CVSSubscriberMergeContext.super.merge(
						(IDiff[]) fileChanges.toArray(new IDiff[fileChanges.size()]), 
						ignoreLocalChanges, 
						Policy.subMonitorFor(monitor, 100 * fileChanges.size()));
				if (!status.isOK()) {
					if (status.isMultiStatus()) {
						result.addAll(Arrays.asList(status.getChildren()));
					} else {
						result.add(status);
					}
				}
			}
			if (!folderDiffs.isEmpty()) {
				// Order the diffs so empty added children will get deleted before their parents are visited
				Collections.sort(folderDiffs, new Comparator() {
					public int compare(Object o1, Object o2) {
						return ((IDiff)o2).getPath().toString().compareTo(((IDiff)o1).getPath().toString());
					}
				});
				for (Iterator iter = folderDiffs.iterator(); iter.hasNext();) {
					IDiff diff = (IDiff) iter.next();
					IResource resource = ResourceDiffTree.getResourceFor(diff);
					IDiff currentDiff = getSubscriber().getDiff(resource);
					merge(currentDiff, ignoreLocalChanges, monitor);
				}
			}
			if (result.isEmpty())
				return Status.OK_STATUS;
			if (result.size() == 1)
				return (IStatus)result.get(0);
			return new MergeStatus(CVSUIPlugin.ID, ((IStatus)result.get(0)).getMessage(), getFailedFiles(result));
		} finally {
			monitor.done();
		}
	}

	private boolean isIncomingDeletion(IDiff diff, boolean ignoreLocalChanges) {
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			if (twd.getKind() == IDiff.REMOVE && twd.getDirection() == IThreeWayDiff.INCOMING)
				return true;
			IDiff remoteChange = twd.getRemoteChange();
			if (ignoreLocalChanges && remoteChange != null)
				return isIncomingDeletion(remoteChange, ignoreLocalChanges);
			IDiff localChange = twd.getLocalChange();
			if (ignoreLocalChanges && localChange != null)
				return isIncomingDeletion(localChange, ignoreLocalChanges);
			return false;
		}
		if (diff instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) diff;
			return (ignoreLocalChanges || getMergeType() == ISynchronizationContext.TWO_WAY) && rd.getAfterState() == null;
		}
		return false;
	}

	private IFile[] getFailedFiles(List result) {
		List failures = new ArrayList();
		for (Iterator iter = result.iterator(); iter.hasNext();) {
			IStatus status = (IStatus) iter.next();
			if (status instanceof MergeStatus) {
				MergeStatus ms = (MergeStatus) status;
				failures.addAll(Arrays.asList(ms.getConflictingFiles()));
			}
		}
		return (IFile[]) failures.toArray(new IFile[failures.size()]);
	}
	
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IStorageMerger.class)
			return adapter.cast(MERGER);
		return super.getAdapter(adapter);
	}

	protected SubscriberDiffTreeEventHandler getHandler() {
		Object o = getAdapter(SubscriberDiffTreeEventHandler.class);
		if (o instanceof SubscriberDiffTreeEventHandler) {
			return (SubscriberDiffTreeEventHandler) o;
		}
		return null;
	}
}
