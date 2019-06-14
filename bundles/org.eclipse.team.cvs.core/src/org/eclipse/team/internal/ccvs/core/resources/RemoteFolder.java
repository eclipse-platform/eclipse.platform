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
package org.eclipse.team.internal.ccvs.core.resources;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.IUpdateMessageListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.UpdateListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * This class provides the implementation of ICVSRemoteFolder
 * 
 * The parent of the RemoteFolder represents the folders parent in a local configuration.
 * For instance, the parent may correspond to the remote parent or may be a folder in the
 * same repository that has no physical relationship to the RemoteFolder (resulting from the use
 * of a module definition, for instance). A RemoteFolder may not have a parent, indicating that it is
 * the root of the local configuration it represents. 
 * 
 * A RemoteFolder has the following:
 *   A name in the folder's local configuration
 *   
 */
public class RemoteFolder extends RemoteResource implements ICVSRemoteFolder, ICVSFolder {
	
	protected static final int CHILD_DOES_NOT_EXIST = 1000;
	
	protected FolderSyncInfo folderInfo;
	private ICVSRemoteResource[] children;
	private ICVSRepositoryLocation repository;
	
	public static RemoteFolder fromBytes(IResource local, byte[] bytes) throws CVSException {
		Assert.isNotNull(bytes);
		Assert.isTrue(local.getType() != IResource.FILE);
		FolderSyncInfo syncInfo = FolderSyncInfo.getFolderSyncInfo(bytes);
		return new RemoteFolder(null, local.getName(), KnownRepositories.getInstance().getRepository(syncInfo.getRoot()), syncInfo.getRepository(), syncInfo.getTag(), syncInfo.getIsStatic());
	}
	
	/**
	 * Constructor for RemoteFolder.
	 */
	public RemoteFolder(RemoteFolder parent, ICVSRepositoryLocation repository, String repositoryRelativePath, CVSTag tag) {
		this(parent, 
			repositoryRelativePath == null ? "" : Util.getLastSegment(repositoryRelativePath), //$NON-NLS-1$
			repository,
			repositoryRelativePath,
			tag, 
			false);	
	}
	
	public RemoteFolder(RemoteFolder parent, String name, ICVSRepositoryLocation repository, String repositoryRelativePath, CVSTag tag, boolean isStatic) {
		super(parent, name);
		if (repository != null) {
			this.folderInfo = new FolderSyncInfo(repositoryRelativePath.toString(), repository.getLocation(false), tag, isStatic);
		}
		this.repository = repository;	
	}
	
	@Override
	public void accept(ICVSResourceVisitor visitor) throws CVSException {
		visitor.visitFolder(this);
	}

	@Override
	public void accept(ICVSResourceVisitor visitor, boolean recurse) throws CVSException {
		visitor.visitFolder(this);
		ICVSResource[] resources;
		if (recurse) {
			resources = members(ICVSFolder.ALL_MEMBERS);
		} else {
			resources = members(ICVSFolder.FILE_MEMBERS);
		}
		for (ICVSResource resource : resources) {
			resource.accept(visitor, recurse);
		}
	}
	
	@Override
	public boolean exists(IProgressMonitor monitor) throws TeamException {
		try {
			members(monitor);
			return true;
		} catch (CVSException e) {
			if (e.getStatus().getCode() == CVSStatus.DOES_NOT_EXIST) {
				return false;
			} else {
				throw e;
			}
		}
	}

	/*
	 * Check whether the given resource is a child of the receiver remotely
	 */
	protected boolean exists(ICVSRemoteResource child, IProgressMonitor monitor) throws CVSException {
		return exists(child, getTag(), monitor);
	}
	
