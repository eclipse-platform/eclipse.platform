/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.core.externaltools.internal.model;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.registry.ExternalToolMigration;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

/**
 * Utility methods for working with external tool project builders.
 */
public class BuilderCoreUtils {

	public static final String LAUNCH_CONFIG_HANDLE = "LaunchConfigHandle"; //$NON-NLS-1$
	/**
	 * Constant added to the build command to determine if we are doing an incremental build after a clean
	 *
	 * @since 3.7
	 */
	public static final String INC_CLEAN = "incclean"; //$NON-NLS-1$

	/**
	 * Constant used to find a builder using the 3.0-interim format
	 */
	public static final String BUILDER_FOLDER_NAME = ".externalToolBuilders"; //$NON-NLS-1$
	/**
	 * Constant used to represent the current project in the 3.0-final format.
	 */
	public static final String PROJECT_TAG = "<project>"; //$NON-NLS-1$

	public static final String VERSION_1_0 = "1.0"; //$NON-NLS-1$
	public static final String VERSION_2_1 = "2.1"; //$NON-NLS-1$
	// The format shipped up to and including Eclipse 3.0 RC1
	public static final String VERSION_3_0_interim = "3.0.interim"; //$NON-NLS-1$
	// The format shipped in Eclipse 3.0 final
	public static final String VERSION_3_0_final = "3.0"; //$NON-NLS-1$

	private static final String BUILD_TYPE_SEPARATOR = ","; //$NON-NLS-1$
	private static final int[] DEFAULT_BUILD_TYPES = new int[] {
			IncrementalProjectBuilder.INCREMENTAL_BUILD,
			IncrementalProjectBuilder.FULL_BUILD };

	/**
	 * Returns a launch configuration from the given ICommand arguments. If the
	 * given arguments are from an old-style external tool, an unsaved working
	 * copy will be created from the arguments and returned.
	 *
	 * @param commandArgs
	 *            the builder ICommand arguments
	 * @return a launch configuration, a launch configuration working copy, or
	 *         <code>null</code> if not possible.
	 */
	public static ILaunchConfiguration configFromBuildCommandArgs(IProject project, Map<String, String> commandArgs, String[] version) {
		String configHandle = commandArgs.get(LAUNCH_CONFIG_HANDLE);
		if (configHandle == null) {
			// Probably an old-style (Eclipse 1.0 or 2.0) external tool. Try to
			// migrate.
			version[0] = VERSION_1_0;
			return ExternalToolMigration.configFromArgumentMap(commandArgs);
		}
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfiguration configuration = null;
		if (configHandle.startsWith(PROJECT_TAG)) {
			version[0] = VERSION_3_0_final;
			IPath path = IPath.fromOSString(configHandle);
			IFile file = project.getFile(path.removeFirstSegments(1));
			if (file.exists()) {
				configuration = manager.getLaunchConfiguration(file);
			}
		} else {
			// Try treating the handle as a file name.
			// This is the format used in 3.0 RC1.
			IPath path = IPath.fromOSString(BUILDER_FOLDER_NAME).append(configHandle);
			IFile file = project.getFile(path);
			if (file.exists()) {
				version[0] = VERSION_3_0_interim;
				configuration = manager.getLaunchConfiguration(file);
			} else {
				try {
					// Treat the configHandle as a memento. This is the format
					// used in Eclipse 2.1.
					configuration = manager
							.getLaunchConfiguration(configHandle);
				} catch (CoreException e) {
				}
				if (configuration != null) {
					version[0] = VERSION_2_1;
				}
			}
		}
		return configuration;
	}

