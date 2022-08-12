/*******************************************************************************
 *  Copyright (c) 2011, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Serge Beauchamp
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.filesystem.provider.FileTree;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class Test342968FileSystem extends FileSystem {

	public Test342968FileSystem() {
	}

	@Override
	public IFileStore getStore(URI uri) {
		try {
			return new Test342968FileStore(EFS.getStore(new URI(EFS.getLocalFileSystem().getScheme(), uri.getHost(), uri.getPath(), uri.getFragment())));
		} catch (CoreException | URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean canDelete() {
		return true;
	}

	@Override
	public IFileTree fetchFileTree(IFileStore root, IProgressMonitor monitor) {
		Test342968FileTree tree = new Test342968FileTree(root);
		return tree;
	}

	public static URI getTestUriFor(URI baseUri) {
		try {
			return new URI("test342968", baseUri.getHost(), baseUri.getPath(), baseUri.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}

class Test342968FileStore extends FileStore {

	private final IFileStore baseStore;

	public Test342968FileStore(IFileStore baseStore) {
		this.baseStore = baseStore;
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.childNames(options, monitor);
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.fetchInfo(options, monitor);
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		baseStore.delete(options, monitor);
	}

	@Override
	public IFileStore getChild(String name) {
		return new Test342968FileStore(baseStore.getChild(name));
	}

	@Override
	public String getName() {
		return baseStore.getName();
	}

	@Override
	public IFileStore getParent() {
		return new Test342968FileStore(baseStore.getParent());
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.openInputStream(options, monitor);
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.openOutputStream(options, monitor);
	}

	@Override
	public URI toURI() {
		URI baseUri = baseStore.toURI();
		return Test342968FileSystem.getTestUriFor(baseUri);
	}

	@Override
	public File toLocalFile(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.toLocalFile(options, monitor);
	}

}

class Test342968FileTree extends FileTree {

	public Test342968FileTree(IFileStore root) {
		super(root);
	}

	@Override
	public IFileInfo[] getChildInfos(IFileStore store) {
		if (store instanceof Test342968FileStore) {
			try {
				return store.childInfos(EFS.NONE, null);
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		return new IFileInfo[0];
	}

	@Override
	public IFileInfo getFileInfo(IFileStore store) {
		if (store instanceof Test342968FileStore) {
			return store.fetchInfo();
		}
		return null;
	}

	@Override
	public IFileStore[] getChildStores(IFileStore store) {
		if (store instanceof Test342968FileStore) {
			try {
				return store.childStores(EFS.NONE, null);
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		return new IFileStore[0];
	}

}