	/*
	 * Check whether the child exists for the given tag. This additional method is required because
	 * CVS will signal an error if a folder only contains subfolders when a tag is used. If we get this
	 * error and we're looking for a folder, we need to reissue the command without a tag.
	 */
	protected boolean exists(final ICVSRemoteResource child, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		final IProgressMonitor progress = Policy.monitorFor(monitor);
		progress.beginTask(CVSMessages.RemoteFolder_exists, 100); 
		try {
			// Create the listener for remote files and folders
			final boolean[] exists = new boolean[] {true};
			final IUpdateMessageListener listener = new IUpdateMessageListener() {
				public void directoryInformation(ICVSFolder parent, String path, boolean newDirectory) {
					exists[0] = true;
				}
				public void directoryDoesNotExist(ICVSFolder parent, String path) {
					exists[0] = false;
				}
				public void fileInformation(int type, ICVSFolder parent, String filename) {
					// We can't set exists true here as we may get a conflict on a deleted file.
					// i.e. remote files are always communicated to the server as modified.
					if (type == Update.STATE_ADDED_LOCAL)
						exists[0] = false;
				}
				public void fileDoesNotExist(ICVSFolder parent, String filename) {
					exists[0] = false;
				}
			};
			
			// Build the local options
			final List<LocalOption> localOptions = new ArrayList<>();
			localOptions.add(Update.RETRIEVE_ABSENT_DIRECTORIES);
			if (tag != null && tag.getType() != CVSTag.HEAD)
				localOptions.add(Update.makeTagOption(tag));
			
			// Retrieve the children and any file revision numbers in a single connection
			// Perform a "cvs -n update -d -r tagName folderName" with custom message and error handlers
			boolean retry = false;
			Session session = new Session(getRepository(), this, false /* output to console */);
			session.open(Policy.subMonitorFor(progress, 10), false /* read-only */);
			try {
				IStatus status = Command.UPDATE.execute(
					session,
					new GlobalOption[] { Command.DO_NOT_CHANGE },
					localOptions.toArray(new LocalOption[localOptions.size()]),
					new ICVSResource[] { child }, new UpdateListener(listener),
					Policy.subMonitorFor(progress, 70));
				if (status.getCode() == CVSStatus.SERVER_ERROR) {
					CVSServerException e = new CVSServerException(status);
					if (e.isNoTagException() && child.isContainer()) {
						retry = true;
					} else {
						if (e.containsErrors()) {
							throw e;
						}
					}
				}
			} finally {
				session.close();
			}

			// We now know that this is an exception caused by a cvs bug.
			// If the folder has no files in it (just subfolders) CVS does not respond with the subfolders...
			// Workaround: Retry the request with no tag to get the directory names (if any)
			if (retry) {
				Policy.checkCanceled(progress);
				return exists(child, null, Policy.subMonitorFor(progress, 20));
			}
			return exists[0];
		} finally {
			progress.done();
		}
	}

	/**
	 * @see ICVSRemoteFolder#getMembers()
	 */
	public ICVSRemoteResource[] getMembers(IProgressMonitor monitor) throws TeamException {
		return getMembers(getTag(), monitor);
	}

	/**
	 * This method gets the members for a given tag and returns them.
	 * During the execution of this method, the instance variable children
	 * will be used to contain the children. However, the variable is reset
	 * and the result returned. Thus, instances of RemoteFolder do not
	 * persist the children. Subclasses (namely RemoteFolderTree) may
	 * persist the children.
	 */
	protected ICVSRemoteResource[] getMembers(CVSTag tag, IProgressMonitor monitor) throws CVSException {
		// Fetch the children
		RemoteFolderMemberFetcher fetcher = new RemoteFolderMemberFetcher(this, tag);
		fetcher.fetchMembers(Policy.monitorFor(monitor));
		// children is assigned in the InternalRemoteFolderMembersFetcher
		return children;
	}

