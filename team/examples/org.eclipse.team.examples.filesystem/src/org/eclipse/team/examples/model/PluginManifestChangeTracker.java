/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.mapping.ChangeTracker;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;

public class PluginManifestChangeTracker extends ChangeTracker {

	Set<IPath> manifestFilePaths;

	public PluginManifestChangeTracker() {
		manifestFilePaths = new HashSet<>();
		manifestFilePaths.add(new Path(null, "plugin.xml"));
		manifestFilePaths.add(new Path(null, "plugin.properties"));
		manifestFilePaths.add(new Path(null, "build.properties"));
		manifestFilePaths.add(new Path(null, "META-INF/MANIFEST.MF"));
	}

	@Override
	protected boolean isProjectOfInterest(IProject project) {
		return super.isProjectOfInterest(project) && hasPDENature(project);
	}

	private boolean hasPDENature(IProject project) {
		try {
			return project.getDescription().hasNature("org.eclipse.pde.PluginNature");
		} catch (CoreException e) {
			FileSystemPlugin.log(new Status(e.getStatus().getSeverity(), FileSystemPlugin.ID, 0,
					NLS.bind("Could not obtain project description for {0}", project.getName()), e));
		}
		return false;
	}

	@Override
	protected void handleChanges(IProject project, IResource[] resources) {
		handleProjectChange(project);
	}

	@Override
	protected void handleProjectChange(IProject project) {
		List<IFile> changes = new ArrayList<>();
		for (IPath path : manifestFilePaths) {
			IFile file = project.getFile(path);
			try {
				if (isModified(file)) {
					changes.add(file);
				}
			} catch (CoreException e) {
				FileSystemPlugin.log(new Status(e.getStatus().getSeverity(), FileSystemPlugin.ID, 0,
						NLS.bind("Could not obtain diff for {0}", file.getFullPath().toString()), e));
			}
		}
		if (changes.size() > 1) {
			groupInSet(project, changes.toArray(new IFile[changes.size()]));
		}
	}

	private void groupInSet(IProject project, IFile[] files) {
		String name = getSetName(project);
		try {
			ensureGrouped(project, name, files);
		} catch (CoreException e) {
			FileSystemPlugin.log(new Status(e.getStatus().getSeverity(), FileSystemPlugin.ID, 0,
					NLS.bind("Could not create change set {0}", name), e));
		}
	}

	private String getSetName(IProject project) {
		return "Plugin manifest files for " + project.getName();
	}

	@Override
	protected boolean isResourceOfInterest(IResource resource) {
		return manifestFilePaths.contains(resource.getProjectRelativePath());
	}
}
