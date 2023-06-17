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
 *     Martin Oberhuber (Wind River) - [44107] Add symbolic links to ResourceAttributes API
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Sergey Prigogin (Google) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.utils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;

/**
 * Static utility methods for manipulating Files and URIs.
 */
public class FileUtil {
	static final boolean MACOSX = Constants.OS_MACOSX.equals(getOS());

	/**
	 * Converts a ResourceAttributes object into an IFileInfo object.
	 * @param attributes The resource attributes
	 * @return The file info
	 */
	public static IFileInfo attributesToFileInfo(ResourceAttributes attributes) {
		IFileInfo fileInfo = EFS.createFileInfo();
		fileInfo.setAttribute(EFS.ATTRIBUTE_READ_ONLY, attributes.isReadOnly());
		fileInfo.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, attributes.isExecutable());
		fileInfo.setAttribute(EFS.ATTRIBUTE_ARCHIVE, attributes.isArchive());
		fileInfo.setAttribute(EFS.ATTRIBUTE_HIDDEN, attributes.isHidden());
		fileInfo.setAttribute(EFS.ATTRIBUTE_SYMLINK, attributes.isSymbolicLink());
		fileInfo.setAttribute(EFS.ATTRIBUTE_GROUP_READ, attributes.isSet(EFS.ATTRIBUTE_GROUP_READ));
		fileInfo.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, attributes.isSet(EFS.ATTRIBUTE_GROUP_WRITE));
		fileInfo.setAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE, attributes.isSet(EFS.ATTRIBUTE_GROUP_EXECUTE));
		fileInfo.setAttribute(EFS.ATTRIBUTE_OTHER_READ, attributes.isSet(EFS.ATTRIBUTE_OTHER_READ));
		fileInfo.setAttribute(EFS.ATTRIBUTE_OTHER_WRITE, attributes.isSet(EFS.ATTRIBUTE_OTHER_WRITE));
		fileInfo.setAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE, attributes.isSet(EFS.ATTRIBUTE_OTHER_EXECUTE));
		return fileInfo;
	}

	/**
	 * Converts an IPath into its canonical form for the local file system.
	 */
	public static IPath canonicalPath(IPath path) {
		if (path == null)
			return null;
		try {
			final String pathString = path.toOSString();
			final String canonicalPath = new java.io.File(pathString).getCanonicalPath();
			//only create a new path if necessary
			if (canonicalPath.equals(pathString))
				return path;
			return IPath.fromOSString(canonicalPath);
		} catch (IOException e) {
			return path;
		}
	}

	/**
	 * For a path on a case-insensitive file system returns the path with the actual
	 * case as it exists in the file system. If only a prefix of the path exists on
	 * the file system, the case of remaining part of the returned path is the same
	 * as in the original path. For a case-sensitive file system returns the original
	 * path.
	 * <p>
	 * This method is similar to java.nio.file.Path.toRealPath(LinkOption.NOFOLLOW_LINKS)
	 * in Java 1.7.
	 */
	public static IPath realPath(IPath path) {
		if (path == null)
			return null;
		IFileSystem fileSystem = EFS.getLocalFileSystem();
		if (fileSystem.isCaseSensitive())
			return path;
		IPath realPath = path.isAbsolute() ? IPath.ROOT : IPath.EMPTY;
		String device = path.getDevice();
		if (device != null) {
			realPath = realPath.setDevice(device.toUpperCase());
		}
		IFileStore fileStore = null;
		for (int i = 0; i < path.segmentCount(); i++) {
			final String segment = path.segment(i);
			if (i == 0 && path.isUNC()) {
				realPath = realPath.append(segment.toUpperCase());
				realPath = realPath.makeUNC(true);
			} else {
				if (MACOSX) {
					// IFileInfo.getName() may not return the real name of the file on Mac OS X.
					// Obtain the real name of the file from a listing of its parent directory.
					String[] names = realPath.toFile().list((dir, n) -> n.equalsIgnoreCase(segment));
					String realName;
					if (names == null || names.length == 0) {
						// The remainder of the path doesn't exist on the file system - copy from
						// the original path.
						realPath = realPath.append(path.removeFirstSegments(realPath.segmentCount()));
						break;
					} else if (names.length == 1) {
						realName = names[0];
					} else {
						// More than one file matches the file name. Maybe the file system was
						// misreported to be case insensitive. Preserve the original name.
						realName = segment;
					}
					realPath = realPath.append(realName);
				} else {
					if (fileStore == null)
						fileStore = fileSystem.getStore(realPath);
					fileStore = fileStore.getChild(segment);
					IFileInfo info = fileStore.fetchInfo();
					if (!info.exists()) {
						// The remainder of the path doesn't exist on the file system - copy from
						// the original path.
						realPath = realPath.append(path.removeFirstSegments(realPath.segmentCount()));
						break;
					}
					realPath = realPath.append(info.getName());
				}
			}
		}
		if (path.hasTrailingSeparator()) {
			realPath = realPath.addTrailingSeparator();
		}
		// Return the original path if it's the same as the real one.
		return realPath.equals(path) ? path : realPath;
	}

	/**
	 * Returns the current OS.  Equivalent to Platform.getOS(), but tolerant of the platform runtime
	 * not being present.
	 */
	private static String getOS() {
		return System.getProperty("osgi.os", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Converts a URI into its canonical form.
	 */
	public static URI canonicalURI(URI uri) {
		if (uri == null)
			return null;
		if (EFS.SCHEME_FILE.equals(uri.getScheme())) {
			//only create a new URI if it is different
			final IPath inputPath = URIUtil.toPath(uri);
			final IPath canonicalPath = canonicalPath(inputPath);
			if (inputPath == canonicalPath)
				return uri;
			return URIUtil.toURI(canonicalPath);
		}
		return uri;
	}

	/**
	 * Converts a URI by replacing the file system path in the URI with the path
	 * with the actual case as it exists in the file system.
	 *
	 * @see #realPath(IPath)
	 */
	public static URI realURI(URI uri) {
		if (uri == null)
			return null;
		if (EFS.SCHEME_FILE.equals(uri.getScheme())) {
			// Only create a new URI if it is different.
			final IPath inputPath = URIUtil.toPath(uri);
			final IPath realPath = realPath(inputPath);
			if (inputPath == realPath)
				return uri;
			return URIUtil.toURI(realPath);
		}
		return uri;
	}

	/**
	 * Returns true if the given file system locations overlap. If "bothDirections" is true,
	 * this means they are the same, or one is a proper prefix of the other.  If "bothDirections"
	 * is false, this method only returns true if the locations are the same, or the first location
	 * is a prefix of the second.  Returns false if the locations do not overlap
	 * Does the right thing with respect to case insensitive platforms.
	 */
	private static boolean computeOverlap(IPath location1, IPath location2, boolean bothDirections) {
		IPath one = location1;
		IPath two = location2;
		// If we are on a case-insensitive file system then convert to all lower case.
		if (!Workspace.caseSensitive) {
			one = IPath.fromOSString(location1.toOSString().toLowerCase());
			two = IPath.fromOSString(location2.toOSString().toLowerCase());
		}
		return one.isPrefixOf(two) || (bothDirections && two.isPrefixOf(one));
	}

	/**
	 * Returns true if the given file system locations overlap. If "bothDirections" is true,
	 * this means they are the same, or one is a proper prefix of the other.  If "bothDirections"
	 * is false, this method only returns true if the locations are the same, or the first location
	 * is a prefix of the second.  Returns false if the locations do not overlap
	 */
	private static boolean computeOverlap(URI location1, URI location2, boolean bothDirections) {
		if (location1.equals(location2))
			return true;
		String scheme1 = location1.getScheme();
		String scheme2 = location2.getScheme();
		if (scheme1 == null ? scheme2 != null : !scheme1.equals(scheme2))
			return false;
		if (EFS.SCHEME_FILE.equals(scheme1) && EFS.SCHEME_FILE.equals(scheme2))
			return computeOverlap(URIUtil.toPath(location1), URIUtil.toPath(location2), bothDirections);
		IFileSystem system = null;
		try {
			system = EFS.getFileSystem(scheme1);
		} catch (CoreException e) {
			//handled below
		}
		if (system == null) {
			//we are stuck with string comparison
			String string1 = location1.toString();
			String string2 = location2.toString();
			return string1.startsWith(string2) || (bothDirections && string2.startsWith(string1));
		}
		IFileStore store1 = system.getStore(location1);
		IFileStore store2 = system.getStore(location2);
		return store1.equals(store2) || store1.isParentOf(store2) || (bothDirections && store2.isParentOf(store1));
	}

	/**
	 * Converts an IFileInfo object into a ResourceAttributes object.
	 * @param fileInfo The file info
	 * @return The resource attributes
	 */
	public static ResourceAttributes fileInfoToAttributes(IFileInfo fileInfo) {
		ResourceAttributes attributes = new ResourceAttributes();
		attributes.setReadOnly(fileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		attributes.setArchive(fileInfo.getAttribute(EFS.ATTRIBUTE_ARCHIVE));
		attributes.setExecutable(fileInfo.getAttribute(EFS.ATTRIBUTE_EXECUTABLE));
		attributes.setHidden(fileInfo.getAttribute(EFS.ATTRIBUTE_HIDDEN));
		attributes.setSymbolicLink(fileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		attributes.set(EFS.ATTRIBUTE_GROUP_READ, fileInfo.getAttribute(EFS.ATTRIBUTE_GROUP_READ));
		attributes.set(EFS.ATTRIBUTE_GROUP_WRITE, fileInfo.getAttribute(EFS.ATTRIBUTE_GROUP_WRITE));
		attributes.set(EFS.ATTRIBUTE_GROUP_EXECUTE, fileInfo.getAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE));
		attributes.set(EFS.ATTRIBUTE_OTHER_READ, fileInfo.getAttribute(EFS.ATTRIBUTE_OTHER_READ));
		attributes.set(EFS.ATTRIBUTE_OTHER_WRITE, fileInfo.getAttribute(EFS.ATTRIBUTE_OTHER_WRITE));
		attributes.set(EFS.ATTRIBUTE_OTHER_EXECUTE, fileInfo.getAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE));
		return attributes;
	}

	/**
	 * Returns true if the given file system locations overlap, and false otherwise.
	 * Overlap means the locations are the same, or one is a proper prefix of the other.
	 */
	public static boolean isOverlapping(URI location1, URI location2) {
		return computeOverlap(location1, location2, true);
	}

	/**
	 * Returns true if location1 is the same as, or a proper prefix of, location2.
	 * Returns false otherwise.
	 */
	public static boolean isPrefixOf(IPath location1, IPath location2) {
		return computeOverlap(location1, location2, false);
	}

	/**
	 * Returns true if location1 is the same as, or a proper prefix of, location2.
	 * Returns false otherwise.
	 */
	public static boolean isPrefixOf(URI location1, URI location2) {
		return computeOverlap(location1, location2, false);
	}

	/**
	 * Closes a stream and ignores any resulting exception. This is useful
	 * when doing stream cleanup in a finally block where secondary exceptions
	 * are not worth logging.
	 *
	 *<p>
	 * <strong>WARNING:</strong>
	 * If the API contract requires notifying clients of I/O problems, then you <strong>must</strong>
	 * explicitly close() output streams outside of safeClose().
	 * Some OutputStreams will defer an IOException from write() to close().  So
	 * while the writes may 'succeed', ignoring the IOExcpetion will result in silent
	 * data loss.
	 * </p>
	 * <p>
	 * This method should only be used as a fail-safe to ensure resources are not
	 * leaked.
	 * </p>
	 * See also: https://bugs.eclipse.org/bugs/show_bug.cgi?id=332543
	 */
	public static void safeClose(Closeable stream) {
		try {
			if (stream != null)
				stream.close();
		} catch (IOException e) {
			//ignore
		}
	}

	/**
	 * Converts a URI to an IPath.  Returns null if the URI cannot be represented
	 * as an IPath.
	 * <p>
	 * Note this method differs from URIUtil in its handling of relative URIs
	 * as being relative to path variables.
	 */
	public static IPath toPath(URI uri) {
		if (uri == null)
			return null;
		final String scheme = uri.getScheme();
		// null scheme represents path variable
		if (scheme == null || EFS.SCHEME_FILE.equals(scheme))
			return IPath.fromOSString(uri.getSchemeSpecificPart());
		return null;
	}

	public static final void transferStreams(InputStream source, OutputStream destination, String path,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		try {
			try {
				if (source instanceof ByteArrayInputStream) {
					// ByteArrayInputStream does overload transferTo avoiding buffering
					((ByteArrayInputStream) source).transferTo(destination);
					subMonitor.split(1);
				} else {
					byte[] buffer = new byte[8192];
					while (true) {
						int bytesRead = -1;
						try {
							bytesRead = source.read(buffer);
						} catch (IOException e) {
							String msg = NLS.bind(Messages.localstore_failedReadDuringWrite, path);
							throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, IPath.fromOSString(path), msg, e);
						}
						if (bytesRead == -1) {
							break;
						}
						destination.write(buffer, 0, bytesRead);
						subMonitor.split(1);
					}
				}
				// Bug 332543 - ensure we don't ignore failures on close()
				destination.close();
			} catch (IOException e) {
				String msg = NLS.bind(Messages.localstore_couldNotWrite, path);
				throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, IPath.fromOSString(path), msg, e);
			}
		} finally {
			safeClose(source);
			safeClose(destination);
		}
	}

	/**
	 * Not intended for instantiation.
	 */
	private FileUtil() {
		super();
	}
}