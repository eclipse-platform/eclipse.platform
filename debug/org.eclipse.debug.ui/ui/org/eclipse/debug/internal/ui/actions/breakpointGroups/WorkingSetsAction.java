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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

/**
 * Allows the user to manage working sets.
 */
public class WorkingSetsAction extends AbstractBreakpointsViewAction {

	@Override
	public void run(IAction action) {
		IWorkingSetSelectionDialog selectionDialog = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(
				DebugUIPlugin.getShell(),
				false,
				new String[] {IDebugUIConstants.BREAKPOINT_WORKINGSET_ID});
		selectionDialog.open();
	}
}
