/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.history;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;

public class FileSystemFileRevision extends FileRevision {

	java.io.File remoteFile;

	public FileSystemFileRevision(java.io.File file) {
		this.remoteFile = file;
	}

	@Override
	public String getName() {
		return remoteFile.getName();
	}

	@Override
	public long getTimestamp() {
		return remoteFile.lastModified();
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) {
		return new IStorage() {

			@Override
			public InputStream getContents() {
				try {
					return new FileInputStream(remoteFile);
				} catch (FileNotFoundException e) {
					// ignore
				}

				return null;
			}

			@Override
			public IPath getFullPath() {
				return IPath.fromOSString(remoteFile.getAbsolutePath());
			}

			@Override
			public String getName() {
				return remoteFile.getName();
			}

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}

		};
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	@Override
	public IFileRevision withAllProperties(IProgressMonitor monitor) {
		return null;
	}

	@Override
	public String getContentIdentifier() {
		return "[File System Revision]"; //$NON-NLS-1$
	}

}
