/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui.externaltools;

import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getLaunchManager;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getProject;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.model.BuilderCoreUtils;
import org.eclipse.core.externaltools.internal.registry.ExternalToolMigration;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.ui.externaltools.internal.model.BuilderUtils;

import junit.framework.Test;

/**
 * Abstract {@link Test} class for external tools
 *
 * @since 3.5.100 org.eclipse.ant.tests.ui
 */
@SuppressWarnings("restriction")
public final class ExternalToolTestUtil {

	static final String EXT_BUILD_FILE_NAME = "ext-builders.xml"; //$NON-NLS-1$

	private ExternalToolTestUtil() {
	}

	/**
	 * Creates a new external tool builder for the given project from the given {@link ILaunchConfiguration}
	 *
	 * @param project
	 *            the parent project
	 * @param name
	 *            the name of the config
	 * @param args
	 *            the argument map to set in the new configuration
	 * @return a new Ant build {@link ILaunchConfiguration} or <code>null</code>
	 */
	public static ILaunchConfiguration createExternalToolBuilder(IProject project, String name,
			Map<String, ? extends Object> args) throws Exception {
		IFolder dir = project.getFolder(BuilderCoreUtils.BUILDER_FOLDER_NAME);
		if (!dir.exists()) {
			dir.create(true, true, null);
		}
		ILaunchConfigurationType type = getLaunchManager()
				.getLaunchConfigurationType(IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE);
		if (type != null) {
			ILaunchConfigurationWorkingCopy config = type.newInstance(dir, name);
			config.setAttributes(args);
			return config.doSave();
		}
		return null;
	}

	/**
	 * Creates a new external tool Ant build configuration that has never been saved
	 */
	public static ILaunchConfigurationWorkingCopy createExternalToolBuilderWorkingCopy(IProject project, String name,
			Map<String, Object> args) throws Exception {
		IFolder dir = project.getFolder(BuilderCoreUtils.BUILDER_FOLDER_NAME);
		if (!dir.exists()) {
			dir.create(true, true, null);
		}
		ILaunchConfigurationType type = getLaunchManager()
				.getLaunchConfigurationType(IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE);
		if (type != null) {
			ILaunchConfigurationWorkingCopy config = type.newInstance(dir, name);
			config.setAttributes(args);
			return config;
		}
		return null;
	}

	/**
	 * Creates a new builder {@link ICommand}
	 *
	 * @return the new builder {@link ICommand}
	 */
	public static ICommand createBuildCommand(ILaunchConfiguration config) throws Exception {
		return BuilderUtils.commandFromLaunchConfig(getProject(), config);
	}

	/**
	 * Returns a map of arguments for an Ant buildfile using Eclipse 2.0 arguments.
	 *
	 * @return a map of 2.0 arguments for an Ant buildfile.
	 */
	public static Map<String, String> get20AntArgumentMap() {
		HashMap<String, String> arguments = new HashMap<>();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.0"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_TYPE, "org.eclipse.ui.externaltools.type.ant"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_NAME, "ant tool"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_LOCATION, "location"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_REFRESH, "refresh scope"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_ARGUMENTS, "arg ${ant_target:target1} ${ant_target:target2}"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_SHOW_LOG, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_BLOCK, "false"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_BUILD_TYPES, "build kinds"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_DIRECTORY, "working dir"); //$NON-NLS-1$
		return arguments;
	}

	/**
	 * Returns a map of arguments for executing a program using Eclipse 2.0 arguments.
	 *
	 * @return a map of 2.0 arguments for a program
	 */
	public static Map<String, String> get20ProgramArgumentMap() {
		HashMap<String, String> arguments = new HashMap<>();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.0"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_TYPE, "org.eclipse.ui.externaltools.type.program"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_NAME, "program tool"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_LOCATION, "location"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_REFRESH, "refresh scope"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_ARGUMENTS, "arg ${ant_target:target1} ${ant_target:target2}"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_SHOW_LOG, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_BLOCK, "false"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_BUILD_TYPES, "build kinds"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TOOL_DIRECTORY, "working dir"); //$NON-NLS-1$
		return arguments;
	}

	/**
	 * Returns a map of arguments for executing an Ant buildfile using Eclipse 2.1 arguments.
	 *
	 * @return a map of 2.1 arguments for an Ant buildfile
	 */
	public static Map<String, String> get21AntArgumentMap() {
		HashMap<String, String> arguments = new HashMap<>();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.1"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_NAME, "ant config"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TYPE, ExternalToolMigration.TOOL_TYPE_ANT_BUILD);
		arguments.put(ExternalToolMigration.TAG_LOCATION, "location"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_WORK_DIR, "working directory"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_CAPTURE_OUTPUT, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_RUN_BKGRND, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_PROMPT_ARGS, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_REFRESH_SCOPE, "refresh scope"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_REFRESH_RECURSIVE, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_RUN_BUILD_KINDS, "build kinds"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_ARGS, "arg1 arg2"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_EXTRA_ATTR, ExternalToolMigration.RUN_TARGETS_ATTRIBUTE + "=target1,target2"); //$NON-NLS-1$
		return arguments;
	}

	/**
	 * Returns a map of arguments for executing a program buildfile using Eclipse 2.1 arguments.
	 *
	 * @return a map of 2.1 arguments for a program
	 */
	public static Map<String, String> get21ProgramArgumentMap() {
		HashMap<String, String> arguments = new HashMap<>();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.1"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_NAME, "program config"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_TYPE, IExternalToolConstants.TOOL_TYPE_PROGRAM);
		arguments.put(ExternalToolMigration.TAG_LOCATION, "location"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_WORK_DIR, "working directory"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_CAPTURE_OUTPUT, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_RUN_BKGRND, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_PROMPT_ARGS, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_REFRESH_SCOPE, "refresh scope"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_REFRESH_RECURSIVE, "true"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_RUN_BUILD_KINDS, "build kinds"); //$NON-NLS-1$
		arguments.put(ExternalToolMigration.TAG_ARGS, "arg1 arg2"); //$NON-NLS-1$
		return arguments;
	}
}
