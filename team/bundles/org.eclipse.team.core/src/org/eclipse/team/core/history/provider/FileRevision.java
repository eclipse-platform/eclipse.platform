/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

		@Override
		public IStorage getStorage(IProgressMonitor monitor) {
			return file;
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public boolean exists() {
			return file.exists();
		}

		@Override
		public long getTimestamp() {
			return file.getLocalTimeStamp();
		}

		@Override
		public URI getURI() {
			return file.getLocationURI();
		}

		@Override
		public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
			return this;
		}

		@Override
		public boolean isPropertyMissing() {
			return false;
		}

		@Override
		public int hashCode() {
			return file.hashCode();
		}

		@Override
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
	@Deprecated
	public static IFileRevision getFileRevisionFor(final IFile file) {
		return new LocalFileRevision(file);
	}

	@Override
	public URI getURI() {
		return null;
	}

	@Override
	public long getTimestamp() {
		return -1;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public String getContentIdentifier() {
		return null;
	}
	@Override
	public String getAuthor() {
		return null;
	}
	@Override
	public String getComment() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @since 3.6
	 */
	@Override
	public ITag[] getBranches() {
		return new ITag[0];
	}

	@Override
	public ITag[] getTags() {
		return new ITag[0];
	}

}
