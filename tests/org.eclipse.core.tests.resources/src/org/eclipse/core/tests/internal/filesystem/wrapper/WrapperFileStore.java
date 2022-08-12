/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.internal.filesystem.wrapper;

import java.io.*;
import java.net.URI;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.*;

/**
 * A simple file system implementation that acts as a wrapper around the
 * local file system.
 */
public class WrapperFileStore extends FileStore {
	private final IFileStore baseStore;

	public WrapperFileStore(IFileStore baseStore) {
		this.baseStore = baseStore;
	}

	public static IFileStore newInstance(Class<? extends WrapperFileStore> clazz, IFileStore baseStore) {
		try {
			return clazz.getConstructor(IFileStore.class).newInstance(baseStore);
		} catch (Exception e) {
			// Test infrastructure failure...
			throw new Error(e);
		}
	}

	protected IFileStore createNewWrappedStore(IFileStore store) {
		return newInstance(getClass(), store);
	}

	@Override
	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.childInfos(options, monitor);
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.childNames(options, monitor);
	}

	@Override
	public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
		IFileStore[] childStores = baseStore.childStores(options, monitor);
		for (int i = 0; i < childStores.length; i++) {
			// replace ordinary file store with wrapper version
			childStores[i] = createNewWrappedStore(childStores[i]);
		}
		return childStores;
	}

	@Override
	public void copy(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		baseStore.copy(destination, options, monitor);
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		baseStore.delete(options, monitor);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof WrapperFileStore)) {
			return false;
		}
		return baseStore.equals(((WrapperFileStore) obj).baseStore);
	}

	@Override
	public IFileInfo fetchInfo() {
		return baseStore.fetchInfo();
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.fetchInfo(options, monitor);
	}

	public IFileStore getBaseStore() {
		return baseStore;
	}

	@Deprecated
	@Override
	public IFileStore getChild(IPath path) {
		return createNewWrappedStore(baseStore.getChild(path));
	}

	@Override
	public IFileStore getFileStore(IPath path) {
		return createNewWrappedStore(baseStore.getFileStore(path));
	}

	@Override
	public IFileStore getChild(String name) {
		return createNewWrappedStore(baseStore.getChild(name));
	}

	@Override
	public IFileSystem getFileSystem() {
		return WrapperFileSystem.getInstance();
	}

	@Override
	public String getName() {
		return baseStore.getName();
	}

	@Override
	public IFileStore getParent() {
		IFileStore baseParent = baseStore.getParent();
		return baseParent == null ? null : createNewWrappedStore(baseParent);
	}

	@Override
	public int hashCode() {
		return baseStore.hashCode();
	}

	@Override
	public boolean isParentOf(IFileStore other) {
		if (!(other instanceof WrapperFileStore)) {
			return false;
		}
		IFileStore otherBaseStore = ((WrapperFileStore) other).baseStore;
		return baseStore.isParentOf(otherBaseStore);
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		baseStore.mkdir(options, monitor);
		return this;
	}

	@Override
	public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		if (destination instanceof WrapperFileStore) {
			destination = ((WrapperFileStore) destination).baseStore;
		}
		baseStore.move(destination, options, monitor);
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
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		baseStore.putInfo(info, options, monitor);
	}

	@Override
	public File toLocalFile(int options, IProgressMonitor monitor) throws CoreException {
		return baseStore.toLocalFile(options, monitor);
	}

	@Override
	public URI toURI() {
		return WrapperFileSystem.getWrappedURI(baseStore.toURI());
	}
}
