/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Adapted from classes in org.eclipse.ui.ide.undo and org.eclipse.ui.internal.ide.undo
 ******************************************************************************/

package org.eclipse.core.internal.resources.undo.snapshot;

import java.net.URI;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * FolderDescription is a lightweight snapshot that describes a folder to be
 * created.
 *
 * This class is not intended to be instantiated or used by clients.
 *
 * @since 3.20
 *
 */
public class FolderSnapshot extends ContainerSnapshot {

	private boolean virtual = false;

	/**
	 * Create a FolderSnapshot from the specified folder handle. Typically used when
	 * the folder handle represents a resource that actually exists, although it
	 * will not fail if the resource is non-existent.
	 *
	 * @param folder  the folder to be described
	 * @param virtual the folder is a virtual folder
	 */
	public FolderSnapshot(IFolder folder, boolean virtual) {
		super(folder);
		this.virtual = virtual;
	}

	/**
	 * Create a FolderSnapshot from the specified folder handle. If the folder to be
	 * created should be linked to a different location, specify the location.
	 *
	 * @param folder       the folder to be described
	 * @param linkLocation the location to which the folder is linked, or
	 *                     <code>null</code> if it is not linked
	 */
	public FolderSnapshot(IFolder folder, URI linkLocation) {
		super(folder);
		this.name = folder.getName();
		this.location = linkLocation;
	}

	@Override
	public IResource createResourceHandle() {
		IWorkspaceRoot workspaceRoot = getWorkspace().getRoot();
		IPath folderPath = parent.getFullPath().append(name);
		return workspaceRoot.getFolder(folderPath);
	}

	@Override
	public void createExistentResourceFromHandle(IResource resource, IProgressMonitor mon) throws CoreException {
		Assert.isLegal(resource instanceof IFolder);
		if (resource.exists()) {
			return;
		}
		IFolder folderHandle = (IFolder) resource;
		SubMonitor subMonitor = SubMonitor.convert(mon, 300);
		subMonitor.setTaskName(ResourceSnapshotMessages.FolderDescription_NewFolderProgress);
		if (filters != null) {
			SubMonitor loopMonitor = subMonitor.split(100).setWorkRemaining(filters.length);
			for (IResourceFilterDescription filter : filters) {
				folderHandle.createFilter(filter.getType(), filter.getFileInfoMatcherDescription(), 0,
						loopMonitor.split(1));
			}
		}
		subMonitor.setWorkRemaining(200);
		if (location != null) {
			folderHandle.createLink(location, IResource.ALLOW_MISSING_LOCAL, subMonitor.split(100));
		} else {
			folderHandle.create(virtual ? IResource.VIRTUAL : 0, true, subMonitor.split(100));
		}
		createChildResources(folderHandle, subMonitor.split(100));
	}
}