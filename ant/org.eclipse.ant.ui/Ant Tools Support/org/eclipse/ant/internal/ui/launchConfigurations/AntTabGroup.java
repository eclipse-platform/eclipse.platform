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
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsBuildTab;

public class AntTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	@SuppressWarnings("deprecation")
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			boolean captureOutput = configuration.getAttribute(IExternalToolConstants.ATTR_CAPTURE_OUTPUT, true);
			if (!captureOutput && configuration instanceof ILaunchConfigurationWorkingCopy copy) {
				copy.setAttribute(IExternalToolConstants.ATTR_CAPTURE_OUTPUT, (String) null);
				copy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false);
				copy.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
			}
		}
		catch (CoreException e) {
			// do nothing
		}
		super.initializeFrom(configuration);
	}

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		RefreshTab refresh = new RefreshTab();
		refresh.setHelpContextId(IAntUIHelpContextIds.ANT_REFRESH_TAB);
		ExternalToolsBuildTab build = new ExternalToolsBuildTab();
		build.setHelpContextId(IAntUIHelpContextIds.ANT_BUILD_TAB);
		AntClasspathTab classpath = new AntClasspathTab();
		classpath.setHelpContextId(IAntUIHelpContextIds.ANT_CLASSPATH_TAB);
		AntJRETab jre = new AntJRETab();
		jre.setHelpContextId(IAntUIHelpContextIds.ANT_JRE_TAB);
		AntEnvironmentTab env = new AntEnvironmentTab();
		env.setHelpContextId(IAntUIHelpContextIds.ANT_ENVIRONMENT_TAB);
		CommonTab common = new CommonTab();
		common.setHelpContextId(IAntUIHelpContextIds.ANT_COMMON_TAB);
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new AntMainTab(), refresh, build, new AntTargetsTab(), classpath,
				new AntPropertiesTab(), jre, env, common };
		setTabs(tabs);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// set default name for script
		IResource resource = DebugUITools.getSelectedResource();
		if (resource != null && resource instanceof IFile file) {
			if (AntUtil.isKnownAntFile(file)) {
				String projectName = file.getProject().getName();
				StringBuilder buffer = new StringBuilder(projectName);
				buffer.append(' ');
				buffer.append(file.getName());
				String name = buffer.toString().trim();
				name = DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(name);
				configuration.rename(name);
				// set the project name so that the correct default VM install can be determined
				configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
				configuration.setAttribute(IExternalToolConstants.ATTR_LOCATION, VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", file.getFullPath().toString())); //$NON-NLS-1$
			}
		}
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
		super.setDefaults(configuration);
	}
}
