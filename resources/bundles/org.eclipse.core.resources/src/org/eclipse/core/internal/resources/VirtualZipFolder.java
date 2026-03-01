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

package org.eclipse.core.internal.resources;

import java.net.URISyntaxException;
import org.eclipse.core.filesystem.ZipFileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.ZipFileTransformer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**
 *
 */
public class VirtualZipFolder extends Folder {

	public VirtualZipFolder(IPath path, Workspace container) {
		super(path, container);
	}

	@Override
	public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		try {
			ZipFileTransformer.closeZipFile(this);
			IFile closedZipFile = this.getParent().getFile(new Path(this.getName()));
			closedZipFile.copy(destination, updateFlags, monitor);
			IFile copiedZipFile = ResourcesPlugin.getWorkspace().getRoot().getFile(destination);

			// If the destination is not a nested ZIP, open the copied ZIP file
			if (!ZipFileUtil.isInsideOpenZipFile(copiedZipFile.getLocationURI())) {
				ZipFileTransformer.openZipFile(copiedZipFile, false);
			}
			// Reopen the original ZIP file
			if (!ZipFileUtil.isInsideOpenZipFile(closedZipFile.getLocationURI())) {
				ZipFileTransformer.openZipFile(closedZipFile, false);
			}
		} catch (URISyntaxException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Error copying ZIP file", e)); //$NON-NLS-1$
		}
	}

	@Override
	public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
		try {
			if ((updateFlags & IResource.CLOSE_ZIP_FILE) != 0) {
				super.delete(updateFlags, monitor);
				return;
			}

			ZipFileTransformer.closeZipFile(this);
			IFile closedZipFile = this.getParent().getFile(IPath.fromOSString(this.getName()));
			closedZipFile.delete(updateFlags, monitor);
		} catch (URISyntaxException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Error deleting ZIP file", e)); //$NON-NLS-1$
		}
	}

	@Override
	public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		try {
			ZipFileTransformer.closeZipFile(this);
			IFile closedZipFile = this.getParent().getFile(new Path(this.getName()));
			closedZipFile.move(destination, updateFlags, monitor);
			IFile movedZipFile = ResourcesPlugin.getWorkspace().getRoot().getFile(destination);

			// If the destination is not a nested ZIP, open the moved ZIP file
			if (!ZipFileUtil.isInsideOpenZipFile(movedZipFile.getLocationURI())) {
				ZipFileTransformer.openZipFile(movedZipFile, false);
			}
		} catch (URISyntaxException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Error moving ZIP file", e)); //$NON-NLS-1$
		}
	}

	@Override
	public IPath getLocation() {
		String nameSegment = path.lastSegment();
		IPath parentPath = this.getParent().getLocation();
		return parentPath.append(nameSegment);
	}

	@Override
	public IFile changeToFile() {
		return this.getParent().getFile(new Path(this.getName()));
	}
}
