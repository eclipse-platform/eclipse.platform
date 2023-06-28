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
 *     dakshinamurthy.karra@gmail.com - bug 165371
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.program.launchConfigurations;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsBuildTab;
import org.eclipse.ui.externaltools.internal.model.IExternalToolsHelpContextIds;

public class ProgramTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		RefreshTab refresh = new RefreshTab();
		refresh.setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_REFRESH_TAB);
		EnvironmentTab env = new EnvironmentTab();
		env.setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_ENVIRONMENT_TAB);
		CommonTab common = new CommonTab();
		common.setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_COMMON_TAB);
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new ProgramMainTab(),
			refresh,
			new ExternalToolsBuildTab(),
			env,
			common
		};
		setTabs(tabs);
	}
}
