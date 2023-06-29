/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.views.breakpoints.WorkingSetCategory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;

/**
 * An action to edit a breakpoint working set.
 */
public class EditBreakpointGroupAction extends AbstractBreakpointsViewAction {

	/**
	 * The currently selected breakpoints
	 */
	private IWorkingSet fSet = null;

	@Override
	public void run(IAction action) {
		IWorkingSetEditWizard editWizard = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetEditWizard(fSet);
		WizardDialog dialog = new WizardDialog(DebugUIPlugin.getShell(), editWizard);
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection sel) {
		fSet = null;
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection= (IStructuredSelection) sel;

			if (selection.size() == 1) {
				Object element = selection.getFirstElement();
				if (element instanceof IBreakpointContainer) {
					IBreakpointContainer container = (IBreakpointContainer)element;
					IAdaptable category = container.getCategory();
					if (category instanceof WorkingSetCategory) {
						IWorkingSet set = ((WorkingSetCategory)category).getWorkingSet();
						action.setEnabled(true);
						fSet = set;
						return;
					}
				}
			}
		}
		action.setEnabled(false);
	}

}
