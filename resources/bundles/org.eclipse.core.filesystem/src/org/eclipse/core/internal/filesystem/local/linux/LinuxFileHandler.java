/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local.linux;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.NativeHandler;
import org.eclipse.core.internal.filesystem.local.nio.PosixHandler;

/**
 * Native handler that delegates to LinuxFileNatives
 */
public class LinuxFileHandler extends NativeHandler {
	private final PosixHandler posixHandler = new PosixHandler();

	@Override
	public int getSupportedAttributes() {
		return posixHandler.getSupportedAttributes();
	}

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		return posixHandler.fetchFileInfo(fileName);
	}

	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		return posixHandler.putFileInfo(fileName, info, options);
	}

	@Override
	public String[] listDirectoryNames(String fileName) {
		return LinuxFileNatives.listDirectoryNames(fileName);
	}

	@Override
	public IFileInfo[] listDirectoryAndGetFileInfos(String fileName) {
		return LinuxFileNatives.listDirectoryAndGetFileInfos(fileName);
	}
}
