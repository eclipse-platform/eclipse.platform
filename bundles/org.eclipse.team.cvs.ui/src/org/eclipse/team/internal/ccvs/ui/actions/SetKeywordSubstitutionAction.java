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
package org.eclipse.team.internal.ccvs.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.wizards.ModeWizard;

/**
 * TagAction tags the selected resources with a version tag specified by the user.
 */
public class SetKeywordSubstitutionAction extends WorkspaceTraversalAction {

	@Override
	public void execute(IAction action) {
		ModeWizard.run(getShell(), getSelectedResources());
	}
	
	@Override
	public String getId() {
		return ICVSUIConstants.CMD_SETFILETYPE;
	}
}
