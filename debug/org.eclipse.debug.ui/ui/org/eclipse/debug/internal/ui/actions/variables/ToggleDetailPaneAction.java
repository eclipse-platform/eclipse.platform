/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.variables;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * Action that controls the appearance of the details pane in debug views such
 * as the VariablesView and the ExpressionsView.  Instances of this class can be
 * created to show the detail pane underneath the main tree, to the right of the
 * main tree, or not shown at all.
 *
 * @since 3.0
 */
public class ToggleDetailPaneAction extends Action {

	private VariablesView fVariablesView;

	private String fOrientation;

	public ToggleDetailPaneAction(VariablesView view, String orientation, String hiddenLabel) {
		super(IInternalDebugCoreConstants.EMPTY_STRING, AS_RADIO_BUTTON);
		setVariablesView(view);
		setOrientation(orientation);

		if (orientation == IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH) {
			setText(ActionMessages.ToggleDetailPaneAction_1);
			setToolTipText(ActionMessages.ToggleDetailPaneAction_2);
			setDescription(ActionMessages.ToggleDetailPaneAction_2);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_UNDER));
			setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_UNDER));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DETAIL_PANE_UNDER));
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.VERTICAL_DETAIL_PANE_LAYOUT_ACTION);
		} else if (orientation == IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_RIGHT) {
			setText(ActionMessages.ToggleDetailPaneAction_4);
			setToolTipText(ActionMessages.ToggleDetailPaneAction_5);
			setDescription(ActionMessages.ToggleDetailPaneAction_5);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_RIGHT));
			setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_RIGHT));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DETAIL_PANE_RIGHT));
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.HORIZONTAL_DETAIL_PANE_LAYOUT_ACTION);
		} else if (orientation == IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_AUTO) {
			setText(ActionMessages.ToggleDetailPaneAction_0);
			setToolTipText(ActionMessages.ToggleDetailPaneAction_3);
			setDescription(ActionMessages.ToggleDetailPaneAction_3);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_AUTO));
			setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_AUTO));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_AUTO));
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.HORIZONTAL_DETAIL_PANE_LAYOUT_ACTION);
		} else {
			setText(hiddenLabel);
			setToolTipText(ActionMessages.ToggleDetailPaneAction_8);
			setDescription(ActionMessages.ToggleDetailPaneAction_8);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_HIDE));
			setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_HIDE));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DETAIL_PANE_HIDE));
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.DETAIL_PANE_HIDDEN_LAYOUT_ACTION);
		}
	}

	@Override
	public void run() {
		getVariablesView().setDetailPaneOrientation(getOrientation());
	}

	private VariablesView getVariablesView() {
		return fVariablesView;
	}

	private void setVariablesView(VariablesView variablesView) {
		fVariablesView = variablesView;
	}

	private void setOrientation(String orientation) {
		fOrientation = orientation;
	}

	public String getOrientation() {
		return fOrientation;
	}
}

