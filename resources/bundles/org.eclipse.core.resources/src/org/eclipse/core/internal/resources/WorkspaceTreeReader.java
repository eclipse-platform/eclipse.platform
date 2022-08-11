/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 * Francis Lynch (Wind River) - [305718] Allow reading snapshot into renamed project
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.DataInputStream;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * Default tree reader that does not read anything. This is used in cases
 * where the tree format is unknown (for example when opening a workspace
 * from a future version).
 */
public abstract class WorkspaceTreeReader {

	/**
	 * Configuration setting to have an existing workspace
	 * project name take precedence over data being read,
	 * when set to <code>true</code>.
	 */
	protected boolean renameProjectNode;

	/**
	 * Returns the tree reader associated with the given tree version number.
	 * @param renameProjectNode if <code>true</code>, set up the reader to have
	 *     the existing root node in the workspace (that is, the project being
	 *     read into) take precedence over the root node being read from the file.
	 *     Otherwise, the tree file is read unmodified.
	 */
	public static WorkspaceTreeReader getReader(Workspace workspace, int version, boolean renameProjectNode) throws CoreException {
		WorkspaceTreeReader w = null;
		switch (version) {
			case ICoreConstants.WORKSPACE_TREE_VERSION_1 :
				w = new WorkspaceTreeReader_1(workspace);
				w.renameProjectNode = renameProjectNode;
				return w;
			case ICoreConstants.WORKSPACE_TREE_VERSION_2 :
				w = new WorkspaceTreeReader_2(workspace);
				w.renameProjectNode = renameProjectNode;
				return w;
			default :
				// Unknown tree version - fail to read the tree
				String msg = NLS.bind(Messages.resources_format, version);
				throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, null, msg, null);
		}
	}

	/**
	 * Returns the tree reader associated with the given tree version number.
	 */
	public static WorkspaceTreeReader getReader(Workspace workspace, int version) throws CoreException {
		return getReader(workspace, version, false);
	}

	/**
	 * Returns a snapshot from the stream. This default implementation does nothing.
	 */
	public abstract ElementTree readSnapshotTree(DataInputStream input, ElementTree complete, IProgressMonitor monitor) throws CoreException;

	/**
	 * Reads all workspace trees from the stream. This default implementation does nothing.
	 */
	public abstract void readTree(DataInputStream input, IProgressMonitor monitor) throws CoreException;

	/**
	 * Reads a project's trees from the stream. This default implementation does nothing.
	 */
	public abstract void readTree(IProject project, DataInputStream input, IProgressMonitor monitor) throws CoreException;
}
