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

import java.io.File;
import java.io.IOException;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;

/**
 * An instance of this class represents a directory on disk where cached
 * files can be stored. Files in the cache expire on VM exit.
 */
public class FileCache {
	private static final String CACHE_DIR_NAME = "filecache";//$NON-NLS-1$

	/**
	 * Cached constant indicating if the current OS is Mac OSX
	 */
	static final boolean MACOSX = FileCache.getOS().equals(Constants.OS_MACOSX);

	/**
	 * The singleton file cache instance.
	 * Lazy initialization holder class idiom for static fields
	 */
	private static class FileCacheHolder {
		static final FileCache instance = createFileCache();
		static CoreException ce;

		private static FileCache createFileCache() {
			try {
				return new FileCache();
			} catch (CoreException e) {
				ce = e;
				return null;
			}
		}
	}

	private File cacheDir;

	/**
	 * Public accessor to obtain the singleton file cache instance,
	 * creating the cache lazily if necessary.
	 * @return The file cache instance
	 * @throws CoreException If the file cache could not be created
	 */
	public static FileCache getCache() throws CoreException {
		FileCache instance = FileCacheHolder.instance;
		if (instance == null)
			throw FileCacheHolder.ce;
		return instance;
	}

	/**
	 * Creates a new file cache.
	 *
	 * @throws CoreException If the file cache could not be created
	 */
	FileCache() throws CoreException {
		IPath location = FileSystemAccess.getCacheLocation();
		File cacheParent = new File(location.toFile(), CACHE_DIR_NAME);
		cleanOldCache(cacheParent);
		cacheParent.mkdirs();
		//make sure we have a unique non-existing cache directory
		cacheDir = getUniqueDirectory(cacheParent, true);
	}

	/**
	 * Implements {@link FileStore#toLocalFile(int, IProgressMonitor)}
	 * @param source source data to cache on disk
	 * @param monitor
	 *            monitor to indicate progress and receive cancellation
	 * @return The cached file
	 * @throws CoreException on errors using the source filestore or writing the cache file
	 */
	public java.io.File cache(IFileStore source, IProgressMonitor monitor) throws CoreException {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(Messages.copying, toString()), 3);
			IFileInfo myInfo = source.fetchInfo(EFS.NONE, subMonitor.newChild(1));
			if (!myInfo.exists())
				return new File(cacheDir, "Non-Existent-" + System.currentTimeMillis()); //$NON-NLS-1$
			File result;
			if (myInfo.isDirectory()) {
				result = getUniqueDirectory(cacheDir, false);
			} else {
				result = File.createTempFile(source.getFileSystem().getScheme(), "efs", cacheDir); //$NON-NLS-1$
			}
			subMonitor.worked(1);
			IFileStore resultStore = new LocalFile(result);
			source.copy(resultStore, EFS.OVERWRITE, subMonitor.newChild(1));
			result.deleteOnExit();
			return result;
		} catch (IOException e) {
			Policy.error(EFS.ERROR_WRITE, NLS.bind(Messages.couldNotWrite, toString()));
			return null; // Can't get here
		}
	}

	/**
	 * Performs initial cleanup of any old cached state left over from previous
	 * sessions.
	 */
	private void cleanOldCache(File cacheParent) throws CoreException {
		//clear any old cache - this could be moved to a background thread
		if (MACOSX) {
			// fix for bug 323833: clear the immutable flag before old cache deletion on MacOS
			clearImmutableFlag(cacheParent);
		}
		new LocalFile(cacheParent).delete(EFS.NONE, null);
	}

	private void clearImmutableFlag(File target) {
		if (!target.exists()) {
			return;
		}
		if (target.isDirectory()) {
			File[] children = target.listFiles();
			if (children != null) {
				for (File element : children) {
					clearImmutableFlag(element);
				}
			}
		} else {
			LocalFile lfile = new LocalFile(target);
			try {
				IFileInfo info = lfile.fetchInfo(EFS.NONE, null);
				if (info.getAttribute(EFS.ATTRIBUTE_IMMUTABLE)) {
					info.setAttribute(EFS.ATTRIBUTE_IMMUTABLE, false);
					lfile.putInfo(info, EFS.SET_ATTRIBUTES, null);
				}
			} catch (CoreException e) {
				// ignore and continue since failed deletions will be reported by LocalFile.delete()
			}
		}
	}

	/**
	 * Returns the current OS.  This is equivalent to Platform.getOS(), but
	 * is tolerant of the platform runtime not being present.
	 */
	static String getOS() {
		return System.getProperty("osgi.os", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns a new unique directory in the given parent directory.
	 *
	 * @param parent
	 * @param create <code>true</code> if the directory should
	 * be created, and false otherwise.
	 * @return The unique directory
	 */
	private File getUniqueDirectory(File parent, boolean create) {
		File dir;
		long i = 0;
		//find an unused directory name
		do {
			dir = new File(parent, Long.toString(System.currentTimeMillis() + i++));
		} while (dir.exists());
		if (create)
			dir.mkdir();
		return dir;
	}
}
