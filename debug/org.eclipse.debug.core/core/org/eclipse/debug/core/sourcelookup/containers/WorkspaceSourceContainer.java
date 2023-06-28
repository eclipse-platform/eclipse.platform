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
package org.eclipse.debug.core.sourcelookup.containers;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;

/**
 * All projects in the workspace.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class WorkspaceSourceContainer extends CompositeSourceContainer {

	/**
	 * Unique identifier for the workspace source container type
	 * (value <code>org.eclipse.debug.core.containerType.workspace</code>).
	 */
	public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.workspace"; //$NON-NLS-1$

	public WorkspaceSourceContainer() {
	}

	@Override
	public String getName() {
		return SourceLookupMessages.WorkspaceSourceContainer_0;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof WorkspaceSourceContainer;
	}

	@Override
	public int hashCode() {
		return ResourcesPlugin.getWorkspace().hashCode();
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ISourceContainer[] containers = new ISourceContainer[projects.length];
		for (int i = 0; i < projects.length; i++) {
			ISourceContainer container = new ProjectSourceContainer(projects[i], false);
			container.init(getDirector());
			containers[i] = container;
		}
		return containers;
	}

}
