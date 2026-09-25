/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local.nio;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.Convert;
import org.eclipse.core.runtime.Platform;
import org.linux.LibC;
import org.linux.dirent;
import org.linux.statx;
import org.linux.statx_timestamp;

/**
 * Lists a directory together with the attributes of its entries on Linux, with
 * one {@code readdir} and one {@code statx} per entry through the Foreign
 * Function &amp; Memory API instead of a {@code java.nio} lookup per file.
 */
final class LinuxDirectoryReader {

	private static final IFileInfo[] NO_INFOS = {};

	private static final Charset PLATFORM_CHARSET = Platform.getSystemCharset();

	private static final MemoryLayout ERRNO_LAYOUT = Linker.Option.captureStateLayout();
	private static final VarHandle ERRNO = ERRNO_LAYOUT
			.varHandle(MemoryLayout.PathElement.groupElement("errno")); //$NON-NLS-1$

	// jextract emits no captureCallState, and errno is what tells a missing file from an
	// unreadable one.
	private static final MethodHandle STATX = Platform.OS.isLinux() ? statxWithErrno() : null;

	private LinuxDirectoryReader() {
	}

	private static MethodHandle statxWithErrno() {
		try {
			return Linker.nativeLinker().downcallHandle(LibC.statx$address(), LibC.statx$descriptor(),
					Linker.Option.captureCallState("errno")); //$NON-NLS-1$
		} catch (NoSuchElementException | IllegalCallerException | UnsupportedOperationException
				| ExceptionInInitializerError | NoClassDefFoundError e) {
			// statx needs glibc 2.28, and native access may be denied outright
			return null;
		}
	}

	static boolean isAvailable() {
		return STATX != null;
	}

	/**
	 * The native buffers these calls need, held per thread and reused, so that a
	 * listing allocates nothing per entry beyond its {@link FileInfo}.
	 */
	private static final class Scratch {
		private static final ThreadLocal<Scratch> CURRENT = ThreadLocal.withInitial(Scratch::new);

		private final Arena arena = Arena.ofAuto();
		final MemorySegment errno = arena.allocate(ERRNO_LAYOUT);
		final MemorySegment stat = arena.allocate(statx.sizeof());
		final MemorySegment linkTarget = arena.allocate(LibC.PATH_MAX());
		private final MemorySegment path = arena.allocate(LibC.PATH_MAX() + 1);

		/** The file name as a NUL terminated C string. */
		MemorySegment path(String fileName) {
			byte[] bytes = Convert.toPlatformBytes(fileName);
			MemorySegment target = bytes.length < path.byteSize() ? path : arena.allocate(bytes.length + 1);
			MemorySegment.copy(bytes, 0, target, ValueLayout.JAVA_BYTE, 0, bytes.length);
			target.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
			return target;
		}
	}

	static IFileInfo[] listDirectoryAndGetFileInfos(String fileName) {
		Scratch scratch = Scratch.CURRENT.get();
		MemorySegment dir = LibC.opendir(scratch.path(fileName));
		if (dir.address() == 0) {
			return NO_INFOS;
		}
		try {
			int dirFd = LibC.dirfd(dir);
			List<IFileInfo> infos = new ArrayList<>();
			while (true) {
				MemorySegment entry = LibC.readdir(dir);
				if (entry.address() == 0) {
					return infos.toArray(IFileInfo[]::new);
				}
				// d_name is NUL terminated inside the struct, so bounding the entry by its
				// declared size never cuts the name short.
				MemorySegment name = dirent.d_name(entry.reinterpret(dirent.sizeof()));
				String nameString = name.getString(0, PLATFORM_CHARSET);
				if (!".".equals(nameString) && !"..".equals(nameString)) { //$NON-NLS-1$ //$NON-NLS-2$
					infos.add(fetchFileInfo(scratch, dirFd, name, nameString));
				}
			}
		} finally {
			LibC.closedir(dir);
		}
	}

	/**
	 * Mirrors {@link PosixHandler#fetchFileInfo(String)}: a symbolic link carries
	 * the attributes of its target, a missing file is no error, and any other
	 * failure is an I/O error.
	 */
	private static FileInfo fetchFileInfo(Scratch scratch, int dirFd, MemorySegment path, String name) {
		FileInfo info = new FileInfo(name);
		int errno = statx(scratch, dirFd, path, false);
		if (errno == 0 && (mode(scratch.stat) & LibC.S_IFMT()) == LibC.S_IFLNK()) {
			info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
			long length = LibC.readlinkat(dirFd, path, scratch.linkTarget, scratch.linkTarget.byteSize());
			if (length > 0) {
				byte[] target = scratch.linkTarget.asSlice(0, length).toArray(ValueLayout.JAVA_BYTE);
				info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, new String(target, PLATFORM_CHARSET));
			}
			errno = statx(scratch, dirFd, path, true);
		}
		if (errno == LibC.ENOENT()) {
			return info;
		}
		if (errno != 0) {
			info.setError(IFileInfo.IO_ERROR);
			return info;
		}
		MemorySegment stat = scratch.stat;
		int mode = mode(stat);
		MemorySegment modified = statx.stx_mtime(stat);
		info.setExists(true);
		info.setLastModified(statx_timestamp.tv_sec(modified) * 1_000
				+ Integer.toUnsignedLong(statx_timestamp.tv_nsec(modified)) / 1_000_000);
		info.setLength(statx.stx_size(stat));
		info.setDirectory((mode & LibC.S_IFMT()) == LibC.S_IFDIR());
		info.setAttribute(EFS.ATTRIBUTE_OWNER_READ, (mode & LibC.S_IRUSR()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_OWNER_WRITE, (mode & LibC.S_IWUSR()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE, (mode & LibC.S_IXUSR()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_GROUP_READ, (mode & LibC.S_IRGRP()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, (mode & LibC.S_IWGRP()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE, (mode & LibC.S_IXGRP()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_OTHER_READ, (mode & LibC.S_IROTH()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_OTHER_WRITE, (mode & LibC.S_IWOTH()) != 0);
		info.setAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE, (mode & LibC.S_IXOTH()) != 0);
		return info;
	}

	/** Fills {@code scratch.stat} and returns 0, or the errno of the failure. */
	private static int statx(Scratch scratch, int dirFd, MemorySegment path, boolean followLinks) {
		int flags = LibC.AT_STATX_SYNC_AS_STAT() | (followLinks ? 0 : LibC.AT_SYMLINK_NOFOLLOW());
		int mask = LibC.STATX_TYPE() | LibC.STATX_MODE() | LibC.STATX_SIZE() | LibC.STATX_MTIME();
		int result;
		try {
			result = (int) STATX.invokeExact(scratch.errno, dirFd, path, flags, mask, scratch.stat);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
		return result == 0 ? 0 : (int) ERRNO.get(scratch.errno, 0L);
	}

	private static int mode(MemorySegment stat) {
		return Short.toUnsignedInt(statx.stx_mode(stat));
	}
}
