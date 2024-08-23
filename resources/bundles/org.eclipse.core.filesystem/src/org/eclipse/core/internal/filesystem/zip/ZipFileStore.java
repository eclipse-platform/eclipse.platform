/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.internal.filesystem.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

/**
 * File store implementation representing a file or directory inside a zip file.
 * @since 1.11
 */
public class ZipFileStore extends FileStore {

	/**
	 * A thread-safe map that associates each zip file's URI with a corresponding {@link ReentrantLock}.
	 * <p>
	 * This map is used to ensure that each zip file is accessed by only one thread at a time, preventing
	 * concurrent access issues. The keys in the map are {@link URI} objects representing the zip files, and
	 * the values are {@link ReentrantLock} objects that are used to control access to the corresponding zip file.
	 * The map itself is wrapped with {@link Collections#synchronizedMap(Map)} to ensure thread safety
	 * when accessing the map.
	 * </p>
	 */
	private static final Map<URI, ReentrantLock> uriLockMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * The path of this store within the zip file.
	 */
	private final IPath path;

	/**
	 * The file store that represents the actual zip file.
	 */
	private final IFileStore rootStore;

	public ZipFileStore(IFileStore rootStore, IPath path) {
		this.rootStore = rootStore;
		this.path = path.makeRelative();
	}

	private ZipEntry[] childEntries(IProgressMonitor monitor) throws CoreException {
		try (FileSystem zipFs = openZipFileSystem()) {
			Path zipRoot = zipFs.getPath(path.toString());
			ZipEntryFileVisitor visitor = new ZipEntryFileVisitor(zipRoot);
			Files.walkFileTree(zipRoot, visitor);
			return visitor.getEntries().toArray(new ZipEntry[0]);
		} catch (IOException | URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error reading ZIP file", e)); //$NON-NLS-1$
		} finally {
			unlock();
		}
	}

