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
import java.util.zip.ZipException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.filesystem.ZipFileUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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
	 * @param file              The file representing the zip file to open.
	 * @param backgroundRefresh A boolean indicating wether the zip file should be
	 *                          loaded in the background or in the foreground. When
	 *                          testing the boolean should be false.
	 *
	 */
	public static void openZipFile(IFile file, boolean backgroundRefresh)
			throws URISyntaxException, CoreException {
		try (InputStream fis = file.getContents()) {
			ZipFileUtil.canZipFileBeOpened(fis);
			// Additional operations can continue here if header is correct
		} catch (IOException e) {
			if (e instanceof ZipException && e.getMessage().equals("encrypted ZIP entry not supported")) { //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES,
						"Opening encrypted ZIP files is not supported: " + file.getName(), e)); //$NON-NLS-1$
			}
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES,
					"The file is either empty or doesn't represent a ZIP file: " + file.getName(), e)); //$NON-NLS-1$
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
		int flags = backgroundRefresh ? IResource.REPLACE | IResource.BACKGROUND_REFRESH : IResource.REPLACE;

		try {
			link.createLink(zipURI, flags, null);
			link.refreshLocal(0, null);
		} catch (CoreException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Zip File could not be opened")); //$NON-NLS-1$
		}
	}
}
