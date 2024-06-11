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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import org.eclipse.core.internal.filesystem.zip.ZipFileStore;
import org.eclipse.core.runtime.CoreException;

/**
 * Utility class to determine if a file is an archive based on file header information.
 * This class checks for known file signatures to identify if a given file is a ZIP archive
 * or a format based on ZIP, such as EPUB, JAR, ODF, and OOXML.
 *
 * @since 1.11
 */
public class ZipFileUtil {

	// Initializes known archive file signatures from Wikipedia's list of file signatures in the following order:
	// 1. Standard ZIP file, 2. Empty archive, 3.  Spanned archive
	// (https://en.wikipedia.org/wiki/List_of_file_signatures)
	private static final Set<Integer> ARCHIVE_FILE_SIGNATURES = Set.of(0x504B0304, 0x504B0506, 0x504B0708);

	/**
	 * Determines if the given {@link IFileStore} represents an open ZIP file.
	 * This can be used to check if operations on a ZIP file should be allowed or handled differently.
	 *
	 * @param store The file store to check.
	 * @return true if the store is an instance of {@link ZipFileStore}, false otherwise.
	 */
	public static boolean isInsideOpenZipFile(IFileStore store) {
		return store instanceof ZipFileStore;
	}

	public static boolean isInsideOpenZipFile(URI locationURI) {
		IFileStore store;
		try {
			store = EFS.getStore(locationURI);
		} catch (CoreException e) {
			return false;
		}
		return isInsideOpenZipFile(store);
	}

	//TODO Implement this method
	public static boolean isOpenZipFile(IFileStore store) {
		if (isInsideOpenZipFile(store)) {
			ZipFileStore zipStore = (ZipFileStore) store;
			return zipStore.getPath().isEmpty(); //if path is empty its the root
		}
		return false;
	}

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
	 * by reading its first four bytes and comparing them against known ZIP file signatures.
	 * This method throws {@link IOException} if the file signature does not match any known ZIP archive signatures.
	 *
	 * @param fis The {@link InputStream} of the file to check.
	 * @throws IOException If the file signature does not match known ZIP archive signatures
	 *                     or an I/O error occurs during reading from the stream.
	 */
	public static void checkFileForZipHeader(InputStream fis) throws IOException {
		byte[] bytes = new byte[4];
		if (fis.read(bytes) == bytes.length) {
			ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
			int header = buffer.getInt();

			if (!ARCHIVE_FILE_SIGNATURES.contains(header)) {
				throw new IOException("Invalid archive file signature."); // Throws IOException if header is not recognized //$NON-NLS-1$
			}
		} else {
			// Handle the case where not enough bytes are read
			throw new IOException("Could not read enough data to check ZIP file header."); //$NON-NLS-1$
		}
	}
}