	@Override
	public ICVSResource[] members(int flags) throws CVSException {		
		final List<ICVSResource> result = new ArrayList<>();
		ICVSRemoteResource[] resources = getChildren();
		if (children == null) {
			return new ICVSResource[0];
		}
		// RemoteFolders never have phantom members
		if ((flags & EXISTING_MEMBERS) == 0 && (flags & PHANTOM_MEMBERS) == PHANTOM_MEMBERS) {
			return new ICVSResource[0];
		}
		boolean includeFiles = (((flags & FILE_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeFolders = (((flags & FOLDER_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeManaged = (((flags & MANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		boolean includeUnmanaged = (((flags & UNMANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		boolean includeIgnored = ((flags & IGNORED_MEMBERS) != 0);
		for (ICVSRemoteResource cvsResource : resources) {
			if ((includeFiles && ( ! cvsResource.isFolder())) 
					|| (includeFolders && (cvsResource.isFolder()))) {
				boolean isManaged = cvsResource.isManaged();
				boolean isIgnored = cvsResource.isIgnored();
				if ((isManaged && includeManaged)|| (isIgnored && includeIgnored)
						|| ( ! isManaged && ! isIgnored && includeUnmanaged)) {
					result.add(cvsResource);
				}
						
			}		
		}
		return result.toArray(new ICVSResource[result.size()]);
	}
	
	@Override
	public ICVSFolder getFolder(String name) throws CVSException {
		if (name.equals(Session.CURRENT_LOCAL_FOLDER) || name.equals(Session.CURRENT_LOCAL_FOLDER + Session.SERVER_SEPARATOR))
			return this;
		ICVSResource child = getChild(name);
		if (child.isFolder())
			return (ICVSFolder)child;
		IStatus status = new CVSStatus(IStatus.ERROR, CHILD_DOES_NOT_EXIST, NLS.bind(CVSMessages.RemoteFolder_invalidChild, new String[] { name, getName() }),child.getIResource());
		throw new CVSException(status); 
	}

	@Override
	public ICVSFile getFile(String name) throws CVSException {
		ICVSResource child = getChild(name);
		if (!child.isFolder())
			return (ICVSFile)child;
		IStatus status = new CVSStatus(IStatus.ERROR, CHILD_DOES_NOT_EXIST, NLS.bind(CVSMessages.RemoteFolder_invalidChild, new String[] { name, getName() }),child.getIResource());
		throw new CVSException(status); 
	}

	public LocalOption[] getLocalOptions() {
		return Command.NO_LOCAL_OPTIONS;
	}
	
	public String getRepositoryRelativePath() {
		// The REPOSITORY property of the folder info is the repository relative path
		return getFolderSyncInfo().getRepository();
	}
	
	@Override
	public String getRelativePath(ICVSFolder ancestor) throws CVSException {
		// Check to see if the receiver is the ancestor
		if (ancestor == this) return Session.CURRENT_LOCAL_FOLDER;
		// Otherwise, we need a parent to continue
		if (parent == null) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.RemoteFolder_invalidChild, new String[] { getName(), ancestor.getName() }),this);
			throw new CVSException(status); 
		}
		return super.getRelativePath(ancestor);
	}
	
	public ICVSRepositoryLocation getRepository() {
		return repository;
	}
	
	@Override
	public boolean isExpandable() {
		return true;
	}
	
	@Override
	public boolean isFolder() {
		return true;
	}
	
	/**
	 * @see ICVSFolder#childExists(String)
	 */
	public boolean childExists(String path) {
		try {
			return getChild(path) != null;
		} catch (CVSException e) {
			return false;
		}
	}

	/**
	 * @see ICVSFolder#getChild(String)
	 * 
	 * This getChild is geared to work with the Command hierarchy. Therefore it only returns 
	 * children that were previously fetched by a call to getMembers(). If the request child
	 * does not exist, an exception is thrown.
	 */
	public ICVSResource getChild(String path) throws CVSException {
		if (path.equals(Session.CURRENT_LOCAL_FOLDER) || path.length() == 0)
			return this;
		if (path.contains(Session.SERVER_SEPARATOR)) {
			IPath p = new Path(null, path);
			try {
				return ((RemoteFolder)getChild(p.segment(0))).getChild(p.removeFirstSegments(1).toString());
			} catch (CVSException e) {
				// regenerate the exception to give as much info as possible
				IStatus status = new CVSStatus(IStatus.ERROR, CHILD_DOES_NOT_EXIST, NLS.bind(CVSMessages.RemoteFolder_invalidChild, new String[] { path, getName() }),e,repository);
				throw new CVSException(status);
			}
		} else {
			ICVSRemoteResource[] children = getChildren();
			if (children == null){ 
				IStatus status = new CVSStatus(IStatus.ERROR, CHILD_DOES_NOT_EXIST, NLS.bind(CVSMessages.RemoteFolder_invalidChild, new String[] { path, getName() }),repository);
				throw new CVSException(status);
			}
			for (ICVSRemoteResource c : children) {
				if (c.getName().equals(path)) {
					return c;
				}
			}
		}
		IStatus status = new CVSStatus(IStatus.ERROR, CHILD_DOES_NOT_EXIST, NLS.bind(CVSMessages.RemoteFolder_invalidChild, new String[] { path, getName() }),repository);
		throw new CVSException(status);
	}

	@Override
	public void mkdir() throws CVSException {
		IStatus status = new CVSStatus(IStatus.ERROR, CVSMessages.RemoteResource_invalidOperation);
		throw new CVSException(status);
	}

	/**
	 * @see ICVSFolder#flush(boolean)
	 */
	public void flush(boolean deep) {
	}

	@Override
	public FolderSyncInfo getFolderSyncInfo() {
		return folderInfo;
	}

	@Override
	public String getRemoteLocation(ICVSFolder stopSearching) throws CVSException {
		if (folderInfo == null) {
			return Util.appendPath(parent.getRemoteLocation(stopSearching), getName());
		}
		return folderInfo.getRemoteLocation();
	}
	
	@Override
	public boolean isCVSFolder() {
		return folderInfo != null;
	}

	@Override
	public void acceptChildren(ICVSResourceVisitor visitor) throws CVSException {
		IStatus status = new CVSStatus(IStatus.ERROR, CVSMessages.RemoteResource_invalidOperation);
		throw new CVSException(status);		
	}
	
	@Override
	public boolean isContainer() {
		return true;
	}
	
	@Override
	public ICVSRemoteResource[] members(IProgressMonitor progress) throws TeamException {
		return getMembers(progress);
	}

	/*
	 * Answers the immediate cached children of this remote folder or null if the remote folder
	 * handle has not yet queried the server for the its children.
	 */	
	public ICVSRemoteResource[] getChildren() {
		return children;
	}
	/*
	 * This allows subclass to set the children
	 */
	protected void setChildren(ICVSRemoteResource[] children) {
		this.children = children;
	}
	/*
	 * @see ICVSRemoteFolder#setTag(String)
	 */
	public void setTag(CVSTag tag) {
		MutableFolderSyncInfo newInfo = folderInfo.cloneMutable();
		newInfo.setTag(tag);
		setFolderSyncInfo(newInfo);
	}

	@Override
	public CVSTag getTag() {
		if (folderInfo == null) return null;
		return folderInfo.getTag();
	}
	
	@Override
	public void setFolderSyncInfo(FolderSyncInfo folderInfo) {
		this.folderInfo = folderInfo.asImmutable();
	}
	
	@Override
	public void run(ICVSRunnable job, IProgressMonitor monitor) throws CVSException {
		job.run(monitor);
	}
	
	@Override
	public IStatus tag(final CVSTag tag, final LocalOption[] localOptions, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		Session session = new Session(getRepository(), this, true /* output to console */);
		session.open(Policy.subMonitorFor(monitor, 10), true /* open for modification */);
		try {
			return Command.RTAG.execute(
				session,
				Command.NO_GLOBAL_OPTIONS,
				localOptions,
				folderInfo.getTag(),
				tag,
				new ICVSRemoteResource[] { RemoteFolder.this },
			Policy.subMonitorFor(monitor, 90));
		} finally {
			session.close();
		}
	}
	
	@Override
	public ICVSResource[] fetchChildren(IProgressMonitor monitor) throws CVSException {
		try {
			return getMembers(monitor);
		} catch(TeamException e) {
			throw new CVSException(e.getStatus());
		}
	}
	
	public boolean equals(Object target) {
		if ( ! super.equals(target)) return false;
		RemoteFolder folder = (RemoteFolder)target;
		// A simple folder is never equal to a defined module
		if (folder.isDefinedModule() != isDefinedModule()) return false;
		CVSTag tag1 = getTag();
		CVSTag tag2 = folder.getTag();
		if (tag1 == null) tag1 = CVSTag.DEFAULT;
		if (tag2 == null) tag2 = CVSTag.DEFAULT;
		return tag1.equals(tag2);
	}
	
	@Override
	public int hashCode() {
		CVSTag tag = getTag();
		if (tag == null) tag = CVSTag.DEFAULT;
		return super.hashCode() | tag.getName().hashCode();
	}
	
	/*
	 * The given root must be an ancestor of the receiver (or the receiver)
	 * and the path of the receiver must be a prefix of the provided path.
	 */
	protected IPath getRelativePathFromRootRelativePath(ICVSFolder root, IPath path) throws CVSException {
		// If the root is the receiver, then the path is already relative to the receiver
		if (root == this) {
			return path;
		}
		Assert.isTrue( ! path.isEmpty());
		return getRelativePathFromRootRelativePath((ICVSFolder)root.getChild(path.segment(0)), path.removeFirstSegments(1));
	}

	@Override
	public ICVSRemoteResource forTag(ICVSRemoteFolder parent, CVSTag tagName) {
		return new RemoteFolder((RemoteFolder)parent, getName(), repository, folderInfo.getRepository(), tagName, folderInfo.getIsStatic());
	}
	
	@Override
	public ICVSRemoteResource forTag(CVSTag tagName) {
		return forTag(null, tagName);
	}

	@Override
	public boolean isDefinedModule() {
		return false;
	}
	
	@Override
	public ResourceSyncInfo getSyncInfo() {
		return new ResourceSyncInfo(getName());
	}

	@Override
	public byte[] getSyncBytes() {
		try {
			return folderInfo.getBytes();
		} catch (CVSException e) {
			// This shouldn't even happen
			return null;
		}
	}

	@Override
	public String getContentIdentifier() {
		return getTag().getName();
	}
	
	@Override
	public boolean isManaged() {
		return super.isManaged() && isCVSFolder();
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		// This should not get called for folders
	}

}
