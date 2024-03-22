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
package org.eclipse.core.internal.filesystem;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.*;

/**
 * A null file store represents a file whose location is unknown,
 * such as a location based on an undefined variable.  This store
 * acts much like /dev/null on *nix: writes are ignored, reads return
 * empty streams, and modifications such as delete, mkdir, will fail.
 */
public class NullFileStore extends FileStore {
	private final IPath path;

	/**
	 * Creates a null file store
	 * 
	 * @param path The path of the file in this store
	 */
	public NullFileStore(IPath path) {
		Assert.isNotNull(path);
		this.path = path.makeAbsolute();
	}

	@Override
	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) {
		return EMPTY_FILE_INFO_ARRAY;
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) {
		return EMPTY_STRING_ARRAY;
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		//super implementation will always fail
		super.delete(options, monitor);
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
		FileInfo result = new FileInfo(getName());
		result.setExists(false);
		return result;
	}

	@Override
	public IFileStore getChild(String name) {
		return new NullFileStore(path.append(name));
	}

	@Override
	public IFileSystem getFileSystem() {
		return NullFileSystem.getInstance();
	}

	@Override
	public String getName() {
		return String.valueOf(path.lastSegment());
	}

	@Override
	public IFileStore getParent() {
		return path.segmentCount() == 0 ? null : new NullFileStore(path.removeLastSegments(1));
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		//super implementation will always fail
		return super.mkdir(options, monitor);
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) {
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) {
		return new OutputStream() {
			@Override
			public void write(int b) {
				//do nothing
			}
		};
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		//super implementation will always fail
		super.putInfo(info, options, monitor);
	}

	@Override
	public String toString() {
		return path.toString();
	}

	@Override
	public URI toURI() {
		try {
			return new URI(EFS.SCHEME_NULL, null, path.isEmpty() ? "/" : path.toString(), null); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			//should never happen
			Policy.log(IStatus.ERROR, "Invalid URI", e); //$NON-NLS-1$
			return null;
		}
	}
}
