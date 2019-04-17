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
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.operations.ProjectMetaFileOperation;
import org.eclipse.team.internal.ccvs.ui.wizards.CheckoutAsWizard;

public class CheckoutAsAction extends CVSAction {
	
	@Override
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
		boolean withName = CVSUIPlugin.getPlugin().isUseProjectNameOnCheckout();
		ProjectMetaFileOperation op = new ProjectMetaFileOperation(getTargetPart(), folders, withName);
		op.run();
		
		// project configuration allowed only if single folder without metafile
		boolean allowProjectConfig = (folders.length == 1 && !op.metaFileExists());
		
		if (withName) {
			folders = op.getUpdatedFolders();
		}
		
		CheckoutAsWizard wizard = new CheckoutAsWizard(getTargetPart(), folders, allowProjectConfig);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.open();
	}
	
	@Override
	public boolean isEnabled() {
		return getSelectedRemoteFolders().length > 0;
	}
}
