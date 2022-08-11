/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.eclipse.core.internal.filesystem.zip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.*;

/**
 * File store implementation representing a file or directory inside
 * a zip file.
 */
public class ZipFileStore extends FileStore {
	/**
	 * The path of this store within the zip file.
	 */
	private IPath path;

	/**
	 * The file store that represents the actual zip file.
	 */
	private IFileStore rootStore;

	/**
	 * Creates a new zip file store.
	 * @param rootStore
	 * @param path
	 */
	public ZipFileStore(IFileStore rootStore, IPath path) {
		this.rootStore = rootStore;
		this.path = path.makeRelative();
	}

	private ZipEntry[] childEntries(IProgressMonitor monitor) throws CoreException {
		HashMap<String, ZipEntry> entries = new HashMap<>();
		String myName = path.toString();
		try (ZipInputStream in = new ZipInputStream(rootStore.openInputStream(EFS.NONE, monitor))) {
			ZipEntry current;
			while ((current = in.getNextEntry()) != null) {
				final String currentPath = current.getName();
				if (isParent(myName, currentPath)) {
					entries.put(currentPath, current);
				} else if (isAncestor(myName, currentPath)) {
					int myNameLength = myName.length() + 1;
					int nameEnd = currentPath.indexOf('/', myNameLength);
					String dirName = nameEnd == -1 ? currentPath : currentPath.substring(0, nameEnd + 1);
					if (!entries.containsKey(dirName))
						entries.put(dirName, new ZipEntry(dirName));
				}
			}
		} catch (IOException e) {
			throw new CoreException(Status.error("Could not read file: " + rootStore.toString(), e));
		}
		return entries.values().toArray(new ZipEntry[entries.size()]);
	}

	@Override
	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
		ZipEntry[] entries = childEntries(monitor);
		int entryCount = entries.length;
		IFileInfo[] infos = new IFileInfo[entryCount];
		for (int i = 0; i < entryCount; i++) {
			infos[i] = convertZipEntryToFileInfo(entries[i]);
		}
		return infos;
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		ZipEntry[] entries = childEntries(monitor);
		int entryCount = entries.length;
		String[] names = new String[entryCount];
		for (int i = 0; i < entryCount; i++) {
			names[i] = computeName(entries[i]);
		}
		return names;
	}

	/**
	 * Computes the simple file name for a given zip entry.
	 */
	private String computeName(ZipEntry entry) {
		//the entry name is a relative path, with an optional trailing separator
		//We need to strip off the trailing slash, and then take everything after the 
		//last separator as the name
		String name = entry.getName();
		int end = name.length() - 1;
		if (name.charAt(end) == '/') {
			end--;
		}
		return name.substring(name.lastIndexOf('/', end) + 1, end + 1);
	}

	/**
	 * Creates a file info object corresponding to a given zip entry
	 * 
	 * @param entry the zip entry
	 * @return The file info for a zip entry
	 */
	private IFileInfo convertZipEntryToFileInfo(ZipEntry entry) {
		FileInfo info = new FileInfo(computeName(entry));
		info.setLastModified(entry.getTime());
		info.setExists(true);
		info.setDirectory(entry.isDirectory());
		info.setLength(entry.getSize());
		return info;
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		try (ZipInputStream in = new ZipInputStream(rootStore.openInputStream(EFS.NONE, monitor))) {
			String myPath = path.toString();
			ZipEntry current;
			while ((current = in.getNextEntry()) != null) {
				String currentPath = current.getName();
				if (myPath.equals(currentPath)) {
					return convertZipEntryToFileInfo(current);
				}
				//directories don't always have their own entry, but it is implied by the existence of a child
				if (isAncestor(myPath, currentPath)) {
					return createDirectoryInfo(getName());
				}
			}
		} catch (IOException e) {
			throw new CoreException(Status.error("Could not read file: " + rootStore.toString(), e));
		}
		//does not exist
		return new FileInfo(getName());
	}

	/**
	 * @return A directory info for this file store
	 */
	private IFileInfo createDirectoryInfo(String name) {
		FileInfo result = new FileInfo(name);
		result.setExists(true);
		result.setDirectory(true);
		return result;
	}

	/**
	 * Finds the zip entry with the given name in this zip file.  Returns the
	 * entry and leaves the input stream open positioned at the beginning of
	 * the bytes of that entry.  Returns null if the entry could not be found.
	 */
	private ZipEntry findEntry(String name, ZipInputStream in) throws IOException {
		ZipEntry current;
		while ((current = in.getNextEntry()) != null) {
			if (current.getName().equals(name)) {
				return current;
			}
		}
		return null;
	}

	@Override
	public IFileStore getChild(String name) {
		return new ZipFileStore(rootStore, path.append(name));
	}

	@Override
	public String getName() {
		String name = path.lastSegment();
		return name == null ? "" : name; //$NON-NLS-1$
	}

	@Override
	public IFileStore getParent() {
		if (path.segmentCount() > 0) {
			return new ZipFileStore(rootStore, path.removeLastSegments(1));
		}
		//the root entry has no parent
		return null;
	}

	/**
	 * Returns whether ancestor is a parent of child.
	 * @param ancestor the potential ancestor
	 * @param child the potential child
	 * @return <code>true</code> or <code>false</code>
	 */
	private boolean isAncestor(String ancestor, String child) {
		//children will start with myName and have no child path
		int ancestorLength = ancestor.length();
		if (ancestorLength == 0) {
			return true;
		}
		return child.startsWith(ancestor) && child.length() > ancestorLength && child.charAt(ancestorLength) == '/';
	}

	/**
	 * Returns whether parent is the immediate parent of child.
	 * @param parent the potential parent
	 * @param child the potential child
	 * @return <code>true</code> or <code>false</code>
	 */
	private boolean isParent(String parent, String child) {
		//children will start with myName and have no child path
		int chop = parent.length() + 1;
		return child.startsWith(parent) && child.length() > chop && child.substring(chop).indexOf('/') == -1;
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		try (ZipInputStream in = new ZipInputStream(rootStore.openInputStream(EFS.NONE, monitor))) {
			ZipEntry entry = findEntry(path.toString(), in);
			if (entry == null) {
				throw new CoreException(Status.error("File not found: " + rootStore.toString()));
			}
			if (entry.isDirectory()) {
				throw new CoreException(Status.error("Resource is not a file: " + rootStore.toString()));
			}
			return in;
		} catch (IOException e) {
			throw new CoreException(Status.error("Could not read file: " + rootStore.toString(), e));
		}
	}

	@Override
	public URI toURI() {
		try {
			return new URI(ZipFileSystem.SCHEME_ZIP, null, path.makeAbsolute().toString(), rootStore.toURI().toString(), null);
		} catch (URISyntaxException e) {
			//should not happen
			throw new RuntimeException(e);
		}
	}
}
