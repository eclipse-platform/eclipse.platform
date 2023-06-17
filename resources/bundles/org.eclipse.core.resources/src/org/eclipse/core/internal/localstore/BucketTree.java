/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
package org.eclipse.core.internal.localstore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.eclipse.core.internal.localstore.Bucket.Visitor;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;

/**
 * @since 3,1
 */
public class BucketTree {
	public static final int DEPTH_INFINITE = Integer.MAX_VALUE;
	public static final int DEPTH_ONE = 1;
	public static final int DEPTH_ZERO = 0;

	private final static int SEGMENT_QUOTA = 256; //two hex characters

	/**
	 * Store all bucket names to avoid creating garbage when traversing the tree
	 */
	private static final char[][] HEX_STRINGS;

	static {
		HEX_STRINGS = new char[SEGMENT_QUOTA][];
		for (int i = 0; i < HEX_STRINGS.length; i++)
			HEX_STRINGS[i] = Integer.toHexString(i).toCharArray();
	}

	protected Bucket current;

	private Workspace workspace;

	public BucketTree(Workspace workspace, Bucket bucket) {
		this.current = bucket;
		this.workspace = workspace;
	}

	/**
	 * From a starting point in the tree, visit all nodes under it.
	 *
	 * @param visitor the processor for the bucket entries
	 * @param base    the base path to start the tree at
	 * @param depth   the max depth to decent the tree
	 *
	 * @exception CoreException thrown by the visitor or failure to load/save the
	 *                          bucket
	 */
	public void accept(Bucket.Visitor visitor, IPath base, int depth) throws CoreException {
		if (IPath.ROOT.equals(base)) {
			current.load(null, locationFor(IPath.ROOT));
			if (current.accept(visitor, base, DEPTH_ZERO) != Visitor.CONTINUE)
				return;
			if (depth == DEPTH_ZERO)
				return;
			boolean keepVisiting = true;
			depth--;
			IProject[] projects = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
			for (int i = 0; keepVisiting && i < projects.length; i++) {
				IPath projectPath = projects[i].getFullPath();
				keepVisiting = internalAccept(visitor, projectPath, locationFor(projectPath), depth, 1);
			}
		} else
			internalAccept(visitor, base, locationFor(base), depth, 0);
	}

	public void close() throws CoreException {
		current.save();
		saveVersion();
	}

	public Bucket getCurrent() {
		return current;
	}

	public File getVersionFile() {
		return new File(locationFor(IPath.ROOT), current.getVersionFileName());
	}

	/**
	 * This will never be called for a bucket for the workspace root.
	 *
	 * @return whether to continue visiting other branches
	 */
	private boolean internalAccept(Bucket.Visitor visitor, IPath base, File bucketDir, int depthRequested, int currentDepth) throws CoreException {
		current.load(base.segment(0), bucketDir);
		int outcome = current.accept(visitor, base, depthRequested);
		if (outcome != Visitor.CONTINUE)
			return outcome == Visitor.RETURN;
		if (depthRequested <= currentDepth)
			return true;
		File[] subDirs = bucketDir.listFiles();
		if (subDirs == null)
			return true;
		for (File subDir : subDirs) {
			if (subDir.isDirectory()) {
				if (!internalAccept(visitor, base, subDir, depthRequested, currentDepth + 1)) {
					return false;
				}
			}
		}
		return true;
	}

	public void loadBucketFor(IPath path) throws CoreException {
		current.load(IPath.ROOT.equals(path) ? null : path.segment(0), locationFor(path));
	}

	private File locationFor(IPath resourcePath) {
		//optimized to avoid string and path creations
		IPath baseLocation = workspace.getMetaArea().locationFor(resourcePath).removeTrailingSeparator();
		int segmentCount = resourcePath.segmentCount();
		String locationString = baseLocation.toOSString();
		StringBuilder locationBuffer = new StringBuilder(locationString.length() + Bucket.INDEXES_DIR_NAME.length() + 16);
		locationBuffer.append(locationString);
		locationBuffer.append(File.separatorChar);
		locationBuffer.append(Bucket.INDEXES_DIR_NAME);
		// the last segment is ignored
		for (int i = 1; i < segmentCount - 1; i++) {
			// translate all segments except the first one (project name)
			locationBuffer.append(File.separatorChar);
			locationBuffer.append(translateSegment(resourcePath.segment(i)));
		}
		return new File(locationBuffer.toString());
	}

	/**
	 * Writes the version tag to a file on disk.
	 */
	private void saveVersion() throws CoreException {
		File versionFile = getVersionFile();
		if (!versionFile.getParentFile().exists())
			versionFile.getParentFile().mkdirs();
		try (FileOutputStream stream = new FileOutputStream(versionFile)) {
			stream.write(current.getVersion());
		} catch (IOException e) {
			String message = NLS.bind(Messages.resources_writeWorkspaceMeta, versionFile.getAbsolutePath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, null, message, e);
		}
	}

	private char[] translateSegment(String segment) {
		// String.hashCode algorithm is API
		return HEX_STRINGS[Math.abs(Math.abs(segment.hashCode()) % SEGMENT_QUOTA)];
	}
}
