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
package org.eclipse.core.internal.filesystem.local.linux;

import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.PATH_MAX;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IFLNK;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IFMT;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IRGRP;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IROTH;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IRUSR;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IWGRP;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IWOTH;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IWUSR;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IXGRP;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IXOTH;
import static org.eclipse.core.internal.filesystem.local.linux.LinuxFileFlags.S_IXUSR;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.FileSystemAccess;
import org.eclipse.core.internal.filesystem.Messages;
import org.eclipse.core.internal.filesystem.Policy;
import org.eclipse.core.internal.filesystem.local.Convert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

public abstract class LinuxFileNatives {
	private static final String LIBRARY_NAME = "fastlinuxfile_1_0_0"; //$NON-NLS-1$
	private static final int ENOENT = LinuxStructStat.ENOENT; // errno value for "No such file or directory"

	private static final boolean usingNatives;
	protected static final String[] EMPTY_STRING_ARRAY = {};

	static {
		boolean _usingNatives = false;
		try {
			System.loadLibrary(LIBRARY_NAME);
			_usingNatives = true;
			initializeLinuxStructStatFieldIDs();
		} catch (UnsatisfiedLinkError e) {
			if (isLibraryPresent()) {
				logMissingNativeLibrary(e);
			}
		} finally {
			usingNatives = _usingNatives;
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
		if (!usingNatives) {
			return -1;
		}
		int ret = EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE | EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET | EFS.ATTRIBUTE_OWNER_READ | EFS.ATTRIBUTE_OWNER_WRITE | EFS.ATTRIBUTE_OWNER_EXECUTE | EFS.ATTRIBUTE_GROUP_READ | EFS.ATTRIBUTE_GROUP_WRITE | EFS.ATTRIBUTE_GROUP_EXECUTE | EFS.ATTRIBUTE_OTHER_READ | EFS.ATTRIBUTE_OTHER_WRITE | EFS.ATTRIBUTE_OTHER_EXECUTE;
		return ret;
	}

	public static String[] listDirectoryNames(String pathName) {
		byte[] name = fileNameToBytes(pathName);
		byte[][] result = listDir(name);
		if (result == null) {
			return EMPTY_STRING_ARRAY;
		}
		String[] names = new String[result.length];
		for (int i = 0; i < result.length; i++) {
			names[i] = Convert.fromPlatformBytes(result[i], result[i].length);
		}
		return names;
	}

	public static IFileInfo[] listDirectoryAndGetFileInfos(String pathName) {
		byte[] name = fileNameToBytes(pathName);
		LinuxStructStat[] stats = listDirAndGetFileInfos(name);
		if (stats == null) {
			return new IFileInfo[0];
		}
		int count = stats.length;
		IFileInfo[] infos = new IFileInfo[count];
		for (int i = 0; i < count; i++) {
			var st = stats[i].toFileInfo();
			infos[i] = st;
		}
		return infos;
	}

	public static FileInfo fetchFileInfo(String fileName) {
		FileInfo info = null;
		byte[] name = fileNameToBytes(fileName);
		LinuxStructStat stat = new LinuxStructStat();
		if (lstat(name, stat) == 0 || stat.errno == ENOENT) { // lstat fills errno even on failure
			if (stat.errno == ENOENT) {
				// file does not exist
				info = new FileInfo();
			} else if ((stat.st_mode & S_IFMT) == S_IFLNK) { // it's a link!
				LinuxStructStat targetStat = new LinuxStructStat();
				if (stat(name, targetStat) == 0) { // get the information about the file the link points to
					info = targetStat.toFileInfo(); // store the target file stats in info
				} else { // invalid link target
					info = new FileInfo();
					if (targetStat.errno != ENOENT) {
						info.setError(IFileInfo.IO_ERROR);
					}
				}
				info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true); // set symlink attribute
				byte target[] = new byte[PATH_MAX];
				int length = readlink(name, target, target.length);
				if (length > 0) { // set target of the link
					info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, bytesToFileName(target, length));
				}
			} else { // regular file or directory
				info = stat.toFileInfo();
			}
		} else {
			info = new FileInfo();
			if (stat.errno != ENOENT) {
				info.setError(IFileInfo.IO_ERROR);
			}
		}

		if (info.getName().isEmpty()) {
			// If the file system is case insensitive, we don't know the real name of the file.
			// Since obtaining the real name in such situation is pretty expensive, we use the name
			// passed as a parameter, which may differ by case from the real name of the file
			// if the file system is case insensitive.
			info.setName(new File(fileName).getName());
		}
		return info;
	}

	public static boolean putFileInfo(String fileName, IFileInfo info, int options) {
		int code = 0;
		byte[] name = fileNameToBytes(fileName);
		if (name == null) {
			return false;
		}

		// Change permissions
		int mode = 0;
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_READ)) {
			mode |= S_IRUSR;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_WRITE)) {
			mode |= S_IWUSR;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE)) {
			mode |= S_IXUSR;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_READ)) {
			mode |= S_IRGRP;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_WRITE)) {
			mode |= S_IWGRP;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE)) {
			mode |= S_IXGRP;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_READ)) {
			mode |= S_IROTH;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_WRITE)) {
			mode |= S_IWOTH;
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE)) {
			mode |= S_IXOTH;
		}
		code |= chmod(name, mode);

		return code == 0;
	}

	public static boolean isUsingNatives() {
		return usingNatives;
	}

	public static int getFlag(String flag) {
		if (!usingNatives) {
			return -1;
		}
		return getflag(flag.getBytes(StandardCharsets.US_ASCII));
	}

	private static byte[] fileNameToBytes(String fileName) {
		return Convert.toPlatformBytes(fileName);
	}

	private static String bytesToFileName(byte[] buf, int length) {
		return Convert.fromPlatformBytes(buf, length);
	}

	private static final native void initializeLinuxStructStatFieldIDs();

	private static final native int chmod(byte[] path, int mode);

	private static final native int stat(byte[] path, LinuxStructStat buf);

	private static final native int lstat(byte[] path, LinuxStructStat buf);

	private static final native int readlink(byte[] path, byte[] buf, long bufsiz);

	private static final native int getflag(byte[] buf);

	private static final native byte[][] listDir(byte[] path);

	private static final native LinuxStructStat[] listDirAndGetFileInfos(byte[] path);

}
