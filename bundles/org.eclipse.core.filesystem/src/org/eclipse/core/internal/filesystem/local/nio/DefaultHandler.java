/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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
 *     Chris McGee (IBM) - Bug 380325 - Release filesystem fragment providing Java 7 NIO2 support
 *     Sergey Prigogin (Google) - Bug 458006 - Fix tests that fail on Mac when filesystem.java7 is used
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local.nio;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.NativeHandler;

/**
 * Default implementation of NativeHandler using only Java 7 API's. It can be
 * used in cases where neither a DOS/Windows nor POSIX file system could be
 * detected.
 */
public class DefaultHandler extends NativeHandler {
	private static final int ATTRIBUTES = EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET // symbolic link support
			| EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE; // based on Java API

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		Path path = Paths.get(fileName);
		FileInfo info = new FileInfo();
		boolean exists = Files.exists(path);
		info.setExists(exists);

		try {
			BasicFileAttributes readAttributes = Files.readAttributes(path, BasicFileAttributes.class);

			// Even if it doesn't exist then check for symbolic link information.
			if (readAttributes.isSymbolicLink()) {
				info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
				try {
					info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, Files.readSymbolicLink(path).toString());
				} catch (IOException e) {
					// Leave the target alone.
					info.setError(IFileInfo.IO_ERROR);
				}
			}

			// Fill in the name of the file.
			// If the file system is case insensitive, we don't know the real name of the file.
			// Since obtaining the real name in such situation is pretty expensive, we use the name
			// passed as a parameter, which may differ by case from the real name of the file
			// if the file system is case insensitive.
			info.setName(path.toFile().getName());

			// Since we will be using a mixture of pre Java 7 API's which do not support the
			// retrieval of information for the symbolic link itself instead of the target
			// we will only support the following details if the symbolic link target exists.
			if (!exists)
				return info;

			info.setLastModified(readAttributes.lastModifiedTime().toMillis());
			info.setLength(readAttributes.size());
			info.setDirectory(readAttributes.isDirectory());

			info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, !Files.isWritable(path) && Files.isReadable(path));
			info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, Files.isExecutable(path));
		} catch (IOException e) {
			// Leave alone and continue.
			info.setError(IFileInfo.IO_ERROR);
		}
		return info;
	}

	@Override
	public int getSupportedAttributes() {
		return ATTRIBUTES;
	}

	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		// In all cases we will set the information on the symbolic link target
		// and not the symbolic link itself. This is consistent with the approach
		// taken above in fetchInfo.
		File file = new File(fileName);
		if (info.getAttribute(EFS.ATTRIBUTE_READ_ONLY)) {
			if (!file.setReadOnly())
				return false;
		} else {
			if (!file.setWritable(true))
				return false;
		}
		// If setExecutable succeeds, then the whole method succeeds.
		return file.setExecutable(info.getAttribute(EFS.ATTRIBUTE_EXECUTABLE));
	}
}
