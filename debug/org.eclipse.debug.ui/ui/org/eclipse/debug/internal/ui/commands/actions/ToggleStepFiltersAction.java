/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.commands.IStepFiltersHandler;
import org.eclipse.debug.internal.core.StepFilterManager;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * This class provides the action for toggling step filters on or off for the debug view
 */
public class ToggleStepFiltersAction extends DebugCommandAction implements IPreferenceChangeListener {

	private boolean fInitialized = !DebugUITools.isUseStepFilters();

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TOGGLE_STEP_FILTERS);
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.step_with_filters_action_context"; //$NON-NLS-1$
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TOGGLE_STEP_FILTERS);
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.actions.ToggleStepFilters"; //$NON-NLS-1$
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TOGGLE_STEP_FILTERS);
	}

	@Override
	public String getText() {
		return ActionMessages.ToggleStepFiltersAction_1;
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.ToggleStepFiltersAction_0;
	}

	@Override
	protected boolean getInitialEnablement() {
		return true;
	}

	@Override
	protected Class<IStepFiltersHandler> getCommandType() {
		return IStepFiltersHandler.class;
	}

	@Override
	public void run() {
		// ignore initial call to run from abstract debug view
		// that runs the action to initialize it's state when
		// the workbench persisted the action as "on"
		if (fInitialized) {
			DebugUITools.setUseStepFilters(!DebugUITools.isUseStepFilters());
		} else {
			fInitialized = true;
		}
	}

	@Override
	public int getStyle() {
		return AS_CHECK_BOX;
	}

	@Override
	public void debugContextChanged(DebugContextEvent event) {
		ISelection context = event.getContext();
		if (context.isEmpty()) {
			setEnabled(true);
		} else {
			super.debugContextChanged(event);
		}
	}

	@Override
	public void init(IWorkbenchPart part) {
		super.init(part);
		initState();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		super.init(window);
		initState();
	}

	/**
	 * Initializes the state, by adding this action as a property listener
	 */
	protected void initState() {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(DebugPlugin.getUniqueIdentifier());
		if (node != null) {
			node.addPreferenceChangeListener(this);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(DebugPlugin.getUniqueIdentifier());
		if (node != null) {
			node.removePreferenceChangeListener(this);
		}
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(StepFilterManager.PREF_USE_STEP_FILTERS)) {
			boolean checked = DebugUITools.isUseStepFilters();
			setChecked(checked);
			IAction action = getActionProxy();
			if (action != null) {
				action.setChecked(checked);
			}
		}
	}
}
