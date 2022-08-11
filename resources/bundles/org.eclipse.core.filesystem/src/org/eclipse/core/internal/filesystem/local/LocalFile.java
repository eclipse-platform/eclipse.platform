/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
 * 	   Martin Oberhuber (Wind River) - [294429] Avoid substring baggage in FileInfo
 * 	   Martin Lippert (VMware) - [394607] Poor performance when using findFilesForLocationURI
 * 	   Sergey Prigogin (Google) - [433061] Deletion of project follows symbolic links
 *                                [464072] Refresh on Access ignored during text search
 * 	   Andrey Loskutov (loskutov@gmx.de) - [500306] Read-only files, and projects containing them, cannot be deleted
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.internal.filesystem.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

/**
 * File system implementation based on storage of files in the local
 * operating system's file system.
 */
public class LocalFile extends FileStore {
	/**
	 * The java.io.File that this store represents.
	 */
	protected final File file;
	/**
	 * The absolute file system path of the file represented by this store.
	 */
	protected final String filePath;

	/**
	 * cached value for the toURI method
	 */
	private URI uri;

	private static int attributes(File aFile) {
		if (!aFile.exists() || aFile.canWrite())
			return EFS.NONE;
		return EFS.ATTRIBUTE_READ_ONLY;
	}

	/**
	 * Creates a new local file.
	 *
	 * @param file The file this local file represents
	 */
	public LocalFile(File file) {
		this.file = file;
		this.filePath = file.getAbsolutePath();
	}

	/**
	 * This method is called after a failure to modify a file or directory.
	 * Check to see if the parent is read-only and if so then
	 * throw an exception with a more specific message and error code.
	 *
	 * @param target The file that we failed to modify
	 * @param exception The low level exception that occurred, or <code>null</code>
	 * @throws CoreException A more specific exception if the parent is read-only
	 */
	private void checkReadOnlyParent(File target, Throwable exception) throws CoreException {
		File parent = target.getParentFile();
		if (parent != null && (attributes(parent) & EFS.ATTRIBUTE_READ_ONLY) != 0) {
			String message = NLS.bind(Messages.readOnlyParent, target.getAbsolutePath());
			Policy.error(EFS.ERROR_PARENT_READ_ONLY, message, exception);
		}
	}

	/**
	 * This method is called after a failure to modify a directory.
	 * Check to see if the target is not writable (e.g. device doesn't not exist) and if so then
	 * throw an exception with a more specific message and error code.
	 *
	 * @param target The directory that we failed to modify
	 * @param exception The low level exception that occurred, or <code>null</code>
	 * @throws CoreException A more specific exception if the target is not writable
	 */
	private void checkTargetIsNotWritable(File target, Throwable exception) throws CoreException {
		if (!target.canWrite()) {
			String message = NLS.bind(Messages.couldNotWrite, target.getAbsolutePath());
			Policy.error(EFS.ERROR_WRITE, message, exception);
		}
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) {
		String[] names = file.list();
		return (names == null ? EMPTY_STRING_ARRAY : names);
	}

