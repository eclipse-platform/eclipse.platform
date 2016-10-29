/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/

package org.eclipse.team.core.history.provider;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;

/**
 * Abstract implementation of {@link IFileRevision} that can be implemented by
 * clients.
 *
 * @see IFileRevision
 *
 * @since 3.2
 */
public abstract class FileRevision implements IFileRevision {

	private static final class LocalFileRevision extends FileRevision {
		private final IFile file;

		private LocalFileRevision(IFile file) {
			this.file = file;
		}

		public IStorage getStorage(IProgressMonitor monitor) {
			return file;
		}

		public String getName() {
			return file.getName();
		}

		public boolean exists() {
			return file.exists();
		}

		public long getTimestamp() {
			return file.getLocalTimeStamp();
		}

		public URI getURI() {
			return file.getLocationURI();
		}

		public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
			return this;
		}

		public boolean isPropertyMissing() {
			return false;
		}

		public int hashCode() {
			return file.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof LocalFileRevision) {
				LocalFileRevision other = (LocalFileRevision) obj;
				return other.file.equals(this.file);
			}
			return false;
		}
	}

	/**
	 * Return a file state representing the current state of the
	 * local file.
	 * @param file a local file
	 * @return a file state representing the current state of the
	 * local file
	 * @deprecated This method doesn't do anything useful so it has been deprecated.
	 */
	public static IFileRevision getFileRevisionFor(final IFile file) {
		return new LocalFileRevision(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileState#getURI()
	 */
	public URI getURI() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileState#getTimestamp()
	 */
	public long getTimestamp() {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileState#exists()
	 */
	public boolean exists() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileRevision#getContentIdentifier()
	 */
	public String getContentIdentifier() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileRevision#getAuthor()
	 */
	public String getAuthor() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileRevision#getComment()
	 */
	public String getComment() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @since 3.6
	 */
	public ITag[] getBranches() {
		return new ITag[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileRevision#getTags()
	 */
	public ITag[] getTags() {
		return new ITag[0];
	}

}
