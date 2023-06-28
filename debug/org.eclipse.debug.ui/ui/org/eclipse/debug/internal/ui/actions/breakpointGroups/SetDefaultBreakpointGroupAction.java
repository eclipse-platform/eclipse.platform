/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointSetOrganizer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkingSet;

/**
 * Action which prompts the user to set a default breakpoint group.
 */
public class SetDefaultBreakpointGroupAction extends AbstractBreakpointsViewAction {

	@Override
	public void run(IAction action) {
		SelectBreakpointWorkingsetDialog sbwsd = new SelectBreakpointWorkingsetDialog(DebugUIPlugin.getShell());
		sbwsd.setTitle(BreakpointGroupMessages.SetDefaultBreakpointGroupAction_0);
		IWorkingSet workingSet = BreakpointSetOrganizer.getDefaultWorkingSet();
		if (workingSet != null){
			sbwsd.setInitialSelections(new Object[]{workingSet});
		}
		if(sbwsd.open() == Window.OK) {
			BreakpointSetOrganizer.setDefaultWorkingSet((IWorkingSet) sbwsd.getResult()[0]);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {}

}
