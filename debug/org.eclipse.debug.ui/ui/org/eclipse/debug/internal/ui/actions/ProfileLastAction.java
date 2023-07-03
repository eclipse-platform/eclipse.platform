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
package org.eclipse.debug.internal.ui.actions;


import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.actions.RelaunchLastAction;

/**
 * Re-launches the last profile-mode launch
 *
 * This menu item appears in the main 'Run' menu
 *
 * @see RelaunchLastAction
 * @see RunLastAction
 * @see DebugLastAction
 *
 */
public class ProfileLastAction extends RelaunchLastAction {

	@Override
	public String getMode() {
		return ILaunchManager.PROFILE_MODE;
	}

	@Override
	public String getLaunchGroupId() {
		return IDebugUIConstants.ID_PROFILE_LAUNCH_GROUP;
	}

	@Override
	protected String getText() {
		if(LaunchingResourceManager.isContextLaunchEnabled()) {
			return ActionMessages.ProfileLastAction_1;
		}
		else {
			return ActionMessages.ProfileLastAction_0;
		}
	}

	@Override
	protected String getTooltipText() {
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}

	@Override
	protected String getCommandId() {
		return "org.eclipse.debug.ui.commands.ProfileLast"; //$NON-NLS-1$
	}

	@Override
	protected String getDescription() {
		if(LaunchingResourceManager.isContextLaunchEnabled()) {
			return ActionMessages.ProfileLastAction_2;
		}
		else {
			return ActionMessages.ProfileLastAction_3;
		}
	}
}
