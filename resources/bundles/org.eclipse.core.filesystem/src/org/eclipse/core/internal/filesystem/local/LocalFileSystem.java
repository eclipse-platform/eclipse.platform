/*******************************************************************************
 * Copyright (c) 2005, 2025 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [170317] add symbolic link support to API
 * Martin Oberhuber (Wind River) - [183137] liblocalfile for solaris-sparc
 * Martin Oberhuber (Wind River) - [184433] liblocalfile for Linux x86_64
 * Martin Oberhuber (Wind River) - [184534] get attributes from native lib
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import java.io.File;
import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;

/**
 * File system provider for the "file" scheme.  This file system provides access to
 * the local file system that is available via java.io.File.
 */
public class LocalFileSystem extends FileSystem {
	/**
	 * Cached constant indicating if the current OS is Mac OSX
	 */
	static final boolean MACOSX = Platform.OS.isMac();

	/**
	 * Whether the current file system is case sensitive
	 */
	private static final boolean CASE_SENSITIVE = !MACOSX && new java.io.File("a").compareTo(new java.io.File("A")) != 0; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * The attributes of this file system. The initial value of -1 is used
	 * to indicate that the attributes have not yet been computed.
	 */
	private int attributes = -1;
	/**
	 * The singleton instance of this file system.
	 */
	private static final IFileSystem INSTANCE = EFS.getLocalFileSystem();

	/**
	 * Returns the instance of this file system
	 *
	 * @return The instance of this file system.
	 */
	public static IFileSystem getInstance() {
		return INSTANCE;
	}

	@Override
	public int attributes() {
		if (attributes != -1) {
			return attributes;
		}
		attributes = 0;

		//try to query supported attributes from native lib impl
		int nativeAttributes = LocalFileNativesManager.getSupportedAttributes();
		if (nativeAttributes >= 0) {
			attributes = nativeAttributes;
			return attributes;
		}

		//fallback for older lib: compute attributes as known before
		//all known platforms with native implementation support the read only flag
		attributes |= EFS.ATTRIBUTE_READ_ONLY;

		// this must be kept in sync with functionality of previous libs not implementing nativeAttributes method
		String os = Platform.getOS();
		if (Constants.OS_WIN32.equals(os)) {
			attributes |= EFS.ATTRIBUTE_ARCHIVE | EFS.ATTRIBUTE_HIDDEN;
		} else if (Constants.OS_LINUX.equals(os) || (Constants.OS_SOLARIS.equals(os) && Constants.ARCH_SPARC.equals(Platform.getOSArch()))) {
			attributes |= EFS.ATTRIBUTE_EXECUTABLE | EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET;
		} else if (Constants.OS_FREEBSD.equals(os)) {
			attributes |= EFS.ATTRIBUTE_EXECUTABLE | EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET;
		} else if (Constants.OS_MACOSX.equals(os) || Constants.OS_HPUX.equals(os) || Constants.OS_QNX.equals(os)) {
			attributes |= EFS.ATTRIBUTE_EXECUTABLE;
		}
		return attributes;
	}

	@Override
	public boolean canDelete() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public IFileStore fromLocalFile(File file) {
		return new LocalFile(file);
	}

	@Override
	public IFileStore getStore(IPath path) {
		return new LocalFile(path.toFile());
	}

	@Override
	public IFileStore getStore(URI uri) {
		return new LocalFile(new File(uri.getSchemeSpecificPart()));
	}

	@Override
	public boolean isCaseSensitive() {
		return CASE_SENSITIVE;
	}
}
