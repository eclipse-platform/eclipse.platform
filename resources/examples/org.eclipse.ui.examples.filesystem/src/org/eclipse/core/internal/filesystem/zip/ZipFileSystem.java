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
package org.eclipse.core.internal.filesystem.zip;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * 
 */
public class ZipFileSystem extends FileSystem {
	/**
	 * Scheme constant (value "zip") indicating the zip file system scheme.
	 */
	public static final String SCHEME_ZIP = "zip"; //$NON-NLS-1$

	@Override
	public IFileStore getStore(URI uri) {
		if (SCHEME_ZIP.equals(uri.getScheme())) {
			IPath path = IPath.fromOSString(uri.getPath());
			try {
				return new ZipFileStore(EFS.getStore(new URI(uri.getQuery())), path);
			} catch (URISyntaxException e) {
				//ignore and fall through below
			} catch (CoreException e) {
				//ignore and fall through below
			}
		}
		return EFS.getNullFileSystem().getStore(uri);
	}
}
