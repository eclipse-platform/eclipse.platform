/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupUtils;

/**
 * An archive in the local file system. Returns instances
 * of <code>ZipEntryStorage</code> as source elements.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ExternalArchiveSourceContainer extends AbstractSourceContainer {

	private boolean fDisposed;
	private boolean fDetectRoots;
	private Set<String> fPotentialRoots;
	private List<String> fRoots = new ArrayList<>();
	private String fArchivePath;
	/**
	 * Unique identifier for the external archive source container type
	 * (value <code>org.eclipse.debug.core.containerType.externalArchive</code>).
	 */
	public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.externalArchive";	 //$NON-NLS-1$

	/**
	 * Creates an archive source container on the archive at the
	 * specified location in the local file system.
	 *
	 * @param archivePath path to the archive in the local file system
	 * @param detectRootPaths whether root container paths should be detected. When
	 *   <code>true</code>, searching is performed relative to a root path
	 *   within the archive based on fully qualified file names. A root
	 *   path is automatically determined for when the first
	 *   successful search is performed. For example, when searching for a file
	 *   named <code>a/b/c.d</code>, and an entry in the archive named
	 *   <code>r/a/b/c.d</code> exists, a root path is set to <code>r</code>.
	 *   When searching for an unqualified file name, root containers are not
	 *   considered.
	 *   When <code>false</code>, searching is performed by
	 *   matching file names as suffixes to the entries in the archive.
	 */
	public ExternalArchiveSourceContainer(String archivePath, boolean detectRootPaths) {
		fArchivePath = archivePath;
		fDetectRoots = detectRootPaths;
	}

	// Suppress resource leak warning. The ZipFile is provided from
	// SourceLookupUtils which take care to close them at some point.
	@SuppressWarnings("resource")
	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		String newname = name.replace('\\', '/');
		ZipFile file = getArchive();
		if (file == null) {
			return EMPTY;
		}
		// NOTE: archive can be closed between get (above) and synchronized block (below)
		synchronized (file) {
			boolean isQualfied = newname.indexOf('/') > 0;
			if (fDetectRoots && isQualfied) {
				ZipEntry entry = searchRoots(file, newname);
				if (entry != null) {
					return new Object[]{new ZipEntryStorage(file, entry)};
				}
			} else {
				// try exact match
				ZipEntry entry = null;
				try {
					entry = file.getEntry(newname);
				} catch (IllegalStateException e) {
					// archive was closed between retrieving and locking
					throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							e.getMessage(), e));
				}
				if (entry != null) {
					// can't be any duplicates if there is an exact match
					return new Object[]{new ZipEntryStorage(file, entry)};
				}
				// search
				Enumeration<? extends ZipEntry> entries = file.entries();
				List<ZipEntryStorage> matches = null;
				try {
					File zipFile = new File(fArchivePath);
					String zipFileCanonical = zipFile.getCanonicalPath();
					while (entries.hasMoreElements()) {
						entry = entries.nextElement();
						String entryName = entry.getName();
						if (entryName.endsWith(newname)) {
							String zipEntryCanonical = (new File(zipFile, entryName)).getCanonicalPath();
							if (!zipEntryCanonical.startsWith(zipFileCanonical + File.separator)) {
								throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), "Invalid path: " + zipEntryCanonical)); //$NON-NLS-1$
							}
							if (isQualfied || entryName.length() == newname.length() || entryName.charAt(entryName.length() - newname.length() - 1) == '/') {
								if (isFindDuplicates()) {
									if (matches == null) {
										matches = new ArrayList<>();
									}
									matches.add(new ZipEntryStorage(file, entry));
								} else {
									return new Object[] {
											new ZipEntryStorage(file, entry) };
								}
							}
						}
					}
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), "Invalid path: " + fArchivePath)); //$NON-NLS-1$
				}
				if (matches != null) {
					return matches.toArray();
				}
			}
		}
		return EMPTY;
	}

	/**
	 * Returns the root path in this archive for the given file name, based
	 * on its type, or <code>null</code> if none. Detects a root if a root has
	 * not yet been detected for the given file type.
	 *
	 * @param file zip file to search in
	 * @param name file name
	 * @return the {@link ZipEntry} with the given name or <code>null</code>
	 * @exception CoreException if an exception occurs while detecting the root
	 */
	private synchronized ZipEntry searchRoots(ZipFile file, String name) throws CoreException {
		if (fDisposed) {
			return null;
		}
		if (fPotentialRoots == null) {
			fPotentialRoots = new HashSet<>();
			fPotentialRoots.add(""); //$NON-NLS-1$
			// all potential roots are the directories
			try {
				Enumeration<? extends ZipEntry> entries = file.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory()) {
						fPotentialRoots.add(entry.getName());
					} else {
						String entryName = entry.getName();
						int index = entryName.lastIndexOf('/');
						while (index > 0) {
							if (fPotentialRoots.add(entryName.substring(0, index + 1))) {
								entryName = entryName.substring(0, index);
								index = entryName.lastIndexOf('/');
							} else {
								break;
							}
						}
					}
				}
			} catch (IllegalStateException e) {
				// archive was closed between retrieving and locking
				throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
					e.getMessage(), e));
			}
		}
		int i = 0;
		while (i < fRoots.size()) {
			String root = fRoots.get(i);
			ZipEntry entry = file.getEntry(root+name);
			if (entry != null) {
				return entry;
			}
			i++;
		}
		if (!fPotentialRoots.isEmpty()) {
			for (String root : fPotentialRoots) {
				ZipEntry entry = file.getEntry(root + name);
				if (entry != null) {
					if (root != null) {
						fRoots.add(root);
						fPotentialRoots.remove(root);
						// remove any roots that begin with the new root, as
						// roots
						// cannot be nested
						Iterator<String> rs = fPotentialRoots.iterator();
						while (rs.hasNext()) {
							String r = rs.next();
							if (r.startsWith(root)) {
								rs.remove();
							}
						}
					}
					return entry;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the archive to search in.
	 * @return the {@link ZipFile} to search in
	 *
	 * @throws CoreException if unable to access the archive
	 */
	private synchronized ZipFile getArchive() throws CoreException {
		if (fDisposed) {
			return null;
		}
		try {
			return SourceLookupUtils.getZipFile(fArchivePath);
		} catch (IOException e) {
			File file = new File(fArchivePath);
			if (file.exists()) {
				abort(MessageFormat.format(SourceLookupMessages.ExternalArchiveSourceContainer_2, new Object[] { fArchivePath }), e);
			} else {
				warn(MessageFormat.format(SourceLookupMessages.ExternalArchiveSourceContainer_1, new Object[] { fArchivePath }), e);
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return fArchivePath;
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	/**
	 * Returns whether root paths are automatically detected in this
	 * archive source container.
	 *
	 * @return whether root paths are automatically detected in this
	 * archive source container
	 */
	public boolean isDetectRoot() {
		return fDetectRoots;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ExternalArchiveSourceContainer &&
			((ExternalArchiveSourceContainer)obj).getName().equals(getName());
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		if (fPotentialRoots != null) {
			fPotentialRoots.clear();
		}
		fRoots.clear();
		fDisposed = true;
	}
}
