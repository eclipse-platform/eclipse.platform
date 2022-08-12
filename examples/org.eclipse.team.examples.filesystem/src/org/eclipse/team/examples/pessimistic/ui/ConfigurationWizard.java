/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProviderPlugin;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

/**
 * A wizard which adds the <code>PessimisticFilesystemProvider</code> nature
 * to a given project.
 */
public class ConfigurationWizard extends Wizard implements IConfigurationWizard {
	/*
	 * The project in question.
	 */
	private IProject project;

	@Override
	public void addPages() {
		// workaround the wizard problem
		addPage(new BlankPage());
	}

	@Override
	public boolean performFinish() {
		try {
			RepositoryProvider.map(project, PessimisticFilesystemProviderPlugin.NATURE_ID);
		} catch (TeamException e) {
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Could not set sharing on " + project);
			return false;
		}
		return true;
	}

	@Override
	public void init(IWorkbench workbench, IProject project) {
		this.project = project;
	}
}
