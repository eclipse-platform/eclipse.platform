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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.PlatformUI;

public class EditAction extends WorkspaceAction {

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		// Get the editors
		final EditorsAction editors = new EditorsAction();
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				executeProviderAction(editors, Policy.subMonitorFor(monitor, 25));
				
				// If there are editors show them and prompt the user to execute the edit command
				if (!editors.promptToEdit(getShell())) {
					return;
				}
				
				executeProviderAction(new IProviderAction() {
					public IStatus execute(CVSTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws CVSException {
						provider.edit(resources, false /* recurse */, true /* notify server */, false /* notifyForWritable*/, ICVSFile.NO_NOTIFICATION, monitor);
						return Team.OK_STATUS;
					}
				}, Policy.subMonitorFor(monitor, 75));
			}
		});
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForCVSResource(org.eclipse.team.internal.ccvs.core.ICVSResource)
	 */
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		if (cvsResource.isFolder()) return false;
		if (super.isEnabledForCVSResource(cvsResource)) {
			return ((ICVSFile)cvsResource).isReadOnly();
		} else {
			return false;
		}
	}

}
