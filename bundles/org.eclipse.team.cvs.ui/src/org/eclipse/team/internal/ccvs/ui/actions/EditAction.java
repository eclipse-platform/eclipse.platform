/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.PlatformUI;

public class EditAction extends WorkspaceAction {

	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		// Get the editors
		final EditorsAction editors = new EditorsAction();
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
			executeProviderAction(editors, Policy.subMonitorFor(monitor, 25));

			// If there are editors show them and prompt the user to execute the edit
			// command
			if (!editors.promptToEdit(getShell())) {
				return;
			}

			executeProviderAction((provider, resources, monitor1) -> {
				provider.edit(resources, false /* recurse */, true /* notify server */, false /* notifyForWritable */,
						ICVSFile.NO_NOTIFICATION, monitor1);
				return Team.OK_STATUS;
			}, Policy.subMonitorFor(monitor, 75));
		});
	}

	@Override
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	@Override
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		if (cvsResource.isFolder()) return false;
		if (super.isEnabledForCVSResource(cvsResource)) {
			return ((ICVSFile)cvsResource).isReadOnly();
		} else {
			return false;
		}
	}

}
