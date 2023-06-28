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

import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointSetOrganizer;
import org.eclipse.debug.internal.ui.views.breakpoints.WorkingSetCategory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkingSet;

/**
 * Toggles the default breakpoint group based on selection.
 */
public class ToggleDefaultGroupAction extends AbstractBreakpointsViewAction {

	private IWorkingSet fSelectedSet;

	@Override
	public void run(IAction action) {
		IWorkingSet defaultWorkingSet = BreakpointSetOrganizer.getDefaultWorkingSet();
		IWorkingSet set = null;
		if (!fSelectedSet.equals(defaultWorkingSet)) {
			set = fSelectedSet;
		}
		BreakpointSetOrganizer.setDefaultWorkingSet(set);
	}

	@Override
	public void selectionChanged(IAction action, ISelection sel) {
		fSelectedSet = null;
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			if (selection.size() == 1) {
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof IBreakpointContainer) {
					IBreakpointContainer container = (IBreakpointContainer) firstElement;
					if (container.getCategory() instanceof WorkingSetCategory) {
						WorkingSetCategory category = (WorkingSetCategory)container.getCategory();
						if (IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(category.getWorkingSet().getId())) {
							IWorkingSet set = category.getWorkingSet();
							action.setEnabled(true);
							boolean isDefault = set == BreakpointSetOrganizer.getDefaultWorkingSet();
							action.setChecked(isDefault);
							fSelectedSet = set;
							return;
						}
					}
				}
			}
		}
		action.setEnabled(false);
		action.setChecked(false);
	}
}
