/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.core.filesystem;

/**
 * This interface is used to query a tree of file stores.
 * A file tree accurately represents the state of a portion of a file system
 * at the time it is created, but it is never updated. Clients using a file
 * tree must tolerate the fact that the actual file system contents may have
 * changed since the tree was generated.
 *
 * @see IFileSystem#fetchFileTree(IFileStore, org.eclipse.core.runtime.IProgressMonitor)
 * @since org.eclipse.core.filesystem 1.0
 * @noimplement This interface is not intended to be implemented by clients. File tree
 * implementations should use the concrete class {@link org.eclipse.core.filesystem.provider.FileTree}
 */
public interface IFileTree {
	/***
	 * Returns an {@link IFileInfo} instance for each file and directory contained
	 * within the given store at the time this file tree was created.
	 * <p>
	 * An empty array is returned if the given store has no children, or is not
	 * in this file tree.
	 * </p>
	 *
	 * @param store a file store in this tree
	 * @return An array of information about the children of the store, or an empty
	 * array if the store has no children.
	 * @see IFileStore#childInfos(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileInfo[] getChildInfos(IFileStore store);

	/**
	 * Returns an {@link IFileStore} instance for each file and directory contained
	 * within the given store at the time this file tree was created.
	 * <p>
	 * An empty array is returned if the given store has no children, or is not
	 * in this file tree.
	 * </p>
	 * @param store a file store in this tree
	 * @return The children of the store, or an empty array if the store has no children.
	 * @see IFileStore#childStores(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileStore[] getChildStores(IFileStore store);

	/**
	 * Returns information about this file at the time this file tree was created.
	 * <p>
	 * This method succeeds regardless of whether a corresponding
	 * file exists in the file tree. In the case of a non-existent
	 * file, the returned info will include the file's name and will return <code>false</code>
	 * when {@link IFileInfo#exists()} is called, but all other information will assume default
	 * values.
	 *
	 * @param store the store to return the file info for
	 * @return IFileInfo the IFileInfo for the given store
	 * @see IFileStore#fetchInfo(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileInfo getFileInfo(IFileStore store);

	/***
	 * Returns the root of this tree
	 * @return An IFileStore representing the root of the tree
	 */
	public IFileStore getTreeRoot();
}