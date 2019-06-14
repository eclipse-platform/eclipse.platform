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

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfoFilter.ContentComparisonSyncInfoFilter;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.MutableResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * Reconcile an existing unshared local project with an existing remote folder.
 */
public class ReconcileProjectOperation extends ShareProjectOperation {

	private ICVSRemoteFolder folder;
	private ContentComparisonSyncInfoFilter contentCompare = new ContentComparisonSyncInfoFilter(false);

	public ReconcileProjectOperation(Shell shell, IProject project, ICVSRemoteFolder folder) {
		super(shell, folder.getRepository(), project, folder.getRepositoryRelativePath());
		this.folder = folder;
	}

	@Override
	protected String getTaskName() {
		return NLS.bind(CVSUIMessages.ReconcileProjectOperation_0, new String[] { getProject().getName(), folder.getRepositoryRelativePath() }); 
	}
	
	@Override
	protected ICVSRemoteFolder createRemoteFolder(IProgressMonitor monitor) throws CVSException {
		// The folder already exists so just return the handle
		return folder;
	}
	
	@Override
	protected void mapProjectToRemoteFolder(ICVSRemoteFolder remote, IProgressMonitor monitor) throws TeamException {
		// Map the project
		monitor.beginTask(null, 100);
		super.mapProjectToRemoteFolder(remote, Policy.subMonitorFor(monitor, 10));
		// Reconcile the sync info
		reconcileSyncInfo(Policy.subMonitorFor(monitor, 90));
		monitor.done();
	}
	
	/*
	 * @see
	 * org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse
	 * .core.runtime.IProgressMonitor)
	 */
	protected void reconcileSyncInfo(IProgressMonitor monitor) throws CVSException {
		try {
			monitor.beginTask(null, 100);
			// Fetch the entire remote tree
			ICVSRemoteFolder remote = CheckoutToRemoteFolderOperation.checkoutRemoteFolder(getPart(), folder, Policy.subMonitorFor(monitor, 80));
			// Traverse the tree and populate the workspace base or remote
			// with the sync info depending on file contents
			populateWorkspace(remote, Policy.subMonitorFor(monitor, 20));
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		} finally {
			monitor.done();
		}

	}

	private void populateWorkspace(final ICVSRemoteFolder remote, IProgressMonitor monitor) throws CVSException {
		CVSWorkspaceRoot.getCVSFolderFor(getProject())
				.run(monitor1 -> populateWorkspace(getProject(), remote, monitor1), monitor);
		
	}

	/* private */ void populateWorkspace(IResource resource, ICVSRemoteResource remote, IProgressMonitor monitor) throws CVSException {
		try {
			if (resource.getType() == IResource.FILE) {
				if (remote.isContainer()) {
					CVSUIPlugin.log(IStatus.ERROR, NLS.bind(CVSUIMessages.ReconcileProjectOperation_1, new String[] { resource.getFullPath().toString(), remote.getRepositoryRelativePath() }), null); 
				} else {
					IFile file = (IFile)resource;
					IResourceVariant variant = (IResourceVariant)remote;
					if (file.exists() 
							&& variant != null 
							&& contentCompare.compareContents(file, variant, monitor)) {
						// The contents are the same so populate the local workspace
						// with the remote sync info and make the file in-sync
						makeInSync(file, remote, monitor);
					} else {
						// Would like to put the bytes in the remote but this
						// is complicated due to subscriber events.
						// We'll refresh the subcriber at the end.
					}
				}
			} else {
				if (!remote.isContainer()) {
					CVSUIPlugin.log(IStatus.ERROR, NLS.bind(CVSUIMessages.ReconcileProjectOperation_2, new String[] { resource.getFullPath().toString(), remote.getRepositoryRelativePath() }), null); 
				} else {
					// Map the local folder to the remote folder.
					// (Note that this will make phantoms for non-exisiting local folders)
					ICVSFolder folder = (ICVSFolder)CVSWorkspaceRoot.getCVSResourceFor(resource);
					folder.setFolderSyncInfo(((ICVSFolder)remote).getFolderSyncInfo());
					// Traverse the children of the remote
					// (The members were prefetched).
					ICVSRemoteResource[] members = remote.members(monitor);
					for (ICVSRemoteResource member : members) {
						populateWorkspace(getLocalChild((IContainer)resource, member), member, monitor);
					}
				}
			}
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
	}

	/*
	 * Get the local resource handle for corresponding to the remote resource
	 */
	private IResource getLocalChild(IContainer parent, ICVSRemoteResource member) {
		IResource resource = parent.findMember(member.getName());
		if (resource == null) {
			if (member.isContainer()) {
				resource = parent.getFolder(new Path(null, member.getName()));
			} else {
				resource = parent.getFile(new Path(null, member.getName()));
			}
		}
		return resource;
	}

	/*
	 * Make the file in-sync with its corresponding remote.
	 */
	private void makeInSync(IFile file, ICVSRemoteResource remote, IProgressMonitor monitor) throws CVSException {
		ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
		ResourceSyncInfo info = remote.getSyncInfo();
		Date modTime = info.getTimeStamp();
		if (modTime != null) {
			cvsFile.setTimeStamp(modTime);
		}
		modTime = cvsFile.getTimeStamp();
		MutableResourceSyncInfo newInfoWithTimestamp = info.cloneMutable();
		newInfoWithTimestamp.setTimeStamp(modTime);
		cvsFile.setSyncInfo(newInfoWithTimestamp, ICVSFile.CLEAN);
	}

}
