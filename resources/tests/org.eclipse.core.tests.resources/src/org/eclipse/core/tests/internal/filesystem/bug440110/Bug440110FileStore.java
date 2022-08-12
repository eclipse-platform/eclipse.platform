/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.filesystem.bug440110;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileStore;

public class Bug440110FileStore extends MemoryFileStore {
	public Bug440110FileStore(IPath path) {
		super(path);
	}

	@Override
	public IFileStore getChild(String name) {
		return new Bug440110FileStore(path.append(name));
	}

	@Override
	public IFileSystem getFileSystem() {
		return Bug440110FileSystem.getInstance();
	}

	@Override
	public IFileStore getParent() {
		if (path.segmentCount() == 0) {
			return null;
		}
		return new Bug440110FileStore(path.removeLastSegments(1));
	}

	@Override
	public URI toURI() {
		try {
			return new URI(Bug440110FileSystem.SCHEME, path.setDevice(null).toPortableString(), null);
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}
}
