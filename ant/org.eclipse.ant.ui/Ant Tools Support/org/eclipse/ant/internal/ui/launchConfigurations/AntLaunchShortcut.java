/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John-Mason P. Shackelford - bug 34548
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.ui.editor.model.AntElementNode;
import org.eclipse.ant.internal.ui.editor.model.AntProjectNode;
import org.eclipse.ant.internal.ui.editor.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.AntUtil;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.internal.ui.model.IAntUIPreferenceConstants;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;


public class AntLaunchShortcut implements ILaunchShortcut {

	private boolean fShowDialog= false;
	private static final int MAX_TARGET_APPEND_LENGTH = 30;

	/**
	 * Constructor for AntLaunchShortcut.
	 */
	public AntLaunchShortcut() {
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			Object object = structuredSelection.getFirstElement();
			if (object instanceof IAdaptable) {
				IResource resource = (IResource)((IAdaptable)object).getAdapter(IResource.class);
				if (resource != null) {
					launch(resource, mode);
					return;
				} else if (object instanceof AntElementNode){
					launch((AntElementNode) object);
					return;
				}
			}
		}
		antFileNotFound();
	}
	
	/**
	 * Launches the given Ant node, which can correspond to an Ant
	 * target or an Ant project node.
	 * 
	 * @param node the Ant node to launch
	 */
	public void launch(AntElementNode node) {
		String selectedTarget= null;
		if (node instanceof AntTargetNode) {
			AntTargetNode targetNode= (AntTargetNode) node;
			if (targetNode.isDefaultTarget()) {
				selectedTarget= ""; //$NON-NLS-1$
			} else {
				selectedTarget= targetNode.getTarget().getName();
			}
		} else if (node instanceof AntProjectNode) {
			selectedTarget = ""; //$NON-NLS-1$
		}
	
		if (selectedTarget == null) {
			return;
		}
		IFile file= node.getBuildFileResource();
		if (file != null) {
			launch(file, ILaunchManager.RUN_MODE, selectedTarget);
			return;
		} 
		//external buildfile
		IPath filePath= getExternalBuildFilePath();
		if (filePath != null) {
			launch(filePath, ILaunchManager.RUN_MODE, selectedTarget);
			return;
		}
		
		antFileNotFound();
	}
	
	private IPath getExternalBuildFilePath() {
		IWorkbenchPage page= AntUIPlugin.getActiveWorkbenchWindow().getActivePage();
		IEditorInput editorInput= page.getActiveEditor().getEditorInput();
		IPath filePath= null;
		if (editorInput instanceof ILocationProvider) {
	    	filePath= ((ILocationProvider)editorInput).getPath(editorInput);
	    }
		return filePath;
	}
	
	/**
	 * Inform the user that an ant file was not found to run.
	 */
	private void antFileNotFound() {
		reportError(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Unable"), null); //$NON-NLS-1$	
	}
	
	/**
	 * Launch the given file in the specified mode.
	 * 
	 * @param resource either a build file (*.xml file) to execute or a resource
	 * from whose location a build file should be searched for. If the given
	 * resource is a file that does not end in ".xml", a search will begin at
	 * the resource's enclosing folder. The given resource must be of type IFile
	 * or IContainer.
	 * @param mode the mode in which the build file should be executed
	 */
	protected void launch(IResource resource, String mode) {
		if (!("xml".equalsIgnoreCase(resource.getFileExtension()))) { //$NON-NLS-1$
			if (resource.getType() == IResource.FILE) {
				resource= resource.getParent();
			}
			resource= findBuildFile((IContainer)resource);
		} 
		if (resource != null) {
			launch((IFile)resource, mode, null);
		} else {
		 	antFileNotFound();
		 }
	}
	
	/**
	 * Launch the given targets in the given build file. The targets are
	 * launched in the given mode.
	 * 
	 * @param file the build file to launch
	 * @param mode the mode in which the build file should be executed
	 * @param targetAttribute the targets to launch, in the form of the launch
	 * configuration targets attribute.
	 */
	public void launch(IFile file, String mode, String targetAttribute) {
		ILaunchConfiguration configuration= null;
		if (verifyMode(mode)) {
			List configurations = findExistingLaunchConfigurations(file);
			if (configurations.isEmpty()) {
				configuration = createDefaultLaunchConfiguration(file);
			} else {
				if (configurations.size() == 1) {
					configuration= (ILaunchConfiguration)configurations.get(0);
				} else {
					configuration= chooseConfig(configurations);
					if (configuration == null) {
						// User cancelled selection
						return;
					}
				}
			}
		}

		if (configuration == null) {
			antFileNotFound();
		}
		// ensure that the targets are selected in the launch configuration
		try {
			if (targetAttribute != null && ! targetAttribute.equals(configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, ""))) { //$NON-NLS-1$
				String projectName= null;
				try {
					projectName= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
				} catch (CoreException e) {
				}
				String newName= getNewLaunchConfigurationName(file.getFullPath(), projectName, targetAttribute);        
				configuration= configuration.copy(newName);
				((ILaunchConfigurationWorkingCopy) configuration).setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, targetAttribute);
				if (fShowDialog) {
					configuration= ((ILaunchConfigurationWorkingCopy) configuration).doSave();
				}
			}
		} catch (CoreException exception) {
			reportError(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Exception_launching"), new String[] {file.getName()}), exception); //$NON-NLS-1$
			return;
		}
		if (fShowDialog) {
			// Offer to save dirty editors before opening the dialog as the contents
			// of an Ant editor often affect the contents of the dialog.
			if (!DebugUITools.saveBeforeLaunch()) {
				return;
			}
			IStatus status = new Status(IStatus.INFO, IAntUIConstants.PLUGIN_ID, IAntUIConstants.STATUS_INIT_RUN_ANT, "", null); //$NON-NLS-1$
			DebugUITools.openLaunchConfigurationDialog(AntUIPlugin.getActiveWorkbenchWindow().getShell(), configuration, IExternalToolConstants.ID_EXTERNAL_TOOLS_LAUNCH_GROUP, status);
		} else {
			DebugUITools.launch(configuration, mode);
		}
	}
	
	/**
	 * Returns a unique name for a copy of the given launch configuration with the given
	 * targets. The name seed is the same as the name for a new launch configuration with
	 *   " [targetList]" appended to the end.
	 * @param config
	 * @param targetAttribute
	 * @return
	 */
	public static String getNewLaunchConfigurationName(IPath filePath, String projectName, String targetAttribute) {
		StringBuffer buffer = new StringBuffer();
		if (projectName != null) {
			buffer.append(projectName);
			buffer.append(' ');
			buffer.append(filePath.lastSegment());
		} else {
			buffer.append(filePath.toOSString());
		}
		
		if (targetAttribute != null) {
			buffer.append(" ["); //$NON-NLS-1$
			if (targetAttribute.length() > MAX_TARGET_APPEND_LENGTH + 3) {
				// The target attribute can potentially be a long, comma-separated list
				// of target. Make sure the generated name isn't extremely long.
				buffer.append(targetAttribute.substring(0, MAX_TARGET_APPEND_LENGTH));
				buffer.append("..."); //$NON-NLS-1$
			} else {
				buffer.append(targetAttribute);
			}
			buffer.append(']');
		}
		
		String name= DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(buffer.toString());
		return name.toString();
	}
	
	/**
	 * Launch the given targets in the given build file. The targets are
	 * launched in the given mode.
	 * 
	 * @param filePath the path to the build file to launch
	 * @param mode the mode in which the build file should be executed
	 * @param targetAttribute the targets to launch, in the form of the launch
	 * configuration targets attribute.
	 */
	public void launch(IPath filePath, String mode, String targetAttribute) {
		ILaunchConfiguration configuration= null;
		if (verifyMode(mode)) {
			List configurations = findExistingLaunchConfigurations(filePath);
			if (configurations.isEmpty()) {
				configuration = createDefaultLaunchConfiguration(filePath, null);
			} else {
				if (configurations.size() == 1) {
					configuration= (ILaunchConfiguration)configurations.get(0);
				} else {
					configuration= chooseConfig(configurations);
					if (configuration == null) {
						// User cancelled selection
						return;
					}
				}
			}
		}

		if (configuration == null) {
			antFileNotFound();
		}
		// ensure that the targets are selected in the launch configuration
		try {
			if (targetAttribute != null && ! targetAttribute.equals(configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, ""))) { //$NON-NLS-1$
				String projectName= null;
				try {
					projectName= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
				} catch (CoreException e) {
				}
				String newName= getNewLaunchConfigurationName(filePath, projectName, targetAttribute);		        
				configuration= configuration.copy(newName);
				((ILaunchConfigurationWorkingCopy) configuration).setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, targetAttribute);
				if (fShowDialog) {
					configuration= ((ILaunchConfigurationWorkingCopy) configuration).doSave();
				}
			}
		} catch (CoreException exception) {
			reportError(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Exception_launching"), new String[] {filePath.toFile().getName()}), exception); //$NON-NLS-1$
			return;
		}
		if (fShowDialog) {
			// Offer to save dirty editors before opening the dialog as the contents
			// of an Ant editor often affect the contents of the dialog.
			if (!DebugUITools.saveBeforeLaunch()) {
				return;
			}
			IStatus status = new Status(IStatus.INFO, IAntUIConstants.PLUGIN_ID, IAntUIConstants.STATUS_INIT_RUN_ANT, "", null); //$NON-NLS-1$
			DebugUITools.openLaunchConfigurationDialog(AntUIPlugin.getActiveWorkbenchWindow().getShell(), configuration, IExternalToolConstants.ID_EXTERNAL_TOOLS_LAUNCH_GROUP, status);
		} else {
			DebugUITools.launch(configuration, mode);
		}
	}
	
	/**
	 * Walks the file hierarchy looking for a build file.
	 * Returns the first build file found that matches the 
	 * search criteria.
	 */
	private IFile findBuildFile(IContainer parent) {
		String[] names= getBuildFileNames();
		if (names == null) {
			return null;
		}
		IResource file= null;
		while (file == null || file.getType() != IResource.FILE) {		
			for (int i = 0; i < names.length; i++) {
				String string = names[i];
				file= parent.findMember(string);
				if (file != null && file.getType() == IResource.FILE) {
					break;
				}
			}
			parent = parent.getParent();
			if (parent == null) {
				return null;
			}
		}
		return (IFile)file;
	}
	
	private String[] getBuildFileNames() {
		IPreferenceStore prefs= AntUIPlugin.getDefault().getPreferenceStore();
		String buildFileNames= prefs.getString(IAntUIPreferenceConstants.ANT_FIND_BUILD_FILE_NAMES);
		if (buildFileNames.length() == 0) {
			//the user has not specified any names to look for
			return null;
		}
		return AntUtil.parseString(buildFileNames, ","); //$NON-NLS-1$
	}
	
	/**
	 * Creates and returns a default launch configuration for the given file.
	 * 
	 * @param file
	 * @return default launch configuration
	 */
	public static ILaunchConfiguration createDefaultLaunchConfiguration(IFile file) {
		return createDefaultLaunchConfiguration(file.getFullPath(), file.getProject());
	}

	/**
	 * Creates and returns a default launch configuration for the given file path
	 * and project.
	 * 
	 * @param filePath the path to the buildfile
	 * @param project the project containing the buildfile or <code>null</code> if the
	 * buildfile is not contained in a project (is external).
	 * @return default launch configuration or <code>null</code> if one could not be created
	 */
	public static ILaunchConfiguration createDefaultLaunchConfiguration(IPath filePath, IProject project) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
				
		String projectName= project != null ? project.getName() : null;
		String name = getNewLaunchConfigurationName(filePath, projectName, null);
		try {
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
			if (project != null) {
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION,
						VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", filePath.toString())); //$NON-NLS-1$
			} else {
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, filePath.toString());
			}
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
			// set default for common settings
			CommonTab tab = new CommonTab();
			tab.setDefaults(workingCopy);
			tab.dispose();
			
			//set the project name so that the correct default VM install can be determined
			if (project != null) {
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			}
			AntJRETab jreTab = new AntJRETab();
			jreTab.setDefaults(workingCopy);
			jreTab.dispose();
			
			return workingCopy.doSave();
		} catch (CoreException e) {
			reportError(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.2"), new String[]{filePath.toString()}), e); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Returns a list of existing launch configuration for the given file.
	 * 
	 * @param file the buildfile resource
	 * @return list of launch configurations
	 */
	public static List findExistingLaunchConfigurations(IFile file) {
		IPath filePath = file.getLocation();
		return findExistingLaunchConfigurations(filePath);
	}
	
	/**
	 * Returns a list of existing launch configuration for the given file.
	 * 
	 * @param filePath the fully qualified path for the buildfile
	 * @return list of launch configurations
	 */
	public static List findExistingLaunchConfigurations(IPath filePath) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		List validConfigs= new ArrayList();
		if (type != null) {
			ILaunchConfiguration[] configs = null;
			try {
				configs = manager.getLaunchConfigurations(type);
			} catch (CoreException e) {
				reportError(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.3"), e); //$NON-NLS-1$
			}
			if (configs != null && configs.length > 0) {
				if (filePath == null) {
					reportError(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.0"), null); //$NON-NLS-1$
				} else {
					for (int i = 0; i < configs.length; i++) {
						ILaunchConfiguration configuration = configs[i];
						IPath location;
						try {
							location = ExternalToolsUtil.getLocation(configuration);
							if (filePath.equals(location)) {
								validConfigs.add(configuration);
							}
						} catch (CoreException e) {
							// error occurred in variable expand - ignore
						}
					}
				}
			}
		}
		return validConfigs;
	}
	
	/**
	 * Prompts the user to choose from the list of given launch configurations
	 * and returns the config the user choose or <code>null</code> if the user
	 * pressed Cancel or if the given list is empty.
	 */
	public static ILaunchConfiguration chooseConfig(List configs) {
		if (configs.isEmpty()) {
			return null;
		}
		ILabelProvider labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);
		dialog.setElements(configs.toArray(new ILaunchConfiguration[configs.size()]));
		dialog.setTitle(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.4")); //$NON-NLS-1$
		dialog.setMessage(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.5")); //$NON-NLS-1$
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;
	}
	
	/**
	 * Verifies the mode is supported
	 * 
	 * @param mode
	 * @return boolean
	 */
	protected boolean verifyMode(String mode) {
		if (!mode.equals(ILaunchManager.RUN_MODE)) {
			reportError(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.6"), null); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IFile file = (IFile)input.getAdapter(IFile.class);
		if (file != null) {
			launch(file, mode);
			return;
		}
		if (input instanceof ILocationProvider) {
			IPath filePath= ((ILocationProvider)input).getPath(input);
			if ("xml".equals(filePath.getFileExtension())) { //$NON-NLS-1$
				launch(filePath, mode, null);
				return;
			}
		}
	
		antFileNotFound();
	}
	
	protected static void reportError(String message, Throwable throwable) {
		IStatus status = null;
		if (throwable instanceof CoreException) {
			status = ((CoreException)throwable).getStatus();
		} else {
			status = new Status(IStatus.ERROR, IAntUIConstants.PLUGIN_ID, 0, message, throwable);
		}
		ErrorDialog.openError(AntUIPlugin.getActiveWorkbenchWindow().getShell(), AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Error_7"), AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Build_Failed_2"), status); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Sets whether to show the external tools launch configuration dialog
	 * 
	 * @param showDialog If true the launch configuration dialog will always be
	 * 			shown
	 */
	public void setShowDialog(boolean showDialog) {
		fShowDialog = showDialog;
	}
}