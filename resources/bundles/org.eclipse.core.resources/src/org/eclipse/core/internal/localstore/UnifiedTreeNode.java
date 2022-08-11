/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *     Martin Oberhuber (Wind River) - [292267] OutOfMemoryError due to leak in UnifiedTree
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.util.Iterator;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IResource;

/**
 * A node in a {@link UnifiedTree}. A node usually represents a file/folder
 * in the workspace, the file system, or both. There are also special node
 * instances to act as child and level markers in the tree.
 */
public class UnifiedTreeNode implements ILocalStoreConstants {
	protected UnifiedTreeNode child;
	protected boolean existsWorkspace;
	protected IFileInfo fileInfo;
	protected IResource resource;
	protected IFileStore store;
	protected UnifiedTree tree;

	public UnifiedTreeNode(UnifiedTree tree, IResource resource, IFileStore store, IFileInfo fileInfo, boolean existsWorkspace) {
		this.tree = tree;
		this.resource = resource;
		this.store = store;
		this.fileInfo = fileInfo;
		this.existsWorkspace = existsWorkspace;
	}

	public boolean existsInFileSystem() {
		return fileInfo != null && fileInfo.exists();
	}

	/**
	 * Returns <code>true</code> if an I/O error was encountered while accessing
	 * the file or the directory in the file system.
	 */
	public boolean isErrorInFileSystem() {
		return fileInfo != null && fileInfo.getError() != IFileInfo.NONE;
	}

	public boolean existsInWorkspace() {
		return existsWorkspace;
	}

	/**
	 * Returns an iterator of this node's children.
	 */
	public Iterator<UnifiedTreeNode> getChildren() {
		return tree.getChildren(this);
	}

	protected UnifiedTreeNode getFirstChild() {
		return child;
	}

	public long getLastModified() {
		return fileInfo == null ? 0 : fileInfo.getLastModified();
	}

	public int getLevel() {
		return tree.getLevel();
	}

	/**
	 * Gets the name of this node in the local file system.
	 * @return Returns a String
	 */
	public String getLocalName() {
		return fileInfo == null ? null : fileInfo.getName();
	}

	public IResource getResource() {
		return resource;
	}

	/**
	 * Returns the local store of this resource.  May be null.
	 */
	public IFileStore getStore() {
		//initialize store lazily, because it is not always needed
		if (store == null)
			store = ((Resource) resource).getStore();
		return store;
	}

	public boolean isFolder() {
		return fileInfo == null ? false : fileInfo.isDirectory();
	}

	public boolean isSymbolicLink() {
		return fileInfo == null ? false : fileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK);
	}

	public void removeChildrenFromTree() {
		tree.removeNodeChildrenFromQueue(this);
	}

	/**
	 * Reuses this object by assigning all new values for the fields.
	 */
	public void reuse(UnifiedTree aTree, IResource aResource, IFileStore aStore, IFileInfo info, boolean existsInWorkspace) {
		this.tree = aTree;
		this.child = null;
		this.resource = aResource;
		this.store = aStore;
		this.fileInfo = info;
		this.existsWorkspace = existsInWorkspace;
	}

	/**
	 * Releases elements that won't be needed any more for garbage collection.
	 * Should be called before adding a node to the free list.
	 */
	public void releaseForGc() {
		this.child = null;
		this.resource = null;
		this.store = null;
		this.fileInfo = null;
	}

	public void setExistsWorkspace(boolean exists) {
		this.existsWorkspace = exists;
	}

	protected void setFirstChild(UnifiedTreeNode child) {
		this.child = child;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}

	@Override
	public String toString() {
		String s = resource == null ? "null" : resource.getFullPath().toString(); //$NON-NLS-1$
		return "Node: " + s; //$NON-NLS-1$
	}
}
