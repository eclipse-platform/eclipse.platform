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

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;

/**
 * Provides native file operations for Linux using the Java 25 Foreign Function &amp;
 * Memory (FFM) API instead of JNI.
 *
 * <p>This class calls the following libc functions via FFM downcall handles:</p>
 * <ul>
 *   <li>{@code statx(2)} &ndash; query file metadata (replaces {@code stat}/{@code lstat}).
 *       Uses the {@code statx} struct whose layout is ABI-stable across all Linux
 *       architectures since Linux 4.11, eliminating the per-architecture struct
 *       padding complexity of the classic {@code stat} struct.</li>
 *   <li>{@code chmod(2)} &ndash; change file permissions.</li>
 *   <li>{@code readlink(2)} &ndash; read the target of a symbolic link.</li>
 * </ul>
 *
 * <p>No Mac OS X specific code (chflags, UF_IMMUTABLE, SF_IMMUTABLE, tounicode,
 * CoreServices) is present. File names are always encoded as UTF-8, which is the
 * standard on modern Linux.</p>
 *
 * @since 1.11.500
 */
public final class LinuxFileNatives {

	// ---------- statx(2) flags -------------------------------------------------

	/** {@code AT_FDCWD} — interpret pathname relative to the current working directory. */
	private static final int AT_FDCWD = -100;

	/** {@code AT_SYMLINK_NOFOLLOW} — do not follow the final symlink in pathname. */
	private static final int AT_SYMLINK_NOFOLLOW = 0x100;

	/**
	 * {@code STATX_BASIC_STATS} — request all basic stat fields
	 * ({@code stx_mode}, {@code stx_size}, {@code stx_mtime}, etc.).
	 */
	private static final int STATX_BASIC_STATS = 0x07FF;

	/** {@code ENOENT} errno value: no such file or directory. */
	private static final int ENOENT = 2;

	// ---------- statx struct offsets -------------------------------------------
	//
	// The statx struct layout is defined in <linux/stat.h> and is ABI-stable
	// across all Linux architectures since Linux 4.11.  The offsets below are
	// the same on x86_64, aarch64, riscv64, ppc64le, s390x and every other
	// Linux architecture — there is no per-arch padding variation like in the
	// classic stat struct.
	//
	// Relevant fields used by this implementation:
	//
	//   offset 28:  __u16  stx_mode          (file type + permission bits)
	//   offset 40:  __u64  stx_size          (file size in bytes)
	//   offset 112: __s64  stx_mtime.tv_sec  (modification time, seconds)
	//   offset 120: __u32  stx_mtime.tv_nsec (modification time, nanoseconds)

	private static final long STATX_STRUCT_SIZE       = 256L;
	private static final long STATX_MODE_OFFSET       = 28L;
	private static final long STATX_SIZE_OFFSET       = 40L;
	private static final long STATX_MTIME_SEC_OFFSET  = 112L;
	private static final long STATX_MTIME_NSEC_OFFSET = 120L;

	// ---------- FFM handles and state ------------------------------------------

	private static final StructLayout CAPTURED_STATE_LAYOUT;
	private static final VarHandle    ERRNO_VH;
	private static final MethodHandle STATX_MH;
	private static final MethodHandle CHMOD_MH;
	private static final MethodHandle READLINK_MH;
	private static final boolean      AVAILABLE;

