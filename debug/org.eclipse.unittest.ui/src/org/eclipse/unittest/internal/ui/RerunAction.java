/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.eclipse.osgi.util.NLS;
import org.eclipse.unittest.internal.UnitTestPlugin;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Requests to rerun a test.
 */
public class RerunAction extends Action {

	private ILaunchConfiguration fLaunchConfiguration;
	private String fLaunchMode;

	/**
	 * Constructs a rerun action
	 *
	 * @param launchConfiguration a launch configuration object
	 * @param launchMode          a launch mode
	 */
	public RerunAction(ILaunchConfiguration launchConfiguration, String launchMode) {
		super(NLS.bind(Messages.RerunAction_label_rerun,
				DebugPlugin.getDefault().getLaunchManager().getLaunchMode(launchMode).getLabel()));
		fLaunchConfiguration = launchConfiguration;
		fLaunchMode = launchMode;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IUnitTestHelpContextIds.RERUN_ACTION);
	}

	@Override
	public void run() {
		try {
			DebugPlugin.getDefault().getLaunchManager().addLaunch(fLaunchConfiguration.launch(fLaunchMode, null));
		} catch (CoreException e) {
			UnitTestPlugin.log(e);
		}
	}
}
