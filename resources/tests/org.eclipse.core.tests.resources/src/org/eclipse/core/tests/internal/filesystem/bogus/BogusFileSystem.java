/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.filesystem.bogus;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileSystem;

/**
 * A simple file system implementation that maps to a local file system location.
 */
public class BogusFileSystem extends MemoryFileSystem {

	public static final String SCHEME_BOGUS = "bogus";

	private static BogusFileSystem instance;

	public static BogusFileSystem getInstance() {
		BogusFileSystem tmpInstance = instance;
		if (tmpInstance != null) {
			return tmpInstance;
		}
		return instance = new BogusFileSystem();
	}

	public static URI toURI(IPath path) {
		try {
			return new URI(BogusFileSystem.SCHEME_BOGUS, path.setDevice(null).toPortableString(), null);
		} catch (URISyntaxException e) {
			//should not happen
			throw new RuntimeException(e);
		}
	}

	public BogusFileSystem() {
		instance = this;
	}

	@Override
	public IFileStore getStore(URI uri) {
		return new BogusFileStore(IPath.fromPortableString(uri.getSchemeSpecificPart()));
	}
}