	static {
		StructLayout capturedStateLayout = null;
		VarHandle    errnoVH             = null;
		MethodHandle statxMH             = null;
		MethodHandle chmodMH             = null;
		MethodHandle readlinkMH          = null;
		boolean      available           = false;

		try {
			Linker       linker       = Linker.nativeLinker();
			SymbolLookup lookup       = linker.defaultLookup();
			Linker.Option captureErrno = Linker.Option.captureCallState("errno"); //$NON-NLS-1$

			capturedStateLayout = Linker.Option.captureStateLayout();
			errnoVH = capturedStateLayout.varHandle(
					MemoryLayout.PathElement.groupElement("errno")); //$NON-NLS-1$

			// int statx(int dirfd, const char *pathname, int flags,
			//           unsigned int mask, struct statx *statxbuf)
			statxMH = linker.downcallHandle(
					lookup.find("statx").orElseThrow(), //$NON-NLS-1$
					FunctionDescriptor.of(
							ValueLayout.JAVA_INT,   // return: int
							ValueLayout.JAVA_INT,   // dirfd
							ValueLayout.ADDRESS,    // pathname (const char *)
							ValueLayout.JAVA_INT,   // flags
							ValueLayout.JAVA_INT,   // mask (unsigned int)
							ValueLayout.ADDRESS),   // statxbuf (struct statx *)
					captureErrno);

			// int chmod(const char *pathname, mode_t mode)
			// mode_t is unsigned int (32-bit) on all Linux architectures
			chmodMH = linker.downcallHandle(
					lookup.find("chmod").orElseThrow(), //$NON-NLS-1$
					FunctionDescriptor.of(
							ValueLayout.JAVA_INT,   // return: int
							ValueLayout.ADDRESS,    // pathname (const char *)
							ValueLayout.JAVA_INT),  // mode (mode_t)
					captureErrno);

			// ssize_t readlink(const char *pathname, char *buf, size_t bufsiz)
			// ssize_t and size_t are 64-bit on all 64-bit Linux architectures
			readlinkMH = linker.downcallHandle(
					lookup.find("readlink").orElseThrow(), //$NON-NLS-1$
					FunctionDescriptor.of(
							ValueLayout.JAVA_LONG,  // return: ssize_t
							ValueLayout.ADDRESS,    // pathname (const char *)
							ValueLayout.ADDRESS,    // buf (char *)
							ValueLayout.JAVA_LONG), // bufsiz (size_t)
					captureErrno);

			available = true;
		} catch (Exception e) {
			// FFM API is not available or required libc symbols could not be found.
			// isAvailable() returns false and all callers fall back gracefully.
		}

		AVAILABLE             = available;
		CAPTURED_STATE_LAYOUT = capturedStateLayout;
		ERRNO_VH              = errnoVH;
		STATX_MH              = statxMH;
		CHMOD_MH              = chmodMH;
		READLINK_MH           = readlinkMH;
	}

	// ---------- Public API -----------------------------------------------------

	/**
	 * Returns {@code true} if the FFM downcall handles were initialised
	 * successfully and this class can be used.
	 *
	 * @return {@code true} when FFM-based native file operations are available
	 */
	public static boolean isAvailable() {
		return AVAILABLE;
	}

	/**
	 * Returns the bitmask of EFS file attributes supported by this implementation.
	 *
	 * @return supported EFS attribute bitmask, or {@code -1} if FFM is not available
	 */
	public static int getSupportedAttributes() {
		if (!AVAILABLE) {
			return -1;
		}
		// No ATTRIBUTE_IMMUTABLE: Linux does not support the BSD chflags mechanism.
		return EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE
				| EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET
				| EFS.ATTRIBUTE_OWNER_READ  | EFS.ATTRIBUTE_OWNER_WRITE  | EFS.ATTRIBUTE_OWNER_EXECUTE
				| EFS.ATTRIBUTE_GROUP_READ  | EFS.ATTRIBUTE_GROUP_WRITE  | EFS.ATTRIBUTE_GROUP_EXECUTE
				| EFS.ATTRIBUTE_OTHER_READ  | EFS.ATTRIBUTE_OTHER_WRITE  | EFS.ATTRIBUTE_OTHER_EXECUTE;
	}

