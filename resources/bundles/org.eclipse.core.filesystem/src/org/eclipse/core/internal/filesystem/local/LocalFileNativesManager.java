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
import org.eclipse.core.internal.filesystem.local.ffm.LinuxFfmHandler;
import org.eclipse.core.internal.filesystem.local.linux.LinuxFileHandler;
import org.eclipse.core.internal.filesystem.local.linux.LinuxFileNatives;
import org.eclipse.core.internal.filesystem.local.nio.DefaultHandler;
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
 *
 * <p>On Linux the Foreign Function &amp; Memory API is preferred over the native libraries,
 * because it needs no platform specific fragment and therefore offers bulk file info
 * fetching on every architecture. Set -Declipse.filesystem.useFfm=false to fall back to
 * the JNI implementations.</p>
 */
public class LocalFileNativesManager {
	public static final boolean PROPERTY_USE_NATIVE_DEFAULT = true;
	public static final boolean PROPERTY_USE_FAST_LINUX_NATIVES_DEFAULT = true;
	public static final boolean PROPERTY_USE_FFM_DEFAULT = true;
	public static final String PROPERTY_USE_NATIVES = "eclipse.filesystem.useNatives"; //$NON-NLS-1$
	public static final String PROPERTY_USE_FAST_LINUX_NATIVES = "eclipse.filesystem.useFastLinuxNatives"; //$NON-NLS-1$
	public static final String PROPERTY_USE_FFM = "eclipse.filesystem.useFfm"; //$NON-NLS-1$
	private static NativeHandler HANDLER;

	static {
		reset();
	}

	/**
	 * reset the usage of native to the system default
	 */
	public static void reset() {
		setUsingNative(Boolean.parseBoolean(System.getProperty(PROPERTY_USE_NATIVES, String.valueOf(PROPERTY_USE_NATIVE_DEFAULT))),
				Boolean.parseBoolean(System.getProperty(PROPERTY_USE_FAST_LINUX_NATIVES, String.valueOf(PROPERTY_USE_FAST_LINUX_NATIVES_DEFAULT))),
				Boolean.parseBoolean(System.getProperty(PROPERTY_USE_FFM, String.valueOf(PROPERTY_USE_FFM_DEFAULT))));
	}

	/**
	 * Try to set the usage of the native libraries to the provided value, without
	 * using the Foreign Function &amp; Memory API.
	 *
	 * @return <code>true</code> if natives are used as result of this call <code>false</code> otherwise
	 */
	public static boolean setUsingNative(boolean useNatives, boolean useFastLinuxNatives) {
		return setUsingNative(useNatives, useFastLinuxNatives, false);
	}

	/**
	 * Try to set the usage of natives to the provided value
	 * @return <code>true</code> if natives are used as result of this call <code>false</code> otherwise
	 */
	public static boolean setUsingNative(boolean useNatives, boolean useFastLinuxNatives, boolean useFfm) {
		boolean nativesAreUsed;
		if (useNatives && useFfm && LinuxFfmHandler.isAvailable()) {
			// Bulk file info fetching without a platform specific library, so this works on
			// every Linux architecture rather than only on x86_64.
			HANDLER = new LinuxFfmHandler();
			nativesAreUsed = true;
		} else if (useNatives && useFastLinuxNatives && Platform.OS.isLinux() && Platform.ARCH_X86_64.equals(Platform.getOSArch()) && LinuxFileNatives.isUsingNatives()) {
			// Linux x86_64 architecture supports faster (bulk) file info fetching.
			HANDLER = new LinuxFileHandler();
			nativesAreUsed = true;
		} else if (useNatives && !Platform.OS.isWindows() && UnixFileNatives.isUsingNatives()) {
			HANDLER = new UnixFileHandler();
			nativesAreUsed = true;
		} else {
			nativesAreUsed = false;
			Set<String> views = FileSystems.getDefault().supportedFileAttributeViews();
			if (views.contains("posix")) { //$NON-NLS-1$
				HANDLER = new PosixHandler();
			} else if (views.contains("dos")) { //$NON-NLS-1$
				HANDLER = new Win32Handler();
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

	public static IFileInfo[] listDirectoryAndGetFileInfos(String fileName) {
		return HANDLER.listDirectoryAndGetFileInfos(fileName);
	}

	public static String[] listDirectoryNames(String fileName) {
		return HANDLER.listDirectoryNames(fileName);
	}

}
