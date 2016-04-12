/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.actions;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
		run(new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (!isEnabled())
						return;
					IProject[] projects = getSelectedProjects();
					ConfigureProjectWizard.shareProjects(getShell(), projects);
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
			}
		}, TeamUIMessages.ConfigureProjectAction_configureProject, PROGRESS_BUSYCURSOR);
	}

	/**
	 * @see TeamAction#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		IProject[] selectedProjects = getSelectedProjects();
		for (int i = 0; i < selectedProjects.length; i++) {
			IProject project = selectedProjects[i];
			if (!project.isAccessible()) return false;
			if (RepositoryProvider.isShared(project)) return false;
		}
		return selectedProjects.length > 0;
	}
}
