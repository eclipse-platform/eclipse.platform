/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.filesystem.provider;

import java.io.*;
import java.net.URI;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.internal.filesystem.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * The abstract superclass of all {@link IFileStore} implementations.  All
 * file stores must subclass this base class, implementing all abstract
 * methods according to their specification in the {@link IFileStore} API.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since org.eclipse.core.filesystem 1.0
 */
public abstract class FileStore extends PlatformObject implements IFileStore {
	/**
	 * A file info array of size zero that can be used as a return value for methods
	 * that return IFileInfo[] to avoid creating garbage objects.
	 */
	protected static final IFileInfo[] EMPTY_FILE_INFO_ARRAY = {};

	/**
	 * A string array of size zero that can be used as a return value for methods
	 * that return String[] to avoid creating garbage objects.
	 */
	protected static final String[] EMPTY_STRING_ARRAY = {};

	/**
	 * Transfers the contents of an input stream to an output stream, using a large
	 * buffer.
	 *
	 * @param source The input stream to transfer
	 * @param destination The destination stream of the transfer
	 * @param length the size of the file or -1 if not known
	 * @param path A path representing the data being transferred for use in error
	 * messages.
	 * @param monitor A progress monitor
	 * @throws CoreException
	 */
	private static final void transferStreams(InputStream source, OutputStream destination, long length, String path, IProgressMonitor monitor) throws CoreException {
		byte[] buffer = new byte[8192];
		SubMonitor subMonitor = SubMonitor.convert(monitor, length >= 0 ? 1 + (int) (length / buffer.length) : 1000);
		try {
			while (true) {
				int bytesRead = -1;
				try {
					bytesRead = source.read(buffer);
				} catch (IOException e) {
					String msg = NLS.bind(Messages.failedReadDuringWrite, path);
					Policy.error(EFS.ERROR_READ, msg, e);
				}
				try {
					if (bytesRead == -1) {
						destination.close();
						break;
					}
					destination.write(buffer, 0, bytesRead);
				} catch (IOException e) {
					String msg = NLS.bind(Messages.couldNotWrite, path);
					Policy.error(EFS.ERROR_WRITE, msg, e);
				}
				subMonitor.worked(1);
			}
		} finally {
			Policy.safeClose(source);
			Policy.safeClose(destination);
		}
	}

