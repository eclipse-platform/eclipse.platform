/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;

public class ShowResourceInHistoryAction extends WorkspaceAction {

	@Override
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
		run((IRunnableWithProgress) monitor -> {
			IResource[] resources = getSelectedResources();
			if (resources.length != 1)
				return;
			TeamUI.showHistoryFor(TeamUIPlugin.getActivePage(), resources[0], null);
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}

	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.ShowHistoryAction_showHistory; 
	}

	@Override
	protected boolean isEnabledForMultipleResources() {
		return false;
	}

	@Override
	protected boolean isEnabledForAddedResources() {
		return true;
	}

	@Override
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	@Override
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		return (!cvsResource.isFolder() && super.isEnabledForCVSResource(cvsResource));
	}

	@Override
	public String getId() {
		return ICVSUIConstants.CMD_HISTORY;
	}	
	
	@Override
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}
	
	@Override
	protected boolean isEnabledForIgnoredResources() {
		return true;
	}
}
