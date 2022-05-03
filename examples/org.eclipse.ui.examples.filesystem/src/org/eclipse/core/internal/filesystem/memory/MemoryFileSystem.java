/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.memory;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A test file system that keeps everything in memory.
 */
public class MemoryFileSystem extends FileSystem {
	public static final String SCHEME_MEMORY = "memory";

	/**
	 * Converts a path to a URI in the memory file system.
	 * @param path
	 * @return
	 */
	public static URI toURI(IPath path) {
		try {
			return new URI(MemoryFileSystem.SCHEME_MEMORY, null, path.setDevice(null).toPortableString(), null);
		} catch (URISyntaxException e) {
			//should not happen
			throw new RuntimeException(e);
		}
	}

	public MemoryFileSystem() {
		super();
	}

	public IFileStore getStore(URI uri) {
		return new MemoryFileStore(Path.fromPortableString(uri.getSchemeSpecificPart()));
	}

	public boolean isCaseSensitive() {
		return true;
	}
}