	/**
	 * Fetches file information for the given path.
	 *
	 * <p>Follows symlinks for the file metadata ({@code stat} semantics) but
	 * also reports whether the path itself is a symlink and its target
	 * ({@code lstat} semantics for the link itself).</p>
	 *
	 * @param fileName the absolute path of the file
	 * @return a {@link FileInfo} populated from the file's stat data
	 */
	public static FileInfo fetchFileInfo(String fileName) {
		FileInfo info = null;

		try (Arena arena = Arena.ofConfined()) {
			MemorySegment capturedState = arena.allocate(CAPTURED_STATE_LAYOUT);
			MemorySegment nameSeg       = arena.allocateFrom(fileName, StandardCharsets.UTF_8);
			MemorySegment statxBuf      = arena.allocate(STATX_STRUCT_SIZE, 8L);

			// First call: lstat semantics (AT_SYMLINK_NOFOLLOW) to detect symlinks
			int result = (int) STATX_MH.invoke(capturedState, AT_FDCWD, nameSeg,
					AT_SYMLINK_NOFOLLOW, STATX_BASIC_STATS, statxBuf);

			if (result == 0) {
				int mode = Short.toUnsignedInt(statxBuf.get(ValueLayout.JAVA_SHORT, STATX_MODE_OFFSET));

				if ((mode & LinuxFileFlags.S_IFMT) == LinuxFileFlags.S_IFLNK) {
					// The path is a symlink: follow it to get the target's metadata
					MemorySegment targetBuf = arena.allocate(STATX_STRUCT_SIZE, 8L);
					int followResult = (int) STATX_MH.invoke(capturedState, AT_FDCWD, nameSeg,
							0 /* follow symlinks */, STATX_BASIC_STATS, targetBuf);

					if (followResult == 0) {
						info = statxBufToFileInfo(targetBuf);
					} else {
						// Broken symlink (target does not exist)
						info = new FileInfo();
						int errno = (int) ERRNO_VH.get(capturedState, 0L);
						if (errno != ENOENT) {
							info.setError(IFileInfo.IO_ERROR);
						}
					}
					info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);

					// Read the symlink target string
					MemorySegment linkBuf = arena.allocate(LinuxFileFlags.PATH_MAX);
					long len = (long) READLINK_MH.invoke(capturedState, nameSeg, linkBuf,
							(long) LinuxFileFlags.PATH_MAX);
					if (len > 0) {
						byte[] linkBytes = linkBuf.asSlice(0L, len).toArray(ValueLayout.JAVA_BYTE);
						info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET,
								new String(linkBytes, StandardCharsets.UTF_8));
					}
				} else {
					info = statxBufToFileInfo(statxBuf);
				}
			} else {
				info = new FileInfo();
				int errno = (int) ERRNO_VH.get(capturedState, 0L);
				if (errno != ENOENT) {
					info.setError(IFileInfo.IO_ERROR);
				}
			}
		} catch (Throwable e) {
			info = new FileInfo();
			info.setError(IFileInfo.IO_ERROR);
		}

		if (info.getName() == null) {
			// Use the basename of the supplied path as the file name
			info.setName(new File(fileName).getName());
		}
		return info;
	}

	/**
	 * Updates the file permissions for the given path.
	 *
	 * @param fileName the absolute path of the file
	 * @param info     the desired file attributes
	 * @param options  (unused; reserved for future use)
	 * @return {@code true} if the chmod call succeeded
	 */
	public static boolean putFileInfo(String fileName, IFileInfo info, int options) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment capturedState = arena.allocate(CAPTURED_STATE_LAYOUT);
			MemorySegment nameSeg       = arena.allocateFrom(fileName, StandardCharsets.UTF_8);

			int mode = 0;
			if (info.getAttribute(EFS.ATTRIBUTE_OWNER_READ))    mode |= LinuxFileFlags.S_IRUSR;
			if (info.getAttribute(EFS.ATTRIBUTE_OWNER_WRITE))   mode |= LinuxFileFlags.S_IWUSR;
			if (info.getAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE)) mode |= LinuxFileFlags.S_IXUSR;
			if (info.getAttribute(EFS.ATTRIBUTE_GROUP_READ))    mode |= LinuxFileFlags.S_IRGRP;
			if (info.getAttribute(EFS.ATTRIBUTE_GROUP_WRITE))   mode |= LinuxFileFlags.S_IWGRP;
			if (info.getAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE)) mode |= LinuxFileFlags.S_IXGRP;
			if (info.getAttribute(EFS.ATTRIBUTE_OTHER_READ))    mode |= LinuxFileFlags.S_IROTH;
			if (info.getAttribute(EFS.ATTRIBUTE_OTHER_WRITE))   mode |= LinuxFileFlags.S_IWOTH;
			if (info.getAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE)) mode |= LinuxFileFlags.S_IXOTH;

			int code = (int) CHMOD_MH.invoke(capturedState, nameSeg, mode);
			return code == 0;
		} catch (Throwable e) {
			return false;
		}
	}

	// ---------- Private helpers ------------------------------------------------

	/**
	 * Reads the mode, size and mtime fields from a {@code struct statx} memory
	 * segment and converts them into a {@link FileInfo}.
	 */
	private static FileInfo statxBufToFileInfo(MemorySegment statxBuf) {
		int  mode       = Short.toUnsignedInt(statxBuf.get(ValueLayout.JAVA_SHORT, STATX_MODE_OFFSET));
		long size       = statxBuf.get(ValueLayout.JAVA_LONG, STATX_SIZE_OFFSET);
		long mtimeSec   = statxBuf.get(ValueLayout.JAVA_LONG, STATX_MTIME_SEC_OFFSET);
		long mtimeNsec  = Integer.toUnsignedLong(statxBuf.get(ValueLayout.JAVA_INT, STATX_MTIME_NSEC_OFFSET));
		return new LinuxStructStat(mode, size, mtimeSec, mtimeNsec).toFileInfo();
	}

	private LinuxFileNatives() {
		// not instantiable
	}

}
