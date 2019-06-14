/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.filesystem;

import java.util.HashMap;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;

public class CVSFileTree {

	private RemoteFolderTree remoteTree;
	//HashMap of RemoteFolders used to speed up look up
	private HashMap remoteFolders;
	private HashMap logMap;
	private CVSURI baseURI;
	
	public CVSFileTree(IFileStore treeRoot, CVSURI uri, RemoteFolderTree remoteTree, HashMap remoteFolders, HashMap logMap) {
		this.remoteTree = remoteTree;
		this.baseURI = uri;
		this.remoteFolders = remoteFolders;
		this.logMap = logMap;
	}

	public IFileStore[] getChildrenFileStores(IFileStore store) {
		CVSURI cvsUri = CVSURI.fromUri(store.toURI());
		RemoteFolderTree folderTree = (RemoteFolderTree) remoteFolders.get(cvsUri.getProjectStrippedPath().toString());
		
		if (folderTree == null)
			return null;
		
		ICVSRemoteResource[] remoteResources = folderTree.getChildren();
		IFileStore[] fileStore = new IFileStore[remoteResources.length];
		for (int i = 0; i < remoteResources.length; i++) {
			IFileInfo fileInfo;
			try {
				fileInfo = getFileInfo(remoteResources[i], new NullProgressMonitor());
				fileStore[i] = new CVSFileStore(baseURI.append(fileInfo.getName()), fileInfo);
			} catch (TeamException e) {}
		}
		return fileStore;
	}

	private IFileInfo getFileInfo(ICVSResource resource, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		if (resource == null)
			return null;
		FileInfo info = new FileInfo();
		info.setExists(true);
		info.setName(resource.getName());
		if (!resource.isFolder()) {
			ICVSRemoteFile file = (ICVSRemoteFile) resource;
			//TODO: how to handle entries that are not found
			ILogEntry entry = (ILogEntry) logMap.get(file);
			//ILogEntry entry = file.getLogEntry(monitor);
			info.setLastModified(entry.getDate().getTime());
		} else {
			info.setLastModified(0);
			info.setDirectory(true);
		}
		return info;
	}

	public IFileInfo[] getChildrenFileInfos(IFileStore store) {
	
		CVSURI cvsUri = CVSURI.fromUri(store.toURI());
		RemoteFolderTree folderTree = (RemoteFolderTree) remoteFolders.get(cvsUri.getProjectStrippedPath().toString());
		
		if (folderTree == null)
			return null;
		
		ICVSRemoteResource[] remoteResources = folderTree.getChildren();
		IFileInfo[] fileInfos = new IFileInfo[remoteResources.length];
		for (int i = 0; i < remoteResources.length; i++) {
			IFileInfo fileInfo;
			try {
				fileInfo = getFileInfo(remoteResources[i], new NullProgressMonitor());
				fileInfos[i] = fileInfo;
			} catch (TeamException e) {}
		}
		return fileInfos;
	}

	public IFileInfo getFileInfo(IFileStore store) {
		ICVSRemoteFolder folder=null;
		String resourceName = null;
		CVSURI cvsUri = CVSURI.fromUri(store.toURI());
		
		folder = cvsUri.getParentFolder();
		resourceName = cvsUri.getLastSegment();
		
		if (folder.getName().equals(ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME)) {
			// this is the repository root so return an info that indicates this
			FileInfo info = new FileInfo();
			info.setExists(true);
			info.setName(resourceName);
			info.setDirectory(true);
			return info;
		}
		try {
			RemoteFolderTree remoteFolder =  (RemoteFolderTree) remoteFolders.get(folder.getName());
			
			if (remoteFolder == null){
				String repoPath = folder.getRepositoryRelativePath();
				IPath repoPath2 = new Path(repoPath);
				repoPath2 = repoPath2.removeFirstSegments(1);
				remoteFolder = (RemoteFolderTree) remoteFolders.get(repoPath2.toString());
			}
			
			//ok, we tried a short cut, but it wasn't meant to be so look at the entire tree
			if (remoteFolder == null)
				remoteFolder = remoteTree;
			
			
			ICVSRemoteResource[] children =remoteFolder.getChildren();
			ICVSResource resource = null;
			for (ICVSRemoteResource child : children) {
				if (child.getName().equals(resourceName)) {
					resource = child;
					break;
				}
			}
			return getFileInfo(resource, new NullProgressMonitor());
		} catch (CoreException e) {
			CVSProviderPlugin.log(e);
			return null;
		}
	}
	
	

}
