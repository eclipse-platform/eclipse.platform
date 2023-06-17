/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Project Path Variable Support
 *     Sergey Prigogin (Google) - [338010] Resource.createLink() does not preserve symbolic links
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.File;
import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Represents the root of a file system that is connected to the workspace.
 * A file system can be rooted on any resource.
 */
public class FileStoreRoot {
	private int chop;
	/**
	 * When a root is changed, the old root object is marked invalid
	 * so that other resources with a cache of the root will know they need to update.
	 */
	private boolean isValid = true;
	/**
	 * If this root represents a resource in the local file system, this path
	 * represents the root location.  This value is null if the root represents
	 * a non-local file system
	 */
	private IPath localRoot;
	/**
	 * Canonicalized version of localRoot. Initialized lazily.
	 * @see FileUtil#canonicalPath(IPath)
	 */
	private IPath canonicalLocalRoot;

	private URI root;
	/**
	 * Canonicalized version of root. Initialized lazily.
	 * @see FileUtil#canonicalURI(URI)
	 */
	private URI canonicalRoot;

	/**
	 * Defines the root of a file system within the workspace tree.
	 * @param rootURI The virtual file representing the root of the file
	 * system that has been mounted
	 * @param workspacePath The workspace path at which this file
	 * system has been mounted
	 */
	FileStoreRoot(URI rootURI, IPath workspacePath) {
		Assert.isNotNull(rootURI);
		Assert.isNotNull(workspacePath);
		this.root = rootURI;
		this.chop = workspacePath.segmentCount();
		this.localRoot = toLocalPath(root);
	}

	private IPathVariableManager getManager(IPath workspacePath) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = workspaceRoot.findMember(workspacePath);
		if (resource != null)
			return resource.getPathVariableManager();
		// Bug 547691 - deal with requests for the path of a deleted project
		if (workspacePath.segmentCount() == 0) {
			return workspaceRoot.getPathVariableManager();
		}
		if (workspacePath.segmentCount() == ICoreConstants.PROJECT_SEGMENT_LENGTH) {
			return workspaceRoot.getProject(workspacePath.lastSegment()).getPathVariableManager();
		}
		return workspaceRoot.getFile(workspacePath).getPathVariableManager();
	}

	/**
	 * Returns the resolved, absolute file system location of the resource
	 * corresponding to the given workspace path, or null if none could
	 * be computed. No canonicalization is applied to the returned URI.
	 */
	public URI computeURI(IPath workspacePath) {
		return computeURI(workspacePath, false);
	}

	/**
	 * Returns the resolved, absolute file system location of the resource
	 * corresponding to the given workspace path, or null if none could
	 * be computed.
	 *
	 * @param workspacePath the workspace path to compute the URL for
	 * @param canonical if {@code true}, the prefix of the path of the returned URI
	 *     corresponding to this root will be canonicalized
	 */
	public URI computeURI(IPath workspacePath, boolean canonical) {
		IPath childPath = workspacePath.removeFirstSegments(chop);
		URI rootURI = canonical ? getCanonicalRoot() : root;
		rootURI = getManager(workspacePath).resolveURI(rootURI);
		if (childPath.segmentCount() == 0)
			return rootURI;
		try {
			return EFS.getStore(rootURI).getFileStore(childPath).toURI();
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Creates an IFileStore for a given workspace path. The prefix of the path of
	 * the returned IFileStore corresponding to this root is canonicalized.
	 * @exception CoreException If the file system for that resource is undefined
	 */
	IFileStore createStore(IPath workspacePath, IResource resource) throws CoreException {
		IPath childPath = workspacePath.removeFirstSegments(chop);
		// For a linked resource itself we have to use its root, but for its children we prefer
		// to use the canonical root since it provides for faster file system access.
		// See http://bugs.eclipse.org/507084
		final URI uri = resource.getPathVariableManager().resolveURI(resource.isLinked() ? root : getCanonicalRoot());
		if (!uri.isAbsolute()) {
			// Handles case where resource location cannot be resolved such as
			// unresolved path variable or invalid file system scheme.
			return EFS.getNullFileSystem().getStore(workspacePath);
		}
		IFileStore rootStore = EFS.getStore(uri);
		if (childPath.segmentCount() == 0)
			return rootStore;
		return rootStore.getFileStore(childPath);
	}

	boolean isValid() {
		return isValid;
	}

	/**
	 * Returns the resolved, absolute file system location of the given resource.
	 * Returns null if the location could not be resolved.  No canonicalization
	 * is applied to the returned path.
	 *
	 * @param workspacePath the workspace path of the resource
	 * @param resource the resource itself
	 */
	IPath localLocation(IPath workspacePath, IResource resource) {
		return localLocation(workspacePath, resource, false);
	}

	/**
	 * Returns the resolved, absolute file system location of the given resource.
	 * Returns null if the location could not be resolved.
	 *
	 * @param workspacePath the workspace path of the resource
	 * @param resource the resource itself
	 * @param canonical if {@code true}, the prefix of the returned path corresponding
	 *     to this root will be canonicalized
	 */
	IPath localLocation(IPath workspacePath, IResource resource, boolean canonical) {
		if (localRoot == null)
			return null;
		IPath rootPath = canonical ? getCanonicalLocalRoot() : localRoot;
		IPath location;
		if (workspacePath.segmentCount() <= chop)
			location = rootPath;
		else
			location = rootPath.append(workspacePath.removeFirstSegments(chop));
		location = resource.getPathVariableManager().resolvePath(location);

		// if path is still relative then path variable could not be resolved
		// if path is null, it means path variable refers to a non-local filesystem
		if (location == null || !location.isAbsolute())
			return null;
		return location;
	}

	void setValid(boolean value) {
		this.isValid = value;
	}

	/**
	 * Returns the local path for the given URI, or null if not possible.
	 */
	private IPath toLocalPath(URI uri) {
		try {
			final File localFile = EFS.getStore(uri).toLocalFile(EFS.NONE, null);
			return localFile == null ? null : IPath.fromOSString(localFile.getAbsolutePath());
		} catch (CoreException e) {
			return FileUtil.toPath(uri);
		}
	}

	private synchronized IPath getCanonicalLocalRoot() {
		if (canonicalLocalRoot == null && localRoot != null) {
			canonicalLocalRoot = FileUtil.canonicalPath(localRoot);
		}
		return canonicalLocalRoot;
	}

	private synchronized URI getCanonicalRoot() {
		if (canonicalRoot == null) {
			canonicalRoot = FileUtil.canonicalURI(root);
		}
		return canonicalRoot;
	}

	/** for debugging only **/
	@Override
	public String toString() {
		return root.toString();
	}
}
