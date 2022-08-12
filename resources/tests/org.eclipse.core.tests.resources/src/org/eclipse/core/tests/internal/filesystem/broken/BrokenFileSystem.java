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
package org.eclipse.core.tests.internal.filesystem.broken;

import java.net.URI;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.Assert;

/**
 * A simple file system implementation that throws exceptions at every available
 * opportunity.
 */
public class BrokenFileSystem extends FileSystem {

	private static final String SCHEME_BROKEN = "broken";

	private static BrokenFileSystem instance;

	public static BrokenFileSystem getInstance() {
		BrokenFileSystem tmpInstance = instance;
		if (tmpInstance != null) {
			return tmpInstance;
		}
		return instance = new BrokenFileSystem();
	}

	public BrokenFileSystem() {
		instance = this;
	}

	@Override
	public IFileStore getStore(URI uri) {
		Assert.isLegal(SCHEME_BROKEN.equals(uri.getScheme()));
		return new BrokenFileStore(uri);
	}
}
