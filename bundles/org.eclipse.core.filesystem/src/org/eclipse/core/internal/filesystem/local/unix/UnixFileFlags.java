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
package org.eclipse.core.internal.filesystem.local.unix;

public class UnixFileFlags {

	static {
		PATH_MAX = UnixFileNatives.getFlag("PATH_MAX"); //$NON-NLS-1$
		S_IFMT = UnixFileNatives.getFlag("S_IFMT"); //$NON-NLS-1$
		S_IFLNK = UnixFileNatives.getFlag("S_IFLNK"); //$NON-NLS-1$
		S_IFDIR = UnixFileNatives.getFlag("S_IFDIR"); //$NON-NLS-1$
		S_IRUSR = UnixFileNatives.getFlag("S_IRUSR"); //$NON-NLS-1$
		S_IWUSR = UnixFileNatives.getFlag("S_IWUSR"); //$NON-NLS-1$
		S_IXUSR = UnixFileNatives.getFlag("S_IXUSR"); //$NON-NLS-1$
		S_IRGRP = UnixFileNatives.getFlag("S_IRGRP"); //$NON-NLS-1$
		S_IWGRP = UnixFileNatives.getFlag("S_IWGRP"); //$NON-NLS-1$
		S_IXGRP = UnixFileNatives.getFlag("S_IXGRP"); //$NON-NLS-1$
		S_IROTH = UnixFileNatives.getFlag("S_IROTH"); //$NON-NLS-1$
		S_IWOTH = UnixFileNatives.getFlag("S_IWOTH"); //$NON-NLS-1$
		S_IXOTH = UnixFileNatives.getFlag("S_IXOTH"); //$NON-NLS-1$
		UF_IMMUTABLE = UnixFileNatives.getFlag("UF_IMMUTABLE"); //$NON-NLS-1$
		SF_IMMUTABLE = UnixFileNatives.getFlag("SF_IMMUTABLE"); //$NON-NLS-1$
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

	/**
	 * the file may not be changed
	 */
	public static final int UF_IMMUTABLE;
	/**
	 * the file may not be changed
	 */
	public static final int SF_IMMUTABLE;

}
