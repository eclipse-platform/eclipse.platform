/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.operations.ReplaceOperation;

public class ReplaceWithRemoteAction extends WorkspaceTraversalAction {
	
	@Override
	public void execute(IAction action)  throws InvocationTargetException, InterruptedException {
		
		final ReplaceOperation replaceOperation = new ReplaceOperation(getTargetPart(), getCVSResourceMappings(), resourceCommonTag);
		if (hasOutgoingChanges(replaceOperation)) {
			final boolean[] keepGoing = new boolean[] { true };
			Display.getDefault().syncExec(() -> {
				OutgoingChangesDialog dialog = new OutgoingChangesDialog(getShell(), replaceOperation.getScopeManager(),
						CVSUIMessages.ReplaceWithTagAction_2, CVSUIMessages.ReplaceWithTagAction_0,
						CVSUIMessages.ReplaceWithTagAction_1);
				dialog.setHelpContextId(IHelpContextIds.REPLACE_OUTGOING_CHANGES_DIALOG);
				int result = dialog.open();
				keepGoing[0] = result == Window.OK;
			});
			if (!keepGoing[0])
				return;
		}
		replaceOperation.run();
	}
	
	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.ReplaceWithRemoteAction_problemMessage; 
	}

	@Override
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	@Override
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}

	/* 
	 * Update the text label for the action based on the tags in the
	 * selection.
	 * 
	 * @see TeamAction#setActionEnablement(org.eclipse.jface.action.IAction)
	 */
	@Override
	protected void setActionEnablement(IAction action) {
		super.setActionEnablement(action);
		
		action.setText(calculateActionTagValue());
	}
}
