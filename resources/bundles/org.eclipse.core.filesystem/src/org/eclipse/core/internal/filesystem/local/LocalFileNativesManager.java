/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import java.nio.file.FileSystems;
import java.util.Set;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.nio.DefaultHandler;
import org.eclipse.core.internal.filesystem.local.nio.DosHandler;
import org.eclipse.core.internal.filesystem.local.nio.PosixHandler;
import org.eclipse.core.internal.filesystem.local.unix.UnixFileHandler;
import org.eclipse.core.internal.filesystem.local.unix.UnixFileNatives;
import org.eclipse.core.runtime.Platform;

/**
 * <p>Dispatches methods backed by native code to the appropriate platform specific
 * implementation depending on a library provided by a fragment. Failing this it tries
 * to use Java 7 NIO/2 API's.</p>
 * 
 * <p>Use of native libraries can be disabled by adding -Declipse.filesystem.useNatives=false 
 * to VM arguments.</p>
 * 
 * <p>Please notice that the native implementation is significantly faster than the non-native
 * one. The BenchFileStore test runs 3.1 times faster on Linux with the native code than 
 * without it.</p>
 */
public class LocalFileNativesManager {
	public static final boolean PROPERTY_USE_NATIVE_DEFAULT = true;
	public static final String PROPERTY_USE_NATIVES = "eclipse.filesystem.useNatives"; //$NON-NLS-1$
	private static NativeHandler HANDLER;

	static {
		reset();
	}

	/**
	 * reset the usage of native to the system default
	 */
	public static void reset() {
		setUsingNative(Boolean.parseBoolean(System.getProperty(PROPERTY_USE_NATIVES, String.valueOf(PROPERTY_USE_NATIVE_DEFAULT))));
	}

	/**
	 * Try to set the usage of natives to the provided value
	 * @return <code>true</code> if natives are used as result of this call <code>false</code> otherwhise
	 */
	public static boolean setUsingNative(boolean useNatives) {
		boolean nativesAreUsed;
		boolean isWindowsOS = Platform.OS.isWindows();

		if (useNatives && !isWindowsOS && UnixFileNatives.isUsingNatives()) {
			HANDLER = new UnixFileHandler();
			nativesAreUsed = true;
		} else if (useNatives && isWindowsOS && LocalFileNatives.isUsingNatives()) {
			HANDLER = new LocalFileHandler();
			nativesAreUsed = true;
		} else {
			nativesAreUsed = false;
			Set<String> views = FileSystems.getDefault().supportedFileAttributeViews();
			if (views.contains("posix")) { //$NON-NLS-1$
				HANDLER = new PosixHandler();
			} else if (views.contains("dos")) { //$NON-NLS-1$
				HANDLER = new DosHandler();
			} else {
				HANDLER = new DefaultHandler();
			}
		}
		return nativesAreUsed;
	}

	public static int getSupportedAttributes() {
		return HANDLER.getSupportedAttributes();
	}

	public static FileInfo fetchFileInfo(String fileName) {
		return HANDLER.fetchFileInfo(fileName);
	}

	public static boolean putFileInfo(String fileName, IFileInfo info, int options) {
		return HANDLER.putFileInfo(fileName, info, options);
	}

}
