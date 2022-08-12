/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.IProjectSetSerializer;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.examples.filesystem.Policy;

/**
 * This is an old-style (pre-3.0) project set serializer used to test backwards compatibility
 */
public class ProjectSetSerializer implements IProjectSetSerializer {

	@Override
	public String[] asReference(IProject[] providerProjects, Object context, IProgressMonitor monitor) {
		Assert.isTrue(context instanceof Shell);
		List<String> refs = new ArrayList<>();
		for (IProject project : providerProjects) {
			FileSystemProvider provider = (FileSystemProvider)RepositoryProvider.getProvider(project, FileSystemPlugin.PROVIDER_ID);
			if (provider != null) {
				refs.add(asReference(provider));
			}
		}
		return refs.toArray(new String[refs.size()]);
	}

	@Override
	public IProject[] addToWorkspace(String[] referenceStrings, String filename, Object context, IProgressMonitor monitor) {
		Assert.isTrue(context instanceof Shell);
		List<IProject> projects = new ArrayList<>();
		for (String string : referenceStrings) {
			String projectName = getProjectName(string);
			String path = getPath(string);
			if (projectName != null && path != null) {
				try {
					IProject project = createProject(projectName, monitor);
					RepositoryProvider.map(project, FileSystemPlugin.PROVIDER_ID);
					FileSystemProvider provider = (FileSystemProvider) RepositoryProvider.getProvider(project);
					provider.setTargetLocation(path);
					projects.add(project);
				} catch (CoreException e) {
					ErrorDialog.openError(
							(Shell)context,
							Policy.bind("ConfigurationWizard.errorMapping"), //$NON-NLS-1$
							Policy.bind("ConfigurationWizard.error"), //$NON-NLS-1$
							e.getStatus());
				}
			}
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * @param provider
	 * @return
	 */
	private String asReference(FileSystemProvider provider) {
		return provider.getProject().getName() + "," + provider.getRoot().toString(); //$NON-NLS-1$
	}

	/**
	 * @param string
	 * @return
	 */
	private String getProjectName(String string) {
		int i = string.indexOf(',');
		if (i == -1) return null;
		return string.substring(0, i);
	}

	/**
	 * @param string
	 * @return
	 */
	private String getPath(String string) {
		int i = string.indexOf(',');
		if (i == -1) return null;
		return string.substring(i + 1);
	}

	/**
	 * @param projectName
	 * @return
	 * @throws CoreException
	 */
	private IProject createProject(String projectName, IProgressMonitor monitor) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.exists()) {
			project.create(monitor);
		}
		if (!project.isOpen()) {
			project.open(monitor);
		}
		return project;
	}
}
