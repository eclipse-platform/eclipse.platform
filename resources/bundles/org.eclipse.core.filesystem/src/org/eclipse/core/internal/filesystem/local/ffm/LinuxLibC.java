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

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.NoSuchElementException;
import org.linux.LibC;

/**
 * The one call that the generated bindings cannot express: {@code statx} with
 * its {@code errno}, which is what tells a file that is not there from one that
 * could not be read. The descriptor and the symbol come from the generated
 * bindings, only the capture option is added here.
 */
final class LinuxLibC {

	private static final Linker.Option CAPTURE_ERRNO = Linker.Option.captureCallState("errno"); //$NON-NLS-1$
	private static final MemoryLayout ERRNO_LAYOUT = Linker.Option.captureStateLayout();
	private static final VarHandle ERRNO = ERRNO_LAYOUT
			.varHandle(MemoryLayout.PathElement.groupElement("errno")); //$NON-NLS-1$

	private static final MethodHandle STATX;
	private static final boolean AVAILABLE;

	static {
		MethodHandle statx = null;
		try {
			statx = Linker.nativeLinker().downcallHandle(LibC.statx$address(), LibC.statx$descriptor(),
					CAPTURE_ERRNO);
		} catch (NoSuchElementException | IllegalCallerException | UnsupportedOperationException
				| ExceptionInInitializerError | NoClassDefFoundError e) {
			// statx needs glibc 2.28, and native access may be denied outright
		}
		STATX = statx;
		AVAILABLE = statx != null;
	}

	static boolean isAvailable() {
		return AVAILABLE;
	}

	static MemorySegment allocateErrno(Arena arena) {
		return arena.allocate(ERRNO_LAYOUT);
	}

	static int errno(MemorySegment capturedState) {
		return (int) ERRNO.get(capturedState, 0L);
	}

	/**
	 * Fills {@code buffer} with the basic statx fields of the given path and returns
	 * 0 on success, leaving the reason in {@code errno} otherwise.
	 */
	static int statx(MemorySegment errno, int dirFd, MemorySegment path, boolean followLinks, MemorySegment buffer) {
		int flags = LibC.AT_STATX_SYNC_AS_STAT() | (followLinks ? 0 : LibC.AT_SYMLINK_NOFOLLOW());
		int mask = LibC.STATX_TYPE() | LibC.STATX_MODE() | LibC.STATX_SIZE() | LibC.STATX_MTIME();
		try {
			return (int) STATX.invokeExact(errno, dirFd, path, flags, mask, buffer);
		} catch (Throwable e) {
			if (e instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (e instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException(e);
		}
	}

	private LinuxLibC() {
	}
}
