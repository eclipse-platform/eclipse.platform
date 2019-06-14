/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.CachedResourceVariant;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;

public class CVSFileStore extends FileStore {

	
	private final CVSURI uri;
	private IFileInfo info;
	
	public CVSFileStore(CVSURI uri, IFileInfo info) {
		this.uri = uri;
		this.info = info;
	}

	public boolean canReturnFullTree() {
		return true;
	}

	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		IFileInfo[] infos = childInfos(options, monitor);
		String[] names = new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			names[i] = infos[i].getName();
		}
		return names;
	}

	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		if (info != null && !info.isDirectory()) {
			return new IFileInfo[0];
		}
		ICVSRemoteFolder folder = uri.toFolder();
		ICVSResource[] children = folder.fetchChildren(monitor);
		
		IFileInfo[] childInfos = new IFileInfo[children.length];
		for (int i = 0; i < children.length; i++) {
			ICVSResource child = children[i];
			IFileInfo info = getFileInfo(child, monitor);
			childInfos[i] = info;
		}
		return childInfos;
	}

	public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		IFileInfo[] infos = childInfos(options, monitor);
		IFileStore[] children = new IFileStore[infos.length];
		for (int i = 0; i < infos.length; i++) {
			children[i] = getChild(infos[i]);
		}
		return children;
	}

	private IFileStore getChild(IFileInfo info) {
		return new CVSFileStore(uri.append(info.getName()), info);
	}

	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		
		if (isStickyRevision()) {
			ICVSRemoteFile file = uri.toFile();
			return getFileInfo(file, monitor);
		}
		ICVSRemoteFolder folder = uri.getParentFolder();
		
		if (folder == null) {
			// this is the repo root so return an info that indicates this
			FileInfo info = new FileInfo();
			info.setExists(true);
			info.setName(uri.getRepositoryName());
			info.setDirectory(true);
		}
		ICVSResource[] children = folder.fetchChildren(monitor);
		ICVSResource resource = null;
		for (ICVSResource child : children) {
			if (child.getName().equals(getName())) {
				resource = child;
				break;
			}
		}
		return getFileInfo(resource, monitor);
	}

	private boolean isStickyRevision() {
		String revision = uri.getRevision(); 
		CVSTag tag = uri.getTag();
		if (revision == null)
			return false;
		if (tag == null)
			return false;
		return revision.equals(tag.getName());
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
			// Avoid a round trip by looking for the file in the cache
			if (file instanceof RemoteFile) {
				RemoteFile remote = (RemoteFile) file;
				CachedResourceVariant variant = remote.getCachedHandle();
				if (variant instanceof ICVSRemoteFile) {
					file = (ICVSRemoteFile) variant;
				}
			}
			ILogEntry entry = file.getLogEntry(monitor);
			info.setLastModified(entry.getDate().getTime());
		} else {
			info.setLastModified(0);
			info.setDirectory(true);
		}
		return info;
	}

	public IFileStore getChild(String name) {
		if (info != null && !info.isDirectory()) {
			return null;
		}
		return new CVSFileStore(uri.append(name), null);
	}

	public IFileStore getChild(IPath path) {
		return new CVSFileStore(uri.append(path), null);
	}


	public String getName() {
		return uri.getLastSegment();
	}

	public IFileStore getParent() {
		if (uri.isRepositoryRoot()) {
			return null;
		}
		return new CVSFileStore(uri.removeLastSegment(), null);
	}

	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		ICVSRemoteFile file = uri.toFile();
		IStorage storage = ((IResourceVariant) file).getStorage(monitor);
		return storage.getContents();
	}

	public URI toURI() {
		return uri.toURI();
	}
	
	public CVSURI getCVSURI() {
		return uri;
	}

}
