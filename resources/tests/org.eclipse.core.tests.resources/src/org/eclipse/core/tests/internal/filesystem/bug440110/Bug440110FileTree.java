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

import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.provider.FileTree;
import org.eclipse.core.runtime.CoreException;

public class Bug440110FileTree extends FileTree {
	public Bug440110FileTree(IFileStore treeRoot) {
		super(treeRoot);
	}

	@Override
	public IFileInfo[] getChildInfos(IFileStore store) {
		try {
			return store.childInfos(EFS.NONE, null);
		} catch (CoreException e) {
			return new IFileInfo[0];
		}
	}

	@Override
	public IFileStore[] getChildStores(IFileStore store) {
		try {
			return store.childStores(EFS.NONE, null);
		} catch (CoreException e) {
			return new IFileStore[0];
		}
	}

	@Override
	public IFileInfo getFileInfo(IFileStore store) {
		return store.fetchInfo();
	}
}