	@Override
	public void copy(IFileStore destFile, int options, IProgressMonitor monitor) throws CoreException {
		if (destFile instanceof LocalFile) {
			File source = file;
			File destination = ((LocalFile) destFile).file;
			//handle case variants on a case-insensitive OS, or copying between
			//two equivalent files in an environment that supports symbolic links.
			//in these nothing needs to be copied (and doing so would likely lose data)
			try {
				if (isSameFile(source, destination)) {
					//nothing to do
					return;
				}
			} catch (IOException e) {
				String message = NLS.bind(Messages.couldNotRead, source.getAbsolutePath());
				Policy.error(EFS.ERROR_READ, message, e);
			}
		}
		//fall through to super implementation
		super.copy(destFile, options, monitor);
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor = new InfiniteProgress(monitor);
		try {
			monitor.beginTask(NLS.bind(Messages.deleting, this), 200);
			String message = Messages.deleteProblem;
			MultiStatus result = new MultiStatus(Policy.PI_FILE_SYSTEM, EFS.ERROR_DELETE, message, null);
			internalDelete(file, filePath, result, monitor);
			if (!result.isOK())
				throw new CoreException(result);
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LocalFile))
			return false;
		//Mac oddity: file.equals returns false when case is different even when
		//file system is not case sensitive (Radar bug 3190672)
		LocalFile otherFile = (LocalFile) obj;
		if (LocalFileSystem.MACOSX)
			return filePath.equalsIgnoreCase(otherFile.filePath);
		return file.equals(otherFile.file);
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
		FileInfo info = LocalFileNativesManager.fetchFileInfo(filePath);
		//natives don't set the file name on all platforms
		if (info.getName().isEmpty()) {
			String name = file.getName();
			//Bug 294429: make sure that substring baggage is removed
			info.setName(new String(name.toCharArray()));
		}
		return info;
	}

	@Deprecated
	@Override
	public IFileStore getChild(IPath path) {
		return new LocalFile(new File(file, path.toOSString()));
	}

	@Override
	public IFileStore getFileStore(IPath path) {
		return new LocalFile(new Path(file.getPath()).append(path).toFile());
	}

	@Override
	public IFileStore getChild(String name) {
		return new LocalFile(new File(file, name));
	}

	@Override
	public IFileSystem getFileSystem() {
		return LocalFileSystem.getInstance();
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public IFileStore getParent() {
		File parent = file.getParentFile();
		return parent == null ? null : new LocalFile(parent);
	}

	@Override
	public int hashCode() {
		if (LocalFileSystem.MACOSX)
			return filePath.toLowerCase().hashCode();
		return file.hashCode();
	}

	/**
	 * Deletes the given file recursively, adding failure info to
	 * the provided status object.  The filePath is passed as a parameter
	 * to optimize java.io.File object creation.
	 */
	private boolean internalDelete(File target, String pathToDelete, MultiStatus status, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		try {
			try {
				// First try to delete - this should succeed for files and symbolic links to directories.
				Files.deleteIfExists(target.toPath());
				return true;
			} catch (AccessDeniedException e) {
				// If the file is read only, it can't be deleted via Files.deleteIfExists()
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=500306
				if (target.delete()) {
					return true;
				}
				throw e;
			}
		} catch (DirectoryNotEmptyException e) {
			monitor.subTask(NLS.bind(Messages.deleting, target));
			String[] list = target.list();
			if (list == null)
				list = EMPTY_STRING_ARRAY;
			int parentLength = pathToDelete.length();
			boolean failedRecursive = false;
			for (String element : list) {
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				// Optimized creation of child path object
				StringBuilder childBuffer = new StringBuilder(parentLength + element.length() + 1);
				childBuffer.append(pathToDelete);
				childBuffer.append(File.separatorChar);
				childBuffer.append(element);
				String childName = childBuffer.toString();
				// Try best effort on all children so put logical OR at end.
				failedRecursive = !internalDelete(new java.io.File(childName), childName, status, monitor) || failedRecursive;
				monitor.worked(1);
			}
			try {
				// Don't try to delete the root if one of the children failed.
				if (!failedRecursive && Files.deleteIfExists(target.toPath()))
					return true;
			} catch (Exception e1) {
				// We caught a runtime exception so log it.
				String message = NLS.bind(Messages.couldnotDelete, target.getAbsolutePath());
				status.add(new Status(IStatus.ERROR, Policy.PI_FILE_SYSTEM, EFS.ERROR_DELETE, message, e1));
				return false;
			}
			// If we got this far, we failed.
			String message = null;
			if (fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY)) {
				message = NLS.bind(Messages.couldnotDeleteReadOnly, target.getAbsolutePath());
			} else {
				message = NLS.bind(Messages.couldnotDelete, target.getAbsolutePath());
			}
			status.add(new Status(IStatus.ERROR, Policy.PI_FILE_SYSTEM, EFS.ERROR_DELETE, message, null));
			return false;
		} catch (IOException e) {
			String message = NLS.bind(Messages.couldnotDelete, target.getAbsolutePath());
			status.add(new Status(IStatus.ERROR, Policy.PI_FILE_SYSTEM, EFS.ERROR_DELETE, message, e));
			return false;
		}
	}

	@Override
	public boolean isParentOf(IFileStore other) {
		if (!(other instanceof LocalFile))
			return false;
		String thisPath = filePath;
		String thatPath = ((LocalFile) other).filePath;
		int thisLength = thisPath.length();
		int thatLength = thatPath.length();
		//if equal then not a parent
		if (thisLength >= thatLength)
			return false;
		if (getFileSystem().isCaseSensitive()) {
			if (thatPath.indexOf(thisPath) != 0)
				return false;
		} else {
			if (thatPath.toLowerCase().indexOf(thisPath.toLowerCase()) != 0)
				return false;
		}
		//The common portion must end with a separator character for this to be a parent of that
		return thisPath.charAt(thisLength - 1) == File.separatorChar || thatPath.charAt(thisLength) == File.separatorChar;
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		boolean shallow = (options & EFS.SHALLOW) != 0;
		//must be a directory
		try {
			if (shallow) {
				Files.createDirectory(file.toPath());
			} else {
				Files.createDirectories(file.toPath());
			}
		} catch (FileAlreadyExistsException e) {
			if (!file.isDirectory()) {
				String message = NLS.bind(Messages.failedCreateWrongType, filePath);
				Policy.error(EFS.ERROR_WRONG_TYPE, message, e);
			}
		} catch (AccessDeniedException e) {
			if (!file.isDirectory()) {
				checkReadOnlyParent(file, e);
				String message = NLS.bind(Messages.failedCreateAccessDenied, filePath);
				Policy.error(EFS.ERROR_AUTH_FAILED, message, e);
			}
		} catch (NoSuchFileException e) {
			if (!file.isDirectory()) {
				String parentPath = file.getParent();
				String message = NLS.bind(Messages.fileNotFound, parentPath != null ? parentPath : filePath);
				Policy.error(EFS.ERROR_NOT_EXISTS, message, e);
			}
		} catch (IOException e) {
			if (!file.isDirectory()) {
				checkReadOnlyParent(file, e);
				checkTargetIsNotWritable(file, e);
				String message = NLS.bind(Messages.couldNotWrite, filePath);
				Policy.error(EFS.ERROR_WRITE, message, e);
			}
		}
		return this;
	}

	@Override
	public void move(IFileStore destFile, int options, IProgressMonitor monitor) throws CoreException {
		if (!(destFile instanceof LocalFile)) {
			super.move(destFile, options, monitor);
			return;
		}
		File source = file;
		File destination = ((LocalFile) destFile).file;
		boolean overwrite = (options & EFS.OVERWRITE) != 0;
		SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(Messages.moving, source.getAbsolutePath()), 1);
		try {
			//this flag captures case renaming on a case-insensitive OS, or moving
			//two equivalent files in an environment that supports symbolic links.
			//in these cases we NEVER want to delete anything
			boolean sourceEqualsDest = false;
			try {
				sourceEqualsDest = isSameFile(source, destination);
			} catch (IOException e) {
				String message = NLS.bind(Messages.couldNotMove, source.getAbsolutePath());
				Policy.error(EFS.ERROR_WRITE, message, e);
			}
			if (!sourceEqualsDest && !overwrite && destination.exists()) {
				String message = NLS.bind(Messages.fileExists, destination.getAbsolutePath());
				Policy.error(EFS.ERROR_EXISTS, message);
			}
			if (source.renameTo(destination)) {
				// double-check to ensure we really did move
				// since java.io.File#renameTo sometimes lies
				if (!sourceEqualsDest && source.exists()) {
					// XXX: document when this occurs
					if (destination.exists()) {
						// couldn't delete the source so remove the destination and throw an error
						// XXX: if we fail deleting the destination, the destination (root) may still exist
						new LocalFile(destination).delete(EFS.NONE, null);
						String message = NLS.bind(Messages.couldnotDelete, source.getAbsolutePath());
						Policy.error(EFS.ERROR_DELETE, message);
					}
					// source exists but destination doesn't so try to copy below
				} else {
					// destination.exists() returns false for broken links, this has to be handled explicitly
					if (!destination.exists() && !destFile.fetchInfo().getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
						// neither the source nor the destination exist. this is REALLY bad
						String message = NLS.bind(Messages.failedMove, source.getAbsolutePath(), destination.getAbsolutePath());
						Policy.error(EFS.ERROR_WRITE, message);
					}
					// the move was successful
					return;
				}
			}
			// for some reason renameTo didn't work
			if (sourceEqualsDest) {
				String message = NLS.bind(Messages.couldNotMove, source.getAbsolutePath());
				Policy.error(EFS.ERROR_WRITE, message, null);
			}
			// fall back to default implementation
			super.move(destFile, options, subMonitor.newChild(1));
		} finally {
			subMonitor.done();
		}
	}

	private boolean isSameFile(File source, File destination) throws IOException {
		try {
			if (!destination.exists()) {
				// avoid NoSuchFileException for performance reasons
				return false;
			}
			// isSameFile is faster then using getCanonicalPath 
			return java.nio.file.Files.isSameFile(source.toPath(), destination.toPath());
		} catch (NoSuchFileException e) {
			// ignore - it is the normal case that the destination does not exist.
			// slowest path though
			return false;
		}
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			String message;
			if (!file.exists()) {
				message = NLS.bind(Messages.fileNotFound, filePath);
				Policy.error(EFS.ERROR_NOT_EXISTS, message, e);
			} else if (file.isDirectory()) {
				message = NLS.bind(Messages.notAFile, filePath);
				Policy.error(EFS.ERROR_WRONG_TYPE, message, e);
			} else {
				message = NLS.bind(Messages.couldNotRead, filePath);
				Policy.error(EFS.ERROR_READ, message, e);
			}
			return null;
		}
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return new FileOutputStream(file, (options & EFS.APPEND) != 0);
		} catch (FileNotFoundException e) {
			checkReadOnlyParent(file, e);
			String message;
			String path = filePath;
			if (file.isDirectory()) {
				message = NLS.bind(Messages.notAFile, path);
				Policy.error(EFS.ERROR_WRONG_TYPE, message, e);
			} else {
				message = NLS.bind(Messages.couldNotWrite, path);
				Policy.error(EFS.ERROR_WRITE, message, e);
			}
			return null;
		}
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		boolean success = true;
		if ((options & EFS.SET_ATTRIBUTES) != 0) {
			success &= LocalFileNativesManager.putFileInfo(filePath, info, options);
		}
		//native does not currently set last modified
		if ((options & EFS.SET_LAST_MODIFIED) != 0)
			success &= file.setLastModified(info.getLastModified());
		if (!success && !file.exists())
			Policy.error(EFS.ERROR_NOT_EXISTS, NLS.bind(Messages.fileNotFound, filePath));
	}

	@Override
	public File toLocalFile(int options, IProgressMonitor monitor) throws CoreException {
		if (options == EFS.CACHE)
			return super.toLocalFile(options, monitor);
		return file;
	}

	@Override
	public String toString() {
		return file.toString();
	}

	@Override
	public URI toURI() {
		if (this.uri == null) {
			this.uri = URIUtil.toURI(filePath);
		}
		return this.uri;
	}

	@Override
	public int compareTo(IFileStore other) {
		if (other instanceof LocalFile) {
			// We can compare paths in the local file implementation, because LocalFile don't have a query string, port, or authority
			// We use `toURI` here because it performs file normalisation e.g. /a/b/../c -> /a/c
			// The URI is cached by the LocalFile after normalisation so this effectively results in a straight lookup
			return FileStoreUtil.comparePathSegments(this.toURI().getPath(), ((LocalFile) other).toURI().getPath());
		}
		return super.compareTo(other);
	}
}
