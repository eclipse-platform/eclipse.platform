/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;

public class MoveDeleteManager implements IMoveDeleteHook {

	private static final IMoveDeleteHook DEFAULT_HOOK = new DefaultMoveDeleteHook();

	private IMoveDeleteHook getHookFor(IResource resource) {
		IProject project = resource.getProject();
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if(provider==null) {
			return DEFAULT_HOOK;
		}
		IMoveDeleteHook hook = provider.getMoveDeleteHook();
		if (hook == null) {
			return DEFAULT_HOOK;
		}
		return hook;
	}

	@Override
	public boolean deleteFile(
		IResourceTree tree,
		IFile file,
		int updateFlags,
		IProgressMonitor monitor) {

		return getHookFor(file).deleteFile(tree, file, updateFlags, monitor);
	}

	@Override
	public boolean deleteFolder(
		IResourceTree tree,
		IFolder folder,
		int updateFlags,
		IProgressMonitor monitor) {

		return getHookFor(folder).deleteFolder(tree, folder, updateFlags, monitor);
	}

	@Override
	public boolean deleteProject(
		IResourceTree tree,
		IProject project,
		int updateFlags,
		IProgressMonitor monitor) {

		return getHookFor(project).deleteProject(tree, project, updateFlags, monitor);
	}

	@Override
	public boolean moveFile(
		IResourceTree tree,
		IFile source,
		IFile destination,
		int updateFlags,
		IProgressMonitor monitor) {

		return getHookFor(source).moveFile(tree, source, destination, updateFlags, monitor);
	}

	@Override
	public boolean moveFolder(
		IResourceTree tree,
		IFolder source,
		IFolder destination,
		int updateFlags,
		IProgressMonitor monitor) {

		return getHookFor(source).moveFolder(tree, source, destination, updateFlags, monitor);
	}

	@Override
	public boolean moveProject(
		IResourceTree tree,
		IProject source,
		IProjectDescription description,
		int updateFlags,
		IProgressMonitor monitor) {

		return getHookFor(source).moveProject(tree, source, description, updateFlags, monitor);
	}

}
