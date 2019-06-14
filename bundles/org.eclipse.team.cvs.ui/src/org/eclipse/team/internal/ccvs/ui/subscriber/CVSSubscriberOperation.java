/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.PruneFolderVisitor;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

public abstract class CVSSubscriberOperation extends SynchronizeModelOperation {
	
	protected CVSSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		// Divide the sync info by project
		final Map projectSyncInfos = getProjectSyncInfoSetMap();
		monitor.beginTask(null, projectSyncInfos.size() * 100);
		for (Iterator iter = projectSyncInfos.keySet().iterator(); iter.hasNext(); ) {
			final IProject project = (IProject) iter.next();
			run(projectSyncInfos, project, monitor);
		}
		monitor.done();
	}

	/**
	 * Run the operation for the sync infos from the given project. By default, a lock
	 * is acquired on the project.
	 * @param projectSyncInfos the project syncInfos
	 * @param project the project
	 * @param monitor a progress monitor
	 * @throws InvocationTargetException
	 */
	protected void run(final Map projectSyncInfos, final IProject project, IProgressMonitor monitor) throws InvocationTargetException {
		try {
			// Pass the scheduling rule to the synchronizer so that sync change events
			// and cache commits to disk are batched
			EclipseSynchronizer.getInstance().run(
				project,
					monitor1 -> {
						try {
							CVSSubscriberOperation.this.runWithProjectRule(project,
									(SyncInfoSet) projectSyncInfos.get(project), monitor1);
						} catch (TeamException e) {
							throw CVSException.wrapException(e);
					}
				}, Policy.subMonitorFor(monitor, 100));
		} catch (TeamException e) {
			throw new InvocationTargetException(e);
		}
	}

	/**
	 * Run the operation on the sync info in the given set. The sync info will be all
	 * from the same project. Also, a scheduling rule on the project will be
	 * held when this method is invoked.
	 * @param project the project that contaisn the sync info.
	 * @param set the sync info set
	 * @param monitor a progress monitor
	 */
	protected abstract void runWithProjectRule(IProject project, SyncInfoSet set, IProgressMonitor monitor) throws TeamException;

	/*
	 * Indicate that the resource is out of sync if the sync state is not IN_SYNC
	 * or if the local doesn't exist but the remote does.
	 */
	protected boolean isOutOfSync(SyncInfo resource) {
		if (resource == null) return false;
		return (!(resource.getKind() == 0) || 
				(! resource.getLocal().exists() && resource.getRemote() != null));
	}
	
	protected void makeInSync(SyncInfo[] folders, IProgressMonitor monitor) throws TeamException {
		// If a node has a parent that is an incoming folder creation, we have to 
		// create that folder locally and set its sync info before we can get the
		// node itself. We must do this for all incoming folder creations (recursively)
		// in the case where there are multiple levels of incoming folder creations.
		monitor.beginTask(null, folders.length);
		for (SyncInfo resource : folders) {
			makeInSync(resource);
			monitor.worked(1);
		}
		monitor.done();
	}
	
	protected boolean makeInSync(SyncInfo info) throws TeamException {
		if (isOutOfSync(info)) {
			SyncInfo parent = getParent(info);
			if (parent == null) {
				if (info.getLocal().getType() == IResource.ROOT) {
					// ROOT should be null
					return true;
				} else {
					// No other ancestors should be null. Log the problem.
					CVSUIPlugin.log(IStatus.WARNING, NLS.bind(CVSUIMessages.CVSSubscriberAction_0, new String[] { info.getLocal().getFullPath().toString() }), null); 
					return false;
				}
			} else {
				if (!makeInSync(parent)) {
					// The failed makeInSync will log any errors
					return false;
				}
			}
			if (info instanceof CVSSyncInfo) {
				CVSSyncInfo cvsInfo= (CVSSyncInfo) info;
				IStatus status = cvsInfo.makeInSync();
				if (status.getSeverity() == IStatus.ERROR) {
					logError(status);
					return false;
				}
				return true;
			}
			return false;
		} else {
			return true;
		}
	}
	
	protected void makeOutgoing(SyncInfo[] folders, IProgressMonitor monitor) throws TeamException {
		// If a node has a parent that is an incoming folder creation, we have to 
		// create that folder locally and set its sync info before we can get the
		// node itself. We must do this for all incoming folder creations (recursively)
		// in the case where there are multiple levels of incoming folder creations.
		monitor.beginTask(null, 100 * folders.length);
		for (SyncInfo info : folders) {
			makeOutgoing(info, Policy.subMonitorFor(monitor, 100));
		}
		monitor.done();
	}
	
	private void makeOutgoing(SyncInfo info, IProgressMonitor monitor) throws TeamException {
		if (info == null) return;
		if (info instanceof CVSSyncInfo) {
			CVSSyncInfo cvsInfo= (CVSSyncInfo) info;
			IStatus status = cvsInfo.makeOutgoing(monitor);
			if (status.getSeverity() == IStatus.ERROR) {
				logError(status);
			}
		}
	}

	/**
	 * Log an error associated with an operation.
	 * @param status
	 */
	protected void logError(IStatus status) {
		CVSUIPlugin.log(status);
	}

	/**
	 * Handle the exception by showing an error dialog to the user.
	 * Sync actions seem to need to be sync-execed to work
	 * @param t
	 */
	protected void handle(Exception t) {
		CVSUIPlugin.openError(getShell(), getErrorTitle(), null, t, CVSUIPlugin.PERFORM_SYNC_EXEC | CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS);
	}

	/**
	 * Return the error title that will appear in any error dialogs shown to the user
	 * @return
	 */
	protected String getErrorTitle() {
		return null;
	}

	@Override
	protected boolean canRunAsJob() {
		return true;
	}
	
	protected void pruneEmptyParents(SyncInfo[] nodes) throws CVSException {
		// TODO: A more explicit tie in to the pruning mechanism would be prefereable.
		// i.e. I don't like referencing the option and visitor directly
		if (!CVSProviderPlugin.getPlugin().getPruneEmptyDirectories()) return;
		ICVSResource[] cvsResources = new ICVSResource[nodes.length];
		for (int i = 0; i < cvsResources.length; i++) {
			cvsResources[i] = CVSWorkspaceRoot.getCVSResourceFor(nodes[i].getLocal());
		}
		new PruneFolderVisitor().visit(
			CVSWorkspaceRoot.getCVSFolderFor(ResourcesPlugin.getWorkspace().getRoot()),
			cvsResources);
	}
	
	public CVSSyncInfo getCVSSyncInfo(SyncInfo info) {
		if (info instanceof CVSSyncInfo) {
			return (CVSSyncInfo)info;
		}
		return null;
	}
	
	protected SyncInfo getParent(SyncInfo info) throws TeamException {
		return ((CVSSyncInfo)info).getSubscriber().getSyncInfo(info.getLocal().getParent());
	}

	protected IResource[] getIResourcesFrom(SyncInfo[] nodes) {
		List resources = new ArrayList(nodes.length);
		for (SyncInfo node : nodes) {
			resources.add(node.getLocal());
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	/**
	 * Prompt to overwrite those resources that could not be safely updated
	 * Note: This method is designed to be overridden by test cases.
	 * 
	 * @return whether to perform the overwrite
	 */
	protected boolean promptForOverwrite(final SyncInfoSet syncSet) {
		final int[] result = new int[] {Window.CANCEL};
		TeamUIPlugin.getStandardDisplay().syncExec(() -> {
			UpdateDialog dialog = new UpdateDialog(getShell(), syncSet);
			result[0] = dialog.open();
		});
		return (result[0] == UpdateDialog.YES);
	}
	
	/**
	 * Make the contents of the local resource match that of the remote
	 * without modifying the sync info of the local resource.
	 * If called on a new folder, the sync info will be copied.
	 */
	protected void makeRemoteLocal(SyncInfo info, IProgressMonitor monitor) throws TeamException {
		IResourceVariant remote = info.getRemote();
		IResource local = info.getLocal();
		try {
			if(remote==null) {
				if (local.exists()) {
					local.delete(IResource.KEEP_HISTORY, monitor);
				}
			} else {
				if(remote.isContainer()) {
					ensureContainerExists(info);
				} else {
					monitor.beginTask(null, 200);
					try {
						IFile localFile = (IFile)local;
						if(local.exists()) {
							localFile.setContents(remote.getStorage(Policy.subMonitorFor(monitor, 100)).getContents(), false /*don't force*/, true /*keep history*/, Policy.subMonitorFor(monitor, 100));
						} else {
							ensureContainerExists(getParent(info));
							localFile.create(remote.getStorage(Policy.subMonitorFor(monitor, 100)).getContents(), false /*don't force*/, Policy.subMonitorFor(monitor, 100));
						}
					} finally {
						monitor.done();
					}
				}
			}
		} catch(CoreException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, CVSUIMessages.UpdateMergeActionProblems_merging_remote_resources_into_workspace_1,e, local);
			throw new CVSException(status);
		}
	}
	
	private boolean ensureContainerExists(SyncInfo info) throws TeamException {
		IResource local = info.getLocal();
		// make sure that the parent exists
		if (!local.exists()) {
			if (!ensureContainerExists(getParent(info))) {
				return false;
			}
		}
		// make sure that the folder sync info is set;
		if (isOutOfSync(info)) {
			if (info instanceof CVSSyncInfo) {
				CVSSyncInfo cvsInfo = (CVSSyncInfo)info;
				IStatus status = cvsInfo.makeInSync();
				if (status.getSeverity() == IStatus.ERROR) {
					logError(status);
					return false;
				}
			}
		}
		// create the folder if it doesn't exist
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)local);
		if (!cvsFolder.exists()) {
			cvsFolder.mkdir();
		}
		return true;
	}
	
	/*
	 * Divide the sync info for the operation by project
	 */
	private Map getProjectSyncInfoSetMap() {
		Map map = new HashMap();
		SyncInfoSet all = getSyncInfoSet();
		SyncInfo[] infos = all.getSyncInfos();
		for (SyncInfo info : infos) {
			IProject project = info.getLocal().getProject();
			SyncInfoSet set = (SyncInfoSet)map.get(project);
			if (set == null) {
				set = new SyncInfoSet();
				map.put(project, set);
			}
			set.add(info);
		}
		return map;
	}
}
