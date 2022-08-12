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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.NativeHandler;

/**
 * NativeHandler for file system that supports legacy "DOS" attributes using
 * only Java 7 API's. It can be used for DOS/Windows file systems.
 */
public class DosHandler extends NativeHandler {
	private static final int ATTRIBUTES = EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET // symbolic link support
			| EFS.ATTRIBUTE_ARCHIVE | EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_HIDDEN; // standard DOS attributes

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		FileInfo info = new FileInfo();

		try {
			Path path = Paths.get(fileName);

			// Use canonical file to get the correct case of filename. See bug 431983.
			Path fileNamePath = path.toRealPath(LinkOption.NOFOLLOW_LINKS).getFileName();
			String canonicalName = fileNamePath == null ? "" : fileNamePath.toString(); //$NON-NLS-1$
			info.setName(canonicalName);

			// To be consistent with the native implementation we do not follow a symbolic link
			// and return back the information about the target. Instead, we provide the information
			// about the symbolic link itself whether it exists or not.
			DosFileAttributes attrs = Files.readAttributes(path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

			info.setExists(true);
			info.setLastModified(attrs.lastModifiedTime().toMillis());
			info.setLength(attrs.size());
			info.setAttribute(EFS.ATTRIBUTE_ARCHIVE, attrs.isArchive());
			info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, attrs.isReadOnly());
			info.setAttribute(EFS.ATTRIBUTE_HIDDEN, attrs.isHidden());
			if (attrs.isSymbolicLink()) {
				info.setDirectory(isDirectoryLink(attrs));
				info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
				info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, Files.readSymbolicLink(path).toString());
			} else {
				info.setDirectory(attrs.isDirectory());
			}
		} catch (NoSuchFileException e) {
			// A non-existing file is not considered an error.
		} catch (IOException e) {
			// Leave alone and continue.
			info.setError(IFileInfo.IO_ERROR);
		}
		return info;
	}

	private boolean isDirectoryLink(DosFileAttributes attrs) {
		// Use reflection to call package protected WindowsFileAttributes.isDirectoryLink() method.
		try {
			Method method = attrs.getClass().getDeclaredMethod("isDirectoryLink"); //$NON-NLS-1$
			method.setAccessible(true);
			return (Boolean) method.invoke(attrs);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			return false;
		}
	}

	@Override
	public int getSupportedAttributes() {
		return ATTRIBUTES;
	}

	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		Path path = Paths.get(fileName);
		// To be consistent with fetchInfo do not following symbolic links to set archive, read only and hidden attributes.
		DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
		try {
			view.setArchive(info.getAttribute(EFS.ATTRIBUTE_ARCHIVE));
			view.setReadOnly(info.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
			view.setHidden(info.getAttribute(EFS.ATTRIBUTE_HIDDEN));
		} catch (IOException e) {
			return false;
		}
		return true;
	}
}
