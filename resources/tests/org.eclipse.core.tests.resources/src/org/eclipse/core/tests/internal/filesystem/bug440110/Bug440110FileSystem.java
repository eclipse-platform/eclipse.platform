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
import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileSystem;

public class Bug440110FileSystem extends MemoryFileSystem {
	public static final String SCHEME = "bug440110";
	private static IFileSystem instance;
	private static boolean fetchedFileTree = false;

	public static void clearFetchedFileTree() {
		fetchedFileTree = false;
	}

	public static IFileSystem getInstance() {
		return instance;
	}

	public static boolean hasFetchedFileTree() {
		return fetchedFileTree;
	}

	public Bug440110FileSystem() {
		super();
		instance = this;
	}

	@Override
	public IFileTree fetchFileTree(IFileStore root, IProgressMonitor monitor) throws CoreException {
		fetchedFileTree = true;
		return new Bug440110FileTree(root);
	}

	@Override
	public IFileStore getStore(URI uri) {
		return new Bug440110FileStore(Path.fromPortableString(uri.getSchemeSpecificPart()));
	}
}
