/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;


import java.io.File;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

public class ProjectCreationDecorator extends AbstractAntUITest {
	
	public ProjectCreationDecorator(String name) {
		super(name);
	}
	
	public void testProjectCreation() throws Exception {
		try {
			// delete any pre-existing project
			IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(ProjectHelper.PROJECT_NAME);
			if (pro.exists()) {
				pro.delete(true, true, null);
			}
			// create project and import buildfiles and support files
			IProject project = ProjectHelper.createProject(ProjectHelper.PROJECT_NAME);
			IFolder folder = ProjectHelper.addFolder(project, "buildfiles");
			ProjectHelper.addFolder(project, "launchConfigurations");
			File root = AntUITestPlugin.getDefault().getFileInPlugin(ProjectHelper.TEST_BUILDFILES_DIR);
			ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);
			
			createLaunchConfigurationForBoth("echoing");
			createLaunchConfigurationForBoth("102282");
			createLaunchConfigurationForBoth("74840");
            createLaunchConfigurationForBoth("failingTarget");
			createLaunchConfiguration("build");
			createLaunchConfiguration("bad");
			createLaunchConfiguration("importRequiringUserProp");
            createLaunchConfigurationForSeparateVM("echoPropertiesSepVM", "echoProperties");
			createLaunchConfigurationForSeparateVM("extensionPointSepVM", null);
			createLaunchConfigurationForSeparateVM("extensionPointTaskSepVM", null);
			createLaunchConfigurationForSeparateVM("extensionPointTypeSepVM", null);
			createLaunchConfigurationForSeparateVM("input", null);
			createLaunchConfigurationForSeparateVM("environmentVar", null);
            
            createLaunchConfigurationForBoth("breakpoints");
            createLaunchConfigurationForBoth("debugAntCall");
            createLaunchConfigurationForBoth("96022");
            createLaunchConfigurationForBoth("macrodef");
            createLaunchConfigurationForBoth("85769");
			
			createLaunchConfiguration("big", ProjectHelper.PROJECT_NAME + "/buildfiles/performance/build.xml");
			
		} finally {
			//do not show the Ant build failed error dialog
			AntUIPlugin.getDefault().getPreferenceStore().setValue(IAntUIPreferenceConstants.ANT_ERROR_DIALOG, false);
		}
	}
	
	private void createLaunchConfigurationForBoth(String launchConfigName) throws Exception {
		createLaunchConfiguration(launchConfigName);
		createLaunchConfigurationForSeparateVM(launchConfigName + "SepVM", launchConfigName);
		
	}

	/**
	 * Creates a shared launch configuration for launching Ant in a separate VM with the given
	 * name.
	 */
	protected void createLaunchConfigurationForSeparateVM(String launchConfigName, String buildFileName) throws Exception {
		ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType(IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		ILaunchConfigurationWorkingCopy config = type.newInstance(getJavaProject().getProject().getFolder("launchConfigurations"), launchConfigName);
		
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "org.eclipse.ant.internal.ui.antsupport.InternalAntRunner"); //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, getJavaProject().getElementName());
		if (buildFileName == null) {
			buildFileName= launchConfigName;
		} 
		config.setAttribute(IExternalToolConstants.ATTR_LOCATION, "${workspace_loc:/" + ProjectHelper.PROJECT_NAME + "/buildfiles/" + buildFileName + ".xml}");
		
		config.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
		config.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, IAntUIConstants.REMOTE_ANT_PROCESS_FACTORY_ID);
		 
		setVM(config);
				
		config.doSave();
	}
	
	/**
	 * Creates a shared launch configuration for launching Ant in a separate VM with the given
	 * name.
	 */
	protected void createLaunchConfiguration(String launchConfigName) throws Exception {
	    createLaunchConfiguration(launchConfigName, ProjectHelper.PROJECT_NAME + "/buildfiles/" + launchConfigName + ".xml");
	}
	
	public static ILaunchConfiguration createLaunchConfiguration(String launchConfigName, String path) throws CoreException {
	    ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType(IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		ILaunchConfigurationWorkingCopy config = type.newInstance(getJavaProject().getProject().getFolder("launchConfigurations"), launchConfigName);
	
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, getJavaProject().getElementName());
		config.setAttribute(IExternalToolConstants.ATTR_LOCATION, "${workspace_loc:/" + path + "}");
		config.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
			
		config.doSave();
		return config;
	}

	private void setVM(ILaunchConfigurationWorkingCopy config) {
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		String vmName= vm.getName();
		String vmTypeID= vm.getVMInstallType().getId();			
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, vmName);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmTypeID);
	}
}
