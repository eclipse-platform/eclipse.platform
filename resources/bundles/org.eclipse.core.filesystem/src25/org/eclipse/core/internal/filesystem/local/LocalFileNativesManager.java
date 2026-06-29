/*******************************************************************************
 * Copyright (c) 2025 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import java.nio.file.FileSystems;
import java.util.Set;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.linux.LinuxFileHandler;
import org.eclipse.core.internal.filesystem.linux.LinuxFileNatives;
import org.eclipse.core.internal.filesystem.local.nio.DefaultHandler;
import org.eclipse.core.internal.filesystem.local.nio.PosixHandler;
import org.eclipse.core.internal.filesystem.local.unix.UnixFileHandler;
import org.eclipse.core.internal.filesystem.local.unix.UnixFileNatives;
import org.eclipse.core.runtime.Platform;

/**
 * Java 25+ override of {@code LocalFileNativesManager}.
 *
 * <p>When running on Java 25 or later on Linux, this class prefers the
 * {@link LinuxFileHandler} backed by the Java FFM API over the legacy
 * JNI-based {@code UnixFileHandler}.  The FFM implementation calls
 * {@code statx(2)}, {@code chmod(2)} and {@code readlink(2)} directly via
 * downcall handles without requiring a separately shipped native shared
 * library.</p>
 *
 * <p>On all other platforms (macOS, Windows) and when the FFM initialisation
 * fails, the selection logic falls back to the same order as the Java 17
 * base-version of this class:</p>
 * <ol>
 *   <li>JNI {@code UnixFileHandler} (non-Windows, when native library is present)</li>
 *   <li>POSIX NIO/2 {@code PosixHandler}</li>
 *   <li>DOS NIO/2 {@code Win32Handler}</li>
 *   <li>{@code DefaultHandler}</li>
 * </ol>
 *
 * <p>This class is placed in {@code META-INF/versions/25/} of the multi-release
 * JAR and is selected automatically by the JVM when the runtime is Java 25+.
 * The Java 17 version in the root of the JAR is used on older runtimes.</p>
 *
 * @since 1.11.500
 */
public class LocalFileNativesManager {

	/** System property that can be used to disable native file operations. */
	public static final String PROPERTY_USE_NATIVES = "eclipse.filesystem.useNatives"; //$NON-NLS-1$

	/** Default value for {@link #PROPERTY_USE_NATIVES}. */
	public static final boolean PROPERTY_USE_NATIVE_DEFAULT = true;

	private static NativeHandler HANDLER;

	static {
		reset();
	}

	/**
	 * Resets the handler selection to the system default determined by the
	 * {@value #PROPERTY_USE_NATIVES} system property.
	 */
	public static void reset() {
		setUsingNative(Boolean.parseBoolean(
				System.getProperty(PROPERTY_USE_NATIVES, String.valueOf(PROPERTY_USE_NATIVE_DEFAULT))));
	}

	/**
	 * Attempts to configure the native handler according to the {@code useNatives}
	 * flag.
	 *
	 * <p>On Java 25+ running on Linux the FFM-based {@link LinuxFileHandler} is
	 * preferred over the legacy JNI handler. On all other configurations the
	 * selection order matches the Java 17 base-version of this class.</p>
	 *
	 * @param useNatives {@code true} to try to use a native (FFM or JNI) handler
	 * @return {@code true} if a native handler is active after this call
	 */
	public static boolean setUsingNative(boolean useNatives) {
		boolean nativesAreUsed;

		if (useNatives && Platform.OS.isLinux() && LinuxFileNatives.isAvailable()) {
			// Java 25+ on Linux: use the FFM-based handler.
			// No native .so library is needed — libc is accessed directly.
			HANDLER = new LinuxFileHandler();
			nativesAreUsed = true;
		} else if (useNatives && !Platform.OS.isWindows() && UnixFileNatives.isUsingNatives()) {
			// Non-Linux Unix (macOS) or Linux on older Java: fall back to JNI handler.
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

	/**
	 * Returns the bitmask of EFS attributes supported by the active handler.
	 *
	 * @return EFS attribute bitmask
	 */
	public static int getSupportedAttributes() {
		return HANDLER.getSupportedAttributes();
	}

	/**
	 * Fetches file information for the given file path.
	 *
	 * @param fileName absolute path of the file
	 * @return {@link FileInfo} populated with the file's attributes
	 */
	public static FileInfo fetchFileInfo(String fileName) {
		return HANDLER.fetchFileInfo(fileName);
	}

	/**
	 * Writes file information (permissions) for the given file path.
	 *
	 * @param fileName absolute path of the file
	 * @param info     the desired file attributes
	 * @param options  option flags (currently unused)
	 * @return {@code true} if the operation succeeded
	 */
	public static boolean putFileInfo(String fileName, IFileInfo info, int options) {
		return HANDLER.putFileInfo(fileName, info, options);
	}

}
