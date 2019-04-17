/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.history;

import java.net.URI;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.internal.core.Messages;

/**
 * A LocalFileRevision is used for wrapping local files in order to display
 * them in the History View. As such, this class can be used to wrap either
 * a local file's IFileState or an IFile.
 */
public class LocalFileRevision extends FileRevision {
	/*
	 * Either one or the other of these fields is intended
	 * to be used.
	 */
	//Used for wrapping local file history items
	private IFileState state;

	//Used for displaying the "real" current file
	private IFile file;
	//Used for displaying which base revision
	private IFileRevision baseRevision;

	/*
	 * Used for wrapping an IFileState.
	 */
	public LocalFileRevision(IFileState state) {
		this.state = state;
		this.file = null;
		this.baseRevision = null;
	}

	/*
	 * Used for wrapping an IFile. This is generally used to represent the local
	 * current version of the file being displayed in the history. Make sure to
	 * also pass in the base revision associated with this current version.
	 *
	 * @see #setBaseRevision(IFileRevision)
	 */
	public LocalFileRevision(IFile file) {
		this.file = file;
		this.baseRevision = null;
		this.state = null;
	}

	@Override
	public String getContentIdentifier() {
		if (file != null)
			return baseRevision == null ?  NLS.bind(Messages.LocalFileRevision_currentVersion, "") : NLS.bind(Messages.LocalFileRevision_currentVersion, baseRevision.getContentIdentifier()); //$NON-NLS-1$
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getAuthor() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getComment() {
		if (file != null)
			return Messages.LocalFileRevision_currentVersionTag;
		return null;
	}

	@Override
	public ITag[] getTags() {
		return new ITag[0];
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		if (file != null) {
			return file;
		}
		return state;
	}

	@Override
	public String getName() {
		if (file != null) {
			return file.getName();
		}

		return state.getName();
	}

	@Override
	public long getTimestamp() {
		if (file != null) {
			return file.getLocalTimeStamp();
		}

		return state.getModificationTime();
	}

	/*
	 * A LocalFileRevision generally should exist, but if it doesn't, this method
	 * should tell the truth.
	 */
	@Override
	public boolean exists() {
		if (file != null) {
			return file.exists();
		}

		return state.exists();
	}

	/*
	 * Sets the base revision. Can be used to associate a base revision
	 * with an IFile.
	 */
	public void setBaseRevision(IFileRevision baseRevision) {
		this.baseRevision = baseRevision;
	}

	@Override
	public boolean isPropertyMissing() {
		return true;
	}


	@Override
	public IFileRevision withAllProperties(IProgressMonitor monitor) {
		return this;
	}

	public boolean isPredecessorOf(IFileRevision revision) {
		long compareRevisionTime = revision.getTimestamp();
		return (this.getTimestamp() < compareRevisionTime);
	}

	public boolean isDescendentOf(IFileRevision revision) {
		long compareRevisionTime = revision.getTimestamp();
		return (this.getTimestamp() > compareRevisionTime);
	}

	@Override
	public URI getURI() {
		if (file != null)
			return file.getLocationURI();

		return URIUtil.toURI(state.getFullPath());
	}

	public IFile getFile() {
		return file;
	}

	public IFileState getState() {
		return state;
	}

	public boolean isCurrentState() {
		return file != null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof LocalFileRevision) {
			LocalFileRevision other = (LocalFileRevision) obj;
			if (file != null && other.file != null)
				return file.equals(other.file);
			if (state != null && other.state != null)
				return statesEqual(state, other.state);
		}
		return false;
	}

	private boolean statesEqual(IFileState s1, IFileState s2) {
		return (s1.getFullPath().equals(s2.getFullPath()) && s1.getModificationTime() == s2.getModificationTime());
	}

	@Override
	public int hashCode() {
		if (file != null)
			return file.hashCode();
		if (state != null)
			return (int)state.getModificationTime();
		return super.hashCode();
	}
}
