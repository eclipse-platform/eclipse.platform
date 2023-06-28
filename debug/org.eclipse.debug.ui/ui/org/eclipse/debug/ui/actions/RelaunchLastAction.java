/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Wind River Systems - bug 227877
 *******************************************************************************/
package org.eclipse.debug.ui.actions;


import java.text.MessageFormat;

import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.DebugLastAction;
import org.eclipse.debug.internal.ui.actions.ProfileLastAction;
import org.eclipse.debug.internal.ui.actions.RunLastAction;
import org.eclipse.debug.internal.ui.contextlaunching.ContextRunner;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Re-launches the last launch.
 *
 * @see ContextRunner
 * @see ILaunchConfiguration
 * @see RunLastAction
 * @see DebugLastAction
 * @see ProfileLastAction
 * @since 3.8
 */
public abstract class RelaunchLastAction implements IWorkbenchWindowActionDelegate, IActionDelegate2 {

	private class Listener implements IPreferenceChangeListener {
		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			if(event.getKey().equals(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH)) {
				initialize(fAction);
			}
		}
	}

	private Listener fListener = new Listener();
	private IWorkbenchWindow fWorkbenchWindow;
	private IAction fAction;

	@Override
	public void dispose(){
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
		if(prefs != null) {
			prefs.removePreferenceChangeListener(fListener);
		}
	}

	@Override
	public void init(IWorkbenchWindow window){
		fWorkbenchWindow = window;
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
		if(prefs != null) {
			prefs.addPreferenceChangeListener(fListener);
		}
	}

	/**
	 * @since 3.12
	 */
	@Override
	public void init(IAction action) {
		initialize(action);
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
		if (prefs != null) {
			prefs.addPreferenceChangeListener(fListener);
		}
	}

	@Override
	public void run(IAction action) {
		runInternal(false);
	}

	/**
	 * @since 3.12
	 */
	@Override
	public void runWithEvent(IAction action, Event event) {
		runInternal(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
	}

	private void runInternal(boolean isShift) {
		if(LaunchingResourceManager.isContextLaunchEnabled()) {
			ILaunchGroup group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(getLaunchGroupId());
			ContextRunner.getDefault().launch(group, isShift);
			return;
		}
		try {
			final ILaunchConfiguration configuration = getLastLaunch();
			if (configuration != null) {
				if (configuration.supportsMode(getMode())) {
					DebugUITools.launch(configuration, getMode(), isShift);
				} else {
					String configName = configuration.getName();
					String title = ActionMessages.RelaunchLastAction_Cannot_relaunch_1;
					String message = MessageFormat.format(ActionMessages.RelaunchLastAction_Cannot_relaunch___0___because_it_does_not_support__2__mode_2, new Object[] {
							configName, getMode() });
					MessageDialog.openError(getShell(), title, message);
				}
			} else {
				// If the history is empty, just open the launch config dialog
				openLaunchConfigurationDialog();
			}
		} catch (CoreException ce) {
			DebugUIPlugin.errorDialog(getShell(), ActionMessages.RelaunchLastAction_Error_relaunching_3, ActionMessages.RelaunchLastAction_Error_encountered_attempting_to_relaunch_4, ce); //
		}
	}

	/**
	 * Open the launch configuration dialog, passing in the current workbench selection.
	 */
	private void openLaunchConfigurationDialog() {
		IWorkbenchWindow dwindow= DebugUIPlugin.getActiveWorkbenchWindow();
		if (dwindow == null) {
			return;
		}
		LaunchConfigurationsDialog dialog = new LaunchConfigurationsDialog(DebugUIPlugin.getShell(), DebugUIPlugin.getDefault().getLaunchConfigurationManager().getDefaultLaunchGroup(getMode()));
		dialog.setOpenMode(LaunchConfigurationsDialog.LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED);
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection){
		if (fAction == null) {
			initialize(action);
		}
	}

	/**
	 * Set the enabled state of the underlying action based on whether there are any
	 * registered launch configuration types that understand how to launch in the
	 * mode of this action.
	 * @param action the {@link IAction} to initialize
	 */
	private void initialize(IAction action) {
		fAction = action;
		if(fAction != null) {
			fAction.setEnabled(existsConfigTypesForMode());
			fAction.setText(getText());
			fAction.setToolTipText(getTooltipText());
			String commandId = getCommandId();
			ICommandService service = PlatformUI.getWorkbench().getService(ICommandService.class);
			if (service != null) {
				Command command = service.getCommand(commandId);
				command.undefine();
				command = service.getCommand(commandId);
				command.define(DebugUIPlugin.removeAccelerators(getText()), getDescription(), service.getCategory("org.eclipse.debug.ui.category.run")); //$NON-NLS-1$
			}
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

	/**
	 * Return the last launch that occurred in the workspace.
	 * @return the filtered last launch
	 */
	protected ILaunchConfiguration getLastLaunch() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getFilteredLastLaunch(getLaunchGroupId());
	}

	/**
	 * Returns the parent shell for this menu item
	 *
	 * @return the parent shell
	 */
	protected Shell getShell() {
		return fWorkbenchWindow.getShell();
	}

	/**
	 * Returns the mode (run or debug) of this action.
	 * @return the mode
	 */
	protected abstract String getMode();

	/**
	 * Returns the launch group id of this action.
	 *
	 * @return  the launch group id
	 */
	protected abstract String getLaunchGroupId();

	/**
	 * Returns the text to display on the menu item.
	 *
	 * @return the text for the menu item
	 */
	protected abstract String getText();

	/**
	 * Returns the text to display in the menu item tooltip
	 *
	 * @return the text for the tooltip
	 */
	protected abstract String getTooltipText();

	/**
	 * Returns the command id this action is associated with.
	 *
	 * @return command id
	 */
	protected abstract String getCommandId();

	/**
	 * Returns a description for this action (to associate with command).
	 *
	 * @return command description
	 */
	protected abstract String getDescription();
}
