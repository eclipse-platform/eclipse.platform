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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.examples.filesystem.Policy;
import org.eclipse.team.internal.ui.actions.TeamAction;

/**
 * Action for getting the contents of the selected resources
 */
public class DisconnectAction extends TeamAction {

	@Override
	protected void execute(IAction action) {
		IProject projects[] = getSelectedProjects();
		try {
			for (IProject project : projects) {
				RepositoryProvider.unmap(project);
			}
		} catch (TeamException e) {
			ErrorDialog.openError(getShell(), Policy.bind("DisconnectAction.errorTitle"), null, e.getStatus()); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
