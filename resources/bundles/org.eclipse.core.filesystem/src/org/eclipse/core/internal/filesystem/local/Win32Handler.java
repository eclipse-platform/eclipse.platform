/*******************************************************************************
 * Copyright (c) 2024, 2026 Hannes Wellmann and others.
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
 *     Hannes Wellmann - Migrate from JNA to FFM API
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import static org.eclipse.core.internal.filesystem.local.Convert.WIN32_RAW_PATH_PREFIX;
import static org.eclipse.core.internal.filesystem.local.Convert.WIN32_UNC_RAW_PATH_PREFIX;

import com.microsoft.windows.FILETIME;
import com.microsoft.windows.FileAPI;
import com.microsoft.windows.WIN32_FIND_DATAW;
import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
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
 * calls the Windows {@code fileapi.h} API directly through the Java foreigen functions API.
 *
 * See the implementation notes for the rational for direct native method invocations.
 */
public class Win32Handler extends NativeHandler {
	private static final int ATTRIBUTES = EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET // symbolic link support
			| EFS.ATTRIBUTE_ARCHIVE | EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_HIDDEN; // standard DOS attributes

	@Override
	public int getSupportedAttributes() {
		return ATTRIBUTES;
	}

	/**
	 * Fetches the {@link IFileInfo} of the given file.
	 *
	 * @implNote
	 * This implementation invokes the native {@code FindFirstFileW} method of the Windows API directly
	 * to obtain the {@code WIN32_FIND_DATA} for only the last segment of the given file-path.
	 * The Windows file-system is case-insensitive and we need to determine the real casing of the filename, therefore calling this search method is necessary.
	 * The only available Java APIs for this are {@link Path#toRealPath(java.nio.file.LinkOption...)} and {@link File#getCanonicalPath()}.
	 * Internally these methods also call the native {@code FindFirstFileW} method, but for each segment of the path.
	 * Because only the last segment is relevant and considered here, searching the real name of each parent is unnecessary and wasteful.
	 * Depending on the length of the path, this implementation is consequently multiple times, up to a magnitude faster than the mentioned Java API.
	 */
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
			MemorySegment lpFileName = allocateWideString(target, arena);
			@SuppressWarnings("static-access")
			MemorySegment lpFindFileData = arena.allocate(WIN32_FIND_DATAW.layout());
			MemorySegment capturedError = arena.allocate(LAST_ERROR_CAPTURE_LAYOUT);

			// https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-findfirstfilew
			MemorySegment handle = FindFirstFileW(lpFileName, lpFindFileData, capturedError);

			if (FileAPI.INVALID_HANDLE_VALUE().equals(handle)) {
				int error = GetLastError(capturedError);
				if (!(error == FileAPI.ERROR_FILE_NOT_FOUND() // file not found in existing parent directory
						|| error == FileAPI.ERROR_PATH_NOT_FOUND())) { // Not even the parent directory exists
					fileInfo.setError(IFileInfo.IO_ERROR);
				}
				return fileInfo;
			}
			FileAPI.FindClose(handle);