	/**
	 * The default implementation of {@link IFileStore#childInfos(int, IProgressMonitor)}.
	 * Subclasses should override this method where a more efficient implementation
	 * is possible.  This default implementation calls {@link #fetchInfo()} on each
	 * child, which will result in a file system call for each child.
	 */
	@Override
	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
		IFileStore[] childStores = childStores(options, monitor);
		IFileInfo[] childInfos = new IFileInfo[childStores.length];
		for (int i = 0; i < childStores.length; i++) {
			childInfos[i] = childStores[i].fetchInfo();
		}
		return childInfos;
	}

	@Override
	public abstract String[] childNames(int options, IProgressMonitor monitor) throws CoreException;

	/**
	 * The default implementation of {@link IFileStore#childStores(int, IProgressMonitor)}.
	 * Subclasses may override.
	 */
	@Override
	public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
		String[] children = childNames(options, monitor);
		IFileStore[] wrapped = new IFileStore[children.length];
		for (int i = 0; i < wrapped.length; i++)
			wrapped[i] = getChild(children[i]);
		return wrapped;
	}

	/**
	 * The default implementation of {@link IFileStore#copy(IFileStore, int, IProgressMonitor)}.
	 * This implementation performs a copy by using other primitive methods.
	 * Subclasses may override this method.
	 */
	@Override
	public void copy(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		final IFileInfo sourceInfo = fetchInfo(EFS.NONE, null);
		if (sourceInfo.isDirectory()) {
			copyDirectory(sourceInfo, destination, options, monitor);
		} else {
			copyFile(sourceInfo, destination, options, monitor);
		}
	}

	/**
	 * Recursively copies a directory as specified by
	 * {@link IFileStore#copy(IFileStore, int, IProgressMonitor)}.
	 *
	 * @param sourceInfo The current file information for the source of the move
	 * @param destination The destination of the copy.
	 * @param options bit-wise or of option flag constants (
	 * {@link EFS#OVERWRITE} or {@link EFS#SHALLOW}).
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li> This store does not exist.</li>
	 * <li> A file of the same name already exists at the copy destination.</li>
	 * </ul>
	 */
	protected void copyDirectory(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		IFileStore[] children = null;
		int opWork = 1;
		if ((options & EFS.SHALLOW) == 0) {
			children = childStores(EFS.NONE, null);
			opWork += children.length;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, opWork);
		subMonitor.subTask(NLS.bind(Messages.copying, toString()));
		// create directory
		destination.mkdir(EFS.NONE, subMonitor.newChild(1));
		// copy attributes
		transferAttributes(sourceInfo, destination);

		if (children == null)
			return;
		// copy children
		for (IFileStore c : children) {
			c.copy(destination.getChild(c.getName()), options, subMonitor.newChild(1));
		}
	}

	/**
	 * Copies a file as specified by
	 * {@link IFileStore#copy(IFileStore, int, IProgressMonitor)}.

	 * @param sourceInfo The current file information for the source of the move
	 * @param destination The destination of the copy.
	 * @param options bit-wise or of option flag constants (
	 * {@link EFS#OVERWRITE} or {@link EFS#SHALLOW}).
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li> This store does not exist.</li>
	 * <li> The <code>OVERWRITE</code> flag is not specified and a file of the
	 * same name already exists at the copy destination.</li>
	 * <li> A directory of the same name already exists at the copy destination.</li>
	 * </ul>
	 */
	protected void copyFile(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		if ((options & EFS.OVERWRITE) == 0 && destination.fetchInfo().exists())
			Policy.error(EFS.ERROR_EXISTS, NLS.bind(Messages.fileExists, destination));
		long length = sourceInfo.getLength();
		String sourcePath = toString();
		SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(Messages.copying, sourcePath), 100);
		InputStream in = null;
		OutputStream out = null;
		try {
			in = openInputStream(EFS.NONE, subMonitor.newChild(1));
			out = destination.openOutputStream(EFS.NONE, subMonitor.newChild(1));
			transferStreams(in, out, length, sourcePath, subMonitor.newChild(98));
			transferAttributes(sourceInfo, destination);
		} catch (CoreException e) {
			Policy.safeClose(in);
			Policy.safeClose(out);
			//if we failed to write, try to cleanup the half written file
			if (!destination.fetchInfo(0, null).exists())
				destination.delete(EFS.NONE, null);
			throw e;
		}
	}

	/**
	 * The default implementation of {@link IFileStore#delete(int, IProgressMonitor)}.
	 * This implementation always throws an exception indicating that deletion
	 * is not supported by this file system.  This method should be overridden
	 * for all file systems on which deletion is supported.
	 *
	 * @param options bit-wise or of option flag constants
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		Policy.error(EFS.ERROR_DELETE, NLS.bind(Messages.noImplDelete, toString()));
	}

	/**
	 * This implementation of {@link Object#equals(Object)} defines
	 * equality based on the file store's URI.  Subclasses should override
	 * this method to return <code>true</code> if and only if the two file stores
	 * represent the same resource in the backing file system.  Issues to watch
	 * out for include whether the file system is case-sensitive, and whether trailing
	 * slashes are considered significant. Subclasses that override this method
	 * should also override {@link #hashCode()}.
	 *
	 * @param obj The object to compare with the receiver for equality
	 * @return <code>true</code> if this object is equal to the provided object,
	 * and <code>false</code> otherwise.
	 * @since org.eclipse.core.filesystem 1.1
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FileStore))
			return false;
		return toURI().equals(((FileStore) obj).toURI());
	}

	/**
	 * The default implementation of {@link IFileStore#fetchInfo()}.
	 * This implementation forwards to {@link IFileStore#fetchInfo(int, IProgressMonitor)}.
	 * Subclasses may override this method.
	 */
	@Override
	public IFileInfo fetchInfo() {
		try {
			return fetchInfo(EFS.NONE, null);
		} catch (CoreException e) {
			//there was an error contacting the file system, so treat it as non-existent file
			FileInfo result = new FileInfo(getName());
			result.setExists(false);
			return result;
		}
	}

	@Override
	public abstract IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException;

	/**
	 * @deprecated use {@link #getFileStore(IPath)} instead
	 */
	@Deprecated
	@Override
	public IFileStore getChild(IPath path) {
		IFileStore result = this;
		for (int i = 0, imax = path.segmentCount(); i < imax; i++)
			result = result.getChild(path.segment(i));
		return result;
	}

	/**
	 * The default implementation of {@link IFileStore#getFileStore(IPath)}
	 * Subclasses may override.
	 *
	 * @since org.eclipse.core.filesystem 1.2
	 */
	@Override
	public IFileStore getFileStore(IPath path) {
		IFileStore result = this;
		String segment = null;
		for (int i = 0, imax = path.segmentCount(); i < imax; i++) {
			segment = path.segment(i);
			if (segment.equals(".")) //$NON-NLS-1$
				continue;
			else if (segment.equals("..") && result.getParent() != null) //$NON-NLS-1$
				result = result.getParent();
			else
				result = result.getChild(segment);
		}
		return result;
	}

	@Override
	public abstract IFileStore getChild(String name);

	/**
	 * The default implementation of {@link IFileStore#getFileSystem()}.
	 * Subclasses may override.
	 */
	@Override
	public IFileSystem getFileSystem() {
		try {
			return EFS.getFileSystem(toURI().getScheme());
		} catch (CoreException e) {
			//this will only happen if toURI() has been incorrectly implemented
			throw new RuntimeException(e);
		}
	}

	@Override
	public abstract String getName();

	@Override
	public abstract IFileStore getParent();

	/**
	 * This implementation of {@link Object#hashCode()} uses a definition
	 * of equality based on equality of the file store's URI.  Subclasses that
	 * override {@link #equals(Object)} should also override this method
	 * to ensure the contract of {@link Object#hashCode()} is honored.
	 *
	 * @return A hash code value for this file store
	 * @since org.eclipse.core.filesystem 1.1
	 */
	@Override
	public int hashCode() {
		return toURI().hashCode();
	}

	/**
	 * The default implementation of {@link IFileStore#isParentOf(IFileStore)}.
	 * This implementation performs parent calculation using other primitive methods.
	 * Subclasses may override this method.
	 *
	 * @param other The store to test for parentage.
	 * @return <code>true</code> if this store is a parent of the provided
	 * store, and <code>false</code> otherwise.
	 */
	@Override
	public boolean isParentOf(IFileStore other) {
		while (true) {
			other = other.getParent();
			if (other == null)
				return false;
			if (this.equals(other))
				return true;
		}
	}

	/**
	 * The default implementation of {@link IFileStore#mkdir(int, IProgressMonitor)}.
	 * This implementation always throws an exception indicating that this file system
	 * is read only. This method should be overridden for all writable file systems.
	 *
	 * @param options bit-wise or of option flag constants
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		Policy.error(EFS.ERROR_WRITE, NLS.bind(Messages.noImplWrite, toString()));
		return null;//can't get here
	}

	/**
	 * The default implementation of {@link IFileStore#move(IFileStore, int, IProgressMonitor)}.
	 * This implementation performs a move by using other primitive methods.
	 * Subclasses may override this method.
	 */
	@Override
	public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(Messages.moving, destination.toString()), 100);
			copy(destination, options & EFS.OVERWRITE, subMonitor.newChild(70));
			delete(EFS.NONE, subMonitor.newChild(30));
		} catch (CoreException e) {
			//throw new error to indicate failure occurred during a move
			String message = NLS.bind(Messages.couldNotMove, toString());
			Policy.error(EFS.ERROR_WRITE, message, e);
		}
	}

	@Override
	public abstract InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException;

	/**
	 * The default implementation of {@link IFileStore#openOutputStream(int, IProgressMonitor)}.
	 * This implementation always throws an exception indicating that this file system
	 * is read only. This method should be overridden for all writable file systems.
	 * <p>
	 * Implementations of this method are responsible for ensuring that the exact sequence
	 * of bytes written to the output stream are returned on a subsequent call to
	 * {@link #openInputStream(int, IProgressMonitor)}, unless there have been
	 * intervening modifications to the file in the file system. For example, the implementation
	 * of this method must not perform conversion of line terminator characters on text
	 * data in the stream.
	 *
	 * @param options bit-wise or of option flag constants
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		Policy.error(EFS.ERROR_WRITE, NLS.bind(Messages.noImplWrite, toString()));
		return null;//can't get here
	}

	/**
	 * The default implementation of {@link IFileStore#putInfo(IFileInfo, int, IProgressMonitor)}.
	 * This implementation always throws an exception indicating that this file system
	 * is read only. This method should be overridden for all writable file systems.
	 *
	 * @param options bit-wise or of option flag constants
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		Policy.error(EFS.ERROR_WRITE, NLS.bind(Messages.noImplWrite, toString()));
	}

	/**
	 * The default implementation of {@link IFileStore#toLocalFile(int, IProgressMonitor)}.
	 * When the {@link EFS#CACHE} option is specified, this method returns
	 * a cached copy of this store in the local file system, or <code>null</code> if
	 * this store does not exist.
	 */
	@Override
	public java.io.File toLocalFile(int options, IProgressMonitor monitor) throws CoreException {
		//caching is the only recognized option
		if (options != EFS.CACHE)
			return null;
		return FileCache.getCache().cache(this, monitor);
	}

	/**
	 * Default implementation of {@link IFileStore#toString()}. This default implementation
	 * returns a string equal to the one returned by #toURI().toString(). Subclasses
	 * may override to provide a more specific string representation of this store.
	 *
	 * @return A string representation of this store.
	 */
	@Override
	public String toString() {
		return toURI().toString();
	}

	@Override
	public abstract URI toURI();

	private void transferAttributes(IFileInfo sourceInfo, IFileStore destination) throws CoreException {
		int options = EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED;
		destination.putInfo(sourceInfo, options, null);
	}
}