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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.wizards.MergeWizard;

public class MergeAction extends WorkspaceTraversalAction {

	@Override
	public void execute(IAction action) {
		final Shell shell = getShell();
		shell.getDisplay().syncExec(() -> {
			MergeWizard wizard = new MergeWizard(getTargetPart(), getSelectedResources(),
					getSelectedResourceMappings(CVSProviderPlugin.getTypeId()));
			WizardDialog dialog = new WizardDialog(shell, wizard);
			dialog.open();
		});
	}
	
	@Override
	public String getId() {
		return ICVSUIConstants.CMD_MERGE;
	}
}
