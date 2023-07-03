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
 *******************************************************************************/
package org.eclipse.debug.ui.actions;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

/**
 * Launches a launch configuration in a specific mode.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class LaunchAction extends Action {

	/**
	 * The configuration to launch.
	 */
	private ILaunchConfiguration fConfiguration;
	/**
	 * The mode to launch in
	 */
	private String fMode;

	/**
	 * Constructs an action that launches the specified launch configuration
	 * in the specified mode.
	 *
	 * @param configuration launch configuration
	 * @param mode launch mode - one of <code>ILaunchManager.RUN_MODE</code> or
	 * <code>ILaunchManager.DEBUG_MODE</code>
	 */
	public LaunchAction(ILaunchConfiguration configuration, String mode) {
		fConfiguration = configuration;
		fMode = mode;
		setText(configuration.getName());
		setImageDescriptor(DebugUITools.getDefaultImageDescriptor(configuration));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			this,
			IDebugHelpContextIds.RELAUNCH_HISTORY_ACTION);
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		runInternal(false);
	}

	private void runInternal(boolean isShift) {
		DebugUITools.launch(fConfiguration, fMode, isShift);
	}

	/**
	 * If the user has control-clicked the launch history item, open the launch
	 * configuration dialog on the launch configuration, rather than running it.
	 *
	 * @see org.eclipse.jface.action.IAction#runWithEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void runWithEvent(Event event) {
		if ((event.stateMask & SWT.MOD1) > 0 && (event.stateMask & SWT.MOD2) > 0){
			ILaunchGroup[] groups = getAllGroupsForConfiguration(fConfiguration);
			if(groups.length > 0) {
				//prompt based on pref
				IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
				if(store.getBoolean(IInternalDebugUIConstants.PREF_REMOVE_FROM_LAUNCH_HISTORY)) {
					MessageDialogWithToggle mdwt = MessageDialogWithToggle.openYesNoQuestion(DebugUIPlugin.getShell(),
							ActionMessages.LaunchAction_0,
 MessageFormat.format(ActionMessages.LaunchAction_1, new Object[] { fConfiguration.getName() }),
							ActionMessages.LaunchAction_2,
							false,
							null,
							null);
					int ret = mdwt.getReturnCode();
					if(ret == IDialogConstants.YES_ID) {
						removeFromLaunchHistories(fConfiguration, groups);
						store.setValue(IInternalDebugUIConstants.PREF_REMOVE_FROM_LAUNCH_HISTORY, !mdwt.getToggleState());
					}
				}
				else {
					removeFromLaunchHistories(fConfiguration, groups);
				}
			}
		}
		else if ((event.stateMask & SWT.MOD1) > 0) {
			ILaunchGroup group = DebugUITools.getLaunchGroup(fConfiguration, fMode);
			if(group != null) {
				DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(), new StructuredSelection(fConfiguration), group.getIdentifier());
			}
			else {
				runInternal(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
			}
		}
		else {
			runInternal(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
		}
	}

	/**
	 * Removes the specified <code>ILaunchConfiguration</code> from the launch histories associated
	 * with the specified listing of <code>ILaunchGroup</code>s.
	 * @param config the configuration to remove from the histories from the given launch groups
	 * @param groups the launch groups whose histories the given configuration should be removed from
	 *
	 * @since 3.4
	 */
	private void removeFromLaunchHistories(ILaunchConfiguration config, ILaunchGroup[] groups) {
		LaunchHistory history = null;
		for (ILaunchGroup group : groups) {
			history = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchHistory(group.getIdentifier());
			if (history != null) {
				history.removeFromHistory(fConfiguration);
			} else {
				DebugUIPlugin.logErrorMessage(MessageFormat.format("Unable to remove configuration [{0}] from launch history. The launch history for mode [{1}] does not exist.", new Object[]{config.getName(), group.getMode()})); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Collects all of the launch groups associated with the specified <code>ILaunchConfiguration</code>
	 * @param config the config to collect launch groups for
	 * @return the listing of associated <code>ILaunchGroup</code>s for the specified <code>ILaunchConfiguration</code>, or
	 * an empty listing, never <code>null</code>
	 * @since 3.4
	 */
	private ILaunchGroup[] getAllGroupsForConfiguration(ILaunchConfiguration config) {
		ArrayList<ILaunchGroup> list = new ArrayList<>();
		try {
			ILaunchConfigurationType type = config.getType();
			Set<Set<String>> modes = type.getSupportedModeCombinations();
			String mode = null;
			ILaunchGroup group = null;
			for (Set<String> modesets : modes) {
				if(modesets.size() == 1) {
					mode = (String) modesets.toArray()[0];
					group = DebugUITools.getLaunchGroup(config, mode);
					if(group != null && !list.contains(group)) {
						list.add(group);
					}
				}
			}
		}
		catch(CoreException ce) {}
		return list.toArray(new ILaunchGroup[list.size()]);
	}
}
