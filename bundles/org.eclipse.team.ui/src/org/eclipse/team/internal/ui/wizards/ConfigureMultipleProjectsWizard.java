/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.IConfigurationWizardExtension;

/**
 * Wizard that supports the sharing of multiple projects for those repository providers
 * that have not adapted their {@link IConfigurationWizard} to {@link IConfigurationWizardExtension}.
 */
public class ConfigureMultipleProjectsWizard extends Wizard {

	private final IProject[] projects;
	private final ConfigurationWizardElement element;
	private ProjectSelectionPage projectSelectionPage;

	public ConfigureMultipleProjectsWizard(IProject[] projects, ConfigurationWizardElement element) {
		this.projects = projects;
		this.element = element;
	}


	@Override
	public void addPages() {
		projectSelectionPage = new ProjectSelectionPage(projects, element);
		addPage(projectSelectionPage);
	}

	@Override
	public boolean performFinish() {
		// Prompt if there are still unshared projects
		if (projectSelectionPage.hasUnsharedProjects()) {
			return MessageDialog.openConfirm(getShell(), TeamUIMessages.ConfigureMultipleProjectsWizard_0, TeamUIMessages.ConfigureMultipleProjectsWizard_1);
		}
		return true;
	}

}
