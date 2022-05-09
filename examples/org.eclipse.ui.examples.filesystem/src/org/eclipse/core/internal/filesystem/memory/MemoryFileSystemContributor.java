/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package org.eclipse.core.internal.filesystem.memory;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ide.fileSystem.FileSystemContributor;

/**
 * ZipFileSystemContributor is the zip example of a file system
 * contributor.
 *
 */
public class MemoryFileSystemContributor extends FileSystemContributor {

	public MemoryFileSystemContributor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ide.fileSystem.FileSystemContributor#browseFileSystem(java.lang.String, org.eclipse.swt.widgets.Shell)
	 */
	@Override
	public URI browseFileSystem(String initialPath, Shell shell) {
		MemoryTreeSelectionDialog dialog = new MemoryTreeSelectionDialog(shell);
		if (dialog.open() != Window.OK)
			return null;
		Object[] result = dialog.getResult();
		if (result.length == 0)
			return null;
		return ((IFileStore) result[0]).toURI();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.ide.fileSystem.FileSystemContributor#getURI(java.lang.String)
	 */
	@Override
	public URI getURI(String string) {
		try {
			if (string.startsWith(MemoryFileSystem.SCHEME_MEMORY))
				return new URI(string);
		} catch (URISyntaxException e) {
			Policy.log(Policy.createStatus(e));
			e.printStackTrace();
		}
		return MemoryFileSystem.toURI(new Path(string));
	}
}
