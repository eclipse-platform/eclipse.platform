/**********************************************************************
 * Copyright (c) 2004, 2012 QNX Software Systems and others.
 *
 *   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * Wind River Systems - Pawel Piech - Added Modules view (bug 211158)
 * IBM - Added the abiliity to update view label, context help and the orientation
 *       action upon input change
***********************************************************************/
package org.eclipse.debug.internal.ui.views.modules;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.variables.ToggleDetailPaneAction;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.internal.ui.views.variables.details.AvailableDetailPanesAction;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

/**
 * Displays modules and symbols with a detail area.
 */
public class ModulesView extends VariablesView {

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.MODULES_VIEW;
	}

	@Override
	protected void configureToolBar(IToolBarManager tbm) {
		tbm.add(new Separator(this.getClass().getName()));
		tbm.add(new Separator(IDebugUIConstants.RENDER_GROUP));
		tbm.add(getAction("CollapseAll")); //$NON-NLS-1$
		tbm.add( new Separator( IDebugUIConstants.MODULES_GROUP ) );
	}

	@Override
	protected void fillContextMenu( IMenuManager menu ) {
		menu.add( new Separator( IDebugUIConstants.EMPTY_MODULES_GROUP ) );
		menu.add( new Separator( IDebugUIConstants.MODULES_GROUP ) );
		menu.add(getAction(FIND_ACTION));
		menu.add(new Separator());
		IAction action = new AvailableDetailPanesAction(this);
		if (isDetailPaneVisible() && action.isEnabled()) {
			menu.add(action);
		}
		menu.add(new Separator(IDebugUIConstants.EMPTY_RENDER_GROUP));
		menu.add(new Separator(IDebugUIConstants.EMPTY_NAVIGATION_GROUP));
		menu.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
	}

	@Override
	protected String getDetailPanePreferenceKey() {
		return IDebugPreferenceConstants.MODULES_DETAIL_PANE_ORIENTATION;
	}

	@Override
	protected String getToggleActionLabel() {

		if (getViewer() != null)
		{
			Object viewerInput = getViewer().getInput();
			if (viewerInput != null)
			{
				String name = getViewName(viewerInput);
				if (name != null)
				{
					String label = NLS.bind(ModulesViewMessages.ModulesView_1, name);
					return label;
				}
			}
		}

		return ModulesViewMessages.ModulesView_0;
	}

	@Override
	protected String getPresentationContextId() {
		return IDebugUIConstants.ID_MODULE_VIEW;
	}

	@Override
	protected void setViewerInput(Object context) {
		super.setViewerInput(context);

		// update view label when viewer input is changed
		updateViewLabels(context);

		// update orientation action based on input
		updateOrientationAction(context);

		// update context help hook when viewer input is changed
		updateContextHelp(context);
	}

	private void updateContextHelp(Object context) {
		if (context instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) context;
			if (adaptable.getAdapter(IHelpContextIdProvider.class) != null) {
				IHelpContextIdProvider provider = adaptable
						.getAdapter(IHelpContextIdProvider.class);
				String helpId = provider.getHelpContextId(IDebugHelpContextIds.MODULES_VIEW);
				if (helpId != null) {
					PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl().getParent(), helpId);
					return;
				}
			}
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl().getParent(), IDebugHelpContextIds.MODULES_VIEW);
	}

	private void updateViewLabels(Object context)
	{
		String viewName = getViewName(context);

		// only update label if the name has changed
		if (!getPartName().equals(viewName)) {
			setPartName(viewName);
		}

		// only update image if the image has changed
		Image image = getViewImage(context);
		if (!getTitleImage().equals(image)) {
			setTitleImage(image);
		}
	}

	/**
	 * @param context
	 */
	private String getViewName(Object context) {
		if (context instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) context;
			if (adaptable.getAdapter(ILabelProvider.class) != null) {
				ILabelProvider provider = adaptable
						.getAdapter(ILabelProvider.class);
				String label = provider.getText(this);
				if (label != null) {
					return label;
				}
			}
		}
		return ModulesViewMessages.ModulesView_2;
	}

	private Image getViewImage(Object context)
	{
		if (context instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) context;
			if (adaptable.getAdapter(ILabelProvider.class) != null) {
				ILabelProvider provider = adaptable
						.getAdapter(ILabelProvider.class);
				Image image = provider.getImage(this);
				if (image != null) {
					return image;
				}
			}
		}
		return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_CVIEW_MODULES_VIEW);
	}

	private void updateOrientationAction(Object context)
	{
		ToggleDetailPaneAction action = getToggleDetailPaneAction(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_HIDDEN);
		if (action != null)
		{
			String label = NLS.bind(ModulesViewMessages.ModulesView_1, getViewName(context));
			action.setText(label);
		}
	}

}
