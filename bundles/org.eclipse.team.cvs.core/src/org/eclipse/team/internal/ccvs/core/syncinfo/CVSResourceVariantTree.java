/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.syncinfo;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.*;

/**
 * CVS Specific refresh operation
 */
public class CVSResourceVariantTree extends ResourceVariantTree {

	private CVSTag tag;
	private boolean cacheFileContentsHint;
	private CVSSyncTreeSubscriber subscriber;

	public CVSResourceVariantTree(ResourceVariantByteStore cache, CVSTag tag, boolean cacheFileContentsHint) {
		super(cache);
		this.tag = tag;
		this.cacheFileContentsHint = cacheFileContentsHint;
	}

	@Override
	public ResourceVariantByteStore getByteStore() {
		return super.getByteStore();
	}

	@Override
	protected byte[] getBytes(IResource local, IResourceVariant remote) throws TeamException {
		if (remote != null) {
			return super.getBytes(local, remote);
		} else {
			if (local.getType() == IResource.FOLDER) {
				// If there is no remote, use the local sync for the folder
				return getBaseBytes((IContainer)local, getTag(local));
			}
			return null;
		}
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant remote, IProgressMonitor progress) throws TeamException {
		ICVSRemoteResource[] children = remote != null ? (ICVSRemoteResource[])((RemoteResource)remote).members(progress) : new ICVSRemoteResource[0];
		IResourceVariant[] result = new IResourceVariant[children.length];
		for (int i = 0; i < children.length; i++) {
			result[i] = (IResourceVariant)children[i];
		}
		return result;
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth, IProgressMonitor monitor) throws TeamException {
		return (IResourceVariant)CVSWorkspaceRoot.getRemoteTree(resource, getTag(resource), isCacheFileContentsHint(), depth, monitor);
	}
	
	@Override
	public IResource[] collectChanges(IResource local,
			IResourceVariant remote, int depth, IProgressMonitor monitor)
			throws TeamException {
		return super.collectChanges(local, remote, depth, monitor);
	}

