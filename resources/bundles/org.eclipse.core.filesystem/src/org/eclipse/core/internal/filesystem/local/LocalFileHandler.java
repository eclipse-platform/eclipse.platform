/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.filesystem.local;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;

/**
 * Native handler that delegates to the LocalFileNatives.
 */
public class LocalFileHandler extends NativeHandler {
	@Override
	public int getSupportedAttributes() {
		return LocalFileNatives.attributes();
	}

	@Override
	public FileInfo fetchFileInfo(String fileName) {
		return LocalFileNatives.fetchFileInfo(fileName);
	}

	@Override
	public boolean putFileInfo(String fileName, IFileInfo info, int options) {
		return LocalFileNatives.putFileInfo(fileName, info, options);
	}
}
