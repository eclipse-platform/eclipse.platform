/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local.unix;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.*;
import org.eclipse.core.internal.filesystem.local.Convert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

public abstract class UnixFileNatives {
	private static final String LIBRARY_NAME = "unixfile_1_0_0"; //$NON-NLS-1$
	private static final int UNICODE_SUPPORTED = 1 << 0;
	private static final int CHFLAGS_SUPPORTED = 1 << 1;
	private static final int ENOENT = 2; // errno value for "No such file or directory"

	private static final boolean usingNatives;
	private static final int libattr;

	static {
		boolean _usingNatives = false;
		int _libattr = 0;
		try {
			System.loadLibrary(LIBRARY_NAME);
			_usingNatives = true;
			initializeStructStatFieldIDs();
			_libattr = libattr();
		} catch (UnsatisfiedLinkError e) {
			if (isLibraryPresent())
				logMissingNativeLibrary(e);
		} finally {
			usingNatives = _usingNatives;
			libattr = _libattr;
		}
	}

	private static boolean isLibraryPresent() {
		String libName = System.mapLibraryName(LIBRARY_NAME);
		Enumeration<URL> entries = FileSystemAccess.findEntries("/", libName, true); //$NON-NLS-1$
		return entries != null && entries.hasMoreElements();
	}

	private static void logMissingNativeLibrary(UnsatisfiedLinkError e) {
		String libName = System.mapLibraryName(LIBRARY_NAME);
		String message = NLS.bind(Messages.couldNotLoadLibrary, libName);
		Policy.log(IStatus.INFO, message, e);
	}

	public static int getSupportedAttributes() {
		if (!usingNatives)
			return -1;
		int ret = EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE | EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET | EFS.ATTRIBUTE_OWNER_READ | EFS.ATTRIBUTE_OWNER_WRITE | EFS.ATTRIBUTE_OWNER_EXECUTE | EFS.ATTRIBUTE_GROUP_READ | EFS.ATTRIBUTE_GROUP_WRITE | EFS.ATTRIBUTE_GROUP_EXECUTE | EFS.ATTRIBUTE_OTHER_READ | EFS.ATTRIBUTE_OTHER_WRITE | EFS.ATTRIBUTE_OTHER_EXECUTE;
		if (isSupported(CHFLAGS_SUPPORTED))
			ret |= EFS.ATTRIBUTE_IMMUTABLE;
		return ret;
	}

	public static FileInfo fetchFileInfo(String fileName) {
		FileInfo info = null;
		byte[] name = fileNameToBytes(fileName);
		StructStat stat = new StructStat();
		if (lstat(name, stat) == 0) {
			if ((stat.st_mode & UnixFileFlags.S_IFMT) == UnixFileFlags.S_IFLNK) {
				if (stat(name, stat) == 0) {
					info = stat.toFileInfo();
				} else {
					info = new FileInfo();
					if (getErrno() != ENOENT)
						info.setError(IFileInfo.IO_ERROR);
				}
				info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
				byte target[] = new byte[UnixFileFlags.PATH_MAX];
				int length = readlink(name, target, target.length);
				if (length > 0)
					info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, bytesToFileName(target, length));
			} else
				info = stat.toFileInfo();
		} else {
			info = new FileInfo();
			if (getErrno() != ENOENT)
				info.setError(IFileInfo.IO_ERROR);
		}

		if (info.getName() == null) {
			// If the file system is case insensitive, we don't know the real name of the file.
			// Since obtaining the real name in such situation is pretty expensive, we use the name
			// passed as a parameter, which may differ by case from the real name of the file
			// if the file system is case insensitive.
			File file = new File(fileName);
			info.setName(file.getName());
		}
		return info;
	}

	public static boolean putFileInfo(String fileName, IFileInfo info, int options) {
		int code = 0;
		byte[] name = fileNameToBytes(fileName);
		if (name == null)
			return false;

		// In case uchg flag is to be removed do it before calling chmod
		if (!info.getAttribute(EFS.ATTRIBUTE_IMMUTABLE) && isSupported(CHFLAGS_SUPPORTED)) {
			StructStat stat = new StructStat();
			if (stat(name, stat) == 0) {
				long flags = stat.st_flags;
				flags &= ~UnixFileFlags.SF_IMMUTABLE;
				flags &= ~UnixFileFlags.UF_IMMUTABLE;
				code |= chflags(name, (int) flags);
			}
		}

		// Change permissions
		int mode = 0;
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_READ))
			mode |= UnixFileFlags.S_IRUSR;
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_WRITE))
			mode |= UnixFileFlags.S_IWUSR;
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE))
			mode |= UnixFileFlags.S_IXUSR;
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_READ))
			mode |= UnixFileFlags.S_IRGRP;
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_WRITE))
			mode |= UnixFileFlags.S_IWGRP;
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE))
			mode |= UnixFileFlags.S_IXGRP;
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_READ))
			mode |= UnixFileFlags.S_IROTH;
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_WRITE))
			mode |= UnixFileFlags.S_IWOTH;
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE))
			mode |= UnixFileFlags.S_IXOTH;
		code |= chmod(name, mode);

		// In case uchg flag is to be added do it after calling chmod
		if (info.getAttribute(EFS.ATTRIBUTE_IMMUTABLE) && isSupported(CHFLAGS_SUPPORTED)) {
			StructStat stat = new StructStat();
			if (stat(name, stat) == 0) {
				long flags = stat.st_flags;
				flags |= UnixFileFlags.UF_IMMUTABLE;
				code |= chflags(name, (int) flags);
			}
		}
		return code == 0;
	}

	public static boolean isUsingNatives() {
		return usingNatives;
	}

	public static int getErrno() {
		return errno();
	}

	public static int getFlag(String flag) {
		if (!usingNatives)
			return -1;
		return getflag(flag.getBytes(StandardCharsets.US_ASCII));
	}

	private static byte[] fileNameToBytes(String fileName) {
		if (isSupported(UNICODE_SUPPORTED))
			return tounicode(fileName.toCharArray());
		return Convert.toPlatformBytes(fileName);
	}

	private static String bytesToFileName(byte[] buf, int length) {
		if (isSupported(UNICODE_SUPPORTED))
			return new String(buf, 0, length);
		return Convert.fromPlatformBytes(buf, length);
	}

	private static boolean isSupported(int attr) {
		return (libattr & attr) != 0;
	}

	private static final native void initializeStructStatFieldIDs();

	private static final native int chmod(byte[] path, int mode);

	private static final native int chflags(byte[] path, int flags);

	private static final native int stat(byte[] path, StructStat buf);

	private static final native int lstat(byte[] path, StructStat buf);

	private static final native int readlink(byte[] path, byte[] buf, long bufsiz);

	private static final native int errno();

	private static final native int libattr();

	private static final native byte[] tounicode(char[] buf);

	private static final native int getflag(byte[] buf);

}
