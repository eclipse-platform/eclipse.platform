/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 17, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.team.internal.core;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;

/**
 * Change listener that detects and handles project moves and
 * meta-file creation.
 */
public final class TeamResourceChangeListener implements IResourceChangeListener {

	private static final Map metaFilePaths; // Map of String (repository id) -> IPath[]

	static {
		metaFilePaths = new HashMap();
		String[] ids = RepositoryProvider.getAllProviderTypeIds();
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			IPath[] paths = TeamPlugin.getMetaFilePaths(id);
			if (paths != null) {
				metaFilePaths.put(id, paths);
			}
		}
	}

	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta[] projectDeltas = event.getDelta().getAffectedChildren();
		for (int i = 0; i < projectDeltas.length; i++) {
			IResourceDelta delta = projectDeltas[i];
			IResource resource = delta.getResource();
			IProject project = resource.getProject();
			if (!RepositoryProvider.isShared(project)) {
				// Look for meta-file creation in unshared projects
				handleUnsharedProjectChanges(project, delta);
			} else {
				// Handle project moves
				// Only consider project additions that are moves
				if (delta.getKind() != IResourceDelta.ADDED) continue;
				if ((delta.getFlags() & IResourceDelta.MOVED_FROM) == 0) continue;
				// Only consider projects that have a provider
				RepositoryProvider provider = RepositoryProvider.getProvider(project);
				if (provider == null) continue;
				// Only consider providers whose project is not mapped properly already
				if (provider.getProject().equals(project)) continue;
				// Tell the provider about it's new project
				provider.setProject(project);
			}
		}
	}

	private void handleUnsharedProjectChanges(IProject project, IResourceDelta delta) {
		String repositoryId = null;
		Set metaFileContainers = new HashSet();
		Set badIds = new HashSet();
		IFile[] files = getAddedFiles(delta);
		for (int i = 0; i < files.length; i++) {
			IFile file = files[i];
			String typeId = getMetaFileType(file);
			if (typeId != null) {
				// The file path matches the path for the given type id
				if (repositoryId == null) {
					repositoryId = typeId;
				} else if (!repositoryId.equals(typeId) && !badIds.contains(typeId)) {
					TeamPlugin.log(IStatus.WARNING, "Meta files for two repository types (" + repositoryId + " and " + typeId + " was found in project " + project.getName() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					badIds.add(typeId);
				}
				if (typeId.equals(repositoryId)) {
					IContainer container = getContainer(typeId, file);
					metaFileContainers.add(container);
				}
			}
		}
		if (repositoryId != null) {
			RepositoryProviderType type = RepositoryProviderType.getProviderType(repositoryId);
			type.metaFilesDetected(project, (IContainer[]) metaFileContainers.toArray(new IContainer[metaFileContainers.size()]));
		}
	}

	private IContainer getContainer(String typeId, IFile file) {
		IPath[] paths = (IPath[])metaFilePaths.get(typeId);
		IPath foundPath = null;
		IPath projectRelativePath = file.getProjectRelativePath();
		for (int i = 0; i < paths.length; i++) {
			IPath path = paths[i];
			if (isSuffix(projectRelativePath, path)) {
				foundPath = path;
			}
		}
		IResource resource = file;
		if (foundPath != null) {
			for (int i = 0; i < foundPath.segmentCount(); i++) {
				resource = resource.getParent();
			}
		}
		if (resource.getType() == IResource.FILE) {
			return file.getParent();
		}
		return (IContainer)resource;
	}

	private String getMetaFileType(IFile file) {
		for (Iterator iter = metaFilePaths.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			IPath[] paths = (IPath[])metaFilePaths.get(id);
			for (int i = 0; i < paths.length; i++) {
				IPath path = paths[i];
				if (isSuffix(file.getProjectRelativePath(), path)) {
					return id;
				}
			}
		}
		return null;
	}

	private boolean isSuffix(IPath path, IPath suffix) {
		if (path.segmentCount() < suffix.segmentCount())
			return false;
		for (int i = 0; i < suffix.segmentCount(); i++) {
			if (!suffix.segment(i).equals(path.segment(path.segmentCount() - suffix.segmentCount() + i))) {
				return false;
			}
		}
		return true;
	}

	private IFile[] getAddedFiles(IResourceDelta delta) {
		final List result = new ArrayList();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					if ((delta.getKind() & IResourceDelta.ADDED) != 0
							&& delta.getResource().getType() == IResource.FILE) {
						result.add(delta.getResource());
					}
					return true;
				}
			});
		} catch (CoreException e) {
			TeamPlugin.log(IStatus.ERROR, "An error occurred while scanning for meta-file changes", e); //$NON-NLS-1$
		}
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}
}