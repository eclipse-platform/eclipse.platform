/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.team.internal.ui.*;
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