	public static void configureTriggers(ILaunchConfiguration config, ICommand newCommand) throws CoreException {
		newCommand.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
		newCommand.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		newCommand.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		newCommand.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		String buildKinds = config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, (String) null);
		boolean isfull = false, isinc = false;
		for (int trigger : buildTypesToArray(buildKinds)) {
			switch (trigger) {
				case IncrementalProjectBuilder.FULL_BUILD:
					newCommand.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
					isfull = true;
					break;
				case IncrementalProjectBuilder.INCREMENTAL_BUILD:
					newCommand.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
					isinc = true;
					break;
				case IncrementalProjectBuilder.AUTO_BUILD:
					newCommand.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, true);
					break;
				case IncrementalProjectBuilder.CLEAN_BUILD:
					newCommand.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, true);
					break;
				default:
					break;
			}
		}
		if(!isfull && isinc) {
			Map<String, String> args = newCommand.getArguments();
			if(args == null) {
				args = new HashMap<>();
			}
			newCommand.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
			args.put(INC_CLEAN, Boolean.TRUE.toString());
			newCommand.setArguments(args);
		}
		if (!config.getAttribute(IExternalToolConstants.ATTR_TRIGGERS_CONFIGURED, false)) {
			ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
			copy.setAttribute(IExternalToolConstants.ATTR_TRIGGERS_CONFIGURED, true);
			copy.doSave();
		}
	}

	/**
	 * Returns whether the given configuration is an "unmigrated" builder.
	 * Unmigrated builders are external tools that are stored in an old format
	 * but have not been migrated by the user. Old format builders are always
	 * translated into launch config working copies in memory, but they're not
	 * considered "migrated" until the config has been saved and the project
	 * spec updated.
	 *
	 * @param config
	 *            the config to examine
	 * @return whether the given config represents an unmigrated builder
	 */
	public static boolean isUnmigratedConfig(ILaunchConfiguration config) {
		return config.isWorkingCopy()
				&& ((ILaunchConfigurationWorkingCopy) config).getOriginal() == null;
	}

	/**
	 * Converts the given config to a build command which is stored in the given
	 * command.
	 *
	 * @return the configured build command
	 */
	public static ICommand toBuildCommand(IProject project, ILaunchConfiguration config, ICommand command) throws CoreException {
		Map<String, String> args = null;
		if (isUnmigratedConfig(config)) {
			// This config represents an old external tool builder that hasn't
			// been edited. Try to find the old ICommand and reuse the
			// arguments.
			// The goal here is to not change the storage format of old,
			// unedited builders.
			ICommand[] commands = project.getDescription().getBuildSpec();
			for (ICommand projectCommand : commands) {
				String name = ExternalToolMigration
						.getNameFromCommandArgs(projectCommand.getArguments());
				if (name != null && name.equals(config.getName())) {
					args = projectCommand.getArguments();
					break;
				}
			}
		} else {
			ILaunchConfiguration temp = config;
			if (config instanceof ILaunchConfigurationWorkingCopy) {
				ILaunchConfigurationWorkingCopy workingCopy = (ILaunchConfigurationWorkingCopy) config;
				if (workingCopy.getOriginal() != null) {
					temp = workingCopy.getOriginal();
				}
			}
			args = new HashMap<>();
			// Launch configuration builders are stored with a project-relative
			// path
			StringBuilder buffer = new StringBuilder(PROJECT_TAG);
			// Append the project-relative path (workspace path minus first
			// segment)
			buffer.append('/').append(temp.getFile().getFullPath().removeFirstSegments(1));
			args.put(LAUNCH_CONFIG_HANDLE, buffer.toString());
		}
		command.setBuilderName(ExternalToolBuilder.ID);
		command.setArguments(args);
		return command;
	}

	/**
	 * Returns the folder where project builders should be stored or
	 * <code>null</code> if the folder could not be created
	 */
	public static IFolder getBuilderFolder(IProject project, boolean create) {
		if (project == null) { // Bug #428479
			return null;
		}

		IFolder folder = project.getFolder(BUILDER_FOLDER_NAME);
		if (!folder.exists() && create) {
			try {
				folder.create(true, true, new NullProgressMonitor());
			} catch (CoreException e) {
				return null;
			}
		}
		return folder;
	}


	/**
	 * Converts the build types string into an array of build kinds.
	 *
	 * @param buildTypes
	 *            the string of built types to convert
	 * @return the array of build kinds.
	 */
	public static int[] buildTypesToArray(String buildTypes) {
		if (buildTypes == null || buildTypes.length() == 0) {
			return DEFAULT_BUILD_TYPES;
		}

		int count = 0;
		boolean incremental = false;
		boolean full = false;
		boolean auto = false;
		boolean clean = false;

		StringTokenizer tokenizer = new StringTokenizer(buildTypes,
				BUILD_TYPE_SEPARATOR);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (IExternalToolConstants.BUILD_TYPE_INCREMENTAL.equals(token)) {
				if (!incremental) {
					incremental = true;
					count++;
				}
			} else if (IExternalToolConstants.BUILD_TYPE_FULL.equals(token)) {
				if (!full) {
					full = true;
					count++;
				}
			} else if (IExternalToolConstants.BUILD_TYPE_AUTO.equals(token)) {
				if (!auto) {
					auto = true;
					count++;
				}
			} else if (IExternalToolConstants.BUILD_TYPE_CLEAN.equals(token)) {
				if (!clean) {
					clean = true;
					count++;
				}
			}
		}

		int[] results = new int[count];
		count = 0;
		if (incremental) {
			results[count] = IncrementalProjectBuilder.INCREMENTAL_BUILD;
			count++;
		}
		if (full) {
			results[count] = IncrementalProjectBuilder.FULL_BUILD;
			count++;
		}
		if (auto) {
			results[count] = IncrementalProjectBuilder.AUTO_BUILD;
			count++;
		}
		if (clean) {
			results[count] = IncrementalProjectBuilder.CLEAN_BUILD;
			count++;
		}

		return results;
	}
}
