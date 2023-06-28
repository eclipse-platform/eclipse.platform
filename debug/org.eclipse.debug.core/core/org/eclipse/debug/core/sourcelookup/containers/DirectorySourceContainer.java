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
 *******************************************************************************/
package org.eclipse.debug.core.sourcelookup.containers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;

/**
 * A directory in the local file system. Source elements returned
 * from <code>findSourceElements(...)</code> are instances
 * of <code>LocalFileStorage</code>.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */

public class DirectorySourceContainer extends CompositeSourceContainer {

	// root directory
	private File fDirectory;
	// whether to search sub-folders
	private boolean fSubfolders = false;
	/**
	 * Unique identifier for the directory source container type
	 * (value <code>org.eclipse.debug.core.containerType.directory</code>).
	 */
	public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.directory"; //$NON-NLS-1$

	/**
	 * Constructs an external folder container for the
	 * directory identified by the given path.
	 *
	 * @param dirPath path to a directory in the local file system
	 * @param subfolders whether folders within the root directory
	 *  should be searched for source elements
	 */
	public DirectorySourceContainer(IPath dirPath, boolean subfolders) {
		this(dirPath.toFile(), subfolders);
	}

	/**
	 * Constructs an external folder container for the
	 * directory identified by the given file.
	 *
	 * @param dir a directory in the local file system
	 * @param subfolders whether folders within the root directory
	 *  should be searched for source elements
	 */
	public DirectorySourceContainer(File dir, boolean subfolders) {
		fDirectory = dir;
		fSubfolders = subfolders;
	}

	@Override
	public String getName() {
		return fDirectory.getName();
	}

	/**
	 * Returns the root directory in the local file system associated
	 * with this source container.
	 *
	 * @return the root directory in the local file system associated
	 * with this source container
	 */
	public File getDirectory() {
		return fDirectory;
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		ArrayList<Object> sources = new ArrayList<>();
		File directory = getDirectory();
		File file = new File(directory, name);
		if (file.exists() && file.isFile()) {
			sources.add(new LocalFileStorage(file));
		}

		//check sub-folders
		if ((isFindDuplicates() && fSubfolders) || (sources.isEmpty() && fSubfolders)) {
			ISourceContainer[] containers = getSourceContainers();
			for (ISourceContainer container : containers) {
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
	public boolean isComposite() {
		return fSubfolders;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DirectorySourceContainer) {
			DirectorySourceContainer container = (DirectorySourceContainer) obj;
			return container.getDirectory().equals(getDirectory());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getDirectory().hashCode();
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		if (isComposite()) {
			String[] files = fDirectory.list();
			if (files != null) {
				List<ISourceContainer> dirs = new ArrayList<>();
				for (String name : files) {
					File file = new File(getDirectory(), name);
					if (file.exists() && file.isDirectory()) {
						dirs.add(new DirectorySourceContainer(file, true));
					}
				}
				ISourceContainer[] containers = dirs.toArray(new ISourceContainer[dirs.size()]);
				for (ISourceContainer container : containers) {
					container.init(getDirector());
				}
				return containers;
			}
		}
		return new ISourceContainer[0];
	}

}
