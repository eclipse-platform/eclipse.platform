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
package org.eclipse.core.internal.filesystem.local.ffm;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.Convert;
import org.eclipse.core.internal.filesystem.local.NativeHandler;
import org.eclipse.core.runtime.Platform;
import org.linux.LibC;
import org.linux.dirent;
import org.linux.statx;
import org.linux.statx_timestamp;

/**
 * Reads file attributes on Linux through the Foreign Function &amp; Memory API,
 * without a platform specific native library. A directory is listed with one
 * {@code opendir} and one {@code statx} per entry, so nothing is allocated per
 * file beyond the resulting {@link FileInfo}.
 */
public final class LinuxFfmHandler extends NativeHandler {

	private static final int ATTRIBUTES = EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE | EFS.ATTRIBUTE_SYMLINK
			| EFS.ATTRIBUTE_LINK_TARGET | EFS.ATTRIBUTE_OWNER_READ | EFS.ATTRIBUTE_OWNER_WRITE
			| EFS.ATTRIBUTE_OWNER_EXECUTE | EFS.ATTRIBUTE_GROUP_READ | EFS.ATTRIBUTE_GROUP_WRITE
			| EFS.ATTRIBUTE_GROUP_EXECUTE | EFS.ATTRIBUTE_OTHER_READ | EFS.ATTRIBUTE_OTHER_WRITE
			| EFS.ATTRIBUTE_OTHER_EXECUTE;

	private static final boolean USE_MILLISECOND_RESOLUTION = Boolean.parseBoolean(System.getProperty(
			"eclipse.filesystem.useNatives.modificationTimestampMillisecondsResolution", "true")); //$NON-NLS-1$ //$NON-NLS-2$

	private static final Charset PLATFORM_CHARSET = Platform.getSystemCharset();

	private static final IFileInfo[] NO_INFOS = {};

	/**
	 * Whether this handler can be used, which needs a Linux kernel offering
	 * {@code statx} and a libc exporting it.
	 */
	public static boolean isAvailable() {
		return Platform.OS.isLinux() && LinuxLibC.isAvailable();
	}

