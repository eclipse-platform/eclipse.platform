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

package org.eclipse.core.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.filesystem.ZipFileUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Utility class for opening and closing zip files.
 *
 * @since 3.21
 */
public class ZipFileTransformer {

	/**
	 * Closes an opened zip file represented as a linked folder in the workspace.
	 * After closing, the zip file in its file state is shown in the workspace.
	 *
	 * This method can only be called when the zip file is local. Otherwise a
	 * CoreException is thrown.
	 *
	 * @param folder The folder representing the zip file to close.
	 *
	 */
	public static void closeZipFile(IFolder folder) throws URISyntaxException, CoreException {
		URI zipURI = new URI(folder.getLocationURI().getQuery());
		IFileStore parentStore = EFS.getStore(folder.getParent().getLocationURI());
		URI childURI = parentStore.getChild(folder.getName()).toURI();
		if (URIUtil.equals(zipURI, childURI)) {
			folder.delete(IResource.CLOSE_ZIP_FILE, null);
			folder.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		} else {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES,
					"Closing of Zip File " + folder.getName() //$NON-NLS-1$
							+ " failed because the Zip File is not local.")); //$NON-NLS-1$
		}
	}

	/**
	 * Opens a zip file represented by a file into a linked folder. The zip file
	 * will not be extracted in this process. In the opened state the linked folder
	 * allows reading and manipulating the children of the zip file in the workspace
	 * and on the filesystem. If the folder has no children after opening, then it
	 * is closed immediately after. In this case it could be that there should be no
	 * children, so there is no need for opening or an error occured that prevented
	 * the children from being loaded.
	 *
	 * This method prevents opening linked zip files. zip files must be local to be
	 * opened. Otherwise a CoreException is thrown.
	 *
	 * @param file    The file representing the zip file to open.
	 * @param monitor monitor indicating the completion progress
	 *
	 */
	public static void openZipFile(IFile file, IProgressMonitor monitor, boolean backgroundRefresh)
			throws URISyntaxException, CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 20);
		try (InputStream fis = file.getContents()) {
			ZipFileUtil.checkFileForZipHeader(fis);
			// Additional operations can continue here if header is correct
		} catch (IOException e) {
			// If the header is incorrect or there's an IO error, handle gracefully
			throw new CoreException(new Status(IStatus.ERROR, "your.plugin.id", //$NON-NLS-1$
					"Failed to open ZIP file due to incorrect file header: " + file.getName(), e)); //$NON-NLS-1$
		}

		if (file.isLinked()) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES,
					"The file " + file.getName() + " is a linked resource and thus can not be opened")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (ZipFileUtil.isNested(file.getLocationURI())) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES,
					"Nested ZIP files are not allowed to be opened: " + file.getName())); //$NON-NLS-1$
		}


		URI zipURI = new URI("zip", null, "/", file.getLocationURI().toString(), null); //$NON-NLS-1$ //$NON-NLS-2$
		IFolder link = file.getParent().getFolder(IPath.fromOSString(file.getName()));
		subMonitor.split(1);
		int flags = backgroundRefresh ? IResource.REPLACE | IResource.BACKGROUND_REFRESH : IResource.REPLACE;

		try {
			link.createLink(zipURI, flags, subMonitor.split(19));
			link.refreshLocal(0, subMonitor);
		} catch (CoreException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Zip File could not be opened")); //$NON-NLS-1$
		}
	}
}
