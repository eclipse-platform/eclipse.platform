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
package org.eclipse.team.internal.ui.actions;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.wizards.ConfigureProjectWizard;

/**
 * Action for configuring a project. Configuring involves associating
 * the project with a Team provider and performing any provider-specific
 * configuration that is necessary.
 */
public class ConfigureProjectAction extends TeamAction {

	@Override
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		run(monitor -> {
			try {
				if (!isEnabled())
					return;
				IProject[] projects = getSelectedProjects();
				ConfigureProjectWizard.shareProjects(getShell(), projects);
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		}, TeamUIMessages.ConfigureProjectAction_configureProject, PROGRESS_BUSYCURSOR);
	}

	/**
	 * @see TeamAction#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		IProject[] selectedProjects = getSelectedProjects();
		for (IProject project : selectedProjects) {
			if (!project.isAccessible()) return false;
			if (RepositoryProvider.isShared(project)) return false;
		}
		return selectedProjects.length > 0;
	}
}
