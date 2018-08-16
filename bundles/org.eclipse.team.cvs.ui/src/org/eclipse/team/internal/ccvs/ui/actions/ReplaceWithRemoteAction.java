/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.operations.ReplaceOperation;

public class ReplaceWithRemoteAction extends WorkspaceTraversalAction {
    
	public void execute(IAction action)  throws InvocationTargetException, InterruptedException {
		
		final ReplaceOperation replaceOperation = new ReplaceOperation(getTargetPart(), getCVSResourceMappings(), resourceCommonTag);
		if (hasOutgoingChanges(replaceOperation)) {
			final boolean[] keepGoing = new boolean[] { true };
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					OutgoingChangesDialog dialog = new OutgoingChangesDialog(getShell(), replaceOperation.getScopeManager(), 
							CVSUIMessages.ReplaceWithTagAction_2, 
							CVSUIMessages.ReplaceWithTagAction_0, 
							CVSUIMessages.ReplaceWithTagAction_1);
					dialog.setHelpContextId(IHelpContextIds.REPLACE_OUTGOING_CHANGES_DIALOG);
					int result = dialog.open();
					keepGoing[0] = result == Window.OK;
				}
			});
			if (!keepGoing[0])
				return;
		}
		replaceOperation.run();
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return CVSUIMessages.ReplaceWithRemoteAction_problemMessage; 
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

	/* 
	 * Update the text label for the action based on the tags in the
	 * selection.
	 * 
	 * @see TeamAction#setActionEnablement(org.eclipse.jface.action.IAction)
	 */
	protected void setActionEnablement(IAction action) {
		super.setActionEnablement(action);
		
		action.setText(calculateActionTagValue());
	}
}
