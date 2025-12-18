/*******************************************************************************
 * Copyright (c) 2005, 2024 IBM Corporation and others.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.internal.filesystem.FileStoreUtil;
import org.eclipse.core.internal.filesystem.Messages;
import org.eclipse.core.internal.filesystem.Policy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
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
		if (!aFile.exists() || aFile.canWrite()) {
			return EFS.NONE;
		}
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
		if (destFile instanceof LocalFile destination) {
			//handle case variants on a case-insensitive OS, or copying between
			//two equivalent files in an environment that supports symbolic links.
			//in these nothing needs to be copied (and doing so would likely lose data)
			try {
				if (isSameFile(this.file, destination.file)) {
					//nothing to do
					return;
				}
			} catch (IOException e) {
				String message = NLS.bind(Messages.couldNotRead, this.file.getAbsolutePath());
				Policy.error(EFS.ERROR_READ, message, e);
			}
		}
		//fall through to super implementation
		super.copy(destFile, options, monitor);
	}

	private static final CopyOption[] NO_OVERWRITE = {};
	private static final CopyOption[] OVERWRITE_EXISTING = {StandardCopyOption.REPLACE_EXISTING};
	public static final int LARGE_FILE_SIZE_THRESHOLD = 1024 * 1024; // 1 MiB experimentally determined

	@Override
	protected void copyFile(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		if (sourceInfo.getLength() > LARGE_FILE_SIZE_THRESHOLD && destination instanceof LocalFile target) {
			SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(Messages.copying, this), 100);
			try {
				boolean overwrite = (options & EFS.OVERWRITE) != 0;
				Files.copy(this.file.toPath(), target.file.toPath(), overwrite ? OVERWRITE_EXISTING : NO_OVERWRITE);
				subMonitor.worked(93);
				transferAttributes(sourceInfo, destination);
				subMonitor.worked(5);
			} catch (FileAlreadyExistsException e) {
				Policy.error(EFS.ERROR_EXISTS, NLS.bind(Messages.fileExists, target.filePath), e);
			} catch (IOException e) {
				Policy.error(EFS.ERROR_WRITE, NLS.bind(Messages.failedCopy, this.filePath, target.filePath), e);
			}
		} else {
			super.copyFile(sourceInfo, destination, options, monitor);
		}
	}

	public static final void transferAttributes(IFileInfo sourceInfo, IFileStore destination) throws CoreException {
		int options = EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED;
		destination.putInfo(sourceInfo, options, null);
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			InfiniteProgress infMonitor = new InfiniteProgress(monitor);
			infMonitor.beginTask(NLS.bind(Messages.deleting, file));
			IStatus result = internalDelete(file, infMonitor, FILE_SERVICE);
			if (!result.isOK()) {
				throw new CoreException(result);
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LocalFile otherFile)) {
			return false;
		}
		//Mac oddity: file.equals returns false when case is different even when
		//file system is not case sensitive (Radar bug 3190672)
		if (LocalFileSystem.MACOSX) {
			return filePath.equalsIgnoreCase(otherFile.filePath);
		}
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
		return new LocalFile(IPath.fromOSString(file.getPath()).append(path).toFile());
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
		if (LocalFileSystem.MACOSX) {
			return filePath.toLowerCase().hashCode();
		}
		return file.hashCode();
	}

	private static final ForkJoinPool FILE_SERVICE = createExecutor(Math.max(1, Runtime.getRuntime().availableProcessors()));

	private static ForkJoinPool createExecutor(int threadCount) {
		return new ForkJoinPool(threadCount, pool -> {
			final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
			worker.setName("LocalFile Deleter"); //$NON-NLS-1$
			worker.setDaemon(true);
			return worker;
		}, //
				/* UncaughtExceptionHandler */ null, //
				/* asyncMode */ false, // Last-In-First-Out is important to delete child before parent folders
				/* corePoolSize */ 0, //
				/* maximumPoolSize */ threadCount, //
				/* minimumRunnable */ 0, //
				pool -> true, // if maximumPoolSize would be exceeded, don't throw RejectedExecutionException
				/* keepAliveTime */ 1, TimeUnit.MINUTES); // pool terminates 1 thread per
	}

	/**
	* Deletes the given file recursively, adding failure info to
	* the provided status object.  The filePath is passed as a parameter
	* to optimize java.io.File object creation.
	*/
	private IStatus internalDelete(File target, InfiniteProgress infMonitor, ExecutorService executorService) {
		infMonitor.subTask(NLS.bind(Messages.deleting, target));
		if (infMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		List<Future<IStatus>> futures = new ArrayList<>();
		try {
			try {
				// First try to delete - this should succeed for files and symbolic links to directories.
				Files.deleteIfExists(target.toPath());
				infMonitor.worked();
				return Status.OK_STATUS;
			} catch (AccessDeniedException e) {
				// If the file is read only, it can't be deleted via Files.deleteIfExists()
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=500306
				// Since Java 25, just calling File#delete() is not sufficient anymore on Windows
				// but the read-only state has to be cleared explicitly,
				// see https://bugs.openjdk.org/browse/JDK-8355954
				if (OS.isWindows()) {
					target.setWritable(true);
				}
				if (target.delete()) {
					infMonitor.worked();
					return Status.OK_STATUS;
				}
				throw e;
			}
		} catch (DirectoryNotEmptyException dne) {
			// The target is a directory and it's not empty
			// Try to delete all children.
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(target.toPath())) {
				ds.forEach(p -> {
					Future<IStatus> future = executorService.submit(() -> internalDelete(p.toFile(), infMonitor, executorService));
					futures.add(future);
				});
			} catch (IOException streamException) {
				return createErrorStatus(target, Messages.deleteProblem, streamException);
			}
			MultiStatus deleteChildrenStatus = new MultiStatus(Policy.PI_FILE_SYSTEM, EFS.ERROR_DELETE, Messages.deleteProblem, null);

			for (Future<IStatus> f : futures) {
				try {
					deleteChildrenStatus.add(f.get());
				} catch (InterruptedException | ExecutionException ee) {
					deleteChildrenStatus.add(createErrorStatus(target, Messages.deleteProblem, ee));
				}
			}
			// Abort if one of the children couldn't be deleted.
			if (!deleteChildrenStatus.isOK()) {
				return deleteChildrenStatus;
			}

			// Try to delete the root directory
			try {
				if (Files.deleteIfExists(target.toPath())) {
					infMonitor.worked();
					return Status.OK_STATUS;
				}
			} catch (Exception e1) {
				// We caught a runtime exception so log it.
				return createErrorStatus(target, Messages.couldnotDelete, e1);
			}

			// If we got this far, we failed.
			String message = fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY) //
					? Messages.couldnotDeleteReadOnly

					// This is the worst-case scenario: something failed but we don't know what. The children were
					// deleted successfully and the directory is NOT read-only... there's nothing else to report.
					: Messages.couldnotDelete;

			return createErrorStatus(target, message, null);

		} catch (IOException e) {
			return createErrorStatus(target, Messages.couldnotDelete, e);
		}
	}

	private IStatus createErrorStatus(File target, String msg, Exception e) {
		String message = NLS.bind(msg, target.getAbsolutePath());
		return new Status(IStatus.ERROR, Policy.PI_FILE_SYSTEM, EFS.ERROR_DELETE, message, e);
	}

	@Override
	public boolean isParentOf(IFileStore other) {
		if (!(other instanceof LocalFile otherFile)) {
			return false;
		}
		String thisPath = filePath;
		String thatPath = otherFile.filePath;
		int thisLength = thisPath.length();
		int thatLength = thatPath.length();
		//if equal then not a parent
		if (thisLength >= thatLength) {
			return false;
		}
		if (getFileSystem().isCaseSensitive()) {
			if (!thatPath.startsWith(thisPath)) {
				return false;
			}
		} else {
			if (!thatPath.toLowerCase().startsWith(thisPath.toLowerCase())) {
				return false;
			}
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
		if (!(destFile instanceof LocalFile destinationFile)) {
			super.move(destFile, options, monitor);
			return;
		}
		File source = file;
		File destination = destinationFile.file;
		boolean overwrite = (options & EFS.OVERWRITE) != 0;
		SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(Messages.moving, source.getAbsolutePath()), 1);
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
	}

	private boolean isSameFile(File source, File destination) throws IOException {
		try {
			if (!destination.exists()) {
				// avoid NoSuchFileException for performance reasons
				return false;
			}
			// isSameFile is faster then using getCanonicalPath
			return Files.isSameFile(source.toPath(), destination.toPath());
		} catch (NoSuchFileException e) {
			// ignore - it is the normal case that the destination does not exist.
			// slowest path though
			return false;
		}
	}

	/** @see #readAllBytes(int, IProgressMonitor) */
	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			handleReadIOException(e);
			return null;
		}
	}

	/** @see #openInputStream(int, IProgressMonitor) */
	@Override
	public byte[] readAllBytes(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			handleReadIOException(e);
			return null;
		}
	}

	private void handleReadIOException(IOException e) throws CoreException {
		if (!file.exists()) {
			String message = NLS.bind(Messages.fileNotFound, filePath);
			Policy.error(EFS.ERROR_NOT_EXISTS, message, e);
		} else if (file.isDirectory()) {
			String message = NLS.bind(Messages.notAFile, filePath);
			Policy.error(EFS.ERROR_WRONG_TYPE, message, e);
		} else {
			String message = NLS.bind(Messages.couldNotRead, filePath);
			Policy.error(EFS.ERROR_READ, message, e);
		}
	}

	/** @see #write(byte[], int, IProgressMonitor) */
	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return new FileOutputStream(file, (options & EFS.APPEND) != 0);
		} catch (FileNotFoundException e) {
			handleWriteIOException(e);
			return null;
		}
	}

	/** @see #openOutputStream(int, IProgressMonitor) */
	@Override
	public void write(byte[] content, int options, IProgressMonitor monitor) throws CoreException {
		try {
			boolean append = (options & EFS.APPEND) != 0;
			if (append) {
				Files.write(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} else {
				Files.write(file.toPath(), content); // default uses StandardOpenOption.TRUNCATE_EXISTING
			}
		} catch (IOException e) {
			handleWriteIOException(e);
		}
	}

	private void handleWriteIOException(IOException e) throws CoreException {
		checkReadOnlyParent(file, e);
		if (file.isDirectory()) {
			String message = NLS.bind(Messages.notAFile, filePath);
			Policy.error(EFS.ERROR_WRONG_TYPE, message, e);
		} else {
			String message = NLS.bind(Messages.couldNotWrite, filePath);
			Policy.error(EFS.ERROR_WRITE, message, e);
		}
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		boolean success = true;
		if ((options & EFS.SET_ATTRIBUTES) != 0) {
			success &= LocalFileNativesManager.putFileInfo(filePath, info, options);
		}
		//native does not currently set last modified
		if ((options & EFS.SET_LAST_MODIFIED) != 0) {
			success &= file.setLastModified(info.getLastModified());
		}
		if (!success && !file.exists()) {
			Policy.error(EFS.ERROR_NOT_EXISTS, NLS.bind(Messages.fileNotFound, filePath));
		}
	}

	@Override
	public File toLocalFile(int options, IProgressMonitor monitor) throws CoreException {
		if (options == EFS.CACHE) {
			return super.toLocalFile(options, monitor);
		}
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
		if (other instanceof LocalFile otherFile) {
			// We can compare paths in the local file implementation, because LocalFile don't have a query string, port, or authority
			// We use `toURI` here because it performs file normalisation e.g. /a/b/../c -> /a/c
			// The URI is cached by the LocalFile after normalisation so this effectively results in a straight lookup
			return FileStoreUtil.comparePathSegments(this.toURI().getPath(), otherFile.toURI().getPath());
		}
		return super.compareTo(other);
	}
}
