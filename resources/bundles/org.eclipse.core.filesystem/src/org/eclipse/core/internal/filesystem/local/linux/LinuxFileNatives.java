/*******************************************************************************
 * Copyright (c) 2010, 2026 IBM Corporation and others.
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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
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

	private static final native int getflag(byte[] buf);

	private static final native byte[][] listDir(byte[] path);

	private static final native LinuxStructStat[] listDirAndGetFileInfos(byte[] path);

}
