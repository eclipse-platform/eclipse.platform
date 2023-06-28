/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpointSortBy;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointUIConstants;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

/**
 * An action which sets the breakpoint sorting order on a breakpoint view,
 * effectively telling the view to sort breakpoints according to some sorting
 * order
 */
public class SortBreakpointsAction extends Action {

	private BreakpointsView fView;
	int actionSort;

	/**
	 * Creates a new action which will sort breakpoints in the given breakpoint
	 * view using the given breakpoint container factory
	 *
	 * @param factory the factory that will be applied to the given view when
	 *            this action is run
	 * @param view the breakpoints view
	 */
	public SortBreakpointsAction(BreakpointsView view, String name, int sortingBy) {
		super(name, IAction.AS_RADIO_BUTTON);
		if (sortingBy == DebugUIPlugin.getDefault().getPreferenceStore().getInt(IInternalDebugUIConstants.PREF_BREAKPOINT_SORTING_ORDER)) {
			this.setChecked(true);
		}
		actionSort = sortingBy;
		fView = view;
	}

	@Override
	public void run() {
		if (isChecked()) {
			DebugUIPlugin.getDefault().getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_BREAKPOINT_SORTING_ORDER, actionSort);
		} else {
			DebugUIPlugin.getDefault().getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_BREAKPOINT_SORTING_ORDER, actionSort);
		}
		// update the presentation context element comparator sorting order
		fView.getTreeModelViewer().getPresentationContext().setProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_ELEMENT_COMPARATOR_SORT, Integer.valueOf(actionSort));
	}

}

