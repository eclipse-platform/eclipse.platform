/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * Helper methods to set up an IProject.
 */
public class ProjectHelper {
	
	public static final IPath TEST_BUILDFILES_DIR= new Path("testbuildfiles");
	public static final IPath TEST_RESOURCES_DIR= new Path("testresources");	
	public static final IPath TEST_LIB_DIR= new Path("testlib");
	
	public static final String PROJECT_NAME= "Ant UI Tests";
	
	/**
	 * Creates a IProject.
	 */	
	public static IProject createProject(String projectName) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		
		if (!project.isOpen()) {
			project.open(null);
		}
		
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID, null);
		}
		
		return project;
	}
	
	private static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
	/**
	 * Removes an IProject.
	 */		
	public static void delete(IProject project) throws CoreException {
		project.delete(true, true, null);
	}


	/**
	 * Adds a folder to an IProject.
	 */		
	public static IFolder addFolder(IProject project, String containerName) throws CoreException {
		
			IFolder folder= project.getFolder(containerName);
			if (!folder.exists()) {
				folder.create(false, true, null);
			}
		
		return folder;
		
	}
	
	public static void importFilesFromDirectory(File rootDir, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, IOException {		
		IImportStructureProvider structureProvider = FileSystemStructureProvider.INSTANCE;
		List files = new ArrayList(100);
		addFiles(rootDir, files);
		try {
			ImportOperation op= new ImportOperation(destPath, rootDir, structureProvider, new ImportOverwriteQuery(), files);
			op.setCreateContainerStructure(false);
			op.run(monitor);
		} catch (InterruptedException e) {
			// should not happen
		}
	}	
	
	private static void addFiles(File dir, List collection) throws IOException {
		File[] files = dir.listFiles();
		List subDirs = new ArrayList(2);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				collection.add(files[i]);
			} else if (files[i].isDirectory()) {
				subDirs.add(files[i]);
			}
		}
		Iterator iter = subDirs.iterator();
		while (iter.hasNext()) {
			File subDir = (File)iter.next();
			addFiles(subDir, collection);
		}
	}
	
	private static class ImportOverwriteQuery implements IOverwriteQuery {
		public String queryOverwrite(String file) {
			return ALL;
		}	
	}

	/**
	 * Creates two launch configurations one standard one and one for a separate VM
	 * @param launchConfigName
	 * @throws Exception
	 * 
	 * @since 3.5
	 */
	public static void createLaunchConfigurationForBoth(String launchConfigName) throws Exception {
		ProjectHelper.createLaunchConfiguration(launchConfigName);
		ProjectHelper.createLaunchConfigurationForSeparateVM(launchConfigName + "SepVM", launchConfigName);
	}

	/**
	 * Creates a shared launch configuration for launching Ant in a separate VM with the given
	 * name.
	 * 
	 * @since 3.5
	 */
	public static void createLaunchConfigurationForSeparateVM(String launchConfigName, String buildFileName) throws Exception {
		String bf = buildFileName;
		ILaunchConfigurationType type = AbstractAntUITest.getLaunchManager().getLaunchConfigurationType(IAntLaunchConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		ILaunchConfigurationWorkingCopy config = type.newInstance(AbstractAntUITest.getJavaProject().getProject().getFolder("launchConfigurations"), launchConfigName);
		
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "org.eclipse.ant.internal.launching.remote.InternalAntRunner"); //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, AbstractAntUITest.getJavaProject().getElementName());
		if (bf == null) {
			bf= launchConfigName;
		} 
		config.setAttribute(IExternalToolConstants.ATTR_LOCATION, "${workspace_loc:/" + PROJECT_NAME + "/buildfiles/" + bf + ".xml}");
		
		config.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
		config.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, IAntUIConstants.REMOTE_ANT_PROCESS_FACTORY_ID);
		 
		ProjectHelper.setVM(config);
				
		config.doSave();
	}

	/**
	 * Sets the workspace default VM on the given working copy
	 * @param config
	 * 
	 * @since 3.5
	 */
	public static void setVM(ILaunchConfigurationWorkingCopy config) {
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		String vmName= vm.getName();
		String vmTypeID= vm.getVMInstallType().getId();			
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, vmName);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmTypeID);
	}

	/**
	 * Creates a shared launch configuration for launching Ant in a separate VM with the given
	 * name.
	 */
	public static void createLaunchConfiguration(String launchConfigName) throws Exception {
	    ProjectHelper.createLaunchConfiguration(launchConfigName, PROJECT_NAME + "/buildfiles/" + launchConfigName + ".xml");
	}

	/**
	 * Creates a launch configuration with the given name in the given location
	 * @param launchConfigName
	 * @param path
	 * @return the handle to the new launch configuration
	 * @throws CoreException
	 */
	public static ILaunchConfiguration createLaunchConfiguration(String launchConfigName, String path) throws CoreException {
	    ILaunchConfigurationType type = AbstractAntUITest.getLaunchManager().getLaunchConfigurationType(IAntLaunchConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		ILaunchConfigurationWorkingCopy config = type.newInstance(AbstractAntUITest.getJavaProject().getProject().getFolder("launchConfigurations"), launchConfigName);
	
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, AbstractAntUITest.getJavaProject().getElementName());
		config.setAttribute(IExternalToolConstants.ATTR_LOCATION, "${workspace_loc:/" + path + "}");
		config.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
			
		config.doSave();
		return config;
	}			
}
