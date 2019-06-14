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
 *     Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 179174 CVS client sets timestamps back when replacing
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.util;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.ConsoleListeners;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * This class is used to prepare a local CVS workspace for replacement by
 * the corresponding remote resources. More specifically, this class will
 * unmanage added and deleted resources so that, after the operation, the
 * resources in the local workspace will either correspond to the remote
 * counterparts or be unmanaged.
 */
public class PrepareForReplaceVisitor implements ICVSResourceVisitor {

	private IProgressMonitor monitor;
	private int depth;
	private CVSTag tag; 
	private Set/*<ICVSFile>*/ deletedFiles;
	private Session session;

	public PrepareForReplaceVisitor(Session session, CVSTag tag){
		this.tag = tag;
		this.session = session;
	}
	
	@Override
	public void visitFile(ICVSFile file) throws CVSException {
		byte[] syncBytes = file.getSyncBytes();
		if (syncBytes == null) {
			// Delete unmanaged files if the user wants them deleted
			if (CVSProviderPlugin.getPlugin().isReplaceUnmanaged()) {
				file.delete();
				deletedFiles.add(file);
			}
		} else if (ResourceSyncInfo.isAddition(syncBytes)) {
			file.delete();
			deletedFiles.add(file);
			file.unmanage(null);
		} else if (ResourceSyncInfo.isDeletion(syncBytes)) {
			// If deleted, null the sync info so the file will be refetched.
			// If we are replacing with the "BASE" tag, the file will not be refetched,
			// it is necessary to restore it from history (see bug 150158).
			if (!shouldDeleteModifications(file)) {
				IFile res = (IFile) file.getIResource();
				try {
					IFileState[] states  = res.getHistory(null);
					if(states.length > 0){
						restoreParentDirectory(file);
						// recreate file using the latest state
						res.create(states[0].getContents(), true, null);
					} else {
						IStatus status = new Status(Status.ERROR, CVSProviderPlugin.ID, 
								CVSMessages.PrepareForReplaceVisitor_DeletedFileWithoutHistoryCannotBeRestoredWhileRevertToBase);
						CVSProviderPlugin.log(status);
						ConsoleListeners.getInstance().errorLineReceived(session, 
								NLS.bind(CVSMessages.PrepareForReplaceVisitor_FileCannotBeReplacedWithBase,
										res.getName()), 
								status);
					}
				} catch (CoreException e) {
					CVSProviderPlugin.log(e);
				}
			} else {
				file.unmanage(null);
			}
		} else if (file.isModified(null) && shouldDeleteModifications(file)) {
			// If the file is modified, delete and unmanage it and allow the 
			// replace operation to fetch it again. This is required because "update -C" 
			// will fail for locally modified resources that have been deleted remotely.
			file.delete();
			deletedFiles.add(file);
			// Only unmanage if the delete was successful (bug 76029)
			file.unmanage(null);
		}
		monitor.worked(1);
	}

	private void restoreParentDirectory(ICVSFile file) throws CVSException {
		List parents = new ArrayList();
		ICVSFolder parent = file.getParent();
		while(!parent.getIResource().exists()){
			parents.add(parent);
			parent = parent.getParent();
		}
		for(int i = parents.size() - 1; i > -1; i--){
			((ICVSFolder)parents.get(i)).mkdir();
		}
	}

	/*
	 * see bug 150158
	 */
	private boolean shouldDeleteModifications(ICVSFile file) {
		return (tag == null && !isStickyRevision(file)) // We don't need to delete sticky files since there can't be conflicting modifications (see bug 199367)
			|| (tag != null && !tag.getName().equals("BASE")); //$NON-NLS-1$
	}

	private boolean isStickyRevision(ICVSFile file) {
		try {
			ResourceSyncInfo info = file.getSyncInfo();
			if (info != null) {
				CVSTag tag = info.getTag();
				if (tag != null) {
					// The problem with tags on files is that they always have the branch type
					// so we need to check if the tag is the file's revision
					return tag.getName().equals(info.getRevision());
				}
			}
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		return false;
	}

	@Override
	public void visitFolder(ICVSFolder folder) throws CVSException {
		// Delete unmanaged folders if the user wants them deleted
		if (!folder.isCVSFolder()) {
			if (CVSProviderPlugin.getPlugin().isReplaceUnmanaged()) {
				// Needed to add files inside to deletedFiles set.
				folder.acceptChildren(this);
				folder.delete();
			}
		} else {
			// Visit the children of the folder as appropriate
			if (depth == IResource.DEPTH_INFINITE) {
				folder.acceptChildren(this);
			} else if (depth == IResource.DEPTH_ONE) {
				ICVSResource[] files = folder.members(ICVSFolder.FILE_MEMBERS);
				for (ICVSResource file : files) {
					file.accept(this);
				}
			}
			// Also delete ignored child files that start with .#
			ICVSResource[] ignoredFiles = folder.members(ICVSFolder.FILE_MEMBERS | ICVSFolder.IGNORED_MEMBERS);
			for (ICVSResource cvsResource : ignoredFiles) {
				if (cvsResource.getName().startsWith(".#")) { //$NON-NLS-1$
					cvsResource.delete();
				}
			}
		}
		monitor.worked(1);
	}
	
	public void visitResources(IProject project, final ICVSResource[] resources, final String oneArgMessage, int depth, IProgressMonitor pm) throws CVSException {
		this.depth = depth;
		deletedFiles = new HashSet();
		CVSWorkspaceRoot.getCVSFolderFor(project).run(pm1 -> {
			monitor = Policy.infiniteSubMonitorFor(pm1, 100);
			monitor.beginTask(null, 512);
			for (ICVSResource resource : resources) {
				if (oneArgMessage != null) {
					monitor.subTask(NLS.bind(oneArgMessage, new String[]{resource.getIResource().getFullPath().toString()})); 
				}
				resource.accept(PrepareForReplaceVisitor.this);
			}
			monitor.done();
		}, pm);
	}
	
	public Set/*<ICVSFile>*/ getDeletedFiles() {
		return Collections.unmodifiableSet(deletedFiles);
	}
}
