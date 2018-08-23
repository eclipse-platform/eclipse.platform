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
import org.eclipse.core.runtime.IProgressMonitor;
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
	
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final EditorsAction editorsAction = new EditorsAction();
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				executeProviderAction(editorsAction, monitor);
			}
		}, true /* cancelable */ , PROGRESS_DIALOG);
		EditorsInfo[] infos = editorsAction.getEditorsInfo();
		EditorsView view = (EditorsView)showView(EditorsView.VIEW_ID);
		if (view != null) {
			view.setInput(infos);
		}

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
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
		// We support one selected Resource,
		// because the editors command will
		// show the editors of all children too.
		return false;
	}

}
