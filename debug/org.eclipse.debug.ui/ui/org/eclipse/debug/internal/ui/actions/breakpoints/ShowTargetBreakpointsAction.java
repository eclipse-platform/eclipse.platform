/*****************************************************************
 * Copyright (c) 2009, 2010 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *****************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements the show target breakpoint action.
 *
 * @since 3.6
 */
public class ShowTargetBreakpointsAction extends Action {
	/**
	 * Breakpoints view
	 */
	BreakpointsView fView;

	/**
	 * Constructor.
	 *
	 * @param view the breakpoints view
	 */
	public ShowTargetBreakpointsAction(BreakpointsView view) {
		super();

		fView = view;

		setText(ActionMessages.ShowSupportedBreakpointsAction_Show_For_Selected);
		setToolTipText(ActionMessages.ShowSupportedBreakpointsAction_tooltip);

		setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET));
		setChecked(false);
		setId(DebugUIPlugin.getUniqueIdentifier() + ".ShowSupportedBreakpointsAction"); //$NON-NLS-1$

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.SHOW_BREAKPOINTS_FOR_MODEL_ACTION);
	}

	@Override
	public void run() {
		if (fView.getViewer().getControl().isDisposed()) {
			return;
		}
		fView.setFilterSelection(isChecked());
	}
}
