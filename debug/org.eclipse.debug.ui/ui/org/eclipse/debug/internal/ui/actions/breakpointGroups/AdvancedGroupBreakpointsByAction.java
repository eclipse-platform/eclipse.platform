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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;

/**
 * Action that opens a dialog to select which breakpoint
 * container factories should be applies to the breakpoints
 * view.
 */
public class AdvancedGroupBreakpointsByAction extends Action {

	private BreakpointsView fView;

	public AdvancedGroupBreakpointsByAction(BreakpointsView view) {
		super(IInternalDebugCoreConstants.EMPTY_STRING, IAction.AS_RADIO_BUTTON);
		fView= view;
	}

	@Override
	public void run() {
		if (isChecked()) {
			GroupBreakpointsByDialog dialog = new GroupBreakpointsByDialog(fView);
			if (dialog.open() == Window.OK) {
				fView.setBreakpointOrganizers(dialog.getOrganizers());
			}
		}
	}

}
