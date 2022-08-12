/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.filesystem.bug369177;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.internal.filesystem.NullFileStore;
import org.eclipse.core.runtime.IPath;

/**
 * Special file store implementation used by TestBug369177.
 */
public class Bug369177FileStore extends NullFileStore {
	private IPath path;

	public Bug369177FileStore(IPath path) {
		super(path);
		this.path = path;
	}

	@Override
	public IFileSystem getFileSystem() {
		return Bug369177FileSystem.getInstance();
	}

	@Override
	public URI toURI() {
		try {
			return new URI(Bug369177FileSystem.SCHEME_BUG_369177, null, path.isEmpty() ? "/" : path.toString(), null);
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}
}
