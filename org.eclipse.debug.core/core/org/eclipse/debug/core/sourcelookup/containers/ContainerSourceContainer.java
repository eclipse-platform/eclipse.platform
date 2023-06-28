/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *     QNX Software Systems - Mikhail Khodjaiants - Bug 80857
 *******************************************************************************/
package org.eclipse.debug.core.sourcelookup.containers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;

/**
 * A source container for a container in the workspace. Source elements are searched
 * for within this container and optionally nested containers.
 * <p>
 * Names specified in <code>findSourceElements</code> method can
 * be simple or qualified. When a name is qualified, a file will
 * be searched for relative to this container, and optionally
 * nested containers.
 * </p>
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class ContainerSourceContainer extends CompositeSourceContainer {

	private IContainer fContainer = null;
	private boolean fSubfolders = false;

	private URI fRootURI = null;
	private IFileStore fRootFile = null;
	private IWorkspaceRoot fRoot = null;

	/**
	 * Constructs a source container on the given workspace container.
	 *
	 * @param container the container to search for source in
	 * @param subfolders whether nested folders should be searched
	 *  for source elements
	 */
	public ContainerSourceContainer(IContainer container, boolean subfolders) {
		fContainer = container;
		fSubfolders = subfolders;
		fRootURI = fContainer.getLocationURI();
		if (fRootURI != null) {
			try {
				fRootFile = EFS.getStore(fRootURI);
			} catch (CoreException e) {
			}
			fRoot = ResourcesPlugin.getWorkspace().getRoot();
		}
	}

	/**
	 * Returns the workspace container this source container is
	 * rooted at.
	 *
	 * @return the workspace container this source container is
	 * rooted at
	 */
	public IContainer getContainer() {
		return fContainer;
	}

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		ArrayList<Object> sources = new ArrayList<>();

		// An IllegalArgumentException is thrown from the "getFile" method
		// if the path created by appending the file name to the container
		// path doesn't conform with Eclipse resource restrictions.
		// To prevent the interruption of the search procedure we check
		// if the path is valid before passing it to "getFile".
		if ( validateFile(name) ) {
			IFile file = fContainer.getFile(IPath.fromOSString(name));
			if (file.exists()) {
				sources.add(file);
			} else {
				// See bug 82627 - perform case insensitive source lookup
				if (fRootURI == null) {
					return EMPTY;
				}
				// bug 295828 root file may be null for an invalid linked resource
				if (fRootFile != null) {
					// See bug 98090 - we need to handle relative path names
					IFileStore target = fRootFile.getFileStore(IPath.fromOSString(name));
					if (target.fetchInfo().exists()) {
						// We no longer have to account for bug 95832, and URIs take care
						// of canonical paths (fix to bug 95679 was removed).
						IFile[] files = fRoot.findFilesForLocationURI(target.toURI());
						if (isFindDuplicates() && files.length > 1) {
							Collections.addAll(sources, files);
						} else if (files.length > 0) {
							sources.add(files[0]);
						}
					}
				}
			}
		}

		//check sub-folders
		if ((isFindDuplicates() && fSubfolders) || (sources.isEmpty() && fSubfolders)) {
			for (ISourceContainer container : getSourceContainers()) {
				Object[] objects = container.findSourceElements(name);
				if (objects == null || objects.length == 0) {
					continue;
				}
				if (isFindDuplicates()) {
					Collections.addAll(sources, objects);
				} else {
					sources.add(objects[0]);
					break;
				}
			}
		}

		if(sources.isEmpty()) {
			return EMPTY;
		}
		return sources.toArray();
	}

	@Override
	public String getName() {
		return getContainer().getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ContainerSourceContainer) {
			ContainerSourceContainer loc = (ContainerSourceContainer) obj;
			return loc.getContainer().equals(getContainer());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getContainer().hashCode();
	}

	@Override
	public boolean isComposite() {
		return fSubfolders;
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		if(fSubfolders) {
			IResource[] resources = getContainer().members();
			List<ISourceContainer> list = new ArrayList<>(resources.length);
			for (IResource resource : resources) {
				if (resource.getType() == IResource.FOLDER) {
					list.add(new FolderSourceContainer((IFolder)resource, fSubfolders));
				}
			}
			ISourceContainer[] containers = list.toArray(new ISourceContainer[list.size()]);
			for (ISourceContainer container : containers) {
				container.init(getDirector());
			}
			return containers;
		}
		return new ISourceContainer[0];
	}

	/**
	 * Validates the given string as a path for a file in this container.
	 *
	 * @param name path name
	 * @return <code>true</code> if the path is valid <code>false</code> otherwise
	 */
	private boolean validateFile(String name) {
		IContainer container = getContainer();
		IPath path = container.getFullPath().append(name);
		return ResourcesPlugin.getWorkspace().validatePath(path.toOSString(), IResource.FILE).isOK();
	}

}
