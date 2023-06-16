/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup.containers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;

/**
 * Implementation of storage for a local file
 * (<code>java.io.File</code>).
 * <p>
 * This class may be instantiated.
 * </p>
 * @see IStorage
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class LocalFileStorage extends PlatformObject implements IStorage {

	/**
	 * The file this storage refers to.
	 */
	private File fFile;

	/**
	 * Constructs and returns storage for the given file.
	 *
	 * @param file a local file
	 */
	public LocalFileStorage(File file){
		setFile(file);
	}

	@Override
	public InputStream getContents() throws CoreException {
		try {
			return new FileInputStream(getFile());
		} catch (IOException e){
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, SourceLookupMessages.LocalFileStorage_0, e));
		}
	}

	@Override
	public IPath getFullPath() {
		try {
			return IPath.fromOSString(getFile().getCanonicalPath());
		} catch (IOException e) {
			DebugPlugin.log(e);
			return null;
		}
	}

	@Override
	public String getName() {
		return getFile().getName();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	/**
	 * Sets the file associated with this storage
	 *
	 * @param file a local file
	 */
	private void setFile(File file) {
		fFile = file;
	}

	/**
	 * Returns the file associated with this storage
	 *
	 * @return file
	 */
	public File getFile() {
		return fFile;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof LocalFileStorage &&
			 getFile().equals(((LocalFileStorage)object).getFile());
	}

	@Override
	public int hashCode() {
		return getFile().hashCode();
	}
}
