/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.sourcelookup;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;


/**
 * Computes the default source lookup path for a PDA launch configuration.
 * The default source lookup path is the folder or project containing
 * the PDA program being launched. If the program is not specified, the workspace
 * is searched by default.
 */
public class PDASourcePathComputerDelegate implements ISourcePathComputerDelegate {

	@Override
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		String path = configuration.getAttribute(DebugCorePlugin.ATTR_PDA_PROGRAM, (String)null);
		ISourceContainer sourceContainer = null;
		if (path != null) {
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(IPath.fromOSString(path));
			if (resource != null) {
				//#ifdef ex4
//#				// TODO: Exercise 4 - seed the source lookup path
				//#else
				IContainer container = resource.getParent();
				if (container.getType() == IResource.PROJECT) {
					sourceContainer = new ProjectSourceContainer((IProject)container, false);
				} else if (container.getType() == IResource.FOLDER) {
					sourceContainer = new FolderSourceContainer(container, false);
				}
				//#endif
			}
		}
		if (sourceContainer == null) {
			sourceContainer = new WorkspaceSourceContainer();
		}
		return new ISourceContainer[]{sourceContainer};
	}
}
