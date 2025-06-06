/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;

/**
 * Status handler called when the launch dialog is opened via the "Run Ant..." action. This allows us to set the initial tab.
 */
public class RunAntStatusHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) {
		ILaunchConfigurationDialog dialog = (ILaunchConfigurationDialog) source;
		dialog.setActiveTab(3);
		return null;
	}

}