	@Override
	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
		ZipEntry[] entries = childEntries(monitor);
		int entryCount = entries.length;
		IFileInfo[] infos = new IFileInfo[entryCount];
		for (int i = 0; i < entryCount; i++) {
			infos[i] = convertZipEntryToFileInfo(entries[i]);
		}
		return infos;
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		ZipEntry[] entries = childEntries(monitor);
		int entryCount = entries.length;
		String[] names = new String[entryCount];
		for (int i = 0; i < entryCount; i++) {
			names[i] = computeName(entries[i]);
		}
		return names;
	}

	private static String computeName(ZipEntry entry) {
		String name = entry.getName();
		// removes "/" at the end
		if (name.endsWith("/")) { //$NON-NLS-1$
			name = name.substring(0, name.length() - 1);
		}

		int lastIndex = name.lastIndexOf('/');

		if (lastIndex != -1) {
			return name.substring(lastIndex + 1);
		}
		//No '/' found
		return name;
	}

	private IFileInfo convertToIFileInfo(Path zipEntryPath, BasicFileAttributes attrs) {
		Path namePath = zipEntryPath.getFileName();
		String name = namePath != null ? namePath.toString() : ""; //$NON-NLS-1$
		FileInfo info = new FileInfo(name);
		info.setExists(true);
		info.setDirectory(attrs.isDirectory());
		info.setLastModified(attrs.lastModifiedTime().toMillis());
		info.setLength(attrs.size());
		return info;
	}

	private static IFileInfo convertZipEntryToFileInfo(ZipEntry entry) {
		FileInfo info = new FileInfo(computeName(entry));
		if (entry.isDirectory()) {
			info.setLastModified(EFS.NONE);
		} else {
			info.setLastModified(entry.getTime());
		}

		info.setExists(true);
		info.setDirectory(entry.isDirectory());
		info.setLength(entry.getSize());
		return info;
	}

	@Override
	protected void copyDirectory(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		if (!(destination instanceof ZipFileStore)) {
			super.copyDirectory(sourceInfo, destination, options, monitor);
			return;
		}

		if (!sourceInfo.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Source is not a directory")); //$NON-NLS-1$
		}

		try (FileSystem zipFs = openZipFileSystem()) {
			Path sourceDir = zipFs.getPath(this.path.toString());
			FileSystem destFs = ((ZipFileStore) destination).openZipFileSystem();
			Path destDir = destFs.getPath(((ZipFileStore) destination).path.toString());

			// Use Files.walk to iterate over each entry in the directory
			Files.walk(sourceDir).forEach(sourcePath -> {
				try {
					Path destPath = destDir.resolve(sourceDir.relativize(sourcePath));
					if (Files.isDirectory(sourcePath)) {
						Files.createDirectories(destPath);
					} else {
						Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (IOException e) {
					throw new RuntimeException("Error copying directory contents", e); //$NON-NLS-1$
				}
			});
		} catch (IOException | URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error copying directory within ZIP", e)); //$NON-NLS-1$
		} finally {
			unlock();
		}
	}

	@Override
	protected void copyFile(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		if (!(destination instanceof ZipFileStore)) {
			super.copyFile(sourceInfo, destination, options, monitor);
			return;
		}

		if (sourceInfo.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Source is a directory, not a file")); //$NON-NLS-1$
		}

		try (FileSystem zipFs = openZipFileSystem()) {
			Path sourcePath = zipFs.getPath(this.path.toString());
			FileSystem destFs = ((ZipFileStore) destination).openZipFileSystem();
			Path destPath = destFs.getPath(((ZipFileStore) destination).path.toString());

			// Copy the file with REPLACE_EXISTING option to overwrite if it already exists
			Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error copying file within ZIP", e)); //$NON-NLS-1$
		} finally {
			unlock();
		}
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		Path toDelete = null;
		try (FileSystem zipFs = openZipFileSystem()) {
			toDelete = zipFs.getPath(path.toString());
			if (Files.exists(toDelete)) {
				deleteRecursive(toDelete);
			}
		} catch (IOException | URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error deleting file from zip: " + toDelete, e)); //$NON-NLS-1$
		} finally {
			unlock();
		}
	}

	private void deleteRecursive(Path pathToDelete) throws IOException {
		if (Files.isDirectory(pathToDelete)) {
			// Use try-with-resources to close the directory stream automatically
			try (Stream<Path> entries = Files.walk(pathToDelete)) {
				// We need to sort it in reverse order so directories come after their contents
				List<Path> sortedPaths = entries.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
				for (Path entry : sortedPaths) {
					Files.delete(entry);
				}
			}
		} else {
			Files.delete(pathToDelete);
		}
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		try (FileSystem zipFs = openZipFileSystem()) {
			Path zipEntryPath = zipFs.getPath(path.toString());
			if (Files.exists(zipEntryPath)) {
				BasicFileAttributes attrs = Files.readAttributes(zipEntryPath, BasicFileAttributes.class);
				return convertToIFileInfo(zipEntryPath, attrs);
			}
		} catch (IOException | URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error accessing ZIP file", e)); //$NON-NLS-1$
		} finally {
			unlock();
		}

		// Correctly set up FileInfo before returning
		FileInfo notFoundInfo = new FileInfo(path.lastSegment());
		notFoundInfo.setExists(false);
		return notFoundInfo;
	}

	/**
	 * Finds the zip entry with the given name in this zip file. Returns the
	 * entry and leaves the input stream open positioned at the beginning of the
	 * bytes of that entry. Returns null if the entry could not be found.
	 */
	private ZipEntry findEntry(String name, ZipInputStream in) throws IOException {
		ZipEntry current;
		while ((current = in.getNextEntry()) != null) {
			if (current.getName().equals(name)) {
				return current;
			}
		}
		return null;
	}

	@Override
	public IFileStore getChild(String name) {
		return new ZipFileStore(rootStore, path.append(name));
	}

	@Override
	public String getName() {
		String name = path.lastSegment();
		return name == null ? "" : name; //$NON-NLS-1$
	}

	@Override
	public IFileStore getParent() {
		if (path.segmentCount() > 0) {
			return new ZipFileStore(rootStore, path.removeLastSegments(1));
		}
		// the root entry has no parent
		return null;
	}

	private String getPluginId() {
		return FrameworkUtil.getBundle(this.getClass()).getSymbolicName();
	}

	/**
	 * Returns the path of this file store.
	 */
	public IPath getPath() {
		return path;
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		// Assuming the directory to create is represented by 'this.path'
		try (FileSystem zipFs = openZipFileSystem()) {
			Path dirInZipPath = zipFs.getPath(this.path.toString());
			if (Files.notExists(dirInZipPath)) {
				Files.createDirectories(dirInZipPath);
			}
		} catch (IOException | URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error creating directory in ZIP file", e)); //$NON-NLS-1$
		} finally {
			unlock();
		}

		// Return a file store representing the newly created directory.
		return new ZipFileStore(rootStore, this.path);
	}

	@Override
	public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		if (!(destination instanceof ZipFileStore)) {
			super.move(destination, options, monitor);
			return;
		}
		ZipFileStore destZipFileStore = (ZipFileStore) destination;

		try (FileSystem srcFs = openZipFileSystem(); FileSystem destFs = destZipFileStore.openZipFileSystem()) {
			Path srcPath = srcFs.getPath(this.path.toString());
			Path destPath = destFs.getPath(destZipFileStore.path.toString());

			if (destPath.getParent() != null) {
				Files.createDirectories(destPath.getParent());
			}

			if (Files.isDirectory(srcPath)) {
				moveDirectory(srcPath, destPath, srcFs, destFs);
			} else {
				Files.move(srcPath, destPath, StandardCopyOption.COPY_ATTRIBUTES);
			}
		} catch (IOException | URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error moving entry within ZIP", e)); //$NON-NLS-1$
		} finally {
			unlock();
		}
	}

	private void moveDirectory(Path srcPath, Path destPath, FileSystem srcFs, FileSystem destFs) throws IOException {
		// Ensure the destination directory structure is ready
		if (destPath.getParent() != null) {
			Files.createDirectories(destPath.getParent());
		}

		// Recursively move the contents
		Files.walk(srcPath).forEach(source -> {
			try {
				Path destination = destPath.resolve(srcPath.relativize(source));
				if (Files.isDirectory(source)) {
					if (!Files.exists(destination)) {
						Files.createDirectories(destination);
					}
				} else {
					Files.move(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to move files", e); //$NON-NLS-1$
			}
		});

		// Delete the source directory after moving its contents
		Files.walk(srcPath).sorted(Comparator.reverseOrder()).forEach(pathToMove -> {
			try {
				Files.delete(pathToMove);
			} catch (IOException e) {
				throw new RuntimeException("Failed to delete original files after move", e); //$NON-NLS-1$
			}
		});
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			ZipInputStream in = new ZipInputStream(rootStore.openInputStream(EFS.NONE, monitor));
			ZipEntry entry = findEntry(path.toString(), in);
			if (entry == null) {
				throw new CoreException(Status.error("File not found: " + rootStore.toString())); //$NON-NLS-1$
			}
			if (entry.isDirectory()) {
				throw new CoreException(Status.error("Resource is not a file: " + rootStore.toString())); //$NON-NLS-1$
			}
			return in;
		} catch (IOException e) {
			throw new CoreException(Status.error("Could not read file: " + rootStore.toString(), e)); //$NON-NLS-1$
		}
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) {
		// Creating a ByteArrayOutputStream to capture the data written to the
		// OutputStream
		return new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				try (FileSystem zipFs = openZipFileSystem()) {
					Path entryPath = zipFs.getPath(path.toString());
					// Ensure parent directories exist
					Path parentPath = entryPath.getParent();
					if (parentPath != null) {
						Files.createDirectories(parentPath);
					}
					// Write the ByteArrayOutputStream's data to the entry
					// in the ZIP file
					Files.write(entryPath, this.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				} catch (Exception e) {
					throw new IOException("Failed to integrate data into ZIP file", e); //$NON-NLS-1$
				} finally {
					try {
						unlock();
					} catch (CoreException e) {
						throw new IOException("Error accessing ZIP file", e); //$NON-NLS-1$
					}
				}
			}
		};
	}

	private static ReentrantLock getLockForURI(URI uri) {
		return uriLockMap.computeIfAbsent(uri, k -> new ReentrantLock());
	}

	private FileSystem openZipFileSystem() throws URISyntaxException, IOException {
		Map<String, Object> env = new HashMap<>();
		URI nioURI = toNioURI();
		ReentrantLock lock = getLockForURI(nioURI);
		if (!lock.isHeldByCurrentThread()) {
			lock.lock();
		}
		try {
			return FileSystems.getFileSystem(nioURI);
		} catch (FileSystemNotFoundException e) {
			return FileSystems.newFileSystem(nioURI, env);
		}
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		if (monitor != null) {
			monitor.beginTask("Updating Zip Entry Information", 1); //$NON-NLS-1$
		}
		try (FileSystem zipFs = openZipFileSystem()) {
			Path filePath = zipFs.getPath(path.toString());
			// Check options for what information is requested to be updated
			if ((options & EFS.SET_ATTRIBUTES) != 0) {
				boolean isHidden = info.getAttribute(EFS.ATTRIBUTE_HIDDEN);
				boolean isArchive = info.getAttribute(EFS.ATTRIBUTE_ARCHIVE);

				if (ZipFileSystem.getOS().startsWith("Windows")) { //$NON-NLS-1$
					Files.setAttribute(filePath, "dos:hidden", isHidden); //$NON-NLS-1$
					Files.setAttribute(filePath, "dos:archive", isArchive); //$NON-NLS-1$
				}
			}
			if ((options & EFS.SET_LAST_MODIFIED) != 0) {
				FileTime lastModified = FileTime.fromMillis(info.getLastModified());
				Files.setLastModifiedTime(filePath, lastModified);
			}

		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error updating ZIP file entry information", e)); //$NON-NLS-1$
		} finally {
			unlock();
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	@Override
	public URI toURI() {
		String scheme = ZipFileSystem.SCHEME_ZIP;
		String pathString = path.makeAbsolute().toString();
		URI rootStoreURI = rootStore.toURI();
		String rootStoreScheme = rootStoreURI.getScheme();
		String rootStorePath = rootStoreURI.getPath();
		String rootStoreQuery = rootStoreScheme + ":" + rootStorePath; //$NON-NLS-1$
		try {
			return new URI(scheme, null, pathString, rootStoreQuery, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private URI toNioURI() throws URISyntaxException {
		String nioScheme = "jar:"; //$NON-NLS-1$
		String rootPath = rootStore.toURI().toString();

		String suffix = "!/"; //$NON-NLS-1$
		String ret = nioScheme + rootPath + suffix;
		return new URI(ret);
	}

	private void unlock() throws CoreException {
		try {
			ReentrantLock lock = getLockForURI(toNioURI());
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		} catch (URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, getPluginId(), "Error accessing ZIP file", e)); //$NON-NLS-1$
		}
	}
}
