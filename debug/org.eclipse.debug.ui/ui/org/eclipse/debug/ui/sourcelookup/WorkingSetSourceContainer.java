/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui.sourcelookup;

import java.util.ArrayList;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.ui.IWorkingSet;

/**
 * A working set in the workspace.  Source is searched for in the projects (referenced
 * projects) and folders (sub-folders) that are part of the working set.
 * <p>
 * This class may be instantiated.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class WorkingSetSourceContainer extends CompositeSourceContainer{

	private IWorkingSet fWorkingSet;
	/**
	 * Unique identifier for the working set source container type
	 * (value <code>org.eclipse.debug.ui.containerType.workingSet</code>.)
	 */
	public static final String TYPE_ID = DebugUIPlugin.getUniqueIdentifier()+".containerType.workingSet"; //$NON-NLS-1$

	/**
	 * Creates a source container for the working set.
	 * @param workingSet the working set represented by this container
	 */
	public WorkingSetSourceContainer(IWorkingSet workingSet) {
		fWorkingSet = workingSet;
	}

	@Override
	public String getName() {
		return fWorkingSet.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof WorkingSetSourceContainer) {
			return ((WorkingSetSourceContainer)obj).fWorkingSet.equals(fWorkingSet);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fWorkingSet.hashCode();
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		IAdaptable[] elements = fWorkingSet.getElements();

		if(elements == null) {
			return new ISourceContainer[0];
		}

		ArrayList<ISourceContainer> locationList = new ArrayList<>();
		for (IAdaptable element : elements) {
			IResource resource = element.getAdapter(IResource.class);
			if (resource != null) {
				switch (resource.getType()) {
					case IResource.FOLDER:
						locationList.add(new FolderSourceContainer((IFolder)resource, true));
						break;
					case IResource.PROJECT:
						locationList.add(new ProjectSourceContainer((IProject)resource, true));
						break;
						//if the element corresponds to an IFile, do nothing
					default:
						break;
				}
			}
		}
		return locationList.toArray(new ISourceContainer[locationList.size()]);
	}
}
