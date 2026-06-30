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

public class LinuxFileFlags {

	static {
		PATH_MAX = LinuxFileNatives.getFlag("PATH_MAX"); //$NON-NLS-1$
		S_IFMT = LinuxFileNatives.getFlag("S_IFMT"); //$NON-NLS-1$
		S_IFLNK = LinuxFileNatives.getFlag("S_IFLNK"); //$NON-NLS-1$
		S_IFDIR = LinuxFileNatives.getFlag("S_IFDIR"); //$NON-NLS-1$
		S_IRUSR = LinuxFileNatives.getFlag("S_IRUSR"); //$NON-NLS-1$
		S_IWUSR = LinuxFileNatives.getFlag("S_IWUSR"); //$NON-NLS-1$
		S_IXUSR = LinuxFileNatives.getFlag("S_IXUSR"); //$NON-NLS-1$
		S_IRGRP = LinuxFileNatives.getFlag("S_IRGRP"); //$NON-NLS-1$
		S_IWGRP = LinuxFileNatives.getFlag("S_IWGRP"); //$NON-NLS-1$
		S_IXGRP = LinuxFileNatives.getFlag("S_IXGRP"); //$NON-NLS-1$
		S_IROTH = LinuxFileNatives.getFlag("S_IROTH"); //$NON-NLS-1$
		S_IWOTH = LinuxFileNatives.getFlag("S_IWOTH"); //$NON-NLS-1$
		S_IXOTH = LinuxFileNatives.getFlag("S_IXOTH"); //$NON-NLS-1$
	}

	/**
	 * chars in a path name including nul
	 */
	public static final int PATH_MAX;

	/**
	 * bitmask for the file type bitfields
	 */
	public static final int S_IFMT;
	/**
	 * symbolic link
	 */
	public static final int S_IFLNK;
	/**
	 * directory
	 */
	public static final int S_IFDIR;
	/**
	 * owner has read permission
	 */
	public static final int S_IRUSR;
	/**
	 * owner has write permission
	 */
	public static final int S_IWUSR;
	/**
	 * owner has execute permission
	 */
	public static final int S_IXUSR;
	/**
	 * group has read permission
	 */
	public static final int S_IRGRP;
	/**
	 * group has write permission
	 */
	public static final int S_IWGRP;
	/**
	 * group has execute permission
	 */
	public static final int S_IXGRP;
	/**
	 * others have read permission
	 */
	public static final int S_IROTH;
	/**
	 * others have write permission
	 */
	public static final int S_IWOTH;
	/**
	 * others have execute permission
	 */
	public static final int S_IXOTH;

}
