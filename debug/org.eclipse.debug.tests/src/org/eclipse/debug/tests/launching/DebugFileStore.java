/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.debug.tests.launching;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Implementation of an in memory file store to test launch configurations on EFS
 */
public class DebugFileStore extends FileStore {

	/**
	 * Output steam for writing a file
	 */
	class DebugOutputStream extends  ByteArrayOutputStream {

		@Override
		public void close() throws IOException {
			super.close();
			DebugFileSystem.getDefault().setContents(toURI(), toByteArray());
		}

	}

	private final URI uri;

	public DebugFileStore(URI id) {
		uri = id;
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		URI[] uris = DebugFileSystem.getDefault().getFileURIs();
		List<String> children = new ArrayList<>();
		IPath me = getPath();
		for (URI id : uris) {
			IPath path = IPath.fromOSString(id.getPath());
			if (path.segmentCount() > 0) {
				if (path.removeLastSegments(1).equals(me)) {
					children.add(path.lastSegment());
				}
			}
		}
		return children.toArray(new String[children.size()]);
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		byte[] contents = DebugFileSystem.getDefault().getContents(toURI());
		FileInfo info = new FileInfo();
		info.setName(getName());
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		if (contents == null) {
			info.setExists(false);
			info.setLength(0L);
		} else {
			info.setExists(true);
			info.setLength(contents.length);
			info.setDirectory(contents == DebugFileSystem.DIRECTORY_BYTES);
			if (info.isDirectory()) {
				info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, true);
			}
		}
		return info;
	}

	@Override
	public IFileStore getChild(String name) {
		try {
			return new DebugFileStore(new URI(getFileSystem().getScheme(), getPath().append(name).toString(), null));
		} catch (URISyntaxException e) {
		}
		return null;
	}

	@Override
	public String getName() {
		IPath path = getPath();
		if (path.segmentCount() > 0) {
			return path.lastSegment();
		}
		return ""; //$NON-NLS-1$
	}

	private IPath getPath() {
		URI me = toURI();
		IPath path = IPath.fromOSString(me.getPath());
		return path;
	}

	@Override
	public IFileStore getParent() {
		IPath path = getPath();
		if (path.segmentCount() > 0) {
			try {
				return new DebugFileStore(new URI(getFileSystem().getScheme(), path.removeLastSegments(1).toString(), null));
			} catch (URISyntaxException e) {
			}
		}
		return null;
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		byte[] contents = DebugFileSystem.getDefault().getContents(toURI());
		if (contents != null) {
			return new ByteArrayInputStream(contents);
		}
		throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.debug.tests", //$NON-NLS-1$
		"File does not exist: " + toURI())); //$NON-NLS-1$
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		return new DebugOutputStream();
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		IFileInfo info = fetchInfo();
		if (info.exists()) {
			if (!info.isDirectory()) {
				throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.debug.tests", //$NON-NLS-1$
				"mkdir failed - file already exists with name: " + toURI())); //$NON-NLS-1$
			}
		} else {
			IFileStore parent = getParent();
			if (parent.fetchInfo().exists()) {
				DebugFileSystem.getDefault().setContents(toURI(), DebugFileSystem.DIRECTORY_BYTES);
			} else if ((options & EFS.SHALLOW) > 0) {
				throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.debug.tests", //$NON-NLS-1$
				"mkdir failed - parent does not exist: " + toURI())); //$NON-NLS-1$
			} else {
				parent.mkdir(EFS.NONE, null);
			}
		}
		return this;
	}

	@Override
	public URI toURI() {
		return uri;
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		DebugFileSystem.getDefault().delete(toURI());
	}
}
