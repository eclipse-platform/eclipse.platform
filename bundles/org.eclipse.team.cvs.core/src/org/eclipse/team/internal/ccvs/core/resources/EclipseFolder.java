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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Implements the ICVSFolder interface on top of an 
 * instance of the ICVSFolder interface
 * 
 * @see ICVSFolder
 */
class EclipseFolder extends EclipseResource implements ICVSFolder {

	protected EclipseFolder(IContainer container) {
		super(container);		
	}
	
	@Override
	public ICVSResource[] members(int flags) throws CVSException {		
		final List<ICVSResource> result = new ArrayList<>();
		IResource[] resources = EclipseSynchronizer.getInstance().members((IContainer)resource);
		boolean includeFiles = (((flags & FILE_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeFolders = (((flags & FOLDER_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeManaged = (((flags & MANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		boolean includeUnmanaged = (((flags & UNMANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		boolean includeIgnored = ((flags & IGNORED_MEMBERS) != 0);
		boolean includeExisting = (((flags & EXISTING_MEMBERS) != 0) || ((flags & (EXISTING_MEMBERS | PHANTOM_MEMBERS)) == 0));
		boolean includePhantoms = (((flags & PHANTOM_MEMBERS) != 0) || ((flags & (EXISTING_MEMBERS | PHANTOM_MEMBERS)) == 0));
		for (IResource resource : resources) {
			int type = resource.getType();
			if ((includeFiles && (type==IResource.FILE)) 
					|| (includeFolders && (type==IResource.FOLDER))) {
				boolean exists = resource.exists();
				if ((includeExisting && exists) || (includePhantoms && !exists)) {
					ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
					boolean includeResource = false;
					if ((includeManaged && includeUnmanaged && includeIgnored)) {
						includeResource = true;
					} else {
						boolean isManaged = cvsResource.isManaged();
						if (isManaged && includeManaged) {
							includeResource = true;
						} else if (exists) {
							boolean isIgnored = cvsResource.isIgnored();
							if (isIgnored && includeIgnored) {
								includeResource = true;
							} else if (! isManaged && ! isIgnored && includeUnmanaged) {
								includeResource = true;
							}
						}
					}
					if (includeResource) {
						result.add(cvsResource);
					}
				}
			}		
		}	
		return result.toArray(new ICVSResource[result.size()]);
	}

	@Override
	public ICVSFolder getFolder(String name) throws CVSException {
		if ((CURRENT_LOCAL_FOLDER.equals(name)) || ((CURRENT_LOCAL_FOLDER + SEPARATOR).equals(name)))
			return this;
		IPath path = new Path(null, name);
		if(resource.getType()==IResource.ROOT && path.segmentCount()==1) {
			return new EclipseFolder(((IWorkspaceRoot)resource).getProject(name));
		} else {
			return new EclipseFolder(((IContainer)resource).getFolder(path));
		}
	}

	@Override
	public ICVSFile getFile(String name) throws CVSException {
		return new EclipseFile(((IContainer)resource).getFile(new Path(null, name)));
	}

	@Override
	public void mkdir() throws CVSException {
		ISchedulingRule rule = null;
		try {
			rule = EclipseSynchronizer.getInstance().beginBatching(resource, null);
			if(resource.getType()==IResource.PROJECT) {
				IProject project = (IProject)resource;
				project.create(null);
				project.open(null);				
			} else {
				((IFolder)resource).create(false /*don't force*/, true /*make local*/, null);
				// We need to signal the creation to the synchronizer immediately because
				// we may do additional CVS operations on the folder before the next delta
				// occurs.
				EclipseSynchronizer.getInstance().created(getIResource());;
			}				
		} catch (CoreException e) {
			throw CVSException.wrapException(resource, NLS.bind(CVSMessages.EclipseFolder_problem_creating, new String[] { resource.getFullPath().toString(), e.getStatus().getMessage() }), e); 
		} finally {
			if (rule != null)
				EclipseSynchronizer.getInstance().endBatching(rule, null);
		}
	}
		
	@Override
	public boolean isFolder() {
		return true;
	}
		
	@Override
	public void acceptChildren(ICVSResourceVisitor visitor) throws CVSException {
		
		// Visit files and then folders
		ICVSResource[] subFiles = members(FILE_MEMBERS);
		for (ICVSResource subFile : subFiles) {
			subFile.accept(visitor);
		}
		ICVSResource[] subFolders = members(FOLDER_MEMBERS);
		for (ICVSResource subFolder : subFolders) {
			subFolder.accept(visitor);
		}
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
		for (ICVSResource r : resources) {
			r.accept(visitor, recurse);
		}
	}

	@Override
	public String getRemoteLocation(ICVSFolder stopSearching) throws CVSException {
				
		if (getFolderSyncInfo() != null) {
			return getFolderSyncInfo().getRemoteLocation();
		}			

		ICVSFolder parent = getParent();
		if(parent!=null && !equals(stopSearching)) {
			String parentLocation;
			parentLocation = parent.getRemoteLocation(stopSearching);
			if (parentLocation!=null) {
				return parentLocation + SEPARATOR + getName();
			}		
		}
		return null;
	}

	@Override
	public FolderSyncInfo getFolderSyncInfo() throws CVSException {
		if (resource.getType() != IResource.ROOT && !resource.getProject().isAccessible()) {
			return null;
		}
		return EclipseSynchronizer.getInstance().getFolderSync((IContainer)resource);
	}

	@Override
	public void setFolderSyncInfo(final FolderSyncInfo folderInfo) throws CVSException {
		// ignore folder sync on the root (i.e. CVSROOT/config/TopLevelAdmin=yes but we just ignore it)
		if (resource.getType() == IResource.ROOT) return;
		run((ICVSRunnable) monitor -> {
			EclipseSynchronizer synchronizer = EclipseSynchronizer.getInstance();
			synchronizer.setFolderSync((IContainer)resource, folderInfo);
			// the server won't add directories as sync info, therefore it must be done when
			// a directory is shared with the repository.
			byte[] newSyncBytes = new ResourceSyncInfo(getName()).getBytes();
			byte[] oldSyncBytes = getSyncBytes();
			// only set the bytes if the new differes from the old.
			// this avoids unnecessary saving of sync files
			if (oldSyncBytes == null || ! Util.equals(newSyncBytes, oldSyncBytes))
				setSyncBytes(newSyncBytes);
		}, null);

	}

	@Override
	public boolean isCVSFolder() throws CVSException {
		return EclipseSynchronizer.getInstance().getFolderSync((IContainer)resource) != null;
	}

	@Override
	public void unmanage(IProgressMonitor monitor) throws CVSException {
		run((ICVSRunnable) monitor1 -> {
			monitor1 = Policy.monitorFor(monitor1);
			monitor1.beginTask(null, 100);
			recursiveUnmanage((IContainer) resource, Policy.subMonitorFor(monitor1, 99));
			EclipseFolder.super.unmanage(Policy.subMonitorFor(monitor1, 1));
			monitor1.done();	
		}, Policy.subMonitorFor(monitor, 99));
	}
	
	/* private */ static void recursiveUnmanage(IContainer container, IProgressMonitor monitor) {
		try {
			monitor.beginTask(null, 10);
			monitor.subTask(NLS.bind(CVSMessages.EclipseFolder_0, new String[] {container.getFullPath().toString() }));
			EclipseSynchronizer.getInstance().deleteFolderSync(container);
	
			IResource[] members = container.members(true);
			for (IResource resource : members) {
				monitor.worked(1);
				if (resource.getType() == IResource.FILE) {
					ResourceAttributes attrs = resource.getResourceAttributes();
					if (attrs != null && attrs.isReadOnly()) {
						attrs.setReadOnly(false);
						resource.setResourceAttributes(attrs);
					}
				} else {
					recursiveUnmanage((IContainer) resource, monitor);
				}
			}
		} catch (CoreException e) {
			// Just ignore and continue
		} finally {
			monitor.done();
		}
	}
	
	@Override
	public boolean isIgnored() throws CVSException {
		if(isCVSFolder()) {
			return false;
		}		
		return super.isIgnored();
	}
	
	@Override
	public ICVSResource getChild(String namedPath) throws CVSException {
		if (namedPath.equals(Session.CURRENT_LOCAL_FOLDER)) {
			return this;
		}
		IPath path = new Path(null, namedPath);
		if(path.segmentCount()==0) {
			return this;
		}
		IResource child = ((IContainer)resource).findMember(path, true /* include phantoms */);
		if(child!=null) {
			if(child.getType()==IResource.FILE) {
				return new EclipseFile((IFile)child);
			} else {
				return new EclipseFolder((IContainer)child);
			}
		}
		return null;
	}
	
	@Override
	public ICVSResource[] fetchChildren(IProgressMonitor monitor) throws CVSException {
		return members(FILE_MEMBERS | FOLDER_MEMBERS);
	}
	@Override
	public void delete() throws CVSException {
		if (!exists()) return;
		try {
			resource.delete(false /*force*/, null);
		} catch(CoreException e) {
			throw new CVSException(e.getStatus());
		}
	}
	
	/**
	 * Assumption this is only called from decorator and isIgnored() is purposely
	 * ommitted here for performance reasons. 
	 */
	public boolean isModified(IProgressMonitor monitor) throws CVSException {
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(NLS.bind(CVSMessages.EclipseFolder_isModifiedProgress, new String[] { resource.getFullPath().toString() }), 1000); 
			
			IContainer container = (IContainer)getIResource();
			
			if(RepositoryProvider.getProvider(container.getProject(), CVSProviderPlugin.getTypeId()) == null) {
				return false;
			}
			
			// Added optimization to avoid loading sync info if possible
			// This will place a modified indicator on non-cvs folders
			// (i.e. the call to getModifiedState will cache a session property)
			int state = EclipseSynchronizer.getInstance().getModificationState(getIResource());
			
			boolean modified;
			if (state == ICVSFile.UNKNOWN) {
				
				if (!isCVSFolder() && container.getType() == IResource.FOLDER) {
					return container.exists();
				}
				
				// We have no cached info for the folder. We'll need to check directly,
				// caching as go. This will recursively determined the modified state
				// for all child resources until a modified child is found.
				modified = calculateAndSaveChildModificationStates(monitor);
				EclipseSynchronizer.getInstance().setModified(this, modified);
			} else {
				modified = (state == ICVSFile.DIRTY);
			}
			return modified;
		} finally {
			monitor.done();
		}
	}
	
	public void handleModification(boolean forAddition) throws CVSException {
		// For non-additions, we are only interested in sync info changes
		if (isIgnored() || !forAddition) return;

		// the folder is an addition.
		FolderSyncInfo info = getFolderSyncInfo();
		// if the folder has sync info, it was handled is setFolderInfo
		// otherwise, flush the ancestors to recalculate
		if (info == null) {
			EclipseSynchronizer.getInstance().setDirtyIndicator(getIResource(), true);
		}
	}
	
	/**
	 * Determines the modification state of the receiver by examining it's children.
	 * This method may result in modification state being cached with the children but
	 * does not cache it for the receiver.
	 */
	private boolean calculateAndSaveChildModificationStates(IProgressMonitor monitor) throws CVSException {
		ICVSResource[] children = members(ALL_UNIGNORED_MEMBERS);

		for (ICVSResource resource : children) {
			if (resource.isModified(null)) {
				// if a child resource is dirty consider the parent dirty as well, there
				// is no need to continue checking other siblings.
				return true;
			}
			monitor.worked(1);
		}
			
		return false;
	}

	@Override
	public String getRepositoryRelativePath() throws CVSException {
		FolderSyncInfo info = getFolderSyncInfo();
		if (info == null) return null;
		// The REPOSITORY property of the folder info is the repository relative path
		return info.getRepository();
	}
}
