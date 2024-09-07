/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup.containers;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;

/**
 * Storage implementation for zip entries.
 * <p>
 * This class may be instantiated.
 * </p>
 * @see IStorage
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ZipEntryStorage extends PlatformObject implements IStorage {

	/**
	 * Zip file associated with zip entry
	 */
	private ZipFile fArchive;

	/**
	 * Zip entry
	 */
	private ZipEntry fZipEntry;

	/**
	 * Constructs a new storage implementation for the
	 * given zip entry in the specified zip file
	 *
	 * @param archive zip file
	 * @param entry zip entry
	 */
	public ZipEntryStorage(ZipFile archive, ZipEntry entry) {
		setArchive(archive);
		setZipEntry(entry);
	}

	@Override
	public InputStream getContents() throws CoreException {
		try {
			return fArchive.getInputStream(getZipEntry());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, SourceLookupMessages.ZipEntryStorage_0, e));
		}
	}

	@Override
	public IPath getFullPath() {
		return IPath.fromOSString(fArchive.getName()).append(getZipEntry().getName());
	}

	@Override
	public String getName() {
		int index = getZipEntry().getName().lastIndexOf('\\');
		if (index == -1) {
			index = getZipEntry().getName().lastIndexOf('/');
		}
		if (index == -1) {
			return getZipEntry().getName();
		}
		return getZipEntry().getName().substring(index + 1);
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	/**
	 * Sets the archive containing the zip entry.
	 *
	 * @param archive a zip file
	 */
	private void setArchive(ZipFile archive) {
		fArchive = archive;
	}

	/**
	 * Returns the archive containing the zip entry.
	 *
	 * @return zip file
	 * @deprecated Granting free access to the backing zip file archive is
	 *             dangerous because a caller could close it prematurely and
	 *             thus break all subsequent usages. Existing callers should use
	 *             derived methods like {@link #getArchivePath()} or
	 *             {@link #getContents()} instead, if possible.
	 */
	@Deprecated(forRemoval = true, since = "3.22 (removal in 2026-12 or later)")
	public ZipFile getArchive() {
		return fArchive;
	}

	/**
	 * Returns the path of the archive containing the zip entry in the
	 * file-system.
	 *
	 * @return the zip file's file-system path
	 * @since 3.22
	 */
	public Path getArchivePath() {
		return Path.of(fArchive.getName());
	}

	/**
	 * Sets the entry that contains the source.
	 *
	 * @param entry the entry that contains the source
	 */
	private void setZipEntry(ZipEntry entry) {
		fZipEntry = entry;
	}

	/**
	 * Returns the entry that contains the source
	 *
	 * @return zip entry
	 */
	public ZipEntry getZipEntry() {
		return fZipEntry;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof ZipEntryStorage other //
				&& fArchive.equals(other.fArchive) //
				&& getZipEntry().getName().equals(other.getZipEntry().getName());
	}

	@Override
	public int hashCode() {
		return getZipEntry().getName().hashCode();
	}
}
