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
package org.eclipse.ant.internal.ui.views.actions;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.views.AntView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

public class RemoveAllAction extends Action implements IUpdate {
	private final AntView view;

	public RemoveAllAction(AntView view) {
		super(AntViewActionMessages.RemoveAllAction_Remove_All, AntUIImages.getImageDescriptor(IAntUIConstants.IMG_REMOVE_ALL));
		setDescription(AntViewActionMessages.RemoveAllAction_Remove_All);
		setToolTipText(AntViewActionMessages.RemoveAllAction_Remove_All);
		this.view = view;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAntUIHelpContextIds.REMOVE_ALL_ACTION);
	}

	@Override
	public void run() {
		boolean proceed = MessageDialog.openQuestion(view.getViewSite().getShell(), AntViewActionMessages.RemoveAllAction_0, AntViewActionMessages.RemoveAllAction_1);
		if (proceed) {
			view.removeAllProjects();
		}
	}

	@Override
	public void update() {
		setEnabled(view.getViewer().getTree().getItemCount() != 0);
	}
}
