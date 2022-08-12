/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [184534] get attributes from native lib
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import java.net.URL;
import java.util.Enumeration;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

abstract class LocalFileNatives {
	private static boolean hasNatives = false;
	private static boolean isUnicode = false;
	private static int nativeAttributes = -1;

	/** instance of this library */
	// The name convention is to use the plugin version at the time the library is changed.
	private static final String LIBRARY_NAME = "localfile_1_0_0"; //$NON-NLS-1$

	static {
		try {
			System.loadLibrary(LIBRARY_NAME);
			hasNatives = true;
			isUnicode = internalIsUnicode();
			try {
				nativeAttributes = nativeAttributes();
			} catch (UnsatisfiedLinkError e) {
				// older native implementations did not support this
				// call, so we need to handle the error silently
			}
		} catch (UnsatisfiedLinkError e) {
			if (isLibraryPresent())
				logMissingNativeLibrary(e);
		}
	}

	private static boolean isLibraryPresent() {
		String libName = System.mapLibraryName(LIBRARY_NAME);
		Enumeration<URL> entries = FileSystemAccess.findEntries("/", libName, true); //$NON-NLS-1$
		return entries != null && entries.hasMoreElements();
	}

	private static void logMissingNativeLibrary(UnsatisfiedLinkError e) {
		String libName = System.mapLibraryName(LIBRARY_NAME);
		String message = NLS.bind(Messages.couldNotLoadLibrary, libName);
		Policy.log(IStatus.INFO, message, e);
	}

	/**
	 * Return the bit-mask of EFS attributes that this native
	 * file system implementation supports.
	 * <p>
	 * This is an optional method: if it has not been compiled
	 * into the native library, the client must catch the
	 * resulting UnsatisfiedLinkError and handle attributes
	 * as known by older version libraries.
	 * </p>
	 * @see IFileSystem#attributes()
	 * @return an integer bit mask of attributes.
	 */
	private static final native int nativeAttributes();

	/**
	 * Return the value that the native library thinks
	 * {@link IFileSystem#attributes()} should return.
	 *
	 * Returns -1 when the native library has not been
	 * loaded, or is a version that does not support
	 * this investigation method yet.
	 *
	 * @return an positive value that is a bit-mask
	 *    suitable for use in {@link IFileSystem#attributes},
	 *    or -1 if native attributes are not available.
	 */
	public static int attributes() {
		return nativeAttributes;
	}

	/**
	 * Copies file attributes from source to destination. The copyLastModified attribute
	 * indicates whether the lastModified attribute should be copied.
	 * @param source
	 * @param destination
	 * @param copyLastModified
	 * @return <code>true</code> for success, and <code>false</code> otherwise.
	 */
	public static boolean copyAttributes(String source, String destination, boolean copyLastModified) {
		if (hasNatives)
			// Note that support for copying last modified info is not implemented on Windows
			return isUnicode ? internalCopyAttributesW(Convert.toPlatformChars(source), Convert.toPlatformChars(destination), copyLastModified) : internalCopyAttributes(Convert.toPlatformBytes(source), Convert.toPlatformBytes(destination), copyLastModified);
		return false; // not supported
	}

	/**
	 * @param fileName
	 * @return The file info
	 */
	public static FileInfo fetchFileInfo(String fileName) {
		FileInfo info = new FileInfo();
		if (isUnicode)
			internalGetFileInfoW(Convert.toPlatformChars(fileName), info);
		else
			internalGetFileInfo(Convert.toPlatformBytes(fileName), info);
		return info;
	}

	/**
	 * Copies file attributes from source to destination. The copyLastModified attribute
	 * indicates whether the lastModified attribute should be copied.
	 */
	private static final native boolean internalCopyAttributes(byte[] source, byte[] destination, boolean copyLastModified);

	/**
	 * Copies file attributes from source to destination. The copyLastModified attribute
	 * indicates whether the lastModified attribute should be copied (Unicode
	 * version - should not be called if <code>isUnicode</code> is
	 * <code>false</code>).
	 */
	private static final native boolean internalCopyAttributesW(char[] source, char[] destination, boolean copyLastModified);

	/**
	 * Stores the file information for the specified filename in the supplied file
	 * information object.  This avoids multiple JNI calls.
	 */
	private static final native boolean internalGetFileInfo(byte[] fileName, IFileInfo info);

	/**
	 * Stores the file information for the specified filename in the supplied file
	 * information object.  This avoids multiple JNI calls.
	 */
	private static final native boolean internalGetFileInfoW(char[] fileName, IFileInfo info);

	/**
	 * Returns <code>true</code> if the underlying file system API supports Unicode,
	 * <code>false</code> otherwise.
	 */
	private static final native boolean internalIsUnicode();

	/** Set the extended attributes specified in the IResource attribute. Only attributes
	 * that the platform supports will be set. */
	private static final native boolean internalSetFileInfo(byte[] fileName, IFileInfo attribute);

	/** Set the extended attributes specified in the IResource attribute object. Only
	 * attributes that the platform supports will be set. (Unicode version - should not
	 * be called if <code>isUnicode</code> is <code>false</code>). */
	private static final native boolean internalSetFileInfoW(char[] fileName, IFileInfo attribute, int options);

	/**
	 * @param fileName
	 * @param info
	 * @param options
	 */
	public static boolean putFileInfo(String fileName, IFileInfo info, int options) {
		if (isUnicode)
			return internalSetFileInfoW(Convert.toPlatformChars(fileName), info, options);
		return internalSetFileInfo(Convert.toPlatformBytes(fileName), info);
	}

	/**
	 * Return <code>true</code> if we have found the core library and are using it for
	 * our file-system calls, and <code>false</code> otherwise.
	 * @return <code>true</code> if native library is available, and <code>false</code>
	 * otherwise.
	 */
	public static boolean isUsingNatives() {
		return hasNatives;
	}
}
