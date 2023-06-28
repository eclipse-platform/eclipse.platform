/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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


import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.actions.ExecutionAction;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Action to launch the last launch configuration that was successfully
 * launched, in run mode. If no configurations have been launched, the launch
 * configuration dialog is opened.
 * <p>
 * This class may be instantiated.
 * </p>
 * @since 2.0
 */
public final class RunAction extends ExecutionAction {

	public RunAction() {
		super(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
	}

	/**
	 * @see ExecutionAction#getMode()
	 */
	@Override
	protected String getMode() {
		return ILaunchManager.RUN_MODE;
	}

}
