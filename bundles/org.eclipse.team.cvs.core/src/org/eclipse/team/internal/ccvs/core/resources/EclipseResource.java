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


import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;


/**
 * Represents handles to CVS resource on the local file system. Synchronization
 * information is taken from the CVS subdirectories. 
 * 
 * @see LocalFolder
 * @see LocalFile
 */
abstract class EclipseResource implements ICVSResource, Comparable {

	// The separator that must be used when creating CVS resource paths. Never use
	// the platform default separator since it is not compatible with CVS resources.
	protected static final String SEPARATOR = Session.SERVER_SEPARATOR;
	protected static final String CURRENT_LOCAL_FOLDER = Session.CURRENT_LOCAL_FOLDER;
		
	/*
	 * The local resource represented by this handle
	 */
	IResource resource;
	
	/*
	 * Creates a CVS handle to the provided resource
	 */
	protected EclipseResource(IResource resource) {
		Assert.isNotNull(resource);
		this.resource = resource;
	}
	
	/*
	 * Get the extention of the path of resource relative to the path of root
	 * 
	 * @throws CVSException if root is not a root-folder of resource
	 */
	public String getRelativePath(ICVSFolder root) throws CVSException {
		try {
			EclipseResource rootFolder;
			String result;
			rootFolder = (EclipseResource)root;
			result = Util.getRelativePath(rootFolder.getPath(), getPath());
			if (result.length() == 0) return CURRENT_LOCAL_FOLDER;
			return result;	
		} catch (ClassCastException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, CVSMessages.EclipseResource_invalidResourceClass, e, root);
			throw new CVSException(status); 
		}
	}

	@Override
	public boolean exists() {
		return resource.exists();
	}

	/*
	 * Returns the parent folder of this resource of <code>null</code> if resource
	 * the resource.
	 * 
	 * @see ICVSResource#getParent()
	 */
	public ICVSFolder getParent() {
		IContainer parent = resource.getParent();
		if (parent==null) {
			return null;
		}
		return new EclipseFolder(parent);
	}

	@Override
	public String getName() {
		return resource.getName();
	}

	@Override
	public boolean isIgnored() throws CVSException {
		// a managed resource is never ignored
		if(isManaged() || resource.getType()==IResource.ROOT || resource.getType()==IResource.PROJECT) {
			return false;
		}
		
		// If the resource is a derived or linked resource, it is ignored
		if (resource.isDerived() || resource.isLinked()) {
			return true;
		}
		
		// always ignore CVS
		String name = getName();
		if (name.equals("CVS")) return true; //$NON-NLS-1$
		
		// check the global ignores from Team
		if (Team.isIgnoredHint(resource)) return true;
		
		// check ignore patterns from the .cvsignore file.
		if(EclipseSynchronizer.getInstance().isIgnored(resource)) {
			return true;
		}
		
		// check the parent, if the parent is ignored or mapped to CVSROOT/Emptydir
		// then this resource is ignored also
		ICVSFolder parent = getParent();
		if(parent==null) return false;
		if (parent.isIgnored()) return true;
		FolderSyncInfo info = parent.getFolderSyncInfo();
		if (info == null) return false;
		return info.isVirtualDirectory();
	}
	
	@Override
	public void setIgnoredAs(final String pattern) throws CVSException {
		run(monitor -> EclipseSynchronizer.getInstance().addIgnored(resource.getParent(), pattern), null);
	}

	@Override
	public boolean isManaged() throws CVSException {
		return isManaged(getSyncBytes());
	}
	
	/*
	 * Helper method that captures the sematics of isManaged given a ResourceSyncInfo
	 */
	public boolean isManaged(byte[] syncBytes) {
		return syncBytes != null;
	}
	
	/**
	 * Two ManagedResources are equal, if there cvsResources are
	 * equal (and that is, if the point to the same file)
	 */
	public boolean equals(Object obj) {
		
		if (!(obj instanceof EclipseResource)) {
			return false;
		} else {
			return getPath().equals(((EclipseResource) obj).getPath());
		}
	}
			
	/*
	 * @see ICVSResource#getPath()
	 */
	public String getPath() {
		return resource.getFullPath().toString();
	}	
	
	@Override
	public boolean isFolder() {
		return false;
	}
	
	/*
	 * @see org.eclipse.team.internal.ccvs.core.ICVSFile#getSyncBytes()
	 */
	public byte[] getSyncBytes() throws CVSException {
		return EclipseSynchronizer.getInstance().getSyncBytes(getIResource());
	}
	
	/*
	 * @see org.eclipse.team.internal.ccvs.core.ICVSFile#setSyncBytes(byte[])
	 */
	public void setSyncBytes(byte[] syncBytes) throws CVSException {
		if (getParent().isCVSFolder()) {
			EclipseSynchronizer.getInstance().setSyncBytes(getIResource(), syncBytes);
		}
	}
	
	@Override
	public ResourceSyncInfo getSyncInfo() throws CVSException {
		return EclipseSynchronizer.getInstance().getResourceSync(resource);
	}
	
	/*
	 * Implement the hashcode on the underlying strings, like it is done in the equals.
	 */
	public int hashCode() {
		return getPath().hashCode();
	}	
	
	/*
	 * Give the pathname back
	 */
	public String toString() {
		return getPath();
	}
	
	@Override
	public void unmanage(IProgressMonitor monitor) throws CVSException {
		EclipseSynchronizer.getInstance().deleteResourceSync(resource);
	}
	
	@Override
	public int compareTo(Object arg0) {
		EclipseResource other = (EclipseResource)arg0;
		return resource.getFullPath().toString().compareTo(other.resource.getFullPath().toString());
	}

	@Override
	public IResource getIResource() {
		return resource;
	}

	/**
	 * Called by a resource change listener when a resource is changed or added. This allows
	 * CVS resources to adjust any internal state based on the change.
	 * 
	 * @param forAddition modification is an addition
	 * @throws CVSException
	 */
	public abstract void handleModification(boolean forAddition) throws CVSException;
	
	public void run(final ICVSRunnable job, IProgressMonitor monitor) throws CVSException {
		final CVSException[] error = new CVSException[1];
		try {
			// Do not use a scheduling rule in the workspace run since one
			// will be obtained by the EclipseSynchronizer
			ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor1 -> {
				try {
					EclipseSynchronizer.getInstance().run(getIResource(), job, monitor1);
				} catch(CVSException e) {
					error[0] = e; 
				}
			}, null /* no rule */, 0, monitor);
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
		if(error[0]!=null) {
			throw error[0];
		}
	}
}
