/*******************************************************************************
 * Copyright (c) 2024, 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *  Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

class OnCloseWritingFileStore extends FileStore {

	private final Path filePath;

	public OnCloseWritingFileStore(Path file) {
		this.filePath = file;
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) {
		return new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				Files.write(filePath, this.toByteArray());
			}
		};
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		if ((options & EFS.SET_LAST_MODIFIED) != 0) {
			FileTime lastModified = FileTime.fromMillis(info.getLastModified());
			try {
				Files.setLastModifiedTime(filePath, lastModified);
			} catch (IOException e) {
				if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
					throw new CoreException(new Status(IStatus.ERROR, "CopyBugFileStore", "File does not exist", e));
				}
				throw new CoreException(new Status(IStatus.ERROR, "CopyBugFileStore", "Failed to set attribute", e));
			}
		}
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		FileInfo info = new FileInfo();
		try {
			info.setLastModified(Files.getLastModifiedTime(filePath).toMillis());
			info.setLength(Files.size(filePath));
			info.setExists(Files.exists(filePath));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, "TestFileStore", "Failed to fetch file info", e));
		}
		return info;
	}

	@Override
	public String getName() {
		return filePath.getFileName().toString();
	}

	@Override
	public IFileStore getParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IFileStore getChild(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	public URI toURI() {
		throw new UnsupportedOperationException();
	}
}