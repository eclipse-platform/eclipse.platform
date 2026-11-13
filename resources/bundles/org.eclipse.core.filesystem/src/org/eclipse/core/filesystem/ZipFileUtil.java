/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.ZipInputStream;
import org.eclipse.core.internal.filesystem.zip.ZipFileStore;
import org.eclipse.core.runtime.CoreException;

/**
 * Utility class for zip files.
 *
 * @since 1.11
 */
public class ZipFileUtil {


	public static boolean isInsideOpenZipFile(IFileStore store) {
		return store instanceof ZipFileStore;
	}

	public static boolean isInsideOpenZipFile(URI locationURI) {
		IFileStore store;
		try {
			if (locationURI != null) {
				store = EFS.getStore(locationURI);
			} else {
				return false;
			}
		} catch (CoreException e) {
			return false;
		}
		return isInsideOpenZipFile(store);
	}

	/**
	 * Determines if the given {@link IFileStore} represents an open ZIP file.
	 * This can be used to check if operations on a ZIP file should be allowed or handled differently.
	 *
	 * @param store The file store to check.
	 * @return true if the store is an instance of {@link ZipFileStore}, false otherwise.
	 */
	public static boolean isOpenZipFile(IFileStore store) {
		if (isInsideOpenZipFile(store)) {
			ZipFileStore zipStore = (ZipFileStore) store;
			return zipStore.getPath().isEmpty(); //if path is empty its the root
		}
		return false;
	}

	/**
	 * @see ZipFileUtil#isOpenZipFile(IFileStore)
	 */
	public static boolean isOpenZipFile(URI locationURI) {
		IFileStore store;
		try {
			store = EFS.getStore(locationURI);
		} catch (CoreException e) {
			return false;
		}
		return isOpenZipFile(store);
	}

	public static boolean isNested(URI fileURI) {
		if (fileURI.getScheme().contains("zip")) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	/**
	 * Checks if the provided {@link InputStream} represents a ZIP archive
	 * by attempting to open it as a ZIP archive.
	 * This method throws {@link IOException} if the stream does not represent a valid ZIP archive.
	 *
	 * @param fis The {@link InputStream} of the file to check.
	 * @throws IOException If the stream does not represent a valid ZIP archive
	 *                     or an I/O error occurs during reading from the stream.
	 */
	public static void canZipFileBeOpened(InputStream fis) throws IOException {
		// Use ZipInputStream to try reading the InputStream as a ZIP file
		try (ZipInputStream zipStream = new ZipInputStream(fis)) {
			// Attempt to read the first entry from the zip stream
			if (zipStream.getNextEntry() == null) {
				// If there are no entries, then it might not be a ZIP file or it's empty
				throw new IOException();
			}
			// Successfully reading an entry implies it's likely a valid ZIP file
		}
	}
}
