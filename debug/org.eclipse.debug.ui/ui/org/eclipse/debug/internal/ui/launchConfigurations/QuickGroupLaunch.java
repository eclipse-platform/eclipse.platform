/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.groups.GroupLaunchConfigurationDelegate;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement.GroupElementPostLaunchAction;
import org.eclipse.debug.internal.core.groups.GroupMemberChangeListener;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

/**
 * Quickly create a group launch configuration with launch configuration
 * selections
 *
 */
public class QuickGroupLaunch extends AbstractLaunchConfigurationAction {

	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_QUICK_GROUP_LAUNCH_ACTION = DebugUIPlugin.getUniqueIdentifier()
			+ ".ID_QUICK_GROUP_LAUNCH_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to apply a prototype to a launch configuration
	 *
	 * @param viewer the viewer
	 * @param mode the mode the action applies to
	 */
	public QuickGroupLaunch(Viewer viewer, String mode) {
		super(LaunchConfigurationsMessages.QuickGroupLaunchActionLabel, viewer, mode);
	}

	/**
	 * @see AbstractLaunchConfigurationAction#performAction()
	 */
	@Override
	protected void performAction() {
		String mode = getMode();
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

		try {
			ILaunchConfigurationType groupType = manager
					.getLaunchConfigurationType(GroupMemberChangeListener.GROUP_TYPE_ID);

			ILaunchConfigurationWorkingCopy wc = groupType.newInstance(
					null,
					manager.generateLaunchConfigurationName(
							LaunchConfigurationsMessages.CreateLaunchConfigurationAction_New_configuration_2));

			List<GroupLaunchElement> input = new ArrayList<>();

			IStructuredSelection selection = getStructuredSelection();

			for (Object selected : selection) {
				if (selected instanceof ILaunchConfiguration launchConfig) {
					GroupLaunchElement element = new GroupLaunchElement();
					element.index = input.size();
					element.enabled = true;
					element.name = launchConfig.getName();
					element.data = launchConfig;
					element.mode = mode;
					element.action = GroupElementPostLaunchAction.NONE;
					element.adoptIfRunning = false;
					element.actionParam = GroupElementPostLaunchAction.NONE;

					input.add(element);
				}
			}

			GroupLaunchConfigurationDelegate.storeLaunchElements(wc, input);

			ILaunchConfiguration config = wc.doSave();

			String launchGroupId = ILaunchManager.DEBUG_MODE.equals(mode) ? IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP
					: IDebugUIConstants.ID_RUN_LAUNCH_GROUP;

			DebugUITools.openLaunchConfigurationDialogOnGroup(Display.getDefault().getActiveShell(),
					new StructuredSelection(config), launchGroupId);

		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return selection.size() > 1;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_OBJS_LAUNCH_GROUP);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.QuickGroupLaunchActionToolTip;
	}
}
