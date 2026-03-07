/*******************************************************************************
 * Copyright (c) 2024, 2024 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import static org.eclipse.core.internal.filesystem.local.Convert.WIN32_RAW_PATH_PREFIX;
import static org.eclipse.core.internal.filesystem.local.Convert.WIN32_UNC_RAW_PATH_PREFIX;

import com.microsoft.Windows;
import com.microsoft._FILETIME;
import com.microsoft._WIN32_FIND_DATAW;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;

/**
 * A NativeHandler for Windows file systems that supports legacy {@code DOS} attributes and
 * uses the Windows {@code fileapi.h} API called through JNA.
 */
public class Win32Handler extends NativeHandler {
	private static final int ATTRIBUTES = EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET // symbolic link support
			| EFS.ATTRIBUTE_ARCHIVE | EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_HIDDEN; // standard DOS attributes

	@Override
	public int getSupportedAttributes() {
		return ATTRIBUTES;
	}

	public static void main(String[] args) {
		FileInfo info = new Win32Handler().fetchFileInfo("C:\\Users\\Hannes\\Desktop\\java-21-25-news\\src\\main\\java\\news\\notExisting.txt"); //$NON-NLS-1$
		System.out.println(info);
	}

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		FileInfo fileInfo = new FileInfo();

		String target = toLongWindowsPath(fileName);

		if (target.length() == 7 && target.startsWith(WIN32_RAW_PATH_PREFIX) && target.endsWith(":\\")) { //$NON-NLS-1$
			// FindFirstFile does not work at the root level. However, we don't need it because the root will never change time-stamp.
			// A root path is for example: \\?\c:\
			fileInfo.setDirectory(true);
			fileInfo.setExists(Files.exists(Path.of(target.substring(WIN32_RAW_PATH_PREFIX.length()))));
			return fileInfo;
		}
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment lpFileName = arena.allocateFrom(target, StandardCharsets.UTF_16LE);
			MemorySegment lpFindFileData = arena.allocate(_WIN32_FIND_DATAW.layout());
			// https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-findfirstfilew
			MemorySegment handle = Windows.FindFirstFileW(lpFileName, lpFindFileData);
			if (Windows.INVALID_HANDLE_VALUE().equals(handle)) {
				int error = Windows.GetLastError();
				//TODO: does it at any time return something different than zero?
				if (!(error == Windows.ERROR_FILE_NOT_FOUND() // file not found in existing parent directory
						|| error == Windows.ERROR_PATH_NOT_FOUND())) { // Not even the parent directory exists
					fileInfo.setError(IFileInfo.IO_ERROR);
				}
				return fileInfo;
			}
			Windows.FindClose(handle);