			convertFindDataWToFileInfo(lpFindFileData, fileInfo, fileName);
		} catch (IOException e) {
			// Leave alone and continue. The name is set before an IOException can be thrown
			fileInfo.setError(IFileInfo.IO_ERROR);
		}
		return fileInfo;
	}

	private static final StructLayout LAST_ERROR_CAPTURE_LAYOUT = Linker.Option.captureStateLayout();
	private static final VarHandle GET_LAST_ERROR_HANDLE = LAST_ERROR_CAPTURE_LAYOUT.varHandle(//
			MemoryLayout.PathElement.groupElement("GetLastError")); //$NON-NLS-1$
	private static final MethodHandle FIND_FIRST_FILE__W_HANDLE = Linker.nativeLinker().downcallHandle( //
			FileAPI.FindFirstFileW$address(), FileAPI.FindFirstFileW$descriptor(), //
			Linker.Option.captureCallState("GetLastError")); //$NON-NLS-1$

	/**
	 * Calls the native method {@code FindFirstFileW}, ensuring the {@code GetLastError()} method usable.
	 * <p>
	 * Enable "the linker option used to save portions of the execution state immediately after calling a foreign function associated with a downcall method handle,
	 * before it can be overwritten by the Java runtime".
	 * For more details, see https://docs.oracle.com/en/java/javase/25/core/checking-native-errors-using-errno.html
	 * </p>
	 * @see java.lang.foreign.Linker.Option#captureCallState(String...)
	 */
	private static MemorySegment FindFirstFileW(MemorySegment lpFileName, MemorySegment lpFindFileData, MemorySegment capturedError) {
		try {
			return (MemorySegment) FIND_FIRST_FILE__W_HANDLE.invokeExact(capturedError, lpFileName, lpFindFileData);
		} catch (Error | RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e); //$NON-NLS-1$
		}
	}

	private static int GetLastError(MemorySegment capturedError) {
		return (int) GET_LAST_ERROR_HANDLE.get(capturedError, 0L);
	}

	/**
	 * Sets the given {@link IFileInfo} to the given file.
	 *
	 * @implNote
	 * This implementation invokes the native Windows API methods {@code GetFileAttributesW} and {@code SetFileAttributesW} to read and write the file attributes.
	 * It allows to set the updated file attributes only once, after all modifications are applied and only if there is any overall change.
	 * Through {@link java.nio.file.attribute.DosFileAttributeView} each file attribute can only be set individually,
	 * which leads to one native file-attributes get and (up to) one set invocation per attribute.
	 * The NIO API does not provide means for batch updates.
	 * Since there are currently there file attributes considered, using the Java NIO API would consequently be up to three times slower.
	 */
	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		String longFilename = toLongWindowsPath(fileName);
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment lpFileName = allocateWideString(longFilename, arena);
			// https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-getfileattributesw
			int dwFileAttributes = FileAPI.GetFileAttributesW(lpFileName);
			if (dwFileAttributes == FileAPI.INVALID_FILE_ATTRIBUTES()) {
				return false;
			}
			if (dwFileAttributes == FileAPI.FILE_ATTRIBUTE_NORMAL()) {
				// Assume nothing is set, as the documentation of FILE_ATTRIBUTE_NORMAL states:
				// "A file that does not have other attributes set. This attribute is valid only when used alone."
				dwFileAttributes = 0;
			}
			int fileAttributes = dwFileAttributes;

			boolean archive = info.getAttribute(EFS.ATTRIBUTE_ARCHIVE);
			boolean readOnly = info.getAttribute(EFS.ATTRIBUTE_READ_ONLY);
			boolean hidden = info.getAttribute(EFS.ATTRIBUTE_HIDDEN);
			fileAttributes = set(fileAttributes, FileAPI.FILE_ATTRIBUTE_ARCHIVE(), archive);
			fileAttributes = set(fileAttributes, FileAPI.FILE_ATTRIBUTE_READONLY(), readOnly);
			fileAttributes = set(fileAttributes, FileAPI.FILE_ATTRIBUTE_HIDDEN(), hidden);

			if (dwFileAttributes == fileAttributes) {
				return true; // Everything is already up to date -> nothing to do
			}
			// https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-setfileattributesw
			return FileAPI.SetFileAttributesW(lpFileName, fileAttributes) != 0;
		}
	}

	public static String getShortPathName(String longPath) {
		longPath = toLongWindowsPath(longPath);
		// https://learn.microsoft.com/de-de/windows/win32/api/fileapi/nf-fileapi-getshortpathnamew
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment lpszLongPath = allocateWideString(longPath, arena);
			MemorySegment lpszShortPath = arena.allocate(lpszLongPath.byteSize()); // short name should be shorter -> will fit into array

			int newLength = FileAPI.GetShortPathNameW(lpszLongPath, lpszShortPath, longPath.length());
			if (0 < newLength && newLength < longPath.length()) { // zero means error and if not shorter it's not useful
				int offset = longPath.startsWith(WIN32_UNC_RAW_PATH_PREFIX) ? WIN32_UNC_RAW_PATH_PREFIX.length() : WIN32_RAW_PATH_PREFIX.length();
				return getWideString(lpszShortPath).substring(offset);
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

	@SuppressWarnings("static-access")
	private static void convertFindDataWToFileInfo(MemorySegment mem, FileInfo info, String fileName) throws IOException {
		/**
		 * For possible values of dwFileAttributes and their descriptions,
		 * see <a href="https://learn.microsoft.com/en-us/windows/win32/fileio/file-attribute-constants">File Attribute Constants</a>.
		 */
		int dwFileAttributes = WIN32_FIND_DATAW.dwFileAttributes(mem);
		Instant ftLastWriteTime = readFILETIME(WIN32_FIND_DATAW.ftLastWriteTime(mem));

		int nFileSizeHigh = WIN32_FIND_DATAW.nFileSizeHigh(mem);
		int nFileSizeLow = WIN32_FIND_DATAW.nFileSizeLow(mem);
		int dwReserved0 = WIN32_FIND_DATAW.dwReserved0(mem);
		String cFileName = getWideString(WIN32_FIND_DATAW.cFileName(mem));

		long fileLength = toLong(nFileSizeHigh, nFileSizeLow);

		info.setName(cFileName);
		info.setExists(true);
		info.setLastModified(ftLastWriteTime.toEpochMilli());
		info.setLength(fileLength);
		info.setDirectory(isSet(dwFileAttributes, FileAPI.FILE_ATTRIBUTE_DIRECTORY()));
		info.setAttribute(EFS.ATTRIBUTE_ARCHIVE, isSet(dwFileAttributes, FileAPI.FILE_ATTRIBUTE_ARCHIVE()));
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, isSet(dwFileAttributes, FileAPI.FILE_ATTRIBUTE_READONLY()));
		info.setAttribute(EFS.ATTRIBUTE_HIDDEN, isSet(dwFileAttributes, FileAPI.FILE_ATTRIBUTE_HIDDEN()));

		boolean isReparsePoint = isSet(dwFileAttributes, FileAPI.FILE_ATTRIBUTE_REPARSE_POINT());
		if (isReparsePoint && dwReserved0 == FileAPI.IO_REPARSE_TAG_SYMLINK()) {
			Path linkTarget = Files.readSymbolicLink(Path.of(fileName));
			info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
			info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, linkTarget.toString());
		}
	}

	private static final Instant WINDOWS_REFERENCE_DATE = LocalDateTime.of(1601, Month.JANUARY, 1, 0, 0).toInstant(ZoneOffset.UTC);

	// https://learn.microsoft.com/en-us/windows/win32/api/minwinbase/ns-minwinbase-filetime
	@SuppressWarnings("static-access")
	private static Instant readFILETIME(MemorySegment struct) {
		int low = FILETIME.dwLowDateTime(struct);
		int high = FILETIME.dwHighDateTime(struct);
		final long filetime = toLong(high, low);
		return WINDOWS_REFERENCE_DATE.plus(filetime / 10, ChronoUnit.MICROS);
	}

	// https://learn.microsoft.com/en-us/windows/win32/learnwin32/working-with-strings
	private static MemorySegment allocateWideString(String longPath, Arena arena) {
		return arena.allocateFrom(longPath, StandardCharsets.UTF_16LE);
	}

	private static String getWideString(MemorySegment memory) {
		return memory.getString(0, StandardCharsets.UTF_16LE);
	}

	// See also https://learn.microsoft.com/en-us/windows/win32/winprog/windows-data-types

	private static long toLong(int highDWORD, int lowDWORD) {
		return (long) highDWORD << 32 | lowDWORD & 0xffffffffL;
	}

	private static boolean isSet(int field, int bit) {
		return (field & bit) != 0;
	}

	private int set(int field, int bit, boolean isSet) {
		return isSet ? (field | bit) : (field & ~bit);
	}

}
