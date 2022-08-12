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
 *     Sergey Prigogin (Google) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local.nio;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.NativeHandler;

/**
 * NativeHandler for POSIX using only Java 7 API's. It can be used for any
 * file system supporting POSIX family of standards.
 */
public class PosixHandler extends NativeHandler {
	private static final int ATTRIBUTES = EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET // symbolic link support
			| EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE // mapped to owner read and owner execute via FileInfo implementation
			| EFS.ATTRIBUTE_OWNER_READ | EFS.ATTRIBUTE_OWNER_WRITE | EFS.ATTRIBUTE_OWNER_EXECUTE // owner
			| EFS.ATTRIBUTE_GROUP_READ | EFS.ATTRIBUTE_GROUP_WRITE | EFS.ATTRIBUTE_GROUP_EXECUTE // group
			| EFS.ATTRIBUTE_OTHER_READ | EFS.ATTRIBUTE_OTHER_WRITE | EFS.ATTRIBUTE_OTHER_EXECUTE; // other

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		Path path = Paths.get(fileName);
		FileInfo info = new FileInfo();

		// Fill in the name of the file.
		// If the file system is case insensitive, we don't know the real name of the file.
		// Since obtaining the real name in such situation is pretty expensive, we use the name
		// passed as a parameter, which may differ by case from the real name of the file
		// if the file system is case insensitive.
		Path fileNamePath = path.getFileName();
		info.setName(fileNamePath == null ? "" : fileNamePath.toString()); //$NON-NLS-1$

		try {
			PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

			if (attrs.isSymbolicLink()) {
				info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
				info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, Files.readSymbolicLink(path).toString());
				attrs = Files.readAttributes(path, PosixFileAttributes.class);
			}

			info.setExists(true);
			info.setLastModified(attrs.lastModifiedTime().toMillis());
			info.setLength(attrs.size());
			info.setDirectory(attrs.isDirectory());

			Set<PosixFilePermission> perms = attrs.permissions();
			info.setAttribute(EFS.ATTRIBUTE_OWNER_READ, perms.contains(PosixFilePermission.OWNER_READ));
			info.setAttribute(EFS.ATTRIBUTE_OWNER_WRITE, perms.contains(PosixFilePermission.OWNER_WRITE));
			info.setAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE, perms.contains(PosixFilePermission.OWNER_EXECUTE));
			info.setAttribute(EFS.ATTRIBUTE_GROUP_READ, perms.contains(PosixFilePermission.GROUP_READ));
			info.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, perms.contains(PosixFilePermission.GROUP_WRITE));
			info.setAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE, perms.contains(PosixFilePermission.GROUP_EXECUTE));
			info.setAttribute(EFS.ATTRIBUTE_OTHER_READ, perms.contains(PosixFilePermission.OTHERS_READ));
			info.setAttribute(EFS.ATTRIBUTE_OTHER_WRITE, perms.contains(PosixFilePermission.OTHERS_WRITE));
			info.setAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE, perms.contains(PosixFilePermission.OTHERS_EXECUTE));
		} catch (NoSuchFileException e) {
			// A non-existing file is not considered an error.
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
		Path path = Paths.get(fileName);
		Set<PosixFilePermission> perms = new HashSet<>();

		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_READ))
			perms.add(PosixFilePermission.OWNER_READ);
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_WRITE))
			perms.add(PosixFilePermission.OWNER_WRITE);
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE))
			perms.add(PosixFilePermission.OWNER_EXECUTE);
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_READ))
			perms.add(PosixFilePermission.GROUP_READ);
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_WRITE))
			perms.add(PosixFilePermission.GROUP_WRITE);
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE))
			perms.add(PosixFilePermission.GROUP_EXECUTE);
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_READ))
			perms.add(PosixFilePermission.OTHERS_READ);
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_WRITE))
			perms.add(PosixFilePermission.OTHERS_WRITE);
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE))
			perms.add(PosixFilePermission.OTHERS_EXECUTE);

		PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
		try {
			view.setPermissions(perms);
		} catch (IOException e) {
			return false;
		}
		return true;
	}
}
