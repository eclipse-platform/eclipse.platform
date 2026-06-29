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

/**
 * Linux-specific file mode flags.
 *
 * <p>These are standard POSIX constants as defined in {@code <sys/stat.h>} and are
 * identical across all Linux architectures. No Mac OS X specific flags
 * (such as {@code UF_IMMUTABLE} or {@code SF_IMMUTABLE}) are present here since
 * they are not supported on Linux.</p>
 *
 * @since 1.11.500
 */
public final class LinuxFileFlags {

	/**
	 * Maximum number of characters in a file path including the null terminator.
	 */
	public static final int PATH_MAX = 4096;

	/** Bitmask for the file type bitfields in {@code st_mode}. */
	public static final int S_IFMT  = 0xF000;

	/** File type: symbolic link. */
	public static final int S_IFLNK = 0xA000;

	/** File type: directory. */
	public static final int S_IFDIR = 0x4000;

	/** Owner has read permission. */
	public static final int S_IRUSR = 0x0100;

	/** Owner has write permission. */
	public static final int S_IWUSR = 0x0080;

	/** Owner has execute permission. */
	public static final int S_IXUSR = 0x0040;

	/** Group has read permission. */
	public static final int S_IRGRP = 0x0020;

	/** Group has write permission. */
	public static final int S_IWGRP = 0x0010;

	/** Group has execute permission. */
	public static final int S_IXGRP = 0x0008;

	/** Others have read permission. */
	public static final int S_IROTH = 0x0004;

	/** Others have write permission. */
	public static final int S_IWOTH = 0x0002;

	/** Others have execute permission. */
	public static final int S_IXOTH = 0x0001;

	private LinuxFileFlags() {
		// not instantiable
	}

}
