/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.memory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.*;

/**
 * In memory file system implementation used for testing.
 */
public class MemoryFileStore extends FileStore {
	private static final MemoryTree TREE = MemoryTree.TREE;

	private final IPath path;

	public MemoryFileStore(IPath path) {
		super();
		this.path = path.setDevice(null);
	}

	public String[] childNames(int options, IProgressMonitor monitor) {
		final String[] names = TREE.childNames(path);
		return names == null ? EMPTY_STRING_ARRAY : names;
	}

	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		TREE.delete(path);
	}

	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
		return TREE.fetchInfo(path);
	}

	public IFileStore getChild(String name) {
		return new MemoryFileStore(path.append(name));
	}

	public String getName() {
		final String name = path.lastSegment();
		return name == null ? "" : name;
	}

	public IFileStore getParent() {
		if (path.segmentCount() == 0)
			return null;
		return new MemoryFileStore(path.removeLastSegments(1));
	}

	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		TREE.mkdir(path, (options & EFS.SHALLOW) == 0);
		return this;
	}

	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		return TREE.openInputStream(path);
	}

	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		return TREE.openOutputStream(path, options);
	}

	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		TREE.putInfo(path, info, options);
	}

	public URI toURI() {
		return MemoryFileSystem.toURI(path);
	}
}