	@Override
	public int getSupportedAttributes() {
		return ATTRIBUTES;
	}

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		Scratch scratch = Scratch.current();
		FileInfo info = fetchFileInfo(scratch, LibC.AT_FDCWD(), scratch.path(fileName), null, true);
		if (info.getName().isEmpty()) {
			// On a case insensitive file system the real name is not known, and finding it
			// out is expensive, so the name as given is used even though its case may
			// differ.
			info.setName(new File(fileName).getName());
		}
		return info;
	}

	@Override
	public String[] listDirectoryNames(String fileName) {
		Scratch scratch = Scratch.current();
		MemorySegment dir = openDirectory(scratch, fileName);
		if (dir == null) {
			return EMPTY_STRING_ARRAY;
		}
		try {
			List<String> names = new ArrayList<>();
			forEachEntry(dir, (_, name) -> names.add(name));
			return names.toArray(String[]::new);
		} finally {
			LibC.closedir(dir);
		}
	}

	@Override
	public IFileInfo[] listDirectoryAndGetFileInfos(String fileName) {
		Scratch scratch = Scratch.current();
		MemorySegment dir = openDirectory(scratch, fileName);
		if (dir == null) {
			return NO_INFOS;
		}
		try {
			int dirFd = LibC.dirfd(dir);
			List<IFileInfo> infos = new ArrayList<>();
			// d_name is already a NUL terminated C string, so it is handed to statx as it
			// is instead of being copied into a buffer of our own.
			forEachEntry(dir,
					(entry, name) -> infos.add(fetchFileInfo(scratch, dirFd, dirent.d_name(entry), name, false)));
			return infos.toArray(IFileInfo[]::new);
		} finally {
			LibC.closedir(dir);
		}
	}

	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		int mode = 0;
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_READ)) {
			mode |= LibC.S_IRUSR();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_WRITE)) {
			mode |= LibC.S_IWUSR();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE)) {
			mode |= LibC.S_IXUSR();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_READ)) {
			mode |= LibC.S_IRGRP();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_WRITE)) {
			mode |= LibC.S_IWGRP();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE)) {
			mode |= LibC.S_IXGRP();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_READ)) {
			mode |= LibC.S_IROTH();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_WRITE)) {
			mode |= LibC.S_IWOTH();
		}
		if (info.getAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE)) {
			mode |= LibC.S_IXOTH();
		}
		Scratch scratch = Scratch.current();
		return LibC.chmod(scratch.path(fileName), mode) == 0;
	}

	/** What is passed to {@link #forEachEntry} for every entry of a directory. */
	private interface EntryVisitor {
		void visit(MemorySegment entry, String name);
	}

	/**
	 * The native buffers these calls need, held per thread and reused, because
	 * allocating them per call made this one of the largest allocators in the whole
	 * IDE during a refresh. They are freed with the thread, since the arena is
	 * garbage collected.
	 */
	private static final class Scratch {
		private static final ThreadLocal<Scratch> CURRENT = ThreadLocal.withInitial(Scratch::new);

		private final Arena arena = Arena.ofAuto();
		final MemorySegment errno = LinuxLibC.allocateErrno(arena);
		final MemorySegment stat = arena.allocate(statx.sizeof());
		private final MemorySegment path = arena.allocate(LibC.PATH_MAX() + 1);
		private MemorySegment linkTarget;

		static Scratch current() {
			return CURRENT.get();
		}

		/** The file name in the reusable buffer, as a NUL terminated C string. */
		MemorySegment path(String fileName) {
			byte[] bytes = Convert.toPlatformBytes(fileName);
			MemorySegment target = bytes.length < path.byteSize() ? path : arena.allocate(bytes.length + 1);
			MemorySegment.copy(bytes, 0, target, ValueLayout.JAVA_BYTE, 0, bytes.length);
			target.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
			return target;
		}

		MemorySegment linkTarget() {
			if (linkTarget == null) {
				linkTarget = arena.allocate(LibC.PATH_MAX());
			}
			return linkTarget;
		}
	}

	private static MemorySegment openDirectory(Scratch scratch, String fileName) {
		MemorySegment dir = LibC.opendir(scratch.path(fileName));
		return dir.address() == 0 ? null : dir;
	}

	/**
	 * Reads the directory to its end, passing every entry except {@code .} and
	 * {@code ..} to the visitor together with its name.
	 */
	private static void forEachEntry(MemorySegment dir, EntryVisitor visitor) {
		while (true) {
			MemorySegment entry = LibC.readdir(dir);
			if (entry.address() == 0) {
				return;
			}
			// readdir hands out a pointer into a buffer of unknown size, so the entry has
			// to be given one before anything can be read from it. d_name is NUL terminated
			// well inside the struct, so nothing past the entry itself is ever read.
			MemorySegment bounded = entry.reinterpret(dirent.sizeof());
			String name = dirent.d_name(bounded).getString(0, PLATFORM_CHARSET);
			if (!".".equals(name) && !"..".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
				visitor.visit(bounded, name);
			}
		}
	}

	/**
	 * Stats {@code path} relative to {@code dirFd} and converts the result, keeping
	 * the behaviour of the JNI handler: a symbolic link is reported with the
	 * attributes of its target plus the link target as a string attribute, and a
	 * stat that fails is converted as if it had returned an all zero struct, so a
	 * file that does not exist yields an info that does not exist rather than an
	 * error.
	 * <p>
	 * {@code defaultAttributesWhenMissing} reproduces that a single file which is
	 * simply not there is reported with the default attributes of a fresh
	 * {@link FileInfo}, as every other handler does, while a directory entry that
	 * disappeared between listing and stating it is reported with none.
	 */
	private static FileInfo fetchFileInfo(Scratch scratch, int dirFd, MemorySegment path, String name,
			boolean defaultAttributesWhenMissing) {
		if (LinuxLibC.statx(scratch.errno, dirFd, path, false, scratch.stat) != 0) {
			int errno = LinuxLibC.errno(scratch.errno);
			if (defaultAttributesWhenMissing && errno == LibC.ENOENT()) {
				return named(name);
			}
			return convert(name, errno, null, null);
		}
		if ((mode(scratch.stat) & LibC.S_IFMT()) != LibC.S_IFLNK()) {
			return convert(name, 0, scratch.stat, null);
		}
		// A link is reported with the attributes of what it points at, so it is stated
		// a second time, following the link this time.
		int errno = 0;
		MemorySegment stat = scratch.stat;
		if (LinuxLibC.statx(scratch.errno, dirFd, path, true, stat) != 0) {
			errno = LinuxLibC.errno(scratch.errno);
			stat = null;
		}
		return convert(name, errno, stat, readLinkTarget(scratch, dirFd, path));
	}

	private static int mode(MemorySegment stat) {
		return Short.toUnsignedInt(statx.stx_mode(stat));
	}

	private static FileInfo named(String name) {
		FileInfo info = new FileInfo();
		if (name != null) {
			info.setName(name);
		}
		return info;
	}

	private static String readLinkTarget(Scratch scratch, int dirFd, MemorySegment path) {
		MemorySegment buffer = scratch.linkTarget();
		long length = LibC.readlinkat(dirFd, path, buffer, buffer.byteSize());
		if (length <= 0) {
			return ""; //$NON-NLS-1$
		}
		byte[] target = buffer.asSlice(0, length).toArray(ValueLayout.JAVA_BYTE);
		return new String(target, PLATFORM_CHARSET);
	}

	/**
	 * Builds the info of one file. A <code>null</code> {@code stat} stands for a
	 * stat that failed and is treated as an all zero struct, and a non
	 * <code>null</code> {@code linkTarget} marks the file as a symbolic link, empty
	 * if the target could not be read.
	 */
	private static FileInfo convert(String name, int errno, MemorySegment stat, String linkTarget) {
		FileInfo info = named(name);
		if (linkTarget != null) {
			info.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
			if (!linkTarget.isEmpty()) {
				info.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, linkTarget);
			}
		}
		if (errno != 0 && errno != LibC.ENOENT()) {
			info.setError(IFileInfo.IO_ERROR);
			return info;
		}
		info.setExists(errno != LibC.ENOENT());
		int mode = stat == null ? 0 : mode(stat);
		if (stat != null) {
			info.setLength(statx.stx_size(stat));
			MemorySegment modified = statx.stx_mtime(stat);
			long lastModified = statx_timestamp.tv_sec(modified) * 1_000;
			if (USE_MILLISECOND_RESOLUTION) {
				lastModified += Integer.toUnsignedLong(statx_timestamp.tv_nsec(modified)) / 1_000_000L;
			}
			info.setLastModified(lastModified);
		}
		if ((mode & LibC.S_IFMT()) == LibC.S_IFDIR()) {
			info.setDirectory(true);
		}
		// FileInfo starts out owner readable and writable, so those two are cleared
		// rather than set.
		if ((mode & LibC.S_IRUSR()) == 0) {
			info.setAttribute(EFS.ATTRIBUTE_OWNER_READ, false);
		}
		if ((mode & LibC.S_IWUSR()) == 0) {
			info.setAttribute(EFS.ATTRIBUTE_OWNER_WRITE, false);
		}
		if ((mode & LibC.S_IXUSR()) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE, true);
		}
		if ((mode & LibC.S_IRGRP()) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_READ, true);
		}
		if ((mode & LibC.S_IWGRP()) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, true);
		}
		if ((mode & LibC.S_IXGRP()) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE, true);
		}
		if ((mode & LibC.S_IROTH()) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OTHER_READ, true);
		}
		if ((mode & LibC.S_IWOTH()) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OTHER_WRITE, true);
		}
		if ((mode & LibC.S_IXOTH()) != 0) {
			info.setAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE, true);
		}
		return info;
	}
}
