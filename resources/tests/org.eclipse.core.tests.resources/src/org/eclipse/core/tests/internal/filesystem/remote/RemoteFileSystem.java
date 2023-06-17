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
package org.eclipse.core.tests.internal.filesystem.remote;

import java.net.URI;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileSystem;

/**
 * A test file system that mocks remote file system and keeps everything in memory.
 */
public class RemoteFileSystem extends MemoryFileSystem {
	public static final String SCHEME_REMOTE = "remote";

	public RemoteFileSystem() {
		super();
	}

	@Override
	public IFileStore getStore(URI uri) {
		return new RemoteFileStore(uri.getUserInfo(), uri.getHost(), uri.getPort(), IPath.fromOSString(uri.getPath()));
	}
}
