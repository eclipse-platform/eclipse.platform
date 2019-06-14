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


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;

/**
 * Whereas the RemoteFolder class provides access to a remote hierarchy using
 * lazy retrieval via <code>getMembers()</code>, the RemoteFolderTree will force 
 * a recursive retrieval of the remote hierarchy in one round trip.
 */
public class RemoteFolderTree extends RemoteFolder  {
	
	public static RemoteFolderTree fromBytes(RemoteFolderTree parent, IResource local, byte[] bytes) throws CVSException {
		Assert.isNotNull(bytes);
		Assert.isTrue(local.getType() != IResource.FILE);
		FolderSyncInfo syncInfo = FolderSyncInfo.getFolderSyncInfo(bytes);
		return new RemoteFolderTree(parent, local.getName(), KnownRepositories.getInstance().getRepository(syncInfo.getRoot()), syncInfo.getRepository(), syncInfo.getTag(), syncInfo.getIsStatic());
	}
	
	public RemoteFolderTree(RemoteFolder parent, ICVSRepositoryLocation repository, String repositoryRelativePath, CVSTag tag) {
		super(parent, repository, repositoryRelativePath, tag);
	}
	
	public RemoteFolderTree(RemoteFolder parent, String name, ICVSRepositoryLocation repository, String repositoryRelativePath, CVSTag tag) {
		this(parent, name, repository, repositoryRelativePath, tag, false);
	}

	public RemoteFolderTree(RemoteFolder parent, String name, ICVSRepositoryLocation repository, String repositoryRelativePath, CVSTag tag, boolean isStatic) {
		super(parent, name, repository, repositoryRelativePath, tag, isStatic);
	}

	/* 
	 * Override of inherited method which persists the children
	 */
	public ICVSRemoteResource[] getMembers(CVSTag tagName, IProgressMonitor monitor) throws CVSException {
		if (getChildren() == null)
			setChildren(super.getMembers(tagName, monitor));
		return getChildren();
	}

	/* 
	 * This method is public to allow access by the RemoteFolderTreeBuilder utility class.
	 * No other external classes should use this method.
	 */
	public void setChildren(ICVSRemoteResource[] children) {
		super.setChildren(children);
	}
	
	public void acceptChildren(ICVSResourceVisitor visitor) throws CVSException {
		ICVSRemoteResource[] children = getChildren();
		if (children == null) return;
		for (ICVSRemoteResource c : children) {
			((ICVSResource) c).accept(visitor);
		}
	}
}

