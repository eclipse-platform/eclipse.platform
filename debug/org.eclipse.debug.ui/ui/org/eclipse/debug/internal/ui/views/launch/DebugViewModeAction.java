/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Action that controls the view mode for Debug view (auto vs. breadcrumb, vs.
 * tree view).
 *
 * @since 3.5
 */
class DebugViewModeAction extends Action {

	private final LaunchView fLaunchView;
	private final Composite fParent;
	private final String fMode;

	/**
	 * Creates a new action to set the debug view mode.
	 *
	 * @param view Reference to the debug view.
	 * @param mode The mode to be set by this action.
	 * @param parent The view's parent control used to calculate view size
	 * in auto mode.
	 */
	public DebugViewModeAction(LaunchView view, String mode, Composite parent) {
		super(IInternalDebugCoreConstants.EMPTY_STRING, AS_RADIO_BUTTON);
		fLaunchView = view;
		fParent = parent;
		fMode = mode;

		if (mode == IDebugPreferenceConstants.DEBUG_VIEW_MODE_AUTO) {
			setText(LaunchViewMessages.DebugViewModeAction_Auto_label);
			setToolTipText(LaunchViewMessages.DebugViewModeAction_Auto_tooltip);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_AUTO));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_AUTO));
			setDescription(LaunchViewMessages.DebugViewModeAction_Auto_description);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.DEBUG_VIEW_MODE_AUTO_ACTION);
		} else if (mode == IDebugPreferenceConstants.DEBUG_VIEW_MODE_FULL) {
			setText(LaunchViewMessages.DebugViewModeAction_Full_label);
			setToolTipText(LaunchViewMessages.DebugViewModeAction_Full_tooltip);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_HIDE));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DETAIL_PANE_HIDE));
			setDescription(LaunchViewMessages.DebugViewModeAction_Full_description);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.DEBUG_VIEW_MODE_FULL_ACTION);
		} else {
			setText(LaunchViewMessages.DebugViewModeAction_Compact_label);
			setToolTipText(LaunchViewMessages.DebugViewModeAction_Compact_tooltip);
			setDescription(LaunchViewMessages.DebugViewModeAction_Compact_description);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DEBUG_VIEW_COMPACT_LAYOUT));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DEBUG_VIEW_COMPACT_LAYOUT));
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.DEBUG_VIEW_MODE_COMPACT_ACTION);
		}
	}

	@Override
	public void run() {
		fLaunchView.setViewMode(fMode, fParent);
	}

	/**
	 * Returns the view mode set by this action.
	 * @return the mode of the action
	 */
	public String getMode() {
		return fMode;
	}
}

