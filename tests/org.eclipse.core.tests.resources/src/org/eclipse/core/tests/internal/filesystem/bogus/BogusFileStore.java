/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.filesystem.bogus;

import java.io.File;
import java.net.URI;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileStore;

/**
 *
 */
public class BogusFileStore extends MemoryFileStore {

	public BogusFileStore(IPath path) {
		super(path);
	}

	@Override
	public URI toURI() {
		return BogusFileSystem.toURI(path);
	}

	@Override
	public java.io.File toLocalFile(int options, IProgressMonitor monitor) {
		IPath parentPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("bogus_fs");
		File parent = new java.io.File(parentPath.toOSString());
		if (!parent.exists()) {
			parent.mkdirs();
		}
		return new java.io.File(parentPath.append(getName()).toOSString());
	}

	@Override
	public void move(IFileStore destination, int options, IProgressMonitor monitor) {
		// ignore
	}

}
