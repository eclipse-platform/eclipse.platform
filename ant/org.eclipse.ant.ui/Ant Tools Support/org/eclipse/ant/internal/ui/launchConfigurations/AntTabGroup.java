/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;


public class AntTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new AntMainTab(),
			new RefreshTab(),
			new AntBuildTab(),
			new AntTargetsTab(),
			new AntClasspathTab(),
			new AntPropertiesTab(),
			new AntJRETab(),
			new AntEnvironmentTab(),
			new CommonTab()
		};
		setTabs(tabs);
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		//set default name for script
		IResource resource = DebugUITools.getSelectedResource();
		if (resource != null && resource instanceof IFile) {
			IFile file = (IFile)resource;
			String extension = file.getFileExtension();
			if (extension != null && extension.equalsIgnoreCase("xml")) { //$NON-NLS-1$
				String projectName= file.getProject().getName();
				StringBuffer buffer = new StringBuffer(projectName);
				buffer.append(' ');
				buffer.append(file.getName());
				String name = buffer.toString().trim();
				name= DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(name);
				configuration.rename(name);
				//set the project name so that the correct default VM install can be determined
				configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
				configuration.setAttribute(IExternalToolConstants.ATTR_LOCATION,
						VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", file.getFullPath().toString())); //$NON-NLS-1$
			}		
		}
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
		super.setDefaults(configuration);
	}	
}
