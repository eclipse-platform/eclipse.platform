/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.resources.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;
import org.eclipse.team.internal.ccvs.core.util.SyncFileChangeListener;


/**
 * CVSSyncInfo
 */
public class CVSSyncInfo extends SyncInfo {

	/*
	 * Codes that are used in returned IStatus
	 */
	private static final int INVALID_RESOURCE_TYPE = 1;
	private static final int INVALID_SYNC_KIND = 2;
	private static final int PARENT_NOT_MANAGED = 3;
	private static final int REMOTE_DOES_NOT_EXIST = 4;
	private static final int SYNC_INFO_CONFLICTS = 5;
	private Subscriber subscriber;

	public CVSSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote, Subscriber subscriber) {
		super(local, base, remote, ((ResourceVariantTreeSubscriber)subscriber).getResourceComparator());
		this.subscriber = subscriber;
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}
	
	@Override
	protected int calculateKind() throws TeamException {
		// special handling for folders, the generic sync algorithm doesn't work well
		// with CVS because folders are not in namespaces (e.g. they exist in all versions
		// and branches).
		IResource local = getLocal();
		if(local.getType() != IResource.FILE) {
			int folderKind = SyncInfo.IN_SYNC;
			ICVSRemoteFolder remote = (ICVSRemoteFolder)getRemote();
			ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)local);
			boolean isCVSFolder = false;
			try {
				isCVSFolder = cvsFolder.isCVSFolder();
			} catch (CVSException e) {
				// Assume the folder is not a CVS folder
			}
			if(!local.exists()) {
				if(remote != null) {
					if (isCVSFolder) {
						// TODO: This assumes all CVS folders are in-sync even if they have been pruned!
						folderKind = SyncInfo.IN_SYNC;
					} else {
						folderKind = SyncInfo.INCOMING | SyncInfo.ADDITION;
					}
				} else {
					// ignore conflicting deletion to keep phantom sync info
				}
			} else {
				if(remote == null) {
					if(isCVSFolder) {
						// TODO: This is not really an incoming deletion
						// The folder will be pruned once any children are commited
						folderKind = SyncInfo.IN_SYNC;
						//folderKind = SyncInfo.INCOMING | SyncInfo.DELETION;
					} else {
						folderKind = SyncInfo.OUTGOING | SyncInfo.ADDITION;
					}
				} else if(!isCVSFolder) {
					folderKind = SyncInfo.CONFLICTING | SyncInfo.ADDITION;
				} else {
					// folder exists both locally and remotely and are considered in sync, however 
					// we aren't checking the folder mappings to ensure that they are the same.
				}
			}
			return folderKind;
		}
	
		// 1. Run the generic sync calculation algorithm, then handle CVS specific
		// sync cases.
		int kind = super.calculateKind();
	
		// 2. Set the CVS specific sync type based on the workspace sync state provided
		// by the CVS server.
		IResourceVariant remote = getRemote();
		if(remote!=null && (kind & SyncInfo.PSEUDO_CONFLICT) == 0) {
			RemoteResource cvsRemote = (RemoteResource)remote;
			int type = cvsRemote.getWorkspaceSyncState();
			switch(type) {
				// the server compared both text files and decided that it cannot merge
				// them without line conflicts.
				case Update.STATE_CONFLICT: 
					return kind | SyncInfo.MANUAL_CONFLICT;

				// the server compared both text files and decided that it can safely merge
				// them without line conflicts. 
				case Update.STATE_MERGEABLE_CONFLICT: 
					return kind | SyncInfo.AUTOMERGE_CONFLICT;				
			}			
		}
	
		// 3. unmanage delete/delete conflicts and return that they are in sync
		kind = handleDeletionConflicts(kind);
	
		return kind;
	}
	
	/*
	 * If the resource has a delete/delete conflict then ensure that the local is unmanaged so that the 
	 * sync info can be properly flushed.
	 */
	protected int handleDeletionConflicts(int kind) {
		if(kind == (SyncInfo.CONFLICTING | SyncInfo.DELETION | SyncInfo.PSEUDO_CONFLICT)) {
			try {				
				IResource local = getLocal();
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(local);
				if(!cvsResource.isFolder() && cvsResource.isManaged()) {
					// Reconcile the conflicting deletion in the background
					SyncFileChangeListener.getDeferredHandler().handleConflictingDeletion(local);
				}
				return SyncInfo.IN_SYNC;
			} catch(CVSException e) {
				CVSProviderPlugin.log(e);
				return SyncInfo.CONFLICTING | SyncInfo.DELETION;
			}
		}
		return kind;
	}

	/*
	 * Update the sync info of the local resource in such a way that the local changes can be committed.
	 * @return IStatus
	 * For folders, the makeInSYnc method is called and the return codes mentioned there apply
	 * for folders.
	 */
	public IStatus makeOutgoing(IProgressMonitor monitor) throws TeamException {
		
		// For folders, there is no outgoing, only in-sync
		if (getLocal().getType() == IResource.FOLDER) {
			return makeInSync();
		}
		int syncKind = getKind();
		boolean incoming = (syncKind & DIRECTION_MASK) == INCOMING;
		boolean outgoing = (syncKind & DIRECTION_MASK) == OUTGOING;

		ICVSResource local = CVSWorkspaceRoot.getCVSResourceFor(getLocal());
		RemoteResource remote = (RemoteResource)getRemote();
		ResourceSyncInfo origInfo = local.getSyncInfo();
		MutableResourceSyncInfo info = null;
		if(origInfo!=null) {
			info = origInfo.cloneMutable();			
		}
	
		if (outgoing) {
				// The sync info is alright, it's already outgoing!
				return Status.OK_STATUS;
		} else if (incoming) {
			// We have an incoming change, addition, or deletion that we want to ignore
			if (local.exists()) {
				if (remote == null) {
					info.setAdded();
				} else {
					// Otherwise change the revision to the remote revision and dirty the file
					info.setRevision(remote.getSyncInfo().getRevision());
					info.setTimeStamp(null);
				}
			} else {
				// We have an incoming add, turn it around as an outgoing delete
				if (remote == null) {
					// Both the local and remote do not exist so clear the sync info
					info = null;
					return Status.OK_STATUS;
				} else {
					info = remote.getSyncInfo().cloneMutable();
					info.setDeleted(true);
				}
			}
		} else if (local.exists()) {
			// We have a conflict and a local resource!
			if (getRemote() != null) {
				if (getBase() != null) {
					// We have a conflicting change, Update the local revision
					info.setRevision(remote.getSyncInfo().getRevision());
				} else {
					try {
						// We have conflicting additions.
						// We need to fetch the contents of the remote to get all the relevant information (timestamp, permissions)
						// The most important thing we get is the keyword substitution mode which must be right to perform the commit
						remote.getStorage(Policy.monitorFor(monitor)).getContents();
						info = remote.getSyncInfo().cloneMutable();
					} catch (CoreException e) {
						throw TeamException.asTeamException(e);
					}
				}
			} else if (getBase() != null) {
				// We have a remote deletion. Make the local an addition
				info.setAdded();
			} else {
				// There's a local, no base and no remote. We can't possible have a conflict!
				Assert.isTrue(false);
			} 
		} else {
			// We have a conflict and there is no local!
			if (getRemote() != null) {
				// We have a local deletion that conflicts with remote changes.
				info.setRevision(remote.getSyncInfo().getRevision());
				info.setDeleted(true);
			} else {
				// We have conflicting deletions. Clear the sync info
				info = null;
				return Status.OK_STATUS;
			}
		}
		if(info!=null) {
			FolderSyncInfo parentInfo = local.getParent().getFolderSyncInfo();
			if (parentInfo == null) {
				return new CVSStatus(IStatus.ERROR, PARENT_NOT_MANAGED, NLS.bind(CVSMessages.CVSSyncInfo_9, new String[] { getLocal().getFullPath().toString()}), getLocal()); 
			}
			info.setTag(parentInfo.getTag());
		}
		((ICVSFile)local).setSyncInfo(info, ICVSFile.UNKNOWN);
		return Status.OK_STATUS;
	}
	
	/*
	 * Load the resource and folder sync info into the local from the remote
	 * 
	 * This method can be used on incoming folder additions to set the folder sync info properly
	 * without hitting the server again. It also applies to conflicts that involves unmanaged
	 * local resources.
	 * 
	 * @return an IStatus with the following severity and codes
	 * <ul>
	 * <li>IStatus.WARNING
	 * 	<ul>
	 *   <li>INVALID_RESOURCE_TYPE - makeInSync only works on folders
	 *   <li>INVALID_SYNC_KIND - sync direction must be incoming or conflicting
	 *  </ul>
	 * <li>IStatus.ERROR
	 *  <ul>
	 *   <li>PARENT_NOT_MANAGED - the local parent of the resource is not under CVS control
	 *   <li>SYNC_INFO_CONFLICTS - Sync info already exists locally and differs from the info
	 *     in the remote handle.
	 *   <li>REMOTE_DOES_NOT_EXIST - There is no local sync info and there is no remote handle
	 *  </ul>
	 * </ul>
	 */
	public IStatus makeInSync() throws CVSException {
		
		// Only works on folders
		if (getLocal().getType() == IResource.FILE) {
			return new CVSStatus(IStatus.WARNING, INVALID_RESOURCE_TYPE, NLS.bind(CVSMessages.CVSSyncInfo_7, new String[] { getLocal().getFullPath().toString()}), getLocal()); 
		} 
		
		// Only works on outgoing and conflicting changes
		boolean outgoing = (getKind() & DIRECTION_MASK) == OUTGOING;
		if (outgoing) {
			return new CVSStatus(IStatus.WARNING, INVALID_SYNC_KIND, NLS.bind(CVSMessages.CVSSyncInfo_8, new String[] { getLocal().getFullPath().toString() }), getLocal()); 
		}
		
		// The parent must be managed
		ICVSFolder local = CVSWorkspaceRoot.getCVSFolderFor((IContainer)getLocal());
		if (getLocal().getType() == IResource.FOLDER && ! local.getParent().isCVSFolder())
			return new CVSStatus(IStatus.ERROR, PARENT_NOT_MANAGED, NLS.bind(CVSMessages.CVSSyncInfo_9, new String[] { getLocal().getFullPath().toString() }), getLocal()); 
		
		// Ensure that the folder exists locally
		if (! local.exists()) {
			local.mkdir();
		}
		
		// If the folder already has CVS info, check that the remote and local match
		RemoteFolder remote = (RemoteFolder)getRemote();
		if((local.isManaged() || getLocal().getType() == IResource.PROJECT) && local.isCVSFolder()) {
			// If there's no remote, assume everything is OK
			if (remote == null) return Status.OK_STATUS;
			// Verify that the root and repository are the same
			FolderSyncInfo remoteInfo = remote.getFolderSyncInfo();
			FolderSyncInfo localInfo = local.getFolderSyncInfo();
			if ( ! localInfo.getRoot().equals(remoteInfo.getRoot())) {
				return new CVSStatus(IStatus.ERROR, SYNC_INFO_CONFLICTS, NLS.bind(CVSMessages.CVSRemoteSyncElement_rootDiffers, (new Object[] {local.getName(), remoteInfo.getRoot(), localInfo.getRoot()})),getLocal());
			} else if ( ! localInfo.getRepository().equals(remoteInfo.getRepository())) {
				return new CVSStatus(IStatus.ERROR, SYNC_INFO_CONFLICTS, NLS.bind(CVSMessages.CVSRemoteSyncElement_repositoryDiffers, (new Object[] {local.getName(), remoteInfo.getRepository(), localInfo.getRepository()})),getLocal());
			}
			// The folders are in sync so just return
			return Status.OK_STATUS;
		}
		
		// The remote must exist if the local is not managed
		if (remote == null) {
			return new CVSStatus(IStatus.ERROR, REMOTE_DOES_NOT_EXIST, NLS.bind(CVSMessages.CVSSyncInfo_10, new String[] { getLocal().getFullPath().toString() }),getLocal()); 
		}
		
		// Since the parent is managed, this will also set the resource sync info. It is
		// impossible for an incoming folder addition to map to another location in the
		// repo, so we assume that using the parent's folder sync as a basis is safe.
		// It is also impossible for an incomming folder to be static.
		FolderSyncInfo remoteInfo = remote.getFolderSyncInfo();
		FolderSyncInfo localInfo = local.getParent().getFolderSyncInfo();
		MutableFolderSyncInfo newInfo = remoteInfo.cloneMutable();
		newInfo.setTag(localInfo.getTag());
		newInfo.setStatic(false);
		local.setFolderSyncInfo(newInfo);
		return Status.OK_STATUS;
	}
	
	public String toString() {
		IResourceVariant base = getBase();
		IResourceVariant remote = getRemote();
		StringBuilder result = new StringBuilder(super.toString());
		result.append("Local: "); //$NON-NLS-1$
		result.append(getLocal().toString());
		result.append(" Base: "); //$NON-NLS-1$
		if (base == null) {
			result.append("none"); //$NON-NLS-1$
		} else {
			result.append(base.toString());
		}
		result.append(" Remote: "); //$NON-NLS-1$
		if (remote == null) {
			result.append("none"); //$NON-NLS-1$
		} else {
			result.append(remote.toString());
		}
		return result.toString();
	}

	public String getLocalContentIdentifier() {
		ResourceSyncInfo info= getSyncInfoForLocal(getCVSFile());
		return info != null ? info.getRevision() : null;
	}

	@Override
	public String getLocalAuthor(IProgressMonitor monitor) {
		final ICVSFile cvsFile= getCVSFile();
		if (cvsFile == null)
			return null;

		final ResourceSyncInfo info= getSyncInfoForLocal(cvsFile);
		if (info == null)
			return null;

		final String localRevision= info.getRevision();
		if (localRevision == null)
			return null;

		final ILogEntry entries[]= getLogEntries(cvsFile, monitor);
		if (entries == null || entries.length == 0)
			return null;
		
		for (ILogEntry entry : entries) {
			try {
				if (localRevision.equals(entry.getRemoteFile().getRevision())) {
					return entry.getAuthor();
				}
			} catch (TeamException e) {
				CVSProviderPlugin.log(e);
			}
		}
		return null;
	}

	private static ResourceSyncInfo getSyncInfoForLocal(ICVSFile cvsFile) {
		if (cvsFile == null)
			return null;

		try {
			return cvsFile.getSyncInfo();
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
			return null;
		}
	}

	private static ILogEntry[] getLogEntries(ICVSFile cvsFile, IProgressMonitor monitor) {
		try {
			return cvsFile.getLogEntries(monitor);
		} catch (TeamException e) {
			CVSProviderPlugin.log(e);
			return null;
		}
	}

	private ICVSFile getCVSFile() {
		IResource local = getLocal();
		if (local != null && local.getType() == IResource.FILE) {
			return CVSWorkspaceRoot.getCVSFileFor((IFile)local);
		}
		return null;
	}

}
