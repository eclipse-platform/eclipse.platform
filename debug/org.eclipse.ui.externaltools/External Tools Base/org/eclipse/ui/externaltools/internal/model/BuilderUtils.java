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
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.model;

import java.util.Map;

import org.eclipse.core.externaltools.internal.model.BuilderCoreUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Utility methods for working with external tool project builders.
 */
public class BuilderUtils {

	// Extension point constants.
	private static final String TAG_CONFIGURATION_MAP = "configurationMap"; //$NON-NLS-1$
	private static final String TAG_SOURCE_TYPE = "sourceType"; //$NON-NLS-1$
	private static final String TAG_BUILDER_TYPE = "builderType"; //$NON-NLS-1$

	/**
	 * Returns a launch configuration from the given ICommand arguments. If the
	 * given arguments are from an old-style external tool, an unsaved working
	 * copy will be created from the arguments and returned.
	 *
	 * @param commandArgs the builder ICommand arguments
	 * @return a launch configuration, a launch configuration working copy, or
	 * <code>null</code> if not possible.
	 */
	public static ILaunchConfiguration configFromBuildCommandArgs(IProject project, Map<String, String> commandArgs, String[] version) {
		return BuilderCoreUtils.configFromBuildCommandArgs(project, commandArgs, version);
	}

	/**
	 * Returns an <code>ICommand</code> from the given launch configuration.
	 *
	 * @param project the project the ICommand is relevant to
	 * @param config the launch configuration to create the command from
	 * @return the new command. <code>null</code> can be returned if problems occur during
	 * the translation.
	 */
	public static ICommand commandFromLaunchConfig(IProject project, ILaunchConfiguration config) {
		ICommand newCommand = null;
		try {
			newCommand = project.getDescription().newCommand();
			newCommand = toBuildCommand(project, config, newCommand);
			configureTriggers(config, newCommand);
		} catch (CoreException exception) {
			Shell shell= ExternalToolsPlugin.getActiveWorkbenchShell();
			if (shell != null) {
				MessageDialog.openError(shell, ExternalToolsModelMessages.BuilderUtils_5, ExternalToolsModelMessages.BuilderUtils_6);
			}
			return null;
		}
		return newCommand;
	}

	public static void configureTriggers(ILaunchConfiguration config, ICommand newCommand) throws CoreException {
		BuilderCoreUtils.configureTriggers(config, newCommand);
	}

	/**
	 * Converts the given config to a build command which is stored in the
	 * given command.
	 *
	 * @return the configured build command
	 */
	public static ICommand toBuildCommand(IProject project, ILaunchConfiguration config, ICommand command) throws CoreException {
		return BuilderCoreUtils.toBuildCommand(project, config, command);
	}

	/**
	 * Returns the type of launch configuration that should be created when
	 * duplicating the given configuration as a project builder. Queries to see
	 * if an extension has been specified to explicitly declare the mapping.
	 */
	public static ILaunchConfigurationType getConfigurationDuplicationType(ILaunchConfiguration config) throws CoreException {
		IExtensionPoint ep= Platform.getExtensionRegistry().getExtensionPoint(ExternalToolsPlugin.PLUGIN_ID, IExternalToolConstants.EXTENSION_POINT_CONFIGURATION_DUPLICATION_MAPS);
		IConfigurationElement[] elements = ep.getConfigurationElements();
		String sourceType= config.getType().getIdentifier();
		String builderType= null;
		for (IConfigurationElement element : elements) {
			if (element.getName().equals(TAG_CONFIGURATION_MAP) && sourceType.equals(element.getAttribute(TAG_SOURCE_TYPE))) {
				builderType= element.getAttribute(TAG_BUILDER_TYPE);
				break;
			}
		}
		if (builderType != null) {
			ILaunchConfigurationType type= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(builderType);
			if (type != null) {
				return type;
			}
		}
		return config.getType();
	}

	/**
	 * Returns the folder where project builders should be stored or
	 * <code>null</code> if the folder could not be created
	 */
	public static IFolder getBuilderFolder(IProject project, boolean create) {
		return BuilderCoreUtils.getBuilderFolder(project, create);
	}

	/**
	 * Returns a duplicate of the given configuration. The new configuration
	 * will be of the same type as the given configuration or of the duplication
	 * type registered for the given configuration via the extension point
	 * IExternalToolConstants.EXTENSION_POINT_CONFIGURATION_DUPLICATION_MAPS.
	 */
	public static ILaunchConfiguration duplicateConfiguration(IProject project, ILaunchConfiguration config) throws CoreException {
		Map<String, Object> attributes = config.getAttributes();
		String newName= new StringBuilder(config.getName()).append(ExternalToolsModelMessages.BuilderUtils_7).toString();
		newName= DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(newName);
		ILaunchConfigurationType newType= getConfigurationDuplicationType(config);
		ILaunchConfigurationWorkingCopy newWorkingCopy= newType.newInstance(getBuilderFolder(project, true), newName);
		newWorkingCopy.setAttributes(attributes);
		return newWorkingCopy.doSave();
	}


	/**
	 * Converts the build types string into an array of
	 * build kinds.
	 *
	 * @param buildTypes the string of built types to convert
	 * @return the array of build kinds.
	 */
	public static int[] buildTypesToArray(String buildTypes) {
		return BuilderCoreUtils.buildTypesToArray(buildTypes);
	}
}
