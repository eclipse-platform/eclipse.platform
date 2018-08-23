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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class UneditAction extends WorkspaceAction {

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		
		if(! MessageDialog.openConfirm(getShell(), CVSUIMessages.Uneditaction_confirmTitle, CVSUIMessages.Uneditaction_confirmMessage)) { // 
			return;
		}
		
		run(new WorkspaceModifyOperation(null) {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				executeProviderAction(new IProviderAction() {
					public IStatus execute(CVSTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws CVSException {
						provider.unedit(resources, false /* recurse */, true /* notify server */, monitor);
						return Team.OK_STATUS;
					}
				}, monitor);
			}
		}, true /* cancelable */, PROGRESS_DIALOG);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForNonExistantResources()
	 */
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForCVSResource(org.eclipse.team.internal.ccvs.core.ICVSResource)
	 */
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		if (cvsResource.isFolder()) return false;
		if (super.isEnabledForCVSResource(cvsResource)) {
			return !((ICVSFile)cvsResource).isReadOnly() && ((ICVSFile)cvsResource).isEdited();
		} else {
			return false;
		}
	}


}
