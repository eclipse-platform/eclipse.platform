/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.resources.undo.snapshot;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import org.eclipse.core.internal.resources.undo.snapshot.ContainerSnapshot;
import org.eclipse.core.internal.resources.undo.snapshot.FileSnapshot;
import org.eclipse.core.internal.resources.undo.snapshot.FolderSnapshot;
import org.eclipse.core.internal.resources.undo.snapshot.IFileContentSnapshot;
import org.eclipse.core.internal.resources.undo.snapshot.MarkerSnapshot;
import org.eclipse.core.internal.resources.undo.snapshot.ProjectSnapshot;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * ResourceSnapshotFactory provides utility methods for creating snapshots of
 * resources or markers.
 *
 * @since 3.20
 */
public class ResourceSnapshotFactory {
	/**
	 * Create a resource snapshot given the specified resource. The resource is
	 * assumed to exist.
	 *
	 * @param resource the resource from which a description should be created
	 * @return the resource description
	 */
	public static IResourceSnapshot fromResource(IResource resource) {
		if (resource.getType() == IResource.PROJECT) {
			return new ProjectSnapshot((IProject) resource);
		} else if (resource.getType() == IResource.FOLDER) {
			return new FolderSnapshot((IFolder) resource, resource.isVirtual());
		} else if (resource.getType() == IResource.FILE) {
			return new FileSnapshot((IFile) resource);
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Create a project snapshot from a specified IProjectDescription. Used when the
	 * project does not yet exist.
	 *
	 * @param projectDescription the project description for the future project
	 */
	public static IContainerSnapshot fromProjectDescription(IProjectDescription projectDescription) {
		return new ProjectSnapshot(projectDescription);
	}

	/**
	 * Create a container description from the specified container handle that can
	 * be used to create the container. The returned ContainerDescription should
	 * represent any non-existing parents in addition to the specified container.
	 *
	 * @param container the handle of the container to be described
	 * @return a container description describing the container and any non-existing
	 *         parents.
	 */
	public static IContainerSnapshot fromContainer(IContainer container) {
		return ContainerSnapshot.fromContainer(container);
	}

	/**
	 * Create a group container description from the specified container handle that
	 * can be used to create the container. The returned ContainerDescription should
	 * represent any non-existing parents in addition to the specified container.
	 *
	 * @param container the handle of the container to be described
	 * @return a container description describing the container and any non-existing
	 *         parents.
	 */
	public static IContainerSnapshot fromVirtualFolderContainer(IContainer container) {
		return ContainerSnapshot.fromContainer(container, true);
	}

	/**
	 * Create a file snapshot from the specified file handle. The handle does not
	 * exist, so no information should be derived from it. If a location path is
	 * specified, this file should represent a link to another location. The content
	 * description describes any state that should be used when the file resource is
	 * created.
	 *
	 * @param file         the file to be described
	 * @param linkLocation the location of the file's link, or <code>null</code> if
	 *                     the file is not linked
	 * @param contents     an input stream representing the contents of the file
	 */
	public static IResourceSnapshot fromFileDetails(IFile file, URI linkLocation, InputStream contents) {
		return new FileSnapshot(file, linkLocation, createFileContentDescription(file, contents));
	}

	/**
	 *
	 * Create a marker snapshot from the specified marker.
	 *
	 * @param marker the marker to be described
	 * @throws CoreException
	 */
	public static IMarkerSnapshot fromMarker(IMarker marker) throws CoreException {
		return new MarkerSnapshot(marker);
	}

	/**
	 * Create a marker snapshot from the specified marker type, attributes, and
	 * resource.
	 *
	 * @param type       the type of marker to be created.
	 * @param attributes the attributes to be assigned to the marker
	 * @param resource   the resource on which the marker should be created
	 */
	public static IMarkerSnapshot fromMarkerDetails(String type, Map<String, Object> attributes, IResource resource) {
		return new MarkerSnapshot(type, attributes, resource);
	}

	/*
	 * Create a file state that represents the desired contents and attributes of
	 * the file to be created. Used to mimic file history when a resource is first
	 * created.
	 */
	private static IFileContentSnapshot createFileContentDescription(final IFile file, final InputStream contents) {
		return new IFileContentSnapshot() {
			@Override
			public InputStream getContents() {
				if (contents != null) {
					return contents;
				}
				return new ByteArrayInputStream(new byte[0]);
			}

			@Override
			public String getCharset() {
				try {
					return file.getCharset(false);
				} catch (CoreException e) {
					return null;
				}
			}

			@Override
			public boolean exists() {
				return true;
			}
		};
	}
}
