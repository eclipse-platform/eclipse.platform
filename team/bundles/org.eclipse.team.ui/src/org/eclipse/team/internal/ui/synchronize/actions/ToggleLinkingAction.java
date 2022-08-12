/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.ui.IWorkbenchCommandConstants;

public class ToggleLinkingAction extends Action {

	private SynchronizeView view;

	public ToggleLinkingAction(SynchronizeView view) {
		super(TeamUIMessages.SynchronizeView_linkWithEditor);
		setDescription(TeamUIMessages.SynchronizeView_linkWithEditorDescription);
		setToolTipText(TeamUIMessages.SynchronizeView_linkWithEditorTooltip);
		setImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_LINK_WITH));
		setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_LINK_WITH_DISABLED));
		setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR);
		this.view = view;
		setChecked(view.isLinkingEnabled());
	}

	@Override
	public void run() {
		view.setLinkingEnabled(isChecked());
	}
}