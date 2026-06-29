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
package org.eclipse.core.internal.filesystem.linux;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.NativeHandler;

/**
 * A {@link NativeHandler} for Linux that uses the Java 25 Foreign Function &amp;
 * Memory (FFM) API instead of JNI to access native file operations.
 *
 * <p>This handler delegates to {@link LinuxFileNatives}, which calls
 * {@code statx(2)}, {@code chmod(2)} and {@code readlink(2)} directly via
 * FFM downcall handles. No native shared library (.so) is required — the
 * standard C library (libc) is used directly.</p>
 *
 * <p>Mac OS X specific code paths present in the old {@code UnixFileHandler}
 * (chflags, UF_IMMUTABLE, SF_IMMUTABLE, CoreServices unicode conversion) are
 * intentionally absent. This handler is exclusively for Linux.</p>
 *
 * @since 1.11.500
 * @see LinuxFileNatives
 */
public class LinuxFileHandler extends NativeHandler {

	@Override
	public int getSupportedAttributes() {
		return LinuxFileNatives.getSupportedAttributes();
	}

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		return LinuxFileNatives.fetchFileInfo(fileName);
	}

	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		return LinuxFileNatives.putFileInfo(fileName, info, options);
	}

}
