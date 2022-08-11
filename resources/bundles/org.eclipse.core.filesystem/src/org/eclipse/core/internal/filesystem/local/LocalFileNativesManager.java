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
import org.eclipse.core.internal.filesystem.local.nio.*;
import org.eclipse.core.internal.filesystem.local.unix.UnixFileHandler;
import org.eclipse.core.internal.filesystem.local.unix.UnixFileNatives;

/**
 * Dispatches methods backed by native code to the appropriate platform specific
 * implementation depending on a library provided by a fragment. Failing this it tries
 * to use Java 7 NIO/2 API's.
 * <p>
 * Use of native libraries can be disabled by adding -Declipse.filesystem.useNatives=false to VM
 * arguments.
 * <p>
 * Please notice that the native implementation is significantly faster than the non-native one.
 * The BenchFileStore test runs 3.1 times faster on Linux with the native code than without it.
 */
public class LocalFileNativesManager {
	private static NativeHandler HANDLER;
	private static boolean USING_NATIVES;

	static {
		boolean nativesAllowed = Boolean.parseBoolean(System.getProperty("eclipse.filesystem.useNatives", "true")); //$NON-NLS-1$ //$NON-NLS-2$
		if (nativesAllowed && UnixFileNatives.isUsingNatives()) {
			HANDLER = new UnixFileHandler();
			USING_NATIVES = true;
		} else if (nativesAllowed && LocalFileNatives.isUsingNatives()) {
			HANDLER = new LocalFileHandler();
			USING_NATIVES = true;
		} else {
			Set<String> views = FileSystems.getDefault().supportedFileAttributeViews();
			if (views.contains("posix")) { //$NON-NLS-1$
				HANDLER = new PosixHandler();
			} else if (views.contains("dos")) { //$NON-NLS-1$
				HANDLER = new DosHandler();
			} else {
				HANDLER = new DefaultHandler();
			}
		}
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

	public static boolean isUsingNatives() {
		return USING_NATIVES;
	}
}
