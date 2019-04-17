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
/**
 * 
 */
package org.eclipse.team.internal.ccvs.core.filesystem;

import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;

class RLogTreeBuilder {

	private ICVSRepositoryLocation location;
	private RemoteFolderTree tree;
	private CVSTag tag;
	private HashMap folderMap;
	private HashMap logMap;
	private LogEntryCache cache;

	public RLogTreeBuilder(ICVSRepositoryLocation location, CVSTag tag, LogEntryCache cache) {
		this.tag = tag;
		this.location = location;
		this.cache = cache;
		reset();
	}

	public RemoteFolderTree getTree() {
		return tree;
	}

	/**
	 * Reset the builder to prepare for a new build
	 */
	public void reset() {
		folderMap = new HashMap(16);
		logMap = new HashMap(16);
		tree = new RemoteFolderTree(null, location, ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, tag);
		tree.setChildren(new ICVSRemoteResource[0]);
	}

	/*
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#newFile(java.lang.String, java.lang.String)
	 */
	public void newFile(IPath remoteFilePath, ICVSRemoteFile remoteFile) {
		try {
			addFile(tree, tag, remoteFile, remoteFilePath);
		} catch (CVSException e) {
		}
	}

	private void addFile(RemoteFolderTree tree, CVSTag tag, ICVSRemoteFile file, IPath filePath) throws CVSException {
		RemoteFolderTree parent = (RemoteFolderTree) getFolder(tree, tag, filePath.removeLastSegments(1), Path.EMPTY);
		addChild(parent, file);
	}

	private void addChild(RemoteFolderTree tree, ICVSRemoteResource resource) {
		//get the log entry info for this file and save it
		logMap.put(resource, cache.getLogEntry(resource));

		ICVSRemoteResource[] children = tree.getChildren();
		ICVSRemoteResource[] newChildren;
		if (children == null) {
			newChildren = new ICVSRemoteResource[] {resource};
		} else {
			newChildren = new ICVSRemoteResource[children.length + 1];
			System.arraycopy(children, 0, newChildren, 0, children.length);
			newChildren[children.length] = resource;
		}
		tree.setChildren(newChildren);
	}

	/* 
	 * Get the folder at the given path in the given tree, creating any missing folders as needed.
	 */
	private ICVSRemoteFolder getFolder(RemoteFolderTree tree, CVSTag tag, IPath remoteFolderPath, IPath parentPath) throws CVSException {
		if (remoteFolderPath.segmentCount() == 0)
			return tree;
		String name = remoteFolderPath.segment(0);
		ICVSResource child;
		IPath childPath = parentPath.append(name);
		if (tree.childExists(name)) {
			child = tree.getChild(name);
		} else {
			child = new RemoteFolderTree(tree, tree.getRepository(), childPath.toString(), tag);
			//Save this folder in hash map
			folderMap.put(childPath.toString(), child);
			((RemoteFolderTree) child).setChildren(new ICVSRemoteResource[0]);
			addChild(tree, (ICVSRemoteResource) child);
		}
		return getFolder((RemoteFolderTree) child, tag, remoteFolderPath.removeFirstSegments(1), childPath);
	}

	public HashMap getFolderMap() {
		return folderMap;
	}

	public HashMap getLogMap() {
		return logMap;
	}
}
