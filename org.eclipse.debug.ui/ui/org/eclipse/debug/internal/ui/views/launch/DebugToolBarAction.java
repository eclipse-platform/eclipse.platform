/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * Action that controls whether Debug actions are shown in Launch view.
 *
 * @since 3.8
 */
class DebugToolBarAction extends Action {

	private final LaunchView fLaunchView;

	/**
	 * Creates a new action to set the debug view mode.
	 *
	 * @param view Reference to the debug view.
	 */
	public DebugToolBarAction(LaunchView view) {
		super(IInternalDebugCoreConstants.EMPTY_STRING, AS_CHECK_BOX);
		fLaunchView = view;

		setText(LaunchViewMessages.DebugToolBarAction_View_label);
		setToolTipText(LaunchViewMessages.DebugToolBarAction_View_tooltip);
		setDescription(LaunchViewMessages.DebugToolBarAction_View_description);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.DEBUG_TOOLBAR_VIEW_ACTION);
	}

	public void setShowingDebugToolbar(boolean showingToolbar) {
		setChecked(showingToolbar);
	}

	@Override
	public void run() {
		fLaunchView.setDebugToolbarInView(isChecked());
	}
}

