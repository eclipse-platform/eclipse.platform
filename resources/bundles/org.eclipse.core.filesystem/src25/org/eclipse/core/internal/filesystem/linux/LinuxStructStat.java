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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.provider.FileInfo;

/**
 * Holds the relevant fields from the Linux {@code struct stat} obtained via
 * {@code statx(2)} and converts them to an Eclipse {@link FileInfo}.
 *
 * <p>Unlike the shared {@code StructStat} used by the JNI/Unix implementation,
 * this class contains <em>no</em> Mac OS X specific fields (no {@code st_flags}).</p>
 *
 * @since 1.11.500
 */
public class LinuxStructStat {

	private static final boolean USE_MILLISECOND_RESOLUTION = Boolean.parseBoolean(
			System.getProperty("eclipse.filesystem.useNatives.modificationTimestampMillisecondsResolution", //$NON-NLS-1$
					"true")); //$NON-NLS-1$

	/** File mode (type and permission bits). */
	public final int st_mode;

	/** File size in bytes. */
	public final long st_size;

	/** Last modification time in whole seconds (since the Unix epoch). */
	public final long st_mtime;

	/** Nanosecond component of the last modification time. */
	public final long st_mtime_nsec;

	/**
	 * Creates a new {@code LinuxStructStat} with the given stat field values.
	 *
	 * @param st_mode       file mode (type + permissions)
	 * @param st_size       file size in bytes
	 * @param st_mtime      modification time, whole seconds
	 * @param st_mtime_nsec modification time, nanosecond component
	 */
	public LinuxStructStat(int st_mode, long st_size, long st_mtime, long st_mtime_nsec) {
		this.st_mode = st_mode;
		this.st_size = st_size;
		this.st_mtime = st_mtime;
		this.st_mtime_nsec = st_mtime_nsec;
	}

	/**
	 * Converts the stat data into an Eclipse {@link FileInfo}.
	 *
	 * @return a {@code FileInfo} populated from the stat fields
	 */
	public FileInfo toFileInfo() {
		FileInfo info = new FileInfo();
		info.setExists(true);
		info.setLength(st_size);

		long lastModified = st_mtime * 1_000L;
		if (USE_MILLISECOND_RESOLUTION) {
			lastModified += st_mtime_nsec / 1_000_000L;
		}
		info.setLastModified(lastModified);

		if ((st_mode & LinuxFileFlags.S_IFMT) == LinuxFileFlags.S_IFDIR) {
			info.setDirectory(true);
		}

		// Owner permissions (OWNER_READ and OWNER_WRITE default to true in FileInfo,
		// so we only need to explicitly set them to false when the bits are absent)
		if ((st_mode & LinuxFileFlags.S_IRUSR) == 0) {
			info.setAttribute(EFS.ATTRIBUTE_OWNER_READ, false);
		}
		if ((st_mode & LinuxFileFlags.S_IWUSR) == 0) {
			info.setAttribute(EFS.ATTRIBUTE_OWNER_WRITE, false);
		}
		if ((st_mode & LinuxFileFlags.S_IXUSR) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE, true);
		}

		// Group permissions
		if ((st_mode & LinuxFileFlags.S_IRGRP) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_READ, true);
		}
		if ((st_mode & LinuxFileFlags.S_IWGRP) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, true);
		}
		if ((st_mode & LinuxFileFlags.S_IXGRP) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE, true);
		}

		// Others permissions
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