	public IResource[] members(IResource resource) throws TeamException {
		if (resource.getType() == IResource.FILE) {
			return new IResource[0];
		}
		// Must ensure that any shared folders are included
		Set members = new HashSet();
		members.addAll(Arrays.asList(super.members(resource)));
		try {
			IResource[]  localMembers = EclipseSynchronizer.getInstance().members((IContainer) resource); //((IContainer)resource).members(true);
			for (IResource local : localMembers) {
				if (local.getType() != IResource.FILE) {
					ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)local);
					if (folder.isCVSFolder()) {
						members.add(local);
					}
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
		return (IResource[]) members.toArray(new IResource[members.size()]);
	}
	
	@Override
	public IResource[] roots() {
		return subscriber.roots();
	}

	@Override
	public IResourceVariant getResourceVariant(IResource resource) throws TeamException {
		byte[] remoteBytes = getByteStore().getBytes(resource);
		if (remoteBytes == null) {
			// There is no remote handle for this resource
			return null;
		} else {
			if (resource.getType() == IResource.FILE) {
				byte[] parentBytes = getParentBytes(resource);
				if (parentBytes == null) {
					IProject project = resource.getProject();
					if (project.exists() && RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId()) != null) {
						IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR,NLS.bind(CVSMessages.ResourceSynchronizer_missingParentBytesOnGet, new String[] { getSyncName(getByteStore()).toString(), resource.getFullPath().toString() }),resource);
						CVSProviderPlugin.log(status); 
						// Assume there is no remote and the problem is a programming error
					}
					return null;
				}
				return RemoteFile.fromBytes(resource, remoteBytes, parentBytes);
			} else {
				return RemoteFolder.fromBytes(resource, remoteBytes);
			}
		}
	}

	private String getSyncName(ResourceVariantByteStore cache) {
		if (cache instanceof PersistantResourceVariantByteStore) {
			return ((PersistantResourceVariantByteStore)cache).getSyncName().toString();
		}
		return cache.getClass().getName();
	}
	
	
	private byte[] getParentBytes(IResource resource) throws TeamException {
		IContainer parent = resource.getParent();
		byte[] bytes =  getByteStore().getBytes(parent);
		if (bytes == null ) {
			bytes = getBaseBytes(parent, getTag(resource));
		}
		return bytes;
	}

	private byte[] getBaseBytes(IContainer parent, CVSTag tag) throws CVSException {
		byte[] bytes;
		// Look locally for the folder bytes
		ICVSFolder local = CVSWorkspaceRoot.getCVSFolderFor(parent);
		FolderSyncInfo info = local.getFolderSyncInfo();
		if (info == null) {
			bytes = null;
		} else {
			// Use the folder sync from the workspace and the tag from the store
			MutableFolderSyncInfo newInfo = info.cloneMutable();
			newInfo.setTag(tag);
			newInfo.setStatic(false);
			bytes = newInfo.getBytes();
		}
		return bytes;
	}
	
	private boolean hasLocalSyncInfo(IContainer folder) {
		ICVSFolder local = CVSWorkspaceRoot.getCVSFolderFor(folder);
		try {
			return local.getFolderSyncInfo() != null;
		} catch (CVSException e) {
			// Say that there is sync info and let the failure occur elsewhere
			return true;
		}
	}

	public CVSTag getTag(IResource resource) {
		return tag;
	}

	/**
	 * Dispose of the underlying byte store
	 */
	public void dispose() {
		getByteStore().dispose();
	}
	
	@Override
	protected boolean setVariant(IResource local, IResourceVariant remote) throws TeamException {
		if (local.getType() == IResource.FOLDER && remote != null 
				&& !hasLocalSyncInfo((IFolder)local)
				&& hasLocalSyncInfo(local.getParent())) {
			// Manage the folder locally since folders exist in all versions, etc
			// Use the info from the remote except get the tag from the local parent
			CVSTag tag = CVSWorkspaceRoot.getCVSFolderFor(local.getParent()).getFolderSyncInfo().getTag();
			FolderSyncInfo info = null;
			try{
				info = FolderSyncInfo.getFolderSyncInfo(remote.asBytes());
			} catch (CVSException e){
				Status status = new Status(Status.ERROR, CVSProviderPlugin.ID, 
						NLS.bind(CVSMessages.CVSResourceVariantTree_GettingSyncInfoError, local.getProjectRelativePath().toString()), 
						e);
				throw new CVSException(status);
			}
			MutableFolderSyncInfo newInfo = info.cloneMutable();
			newInfo.setTag(tag);
			ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor((IFolder)local);
			cvsFolder.setFolderSyncInfo(newInfo);
		}
		if (remote == null && !isManaged(local)) {
			// Do not record the lack of existence of a remote for unmanaged local files
			// Instead, just flush the remote bytes if there are any
			boolean changed = getByteStore().getBytes(local) != null;
			flushVariants(local, IResource.DEPTH_ZERO);
			return changed;
		} else {
			boolean changed = super.setVariant(local, remote);
			if (local.getType() == IResource.FILE && getByteStore().getBytes(local) != null && !parentHasSyncBytes(local)) {
				// Log a warning if there is no sync bytes available for the resource's
				// parent but there is valid sync bytes for the child
				CVSProviderPlugin.log(new TeamException(NLS.bind(CVSMessages.ResourceSynchronizer_missingParentBytesOnSet, new String[] { getSyncName(getByteStore()), local.getFullPath().toString() }))); 
			}
			return changed;
		}
	}
	
	private boolean isManaged(IResource local) {
		try {
			return CVSWorkspaceRoot.getCVSResourceFor(local).isManaged();
		} catch (CVSException e) {
			return false;
		}
	}

	private boolean parentHasSyncBytes(IResource resource) throws TeamException {
		if (resource.getType() == IResource.PROJECT) return true;
		return getParentBytes(resource) != null;
	}
	
	@Override
	protected IResource[] collectedMembers(IResource local, IResource[] members) throws TeamException {
		// Look for resources that have sync bytes but are not in the resources we care about
		IResource[] resources = getStoredMembers(local);
		List children = new ArrayList();
		List changedResources = new ArrayList();
		children.addAll(Arrays.asList(members));
		for (IResource resource : resources) {
			if (!children.contains(resource)) {
				// These sync bytes are stale. Purge them
				flushVariants(resource, IResource.DEPTH_INFINITE);
				changedResources.add(resource);
			}
		}
		return (IResource[]) changedResources.toArray(new IResource[changedResources.size()]);
	}
	
	/**
	 * Return all the members of that have resource variant information associated with them,
	 * such as members that are explicitly flagged as not having a resource variant. This list
	 * is used by the collection algorithm to flush variants for which there is no local and
	 * no remote.
	 * @param local the local resource
	 * @return the local children that have resource variant information cached
	 * @throws TeamException
	 */
	private IResource[] getStoredMembers(IResource local) throws TeamException {			
		try {
			if (local.getType() != IResource.FILE && (local.exists() || local.isPhantom())) {
				IResource[] allChildren = ((IContainer)local).members(true /* include phantoms */);
				List childrenWithSyncBytes = new ArrayList();
				for (IResource resource : allChildren) {
					if (getByteStore().getBytes(resource) != null) {
						childrenWithSyncBytes.add(resource);
					}
				}
				return (IResource[]) childrenWithSyncBytes.toArray(
						new IResource[childrenWithSyncBytes.size()]);
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
		return new IResource[0];
	}
	
	@Override
	protected IResource[] refresh(IResource resource, int depth, IProgressMonitor monitor) throws TeamException {
		IResource[] changedResources = null;
		monitor.beginTask(null, 100);
		// Wait while a build is running
		// Initially wait for a short time and then increase the wait 
		// if the build is still running
		int count = 0;
		int waitTime = 10;
		while (count < 15 
				&& (isJobInFamilyRunning(ResourcesPlugin.FAMILY_AUTO_BUILD)
				|| isJobInFamilyRunning(ResourcesPlugin.FAMILY_MANUAL_BUILD))) {
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				// Continue
			}	
			count++;
			if (count >= 10) {
				waitTime = 1000;
			} else if (count >= 5) {
				waitTime = 100;
			}
			Policy.checkCanceled(monitor);
		}
		try {
			changedResources = super.refresh(resource, depth, Policy.subMonitorFor(monitor, 99));
		} catch (TeamException e) {
			// Try to properly handle exceptions that are due to project modifications
			// performed while the refresh was happening
			if (!resource.getProject().isAccessible()) {
				// The project is closed so silently skip it
				return new IResource[0];
			}
			throw e;
		} finally {
			monitor.done();
		}
		if (changedResources == null) return new IResource[0];
		return changedResources;
	}
	
	private boolean isJobInFamilyRunning(Object family) {
		Job[] jobs = Job.getJobManager().find(family);
		if (jobs != null && jobs.length > 0) {
			for (Job job : jobs) {
				if (job.getState() != Job.NONE) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	public ICVSRemoteResource buildTree(RemoteFolderTree parent, IResource resource, boolean immutable, IProgressMonitor monitor) throws TeamException {
		
		Policy.checkCanceled(monitor);
		
		byte[] remoteBytes = getByteStore().getBytes(resource);
		if (remoteBytes == null) {
			// There is no remote handle for this resource
			return null;
		}
		
		if (resource.getType() == IResource.FILE) {
			if (immutable) {
				remoteBytes = ResourceSyncInfo.setTag(remoteBytes, new CVSTag(ResourceSyncInfo.getRevision(remoteBytes), CVSTag.VERSION));
			}
			if (parent == null) {
				return (ICVSRemoteResource)getResourceVariant(resource);
			}
			return new RemoteFile(parent, remoteBytes);
		} else {
			RemoteFolderTree remote = RemoteFolderTree.fromBytes(parent, resource, remoteBytes);
			IResource[] members = members(resource);
			List children = new ArrayList();
			for (IResource member : members) {
				ICVSRemoteResource child = buildTree(remote, member, immutable, monitor);
				if (child != null)
					children.add(child);
			}
			
			// Add the children to the remote folder tree
			remote.setChildren((ICVSRemoteResource[])children.toArray(new ICVSRemoteResource[children.size()]));
			return remote;
		}
	}

	public boolean isCacheFileContentsHint() {
		return cacheFileContentsHint;
	}
}
