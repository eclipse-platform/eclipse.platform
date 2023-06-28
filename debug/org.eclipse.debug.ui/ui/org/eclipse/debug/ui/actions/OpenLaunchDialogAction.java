/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui.actions;


import java.text.MessageFormat;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 * Opens the launch configuration dialog in the context of a launch group.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenLaunchDialogAction extends Action implements IActionDelegate2, IWorkbenchWindowActionDelegate {

	/**
	 * Launch group identifier
	 */
	private String fIdentifier;

	/**
	 * Constructs an action that opens the launch configuration dialog in
	 * the context of the specified launch group.
	 *
	 * @param identifier unique identifier of a launch group extension
	 */
	public OpenLaunchDialogAction(String identifier) {
		fIdentifier = identifier;
		ILaunchGroup group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(fIdentifier);
		if(group != null) {
			String lbl = group.getLabel();
			String actionLabel = MessageFormat.format(ActionMessages.OpenLaunchDialogAction_1, new Object[] { lbl });
			setText(DebugUIPlugin.adjustDBCSAccelerator(actionLabel));
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.OPEN_LAUNCH_CONFIGURATION_ACTION);
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		LaunchHistory history = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchHistory(fIdentifier);
		ILaunchConfiguration configuration = history.getRecentLaunch();
		IStructuredSelection selection = null;
		if (configuration == null) {
			selection = new StructuredSelection();
		} else {
			selection = new StructuredSelection(configuration);
		}
		int result = DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(), selection, fIdentifier);
		notifyResult(result == Window.OK);
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run();
	}

	@Override
	public void run(IAction action) {
		run();
	}

	@Override
	public void dispose() {}

	@Override
	public void init(IAction action) {
		if(action != null) {
			action.setEnabled(existsConfigTypesForMode());
		}
	}

	/**
	 * Return whether there are any registered launch configuration types for
	 * the mode of this action.
	 *
	 * @return whether there are any registered launch configuration types for
	 * the mode of this action
	 */
	private boolean existsConfigTypesForMode() {
		ILaunchConfigurationType[] configTypes = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes();
		for (ILaunchConfigurationType configType : configTypes) {
			if (configType.supportsMode(getMode())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void init(IWorkbenchWindow window) {}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {}

	/**
	 * Returns the launch mode for this action.
	 *
	 * @return launch mode
	 */
	private String getMode() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(fIdentifier).getMode();
	}
}
