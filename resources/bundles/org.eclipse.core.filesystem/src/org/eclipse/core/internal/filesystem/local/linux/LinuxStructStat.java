/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local.linux;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.Convert;

/**
 * This class mirrors relevant fields of native struct stat
 * and is used by JNI calls wrapping OS file related functions.
 */
public class LinuxStructStat {
	/**  errno value for "No such file or directory" */
	public static final int ENOENT = 2;

	private static final boolean USE_MILLISECOND_RESOLUTION = Boolean.parseBoolean(System.getProperty("eclipse.filesystem.useNatives.modificationTimestampMillisecondsResolution", "true")); //$NON-NLS-1$ //$NON-NLS-2$

	public int st_mode;
	public long st_size;
	public long st_mtime;
	public long st_mtime_msec; // millisecond component of the file timestamp
	public int errno;
	public byte[] name;
	public byte[] linkFile; // Filled target for symbolic links

	public FileInfo toFileInfo() {
		FileInfo info = new FileInfo();
		if (name != null) {
			info.setName(Convert.fromPlatformBytes(name, name.length));
		}
		if (errno != 0 && errno != ENOENT) {
			info.setError(IFileInfo.IO_ERROR);
			return info;
		}
		info.setExists(errno != ENOENT);
		info.setLength(st_size);
		long lastModified = (st_mtime * 1_000);
		if (USE_MILLISECOND_RESOLUTION) {
			lastModified += st_mtime_msec;
		}
		info.setLastModified(lastModified);
		if ((st_mode & LinuxFileFlags.S_IFMT) == LinuxFileFlags.S_IFDIR) {
			info.setDirectory(true);
		}
		if (linkFile != null) {
			info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
			if (linkFile.length > 0) {
				info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, Convert.fromPlatformBytes(linkFile, linkFile.length));
			}
		}
		if ((st_mode & LinuxFileFlags.S_IRUSR) == 0) { // Set to true in FileInfo constructor
			info.setAttribute(EFS.ATTRIBUTE_OWNER_READ, false);
		}
		if ((st_mode & LinuxFileFlags.S_IWUSR) == 0) { // Set to true in FileInfo constructor
			info.setAttribute(EFS.ATTRIBUTE_OWNER_WRITE, false);
		}
		if ((st_mode & LinuxFileFlags.S_IXUSR) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE, true);
		}
		if ((st_mode & LinuxFileFlags.S_IRGRP) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_READ, true);
		}
		if ((st_mode & LinuxFileFlags.S_IWGRP) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, true);
		}
		if ((st_mode & LinuxFileFlags.S_IXGRP) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE, true);
		}
		if ((st_mode & LinuxFileFlags.S_IROTH) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OTHER_READ, true);
		}
		if ((st_mode & LinuxFileFlags.S_IWOTH) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OTHER_WRITE, true);
		}
		if ((st_mode & LinuxFileFlags.S_IXOTH) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE, true);
		}
		return info;
	}

}
