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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.ui.wizards.NewLocationWizard;
import org.eclipse.ui.*;

public class NewRepositoryAction implements IWorkbenchWindowActionDelegate, IViewActionDelegate {
	Shell shell;
	
	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.shell = window.getShell();
	}
	
	@Override
	public void init(IViewPart view) {
		shell = view.getSite().getShell();
	}

	@Override
	public void run(IAction action) {
		NewLocationWizard wizard = new NewLocationWizard();
		wizard.setSwitchPerspectives(false);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
