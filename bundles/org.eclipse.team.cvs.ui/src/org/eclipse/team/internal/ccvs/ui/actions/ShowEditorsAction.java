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
 *     CSC - Intial implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.internal.ccvs.core.EditorsInfo;
import org.eclipse.team.internal.ccvs.ui.EditorsView;
/**
 * 
 * 
 * Action for Show Editors in popup menus
 * 
 * @author <a href="mailto:kohlwes@gmx.net">Gregor Kohlwes</a>
 * 
 */
public class ShowEditorsAction extends WorkspaceAction {
	
	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final EditorsAction editorsAction = new EditorsAction();
		run((IRunnableWithProgress) monitor -> executeProviderAction(editorsAction, monitor), true /* cancelable */ ,
				PROGRESS_DIALOG);
		EditorsInfo[] infos = editorsAction.getEditorsInfo();
		EditorsView view = (EditorsView)showView(EditorsView.VIEW_ID);
		if (view != null) {
			view.setInput(infos);
		}

	}

	@Override
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	@Override
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	@Override
	protected boolean isEnabledForMultipleResources() {
		// We support one selected Resource,
		// because the editors command will
		// show the editors of all children too.
		return false;
	}

}
