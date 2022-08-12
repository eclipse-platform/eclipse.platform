/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.filesystem.provider;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.*;

/**
 * The common superclass for all file system implementations.  Instances
 * of this class are provided using the <code>org.eclipse.core.filesystem.filesystems</code>
 * extension point.
 * <p>
 * On creation, the <code>setInitializationData</code> method is called with
 * any parameter data specified in the declaring plug-in's manifest.
 * </p>
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since org.eclipse.core.filesystem 1.0
 */
public abstract class FileSystem extends PlatformObject implements IFileSystem {
	private String scheme;

	/**
	 * Creates a new file system instance.
	 */
	public FileSystem() {
		super();
	}

	/**
	 * This is the default implementation of {@link IFileSystem#attributes()}.
	 * This implementation always returns <code>0</code>.
	 * Subclasses may override this method.
	 *
	 * @return The attributes supported by this file system
	 * @see IFileSystem#attributes()
	 */
	@Override
	public int attributes() {
		return 0;
	}

	/**
	 * This is the default implementation of {@link IFileSystem#canDelete()}.
	 * This implementation always returns <code>false</code>.
	 * Subclasses may override this method.
	 *
	 * @return <code>true</code> if this file system supports deletion, and
	 * <code>false</code> otherwise.
	 * @see IFileSystem#canDelete()
	 */
	@Override
	public boolean canDelete() {
		return false;
	}

	/**
	 * This is the default implementation of {@link IFileSystem#canWrite()}.
	 * This implementation always returns <code>false</code>.
	 * Subclasses may override this method.
	 *
	 * @return <code>true</code> if this file system allows modification, and
	 * <code>false</code> otherwise.
	 * @see IFileSystem#canWrite()
	 */
	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public final String getScheme() {
		return scheme;
	}

	/**
	 * This is the default implementation of {@link IFileSystem#getStore(IPath)}.
	 * This implementation forwards to {@link IFileSystem#getStore(URI)},
	 * assuming that the provided path corresponds to the path component of the
	 * URI for the file store.
	 * <p>
	 * Subclasses may override this method.  If it is not possible to create a file
	 * store corresponding to the provided path for this file system, a file store
	 * belonging to the null file system should be returned
	 * </p>
	 *
	 * @param path A path to a file store within the scheme of this file system.
	 * @return A handle to a file store in this file system
	 * @see IFileSystem#getStore(IPath)
	 * @see EFS#getNullFileSystem()
	 */
	@Override
	public IFileStore getStore(IPath path) {
		try {
			return getStore(new URI(scheme, path.toString(), null));
		} catch (URISyntaxException e) {
			return EFS.getNullFileSystem().getStore(path);
		}
	}

	/**
	 * Subclasses must implement this method to satisfy the contract
	 * of {@link IFileSystem#getStore(URI)}.  If it is not possible to create a file
	 * store corresponding to the provided URI for this file system, a file store
	 * belonging to the null file system should be returned
	 */
	@Override
	public abstract IFileStore getStore(URI uri);

	/**
	 * {@inheritDoc}
	 * <p>
	 * This default implementation always returns <code>null</code>.  Subclasses
	 * that can efficiently provide an {@link IFileTree} rooted at the given file store
	 * should override.
	 * @throws CoreException if fails.
	 * @see IFileSystem#fetchFileTree(IFileStore, IProgressMonitor)
	 */
	@Override
	public IFileTree fetchFileTree(IFileStore root, IProgressMonitor monitor) throws CoreException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This default implementation always returns <code>null</code>.
	 * Subclasses may override to provide a concrete mapping from local
	 * files to an IFileStore in their file system.
	 */
	@Override
	public IFileStore fromLocalFile(java.io.File file) {
		return null;
	}

	/**
	 * Initializes this file system instance with the provided scheme.
	 * <p>
	 * This method is called by the platform immediately after the
	 * file system instance is created.  This method must not be
	 * called by clients.
	 *
	 * @param aScheme The scheme of the file system.
	 */
	public final void initialize(String aScheme) {
		if (aScheme == null)
			throw new NullPointerException();
		//scheme cannot be changed after creation
		if (this.scheme != null)
			throw new IllegalStateException("File system already initialized"); //$NON-NLS-1$
		this.scheme = aScheme;
	}

	/**
	 * This is the default implementation of {@link IFileSystem#isCaseSensitive()}.
	 * This implementation always returns <code>true</code>. Subclasses may override this method.
	 *
	 * @return <code>true</code> if this file system is case sensitive, and
	 * <code>false</code> otherwise.
	 * @see IFileSystem#isCaseSensitive()
	 */
	@Override
	public boolean isCaseSensitive() {
		return true;
	}
}