			convertFindDataWToFileInfo(lpFindFileData, fileInfo, fileName);
		} catch (IOException e) {
			// Leave alone and continue. The name is set before an IOException can be thrown
			fileInfo.setError(IFileInfo.IO_ERROR);
		}
		return fileInfo;
	}

	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		int dwFileAttributes;
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment lpFileName = arena.allocateFrom(toLongWindowsPath(fileName), StandardCharsets.UTF_16LE);
			// https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-getfileattributesw
			dwFileAttributes = Windows.GetFileAttributesW(lpFileName);
		}
		if (dwFileAttributes == Windows.INVALID_FILE_ATTRIBUTES()) {
			return false;
		}
		if (dwFileAttributes == Windows.FILE_ATTRIBUTE_NORMAL()) {
			// Assume nothing is set, as the documentation of FILE_ATTRIBUTE_NORMAL states:
			// "A file that does not have other attributes set. This attribute is valid only when used alone."
			dwFileAttributes = 0;
		}
		int fileAttributes = dwFileAttributes;

		boolean archive = info.getAttribute(EFS.ATTRIBUTE_ARCHIVE);
		boolean readOnly = info.getAttribute(EFS.ATTRIBUTE_READ_ONLY);
		boolean hidden = info.getAttribute(EFS.ATTRIBUTE_HIDDEN);
		fileAttributes = set(fileAttributes, Windows.FILE_ATTRIBUTE_ARCHIVE(), archive);
		fileAttributes = set(fileAttributes, Windows.FILE_ATTRIBUTE_READONLY(), readOnly);
		fileAttributes = set(fileAttributes, Windows.FILE_ATTRIBUTE_HIDDEN(), hidden);

		if (dwFileAttributes == fileAttributes) {
			return true; // Everything is already up to date -> nothing to do
		}
		//TODO: Combine arenas!
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment lpFileName = arena.allocateFrom(toLongWindowsPath(fileName), StandardCharsets.UTF_16LE);
			// https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-setfileattributesw
			return Windows.SetFileAttributesW(lpFileName, fileAttributes) != 0;
		}
	}

	public static String getShortPathName(String longPath) {
		longPath = toLongWindowsPath(longPath);
		// https://learn.microsoft.com/de-de/windows/win32/api/fileapi/nf-fileapi-getshortpathnamew
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment lpszLongPath = arena.allocateFrom(longPath, StandardCharsets.UTF_16LE);
			MemorySegment lpszShortPath = arena.allocate(lpszLongPath.byteSize());

			int newLength = Windows.GetShortPathNameW(lpszLongPath, lpszShortPath, longPath.length());
			if (0 < newLength && newLength < longPath.length()) { // zero means error
				int offset = longPath.startsWith(WIN32_UNC_RAW_PATH_PREFIX) ? WIN32_UNC_RAW_PATH_PREFIX.length() : WIN32_RAW_PATH_PREFIX.length();
				//TODO: Check this
				return lpszShortPath.reinterpret(newLength * 2).getString(0, StandardCharsets.UTF_16LE).substring(offset);
			}
			return null;

		}
	}

	private static String toLongWindowsPath(String fileName) {
		// See https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file
		if (fileName.startsWith("\\\\") && !fileName.startsWith(WIN32_UNC_RAW_PATH_PREFIX)) { //$NON-NLS-1$
			//convert UNC path of form \\server\path to long/unicode form \\?\UNC\server\path
			return WIN32_UNC_RAW_PATH_PREFIX + fileName.substring(1);
		} else if (!fileName.startsWith(WIN32_RAW_PATH_PREFIX)) {
			//convert simple path of form C:\path to long/unicode form \\?\C:\path
			return WIN32_RAW_PATH_PREFIX + fileName;
		}
		return fileName;
	}

	private static final long MAXDWORD = 0xFFFFFFFFL; // unsigned long from winnt.h. On Windows a C long usually has only 32bit

	private static void convertFindDataWToFileInfo(MemorySegment mem, FileInfo info, String fileName) throws IOException {
		/**
		 * For possible values of dwFileAttributes and their descriptions,
		 * see <a href="https://learn.microsoft.com/en-us/windows/win32/fileio/file-attribute-constants">File Attribute Constants</a>.
		 */

		int dwFileAttributes = _WIN32_FIND_DATAW.dwFileAttributes(mem);
		Instant ftLastWriteTime = readFILETIME(_WIN32_FIND_DATAW.ftLastWriteTime(mem));

		int nFileSizeHigh = _WIN32_FIND_DATAW.nFileSizeHigh(mem);
		int nFileSizeLow = _WIN32_FIND_DATAW.nFileSizeLow(mem);
		int dwReserved0 = _WIN32_FIND_DATAW.dwReserved0(mem);

		String cFileName = _WIN32_FIND_DATAW.cFileName(mem).getString(0, StandardCharsets.UTF_16LE);

		long fileLength = (nFileSizeHigh * (MAXDWORD + 1)) + nFileSizeLow;
		//TODO: check if equals!
		fileLength = toLong(nFileSizeHigh, nFileSizeLow);

		info.setName(cFileName);
		info.setExists(true);
		info.setLastModified(ftLastWriteTime.toEpochMilli());
		info.setLength(fileLength);
		info.setDirectory(isSet(dwFileAttributes, Windows.FILE_ATTRIBUTE_DIRECTORY()));
		info.setAttribute(EFS.ATTRIBUTE_ARCHIVE, isSet(dwFileAttributes, Windows.FILE_ATTRIBUTE_ARCHIVE()));
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, isSet(dwFileAttributes, Windows.FILE_ATTRIBUTE_READONLY()));
		info.setAttribute(EFS.ATTRIBUTE_HIDDEN, isSet(dwFileAttributes, Windows.FILE_ATTRIBUTE_HIDDEN()));

		boolean isReparsePoint = isSet(dwFileAttributes, Windows.FILE_ATTRIBUTE_REPARSE_POINT());
		if (isReparsePoint && dwReserved0 == Windows.IO_REPARSE_TAG_SYMLINK()) {
			Path linkTarget = Files.readSymbolicLink(Path.of(fileName));
			info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
			info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, linkTarget.toString());
		}
	}

	private static final Instant WINDOWS_REFERENCE_DATE = LocalDateTime.of(1601, Month.JANUARY, 1, 0, 0).toInstant(ZoneOffset.UTC);

	private static Instant readFILETIME(MemorySegment struct) {
		int low = _FILETIME.dwLowDateTime(struct);
		int high = _FILETIME.dwHighDateTime(struct);
		final long filetime = toLong(high, low);
		return WINDOWS_REFERENCE_DATE.plus(filetime / 10, ChronoUnit.MICROS);
	}
	// See also https://learn.microsoft.com/en-us/windows/win32/winprog/windows-data-types

	private static long toLong(int high, int low) {
		return (long) high << 32 | low & 0xffffffffL;
	}

	private static boolean isSet(long field, int bit) {
		return (field & bit) != 0;
	}

	private int set(int field, int bit, boolean isSet) {
		return isSet ? (field | bit) : (field & ~bit);
	}

}
