/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.examples.filesystem.Policy;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.IConfigurationWizardExtension;
import org.eclipse.ui.IWorkbench;

/**
 * The file system configuration wizard used when associating a project
 * the the file system provider. It is registered as a Team configuration wizard
 * in the plugin.xml and is invoked when a user chooses to create a File System
 * Repository Provider. One invoked, this wizard makes use of the <code>FileSystemMainPage</code>
 * in order to obtain a target location on disk.
 */
public class ConfigurationWizard extends Wizard implements IConfigurationWizard, IAdaptable {

	IProject[] projects;

	FileSystemMainPage mainPage;

	public ConfigurationWizard() {
		// retrieve the remembered dialog settings
		IDialogSettings workbenchSettings = FileSystemPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("ProviderExamplesWizard"); //$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("ProviderExamplesWizard"); //$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * Remember the project so we can map it on finish
	 *
	 * @see org.eclipse.team.ui.IConfigurationWizard#init(IWorkbench, IProject)
	 */
	@Override
	public void init(IWorkbench workbench, IProject project) {
		setProjects(new IProject[] { project } );
	}

	@Override
	public void addPages() {
		mainPage = new FileSystemMainPage(
			"FileSystemMainPage", //$NON-NLS-1$
			Policy.bind("ConfigurationWizard.name"),  //$NON-NLS-1$
			Policy.bind("ConfigurationWizard.description"),  //$NON-NLS-1$
			null);
		addPage(mainPage);
	}

	/*
	 * Using the information entered in the main page set the provider for
	 * the given project.
	 */
	@Override
	public boolean performFinish() {
		mainPage.finish(null);
		try {
			if (projects.length == 1) {
				// Map the provider and set the location
				RepositoryProvider.map(projects[0], FileSystemPlugin.PROVIDER_ID);
				FileSystemProvider provider = (FileSystemProvider) RepositoryProvider.getProvider(projects[0]);
				provider.setTargetLocation(mainPage.getLocation());
			} else {
				for (IProject project : projects) {
					RepositoryProvider.map(project, FileSystemPlugin.PROVIDER_ID);
					FileSystemProvider provider = (FileSystemProvider) RepositoryProvider.getProvider(project);
					String path = IPath.fromOSString(mainPage.getLocation()).append(project.getName()).toOSString();
					provider.setTargetLocation(path);
				}
			}
		} catch (TeamException e) {
			ErrorDialog.openError(
				getShell(),
				Policy.bind("ConfigurationWizard.errorMapping"), //$NON-NLS-1$
				Policy.bind("ConfigurationWizard.error"), //$NON-NLS-1$
				e.getStatus());
			return false;
		}
		return true;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IConfigurationWizardExtension.class) {
			return adapter.cast((IConfigurationWizardExtension) (workbench, projects) -> setProjects(projects));
		}
		return null;
	}

	/* package */ void setProjects(IProject[] projects) {
		this.projects = projects;
	}
}
