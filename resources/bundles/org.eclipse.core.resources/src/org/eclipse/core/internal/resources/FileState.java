/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.*;
import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.osgi.util.NLS;

public class FileState extends PlatformObject implements IFileState {
	private static final IWorkspace workspace = ResourcesPlugin.getWorkspace();
	protected long lastModified;
	protected UniversalUniqueIdentifier uuid;
	protected IHistoryStore store;
	protected IPath fullPath;

	public FileState(IHistoryStore store, IPath fullPath, long lastModified, UniversalUniqueIdentifier uuid) {
		this.store = store;
		this.lastModified = lastModified;
		this.uuid = uuid;
		this.fullPath = fullPath;
	}

	@Override
	public boolean exists() {
		return store.exists(this);
	}

	@Override
	public String getCharset() throws CoreException {
		// if there is an existing file at this state's path, use the encoding of that file
		IResource file = workspace.getRoot().findMember(fullPath);
		if (file != null && file.getType() == IResource.FILE)
			return ((IFile) file).getCharset();

		// tries to obtain a description for the file contents
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		try (InputStream contents = new BufferedInputStream(getContents())) {
			IContentDescription description = contentTypeManager.getDescriptionFor(contents, getName(), new QualifiedName[] {IContentDescription.CHARSET});
			return description == null ? null : description.getCharset();
		} catch (IOException e) {
			String message = NLS.bind(Messages.history_errorContentDescription, getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_DESCRIBING_CONTENTS, getFullPath(), message, e);
		}
	}

	@Override
	public InputStream getContents() throws CoreException {
		return store.getContents(this);
	}

	@Override
	public IPath getFullPath() {
		return fullPath;
	}

	@Override
	public long getModificationTime() {
		return lastModified;
	}

	@Override
	public String getName() {
		return fullPath.lastSegment();
	}

	public UniversalUniqueIdentifier getUUID() {
		return uuid;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	/**
	 * Returns a string representation of this object. Used for debug only.
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("FileState(uuid: "); //$NON-NLS-1$
		s.append(uuid.toString());
		s.append(", lastModified: "); //$NON-NLS-1$
		s.append(lastModified);
		s.append(", path: "); //$NON-NLS-1$
		s.append(fullPath);
		s.append(')');
		return s.toString();
	}
}
